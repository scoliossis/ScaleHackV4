package com.scale.scalehackv4.modules.impl.client;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.player.KeyboardInputEvent;
import com.scale.scalehackv4.events.impl.player.RotationEvent;
import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.MathUtil;
import com.scale.scalehackv4.utils.client.ModuleUtil;
import lombok.AllArgsConstructor;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;

@RegisterModule(
        name = "Fix Rotations",
        category = Category.CLIENT,
        enabledByDefault = true
)
// todo: ig add option to disable showing rotations, but blehhh
//  pitch rotation isnt shown when freelook is enabled
//  maybe allow no fix rotation on interact
public class FixRotationsModule extends Module {
    @RegisterSubModule(name = "Fix Movement")
    public static boolean fixMovement = true;
    @RegisterSubModule(name = "Move Fix Mode", parent = "Fix Movement")
    public MoveFixMode moveFixMode = MoveFixMode.Straight;
    public enum MoveFixMode {
        Vanilla,
        Smooth,
        Straight
    }

    private static float yawDeficit = 0;

    // goes first to allow the inputs to be modified later
    private final Event handleRotations = new KeyboardInputEvent(this, params -> {
        if (moveFixMode == MoveFixMode.Vanilla || !fixMovement) return false;

        MovementDirection currentDirection = MovementDirection.getCurrentDirection(params);
        // make sure ur moving
        if (currentDirection == null) return false;

        MovementDirection fixedMovementDirection = getMovementDirection(currentDirection);

        params.forwardKey = fixedMovementDirection.forward;
        params.backKey = fixedMovementDirection.back;
        params.leftKey = fixedMovementDirection.left;
        params.rightKey = fixedMovementDirection.right;

        return false;
    }, 1);

    private MovementDirection getMovementDirection(MovementDirection currentDirection) {
        float yawDifference = RotationEvent.clientRotation.yaw - RotationEvent.getLastRotation().yaw;

        if (moveFixMode == MoveFixMode.Straight) {
            float yawDeficitAdded = MathUtil.toNearest(yawDifference + yawDeficit, 45) - yawDifference;

            yawDifference += yawDeficitAdded;
            yawDeficit -= yawDeficitAdded;

            // calculate how much yaw precision we have lost.
            yawDeficit += MathUtil.toNearest(yawDifference, 45) - yawDifference;
        }
        else yawDifference += yawDifference < 0 ? -22.5f : 22.5f;

        int yawOrdinal = Math.floorMod((int) (yawDifference / 45), MovementDirection.values().length);

        return MovementDirection.values()[Math.floorMod(currentDirection.ordinal() + yawOrdinal, MovementDirection.values().length)];
    }

    @AllArgsConstructor
    private enum MovementDirection {
        NORTH       (   true,    false,  false,  false  ),
        NORTH_EAST  (   true,    true,   false,  false  ),
        EAST        (   false,   true,   false,  false  ),
        SOUTH_EAST  (   false,   true,   true,   false  ),
        SOUTH       (   false,   false,  true,   false  ),
        SOUTH_WEST  (   false,   false,  true,   true   ),
        WEST        (   false,   false,  false,  true   ),
        NORTH_WEST  (   true,    false,  false,  true   );

        public static MovementDirection getCurrentDirection(KeyboardInputEvent.Params event) {
            boolean forward = event.forwardKey && !event.backKey;
            boolean backward = !event.forwardKey && event.backKey;
            boolean left = event.leftKey && !event.rightKey;
            boolean right = !event.leftKey && event.rightKey;

            for (MovementDirection direction : MovementDirection.values()) {
                if (direction.forward == forward && direction.left == left && direction.right == right && direction.back == backward) return direction;
            }

            return null;
        }

        private final boolean forward, right, back, left;
    }

    public static boolean shouldFixMovement() {
        return fixMovement && ModuleUtil.isEnabled(FixRotationsModule.class);
    }

    private static boolean shouldShowRotation(LivingEntity entity) {
        return ModuleUtil.isEnabled(FixRotationsModule.class) && RotationEvent.clientRotation != null && entity == C.p();
    }

    public static float getPitch(LivingEntity entity, float tickProgress) {
        return shouldShowRotation(entity)
                ? MathHelper.lerpAngleDegrees(tickProgress, RotationEvent.lastPitch, RotationEvent.currentPitch)
                : entity.getPitch(tickProgress);
    }

    @Override
    public void onEnable() {
        yawDeficit = 0;
    }

    @Override
    public void onDisable() {

    }
}

package com.scale.scalehackv4.modules.impl.combat;

import com.scale.scalehackv4.bridge.KeybindingBridge;
import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.client.WorldJoinEvent;
import com.scale.scalehackv4.events.impl.player.KeyboardInputEvent;
import com.scale.scalehackv4.events.impl.player.RotationEvent;
import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.TargetUtil;
import com.scale.scalehackv4.utils.player.PlayerUtil;
import com.scale.scalehackv4.utils.player.RotationUtil;
import com.scale.scalehackv4.utils.player.WorldUtil;
import lombok.AllArgsConstructor;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.function.BiPredicate;
import java.util.function.Function;

@RegisterModule(
        name = "More KB",
        category = Category.COMBAT
)
public class MoreKbModule extends Module {
    private static long lastAttackTick = Integer.MIN_VALUE;
    private static Entity targetEntity = null;

    @RegisterSubModule(name = "Knockback Mode", hasKeybind = true)
    public static Mode mode = Mode.Displace;
    @RegisterSubModule(name = "Displace Angle", parent = "Knockback Mode", modeParentString = "Displace")
    public static DisplaceAngle displaceAngle = DisplaceAngle.Void;
    @RegisterSubModule(name = "Max Void Distance", parent = "Displace Angle", modeParentString = "Void", min = 1, max = 10)
    public static int maxVoidDistance = 6;
    @RegisterSubModule(name = "Delay Between", min = 1, max = 10)
    public static int delay = 4;

    private HashMap<Entity, Integer> attackedEnemies = new HashMap<>();

    @AllArgsConstructor
    public enum Mode {
        Displace(
                (rotationEvent, entity) -> {
                    Double yaw = displaceAngle.getYaw.apply(entity);
                    if (yaw == Double.MIN_VALUE) return false;

                    rotationEvent.rotation.yaw = displaceAngle.getYaw.apply(entity).floatValue();
                    return true;
                },
                (keyboardInputEvent, entity) -> {
                    keyboardInputEvent.reset();
                    keyboardInputEvent.sprintKey = true;
                    keyboardInputEvent.forwardKey = true;

                    return true;
                }
        );
        // todo: W tap/S tap/Crouch/Block mode

        public final BiPredicate<RotationEvent.Params, Entity> handleRotation;
        public final BiPredicate<KeyboardInputEvent.Params, Entity> handleInputs;
    }

    @AllArgsConstructor
    public enum DisplaceAngle {
        Left(entity -> C.p().getYaw() + 270d),
        Right(entity -> C.p().getYaw() + 90d),
        Pull(entity -> C.p().getYaw() + 180d),
        Random(entity -> C.p().getYaw() + (Math.random() * 360) - 180),
        Void(entity -> {
            BlockPos closestVoid = WorldUtil.getClosestVoid(entity.getBlockPos(), maxVoidDistance);
            return closestVoid == null ? Double.MIN_VALUE : RotationUtil.getRotation(RotationUtil.getClientRotation(), entity.getEyePos(), closestVoid.toCenterPos()).yaw;
        });

        public final Function<Entity, Double> getYaw;
    }

    @RegisterSubModule(name = "Setup Ticks", min = 1, max = 3)
    public static int setupTicks = 1;

    private final Event onRotationEvent = new RotationEvent(this, params -> {
        // check if we would've hit an entity this tick, if we dont, we have failed, commit seppuku
        if (removeAttackedEnemies() && targetEntity == null && (targetEntity = getAttackingEntity()) != null) lastAttackTick = WorldUtil.ticks;

        // sets rotation, if it fails to set rotation, then reset
        if (shouldSetup() && !mode.handleRotation.test(params, targetEntity)) resetAttackTick();
        // when we are ready to attack, look back at the target
        if (shouldAttack()) {
            Entity targetEntity = MoreKbModule.targetEntity;
            MoreKbModule.targetEntity = null;

            Vec3d targetRotation = TargetUtil.getTargetRotationPoint(targetEntity, C.p().getAttackRange().maxRange());
            if (targetRotation != null) {
                params.rotation = RotationUtil.getRotation(targetRotation);

                PlayerUtil.onAttack.add(() -> attackedEnemies.put(targetEntity, WorldUtil.ticks));
            }
        }
        // please dont swing while we are trying to morekb. tyvm in advance
        else if (isActive()) {
            unsetAttack();
            return true;
        }

        return false;
    }, 3000); // runs after killaura

    private boolean removeAttackedEnemies() {
        for (Entity entity : attackedEnemies.keySet().toArray(new Entity[0])) {
            if (WorldUtil.ticks - attackedEnemies.get(entity) >= delay) attackedEnemies.remove(entity);
        }

        return true;
    }

    private Entity getAttackingEntity() {
        HitResult crosshairTarget = C.mc.crosshairTarget;

        if (!C.p().isBlocking()
                && (KeybindingBridge.from(C.mc.options.attackKey).bridge$getTimesPressed() > 0 || !PlayerUtil.onAttack.isEmpty())
                && crosshairTarget instanceof EntityHitResult entityHitResult
                && TargetUtil.isValidTarget(entityHitResult.getEntity())
                && !attackedEnemies.containsKey(entityHitResult.getEntity())) {
            return entityHitResult.getEntity();
        }

        return null;
    }

    private final Event forceSprint = new KeyboardInputEvent(this, params -> {
        if (shouldSetup()) mode.handleInputs.test(params, targetEntity);
        // when attacking ur allowed to strafe/jump, but u still gotta sprint
        else if (shouldAttack()) {
            params.backKey = false;
            params.sprintKey = true;
            params.forwardKey = true;
        }

        return false;
    });

    private static int getTicksSinceAttack() {
        return (int) (WorldUtil.ticks - lastAttackTick);
    }
    private static boolean shouldSetup() {
        return getTicksSinceAttack() <= setupTicks && targetEntity != null;
    }
    private static boolean shouldAttack() {
        return getTicksSinceAttack() == setupTicks+1;
    }
    private static boolean isActive() {
        return shouldAttack() || shouldSetup();
    }
    private static void unsetAttack() {
        while (C.mc.options.attackKey.wasPressed());
        PlayerUtil.onAttack.clear();
    }

    private static void resetAttackTick() {
        lastAttackTick = -setupTicks-1;
        targetEntity = null;
    }

    private final Event resetLastAttackTime = new WorldJoinEvent(this, params -> {
        resetAttackTick();

        return false;
    });

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {

    }
}

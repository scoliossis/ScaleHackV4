package com.scale.scalehackv4.modules.impl.combat;

import com.scale.scalehackv4.events.Bus;
import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.player.CheckClickEvent;
import com.scale.scalehackv4.events.impl.player.RotationEvent;
import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.TargetUtil;
import com.scale.scalehackv4.utils.player.RotationUtil;
import com.scale.scalehackv4.utils.player.WorldUtil;
import net.minecraft.entity.Entity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RegisterModule(
        name = "Kill Aura",
        category = Category.COMBAT
)
public class KillAuraModule extends Module {
    @RegisterSubModule(name = "Attack Range", min = 1, max = 6)
    public float reach = 3;
    @RegisterSubModule(name = "Rotation Range", min = 1, max = 6)
    public float rotationRange = 5;
    @RegisterSubModule(name = "Target Selection")
    public TargetSelection targetSelection = TargetSelection.Switch;

    public enum TargetSelection {
        Random {
            @Override
            public void sort(List<Entity> targets) {
                targets.sort(Comparator.comparingDouble(e -> Math.random()));
            }
        },
        Switch {
            @Override
            public void sort(List<Entity> targets) {
                if (switchOrdinal >= targets.size()) switchOrdinal = 0;
                targets.sort(Comparator.comparingInt(e -> e.getUuid().hashCode()));
                targets.sort(Comparator.comparingInt(entity -> {
                    int index = targets.indexOf(entity);
                    return (index - switchOrdinal + targets.size()) % targets.size();
                }));
            }
        },
        Distance {
            @Override
            public void sort(List<Entity> targets) {
                targets.sort(Comparator.comparingDouble(KillAuraModule::getMinDistance));
            }
        },
        Client_FOV {
            @Override
            public void sort(List<Entity> targets) {
                targets.sort(Comparator.comparingDouble(e -> Math.abs(RotationEvent.clientRotation.yaw - RotationUtil.getRotation(e.getEyePos()).yaw)));
            }
        },
        Server_FOV {
            @Override
            public void sort(List<Entity> targets) {
                targets.sort(Comparator.comparingDouble(e -> Math.abs(RotationEvent.lastRotation.yaw - RotationUtil.getRotation(e.getEyePos()).yaw)));
            }
        };

        public abstract void sort(List<Entity> targets);
    }

    private Entity targetEntity = null;
    private static int switchOrdinal = 0;

    private final Event handleRotations = new RotationEvent(this, params -> {
        // dont wanna override other rotations!
        if (Bus.isEventCancelled(RotationEvent.class)) {
            targetEntity = null;
            return false;
        }

        // get a list of all targets in rotation distance and ones within attack range
        List<Entity> targets = TargetUtil.getTargets(rotationRange);
        List<Entity> attackRangeTargets = targets.stream().filter(entity -> getMinDistance(entity) <= reach).collect(Collectors.toList());

        // ignore all out of swing distance if there are some in attack range
        if (!attackRangeTargets.isEmpty()) targets = attackRangeTargets;

        targetSelection.sort(targets);

        if (getValidEntity(targets, params)) return true;

        targetEntity = null;
        return false;
    }, 2000);

    private final Event handleAttack = new CheckClickEvent.Left(this, params -> {
        if (!isHoveringTargetEntity()) return false;

        C.im().attackEntity(C.p(), targetEntity);
        C.p().swingHand(Hand.MAIN_HAND);
        switchOrdinal++;

        return true;
    });

    private boolean isHoveringTargetEntity() {
        return targetEntity != null
                && WorldUtil.getMouseOver(RotationEvent.lastRotation, reach) instanceof EntityHitResult entityHitResult
                && entityHitResult.getEntity() == targetEntity;
    }

    private boolean getValidEntity(List<Entity> targets, RotationEvent.Params params) {
        // loop entities until we find one that is hittable
        while (!targets.isEmpty()) {
            Entity target = targets.getFirst();

            Vec3d rotationVector = TargetUtil.getTargetRotationPoint(targets.getFirst(), rotationRange);
            if (rotationVector == null) {
                targets.remove(target);
                continue;
            }

            params.rotation = RotationUtil.getRotation(rotationVector);
            targetEntity = targets.getFirst();
            return true;
        }

        return false;
    }

    // net.minecraft.entity.player.PlayerEntity.canInteractWithEntityIn
    private static double getMinDistance(Entity entity) {
        return Math.sqrt(entity.getBoundingBox().squaredMagnitude(C.p().getEyePos()));
    }

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {

    }
}

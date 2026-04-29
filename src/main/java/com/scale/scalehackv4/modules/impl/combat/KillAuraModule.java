package com.scale.scalehackv4.modules.impl.combat;

import com.scale.scalehackv4.events.Bus;
import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.player.CheckClickEvent;
import com.scale.scalehackv4.events.impl.player.RotationEvent;
import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.TargetUtil;
import com.scale.scalehackv4.utils.player.RotationUtil;
import net.minecraft.entity.Entity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.List;

@RegisterModule(
        name = "Kill Aura",
        category = Category.COMBAT
)
public class KillAuraModule extends Module {
    public float reach = 3;

    private Entity targetEntity = null;

    private final Event handleRotations = new RotationEvent(this, params -> {
        // dont wanna override other rotations!
        if (Bus.isEventCancelled(RotationEvent.class)) {
            targetEntity = null;
            return false;
        }

        List<Entity> targets = TargetUtil.getTargets(reach);
        targets.sort(Comparator.comparingDouble(KillAuraModule::getMinDistance));

        if (targets.isEmpty() || getMinDistance(targets.getFirst()) > reach) {
            targetEntity = null;
            return false;
        }

        // loop entities until we find one that is hittable
        while (!targets.isEmpty()) {
            Entity target = targets.getFirst();

            Vec3d rotationVector = TargetUtil.getTargetRotationPoint(targets.getFirst());
            if (rotationVector == null) {
                targets.remove(target);
                continue;
            }

            params.rotation = RotationUtil.getRotation(rotationVector);
            targetEntity = targets.getFirst();
            return true;
        }

        targetEntity = null;
        return false;
    }, 2000);

    private final Event handleAttack = new CheckClickEvent.Left(this, params -> {
        if (targetEntity == null) return false;

        C.im().attackEntity(C.p(), targetEntity);
        C.p().swingHand(Hand.MAIN_HAND);

        return true;
    });

    private static double getMinDistance(Entity entity) {
        return Math.sin(entity.getBoundingBox().squaredMagnitude(C.p().getEyePos()));
    }

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {

    }
}

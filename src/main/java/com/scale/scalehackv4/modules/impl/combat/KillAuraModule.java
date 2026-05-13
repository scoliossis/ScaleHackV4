package com.scale.scalehackv4.modules.impl.combat;

import com.scale.scalehackv4.events.Bus;
import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.player.RotationEvent;
import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.*;
import com.scale.scalehackv4.utils.player.MovementUtil;
import com.scale.scalehackv4.utils.player.PlayerUtil;
import com.scale.scalehackv4.utils.player.RotationUtil;
import com.scale.scalehackv4.utils.player.WorldUtil;
import com.scale.scalehackv4.utils.render.MouseUtil;
import lombok.AllArgsConstructor;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
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
    @RegisterSubModule(name = "Aura FOV", min = 1, max = 180)
    public int auraFOV = 180;
    @RegisterSubModule(name = "Sticky Targets")
    public boolean stickyTargets = false;
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
                targets.sort(Comparator.comparingDouble(e -> Math.abs(RotationEvent.getLastRotation().yaw - RotationUtil.getRotation(e.getEyePos()).yaw)));
            }
        };

        public abstract void sort(List<Entity> targets);
    }

    @RegisterSubModule(name = "Rotation Mode")
    public RotationMode rotationMode = RotationMode.Simple;

    public enum RotationMode {
        Simple {
            @Override
            public void rotate(Entity target, Vec3d rotationVector, RotationEvent.Params params) {
                params.rotation = RotationUtil.getRotation(rotationVector);
            }
        };

        public abstract void rotate(Entity target, Vec3d rotationVector, RotationEvent.Params params);
    }

    @RegisterSubModule(name = "No Rotate Jump")
    public boolean noRotateJump = true;

    @RegisterSubModule(name = "Attack Speed")
    public AttackSpeed attackSpeed = AttackSpeed.CPS;

    private static double lostPrecision = 0;
    private static int nextAttackTick = 0;

    @AllArgsConstructor
    public enum AttackSpeed {
        _1DOT20(e -> C.p().getAttackCooldownProgress(0) >= swingPercentage, () -> {}),
        // generic cps from https://github.com/scoliossis/ScaleHackV3/blob/master/src/main/java/com/github/scoliossis/modules/impl/combat/KillAura.java#L326
        CPS(e -> WorldUtil.ticks >= nextAttackTick, () -> {
            int nextCps = (int) MathUtil.getRandomInRange(Math.min(minCPS, maxCPS), Math.max(minCPS, maxCPS));
            double ticksTillNextAttack = 20d / (Math.max(nextCps, 1)) + lostPrecision;
            lostPrecision += ticksTillNextAttack - Math.floor(ticksTillNextAttack);
            lostPrecision = lostPrecision - Math.floor(lostPrecision);
            ticksTillNextAttack = Math.floor(ticksTillNextAttack);

            nextAttackTick = (int) (WorldUtil.ticks + ticksTillNextAttack);
        });

        public final Predicate<?> shouldAttack;
        public final Runnable handleAttack;
    }
    @RegisterSubModule(name = "Min CPS", max = 20, parent = "Attack Speed", modeParentString = "CPS", increment = 0.1)
    public static float minCPS = 20;
    @RegisterSubModule(name = "Max CPS", max = 20, parent = "Attack Speed", modeParentString = "CPS", increment = 0.1)
    public static float maxCPS = 20;

    @RegisterSubModule(name = "Swing Percentage", parent = "Attack Speed", modeParentString = "_1DOT20", increment = 0.05)
    public static float swingPercentage = 0.9f;

    @RegisterSubModule(name = "Swing Misses")
    public boolean swingMisses = false;

    @RegisterSubModule(name = "Requirements")
    public Requirements requirements = Requirements.Holding_Weapon;
    public enum Requirements implements MultiSelectEnum {
        Holding_Weapon,
        Left_Click_Down;
    }

    private Entity targetEntity = null;
    private static int switchOrdinal = 0;

    private final Event handleRotations = new RotationEvent(this, params -> {
        // dont wanna override other rotations!
        if (Bus.isEventCancelled(RotationEvent.class) || !shouldAura()) {
            targetEntity = null;
            return false;
        }
        if (noRotateJump && MovementUtil.willJump() || !getValidEntity(getTargets(), params)) return false;

        if ((!swingMisses && !isHoveringTargetEntity()) || !attackSpeed.shouldAttack.test(null)) return true;

        // queue an attack
        PlayerUtil.onAttack.add(() -> {
            attackSpeed.handleAttack.run();
            switchOrdinal++;
        });

        return true;
    }, 2000);


    private boolean isHoveringTargetEntity() {
        return targetEntity != null
                && WorldUtil.getMouseOver(RotationEvent.getLastRotation(), reach) instanceof EntityHitResult entityHitResult
                && entityHitResult.getEntity() == targetEntity;
    }

    private List<Entity> getTargets() {
        // get a list of all targets in rotation distance and ones within attack range
        List<Entity> targets = TargetUtil.getTargets(rotationRange);
        targets.removeIf(entity -> getYawToTarget(entity) > auraFOV);

        List<Entity> attackRangeTargets = targets.stream().filter(entity -> getMinDistance(entity) <= reach).collect(Collectors.toList());

        // ignore all out of swing distance if there are some in attack range
        if (!attackRangeTargets.isEmpty()) targets = attackRangeTargets;

        targetSelection.sort(targets);

        return targets;
    }

    private float getYawToTarget(Entity target) {
        return Math.abs(RotationUtil.getRotation(target.getEyePos()).yaw - RotationEvent.clientRotation.yaw);
    }

    private boolean getValidEntity(List<Entity> targets, RotationEvent.Params params) {
        if (stickyTargets && targets.contains(targetEntity) && rotateToTarget(targetEntity, params)) return true;

        // loop entities until we find one that is hittable
        while (!targets.isEmpty()) {
            Entity target = targets.removeFirst();

            if (rotateToTarget(target, params)) return true;
        }

        targetEntity = null;

        return false;
    }

    private boolean rotateToTarget(Entity target, RotationEvent.Params params) {
        Vec3d rotationVector = TargetUtil.getTargetRotationPoint(target, rotationRange);
        if (rotationVector == null) return false;

        rotationMode.rotate(target, rotationVector, params);
        targetEntity = target;
        return true;
    }

    // net.minecraft.entity.player.PlayerEntity.canInteractWithEntityIn
    private static double getMinDistance(Entity entity) {
        return Math.sqrt(entity.getBoundingBox().squaredMagnitude(C.p().getEyePos()));
    }

    private boolean shouldAura() {
        return (!MultiSelectUtil.isEnabled(Requirements.Holding_Weapon) || InventoryUtil.isItemWeapon(C.p().getMainHandStack()))
                && (!MultiSelectUtil.isEnabled(Requirements.Left_Click_Down) || MouseUtil.isButtonDown(0))
                && (C.mc.currentScreen == null);
    }

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {

    }
}

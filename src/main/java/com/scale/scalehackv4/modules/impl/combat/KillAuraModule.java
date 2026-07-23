package com.scale.scalehackv4.modules.impl.combat;

import com.scale.scalehackv4.events.Bus;
import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.player.RotationEvent;
import com.scale.scalehackv4.events.impl.player.UpdateMouseEvent;
import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.modules.impl.client.Targets;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.*;
import com.scale.scalehackv4.utils.player.*;
import com.scale.scalehackv4.utils.render.MouseUtil;
import lombok.AllArgsConstructor;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.MathHelper;
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
    public static float reach = 3;
    @RegisterSubModule(name = "Rotation Range", min = 1, max = 6)
    public float rotationRange = 5;
    @RegisterSubModule(name = "Aura FOV", min = 1, max = 180)
    public int auraFOV = 180;
    @RegisterSubModule(name = "Only Necessary Rotations")
    public boolean onlyNecessary = false;
    @RegisterSubModule(name = "Random Rotation Point")
    public boolean randomRotationPoint = false;
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

    @RegisterSubModule(name = "Sticky Targets", parent = "Target Selection", NOTmodeParentString = "Switch")
    public boolean stickyTargets = false;

    // todo: separate yaw and pitch, ig...
    @RegisterSubModule(name = "Rotation Mode")
    public RotationMode rotationMode = RotationMode.Lock;
    @RegisterSubModule(name = "Easing Mode", parent = "Rotation Mode", modeParentString = "Eased")
    public static EasingUtil.EasingFunctions easingMode = EasingUtil.EasingFunctions.Ease_In_Back;

    @RegisterSubModule(name = "Easing Ticks", min = 2, max = 10, parent = "Rotation Mode", modeParentString = "Eased")
    public static int easingCycle = 10;
    @RegisterSubModule(name = "Min Rotation", min = 1, max = 90, parent = "Rotation Mode", modeParentString = "Limited")
    public static int minRotation = 30;
    @RegisterSubModule(name = "Max Rotation", min = 1, max = 90, parent = "Rotation Mode", modeParentString = "Limited")
    public static int maxRotation = 60;
    @RegisterSubModule(name = "Rotation Smoothing Factor", min = 2, max = 10, parent = "Rotation Mode", modeParentString = "Smooth", increment = 0.1)
    public static float rotationSmoothing = 3;

    @RegisterSubModule(name = "Client Side Rotation")
    public static boolean clientSideRotation = false;

    public enum RotationMode {
        Lock {
            @Override
            public void rotate(Entity target, Vec3d rotationVector, RotationEvent.Params params) {
                params.rotation = RotationUtil.getRotation(rotationVector);
            }
        },
        Snap {
            @Override
            public void rotate(Entity target, Vec3d rotationVector, RotationEvent.Params params) {
                Rotation rotation = RotationUtil.getRotation(rotationVector);
                if (willAttack(rotation)) params.rotation = rotation;
            }
        },
        Limited {
            @Override
            public void rotate(Entity target, Vec3d rotationVector, RotationEvent.Params params) {
                float maxTurnAmount = (float) (minRotation + (Math.random() * (maxRotation - minRotation)));
                params.rotation = RotationUtil.getLimitedRotation(getLastRotation(), rotationVector, maxTurnAmount);
            }
        },
        Smooth {
            @Override
            public void rotate(Entity target, Vec3d rotationVector, RotationEvent.Params params) {
                params.rotation = RotationUtil.getSmoothRotation(getLastRotation(), rotationVector, rotationSmoothing);
            }
        },
        Eased {
            @Override
            public void rotate(Entity target, Vec3d rotationVector, RotationEvent.Params params) {
                params.rotation = RotationUtil.getEasedRotation(startEasingRotation, rotationVector, easingMode, (double) easingTicks / easingCycle);
            }
        };

        public abstract void rotate(Entity target, Vec3d rotationVector, RotationEvent.Params params);
    }

    @RegisterSubModule(name = "No Rotate Jump")
    public boolean noRotateJump = true;

    @RegisterSubModule(name = "Attack Speed")
    public static AttackSpeed attackSpeed = AttackSpeed.CPS;

    private static double lostPrecision = 0;
    private static int nextAttackTick = 0;

    @AllArgsConstructor
    public enum AttackSpeed {
        _1DOT20(e -> C.p().getAttackCooldownProgress(0) >= swingPercentage, () -> {}),
        // generic cps check from https://github.com/scoliossis/ScaleHackV3/blob/master/src/main/java/com/github/scoliossis/modules/impl/combat/KillAura.java#L326
        CPS(e -> WorldUtil.tick >= nextAttackTick, () -> {
            int nextCps = (int) MathUtil.getRandomInRange(Math.min(minCPS, maxCPS), Math.max(minCPS, maxCPS));
            double ticksTillNextAttack = 20d / (Math.max(nextCps, 1)) + lostPrecision;
            lostPrecision += ticksTillNextAttack - Math.floor(ticksTillNextAttack);
            lostPrecision = lostPrecision - Math.floor(lostPrecision);
            ticksTillNextAttack = Math.floor(ticksTillNextAttack);

            nextAttackTick = (int) (WorldUtil.tick + ticksTillNextAttack);
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
    public static boolean swingMisses = false;

    @RegisterSubModule(name = "Requirements")
    public Requirements requirements = Requirements.Holding_Weapon;
    public enum Requirements implements MultiSelectEnum {
        Holding_Weapon,
        Left_Click_Down;
    }

    private static Entity targetEntity = null;
    private static int switchOrdinal = 0;
    private Rotation targetRotation;

    private static int easingTicks = 0;
    private static Rotation startEasingRotation = null;

    private final Event handleRotations = new RotationEvent(this, params -> {
        targetRotation = null;

        // dont wanna override other rotations!
        if (Bus.isEventCancelled(RotationEvent.class) || !shouldAura()) {
            easingTicks = 0;
            targetEntity = null;
            return false;
        }
        if ((noRotateJump && MovementUtil.willJump()) || !getValidEntity(getTargets(), params)) {
            easingTicks = 0;
            return false;
        }

        if (!willAttack(params.rotation)) return true;

        // queue an attack
        PlayerUtil.onAttack.add(() -> {
            attackSpeed.handleAttack.run();
            switchOrdinal++;
        });

        return true;
    }, 2000);

    private final Event onUpdateMouseEvent = new UpdateMouseEvent(this, params -> {
        if (targetRotation == null || !clientSideRotation) return false;

        targetRotation = targetRotation.add(new Rotation(params.cursorDeltaY, params.cursorDeltaX));

        float delta = C.mc.getRenderTickCounter().getTickProgress(true);
        C.p().setYaw(MathHelper.lerp(delta, RotationEvent.clientRotation.yaw, targetRotation.yaw));
        C.p().setPitch(MathHelper.lerp(delta, RotationEvent.clientRotation.pitch, targetRotation.pitch));

        return true;
    });

    private static boolean willAttack(Rotation rotation) {
        return (swingMisses || isHoveringTargetEntity(rotation)) && attackSpeed.shouldAttack.test(null);
    }


    private static boolean isHoveringTargetEntity(Rotation rotation) {
        return targetEntity != null
                && WorldUtil.getMouseOver(rotation, reach) instanceof EntityHitResult entityHitResult
                && entityHitResult.getEntity() == targetEntity;
    }

    private List<Entity> getTargets() {
        // get a list of all targets in rotation distance
        List<Entity> targets = Targets.getTargets(rotationRange);
        targets.removeIf(entity -> getYawToTarget(entity) > auraFOV);

        // and targets within attack range
        List<Entity> attackRangeTargets = targets.stream().filter(entity -> getMinDistance(entity) <= reach).collect(Collectors.toList());

        // ignore all out of swing distance if there are some in attack range
        if (!attackRangeTargets.isEmpty()) targets = attackRangeTargets;

        // sort by the currently selected target selection mode
        targetSelection.sort(targets);

        return targets;
    }

    private float getYawToTarget(Entity target) {
        return Math.abs(RotationUtil.getRotation(getLastRotation(), C.p().getEyePos(), target.getEyePos()).yaw - RotationEvent.clientRotation.yaw) % 180;
    }

    private boolean getValidEntity(List<Entity> targets, RotationEvent.Params params) {
        if (easingTicks == 0) startEasingRotation = RotationEvent.getLastRotation();

        if (stickyTargets && targetSelection != TargetSelection.Switch && targets.contains(targetEntity) && rotateToTarget(targetEntity, params)) {
            easingTicks = (easingTicks + 1) % (easingCycle+1);
            return true;
        }

        Entity lastTarget = targetEntity;

        // loop entities until we find one that is hittable
        while (!targets.isEmpty()) {
            Entity target = targets.removeFirst();

            if (rotateToTarget(target, params)) {
                if (lastTarget != targetEntity) easingTicks = 0;
                else easingTicks = (easingTicks + 1) % (easingCycle+1);

                return true;
            }
        }

        targetEntity = null;
        return false;
    }

    private boolean rotateToTarget(Entity target, RotationEvent.Params params) {
        Vec3d rotationVector = RotationUtil.getTargetRotationPoint(target, rotationRange, randomRotationPoint);
        if (rotationVector == null) return false;

        targetEntity = target;
        if (isHoveringTargetEntity(getLastRotation()) && onlyNecessary && rotationMode != RotationMode.Snap) {
            params.rotation = getLastRotation();
            return true;
        }

        rotationMode.rotate(target, rotationVector, params);
        targetRotation = params.rotation;
        return true;
    }

    private static Rotation getLastRotation() {
        return clientSideRotation ? RotationEvent.clientRotation : RotationEvent.getLastRotation();
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

package com.scale.scalehackv4.utils.player;

import com.scale.scalehackv4.bridge.KeybindingBridge;
import com.scale.scalehackv4.bridge.UseItemBridge;
import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.RegisterListeners;
import com.scale.scalehackv4.events.impl.client.ClientTickEvent;
import com.scale.scalehackv4.events.impl.player.CheckClickEvent;
import com.scale.scalehackv4.events.impl.player.RotationEvent;
import com.scale.scalehackv4.modules.impl.client.options.HideBlinkInboundModule;
import com.scale.scalehackv4.utils.C;
import lombok.Getter;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Predicate;

@RegisterListeners
public class PlayerUtil {
    public static ArrayList<Runnable> onAttack = new ArrayList<>();
    public static int lastAttackTick = 0;
    public static int lastSwingTick = 0;

    private final Event clearQueuedAttack = new RotationEvent(null, params -> {
        onAttack.clear();
        return false;
    }, 1);

    private final Event handleAttack = new CheckClickEvent.Left(null, params -> {
        if (!onAttack.isEmpty()) {
            UseItemBridge.from(C.mc).bridge$doAttack();
            onAttack.forEach(Runnable::run);
            onAttack.clear();

            return true;
        }
        return false;
    }, 9999);

    // great name scale *everyone claps* meow
    public static Entity getThisTickAttackEntity(Predicate<Entity> entityPredicate) {
        HitResult crosshairTarget = WorldUtil.getMouseOver(RotationEvent.getLastRotation(), C.p().getAttackRange().maxRange());

        if (!C.p().isBlocking()
                && (KeybindingBridge.from(C.mc.options.attackKey).bridge$getTimesPressed() > 0 || !PlayerUtil.onAttack.isEmpty())
                && crosshairTarget instanceof EntityHitResult entityHitResult
                && entityPredicate.test(entityHitResult.getEntity())) {
            return entityHitResult.getEntity();
        }

        return null;
    }

    public static void unsetAttack() {
        while (C.mc.options.attackKey.wasPressed());
        PlayerUtil.onAttack.clear();
    }

    public static HashMap<Integer, ArrayList<Runnable>> queuedTasks = new HashMap<>();

    public static void queueTask(Runnable task, int delay) {
        int tick = WorldUtil.tick + delay;
        ArrayList<Runnable> tasks = queuedTasks.getOrDefault(tick, new ArrayList<>());
        tasks.add(task);
        queuedTasks.put(tick, tasks);
    }

    @Getter
    private static final HashMap<Integer, Boolean> overriddenKeybinds = new HashMap<>();

    public static void oneTickKeybind(KeyBinding keyBinding, boolean to) {
        overriddenKeybinds.put(keyBinding.hashCode(), to);
    }

    private final Event onClientTick = new ClientTickEvent(null, params -> {
        ArrayList<Runnable> tasks = queuedTasks.getOrDefault(WorldUtil.tick, new ArrayList<>());
        for (Runnable task : tasks) task.run();
        queuedTasks.remove(WorldUtil.tick);

        overriddenKeybinds.clear();
        return false;
    }, 1);

    public static Vec3d getPlayerRenderPos(Entity entity) {
        return HideBlinkInboundModule.getPlayerRenderPos(entity);
    }

    public static ArrayList<Vec3d> getEntityCorners(Entity entity) {
        Box targetBoundingBox = entity.getBoundingBox();
        return getCorners(
                entity.getEntityPos(),
                targetBoundingBox.getLengthX(),
                targetBoundingBox.getLengthY(),
                targetBoundingBox.getLengthZ()
        );
    }

    public static ArrayList<Vec3d> getCorners(Vec3d pos, double width, double height, double depth) {
        ArrayList<Vec3d> possibleRotations = new ArrayList<>();
        for (int i = 0; i < 8; i++) {

            // add all possible corners, added using the order recommended for binary truth tables by my cs teacher, shoutout
            possibleRotations.add(new Vec3d(
                    i % 2 == 0 ? pos.x : pos.x + width,
                    i % 4 >= 2 ? pos.y : pos.y + height,
                    i % 8 >= 4 ? pos.z : pos.z + depth
            ));
        }

        return possibleRotations;
    }
}
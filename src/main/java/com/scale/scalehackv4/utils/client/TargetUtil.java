package com.scale.scalehackv4.utils.client;

import com.scale.scalehackv4.bridge.antibot.AntiBotBridge;
import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.RegisterListeners;
import com.scale.scalehackv4.events.impl.client.WorldJoinEvent;
import com.scale.scalehackv4.events.impl.player.PlayerUpdateEvent;
import com.scale.scalehackv4.modules.impl.client.Targets;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.player.Rotation;
import com.scale.scalehackv4.utils.player.RotationUtil;
import com.scale.scalehackv4.utils.player.WorldUtil;
import net.minecraft.client.gui.screen.multiplayer.SocialInteractionsPlayerListEntry;
import net.minecraft.client.gui.screen.multiplayer.SocialInteractionsPlayerListWidget;
import net.minecraft.client.gui.screen.multiplayer.SocialInteractionsScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@RegisterListeners
public class TargetUtil {
    public static List<AbstractClientPlayerEntity> getPlayers() {
        return C.w().getPlayers().stream().filter(TargetUtil::isValidTarget).toList();
    }

    public static List<Entity> getTargets(double range) {
        return C.w().getOtherEntities(C.p(), C.p().getBoundingBox().expand(range), TargetUtil::isValidTarget);
    }

    private static final HashMap<UUID, Integer> ENTITY_VALID_TICKS = new HashMap<>();
    private static SocialInteractionsPlayerListWidget PLAYER_LIST_WIDGET;

    private final Event reloadValidTargets = new WorldJoinEvent(null, params -> {
        ENTITY_VALID_TICKS.clear();
        PLAYER_LIST_WIDGET = new SocialInteractionsPlayerListWidget(new SocialInteractionsScreen(), C.mc, Integer.MAX_VALUE, Integer.MAX_VALUE, 0, 0);

        return false;
    });

    private final Event updateValidEntities = new PlayerUpdateEvent(null, params -> {
        PLAYER_LIST_WIDGET.update(C.p().networkHandler.getPlayerUuids(), 0, true);
        List<SocialInteractionsPlayerListEntry> players = AntiBotBridge.from(PLAYER_LIST_WIDGET).bridge$getPlayers();
        for (SocialInteractionsPlayerListEntry player : players) {
            ENTITY_VALID_TICKS.putIfAbsent(player.getUuid(), 0);
            // if player is offline, they lose 1 score, online gains a score, positive score means they likely arent a bot!
            ENTITY_VALID_TICKS.computeIfPresent(player.getUuid(), (k, validTicks) -> validTicks + (player.isOffline() ? -1 : 1));
        }

        return false;
    });

    public static boolean isValidTarget(Entity target) {
        if (target == C.p()) return true;
        // this is possible for some reason, crash occured with a random skyblock mod, idk why it fed this a null target, wtv.
        if (target == null) return false;

        return !isBot(target) && !isTeam(target);
    }

    private static boolean isTeam(Entity target) {
        if (!Targets.teams) return false;
        if (!(target instanceof PlayerEntity player)) return false;

        return player.getTeamColorValue() == C.p().getTeamColorValue();
    }

    private static boolean isBot(Entity target) {
        if (!Targets.antibot) return !(target instanceof LivingEntity);

        int ping = 1;
        if (target instanceof PlayerEntity player) {
            PlayerListEntry playerListEntry = C.p().networkHandler.getPlayerListEntry((player).getUuid());
            if (playerListEntry != null) ping = playerListEntry.getLatency();
        }
        Integer validTicks = ENTITY_VALID_TICKS.getOrDefault(target.getUuid(), -1);
        boolean isValidEntry = !Targets.playerEntry || (validTicks != null && validTicks > 0);
        boolean isValidPing = !Targets.pingCheck || (ping > Targets.botPingMode.ping);
        return !isValidEntry || !isValidPing;
    }

    public static Vec3d getClosestPointToEntity(Entity target) {
        return WorldUtil.getClosestPoint(target.getBoundingBox());
    }

    // from scalehackv3 https://github.com/scoliossis/ScaleHackOpenSrc/blob/master/src/main/java/com/github/scoliossis/utils/minecraft/TargetUtil.java#L88
    public static Vec3d getTargetRotationPoint(Entity entity, double range) {
        if (entity == null) return null;

        double blockReach = Math.max(C.p().getBlockInteractionRange(), range);
        Box targetBoundingBox = entity.getBoundingBox();
        Vec3d closestPoint = TargetUtil.getClosestPointToEntity(entity).subtract(0, 0, 0);

        Rotation bestRotation = RotationUtil.getRotation(closestPoint);
        // check that looking at the closest point doesnt have a wall between us and the entity
        if (WorldUtil.getMouseOver(bestRotation, blockReach, range) instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() == entity) return closestPoint;


        // awesome. there was no comments in this entire function
        ArrayList<Vec3d> possibleRotations = new ArrayList<>();
        for (int i = 0; i < 8; i++) {

            // add all possible corners, added using the order recommended for binary truth tables
            possibleRotations.add(new Vec3d(
                    i % 2 == 0 ? targetBoundingBox.minX : targetBoundingBox.maxX,
                    i % 4 >= 2 ? targetBoundingBox.minY : targetBoundingBox.maxY,
                    i % 8 >= 4 ? targetBoundingBox.minZ : targetBoundingBox.maxZ
            ));
        }

        // add the top middle and bottom of the player centre aswell, probably doesnt help
        possibleRotations.add(new Vec3d(entity.getX(), (targetBoundingBox.maxY + targetBoundingBox.minY) / 2, entity.getZ()));
        possibleRotations.add(new Vec3d(entity.getX(), targetBoundingBox.minY, entity.getZ()));
        possibleRotations.add(new Vec3d(entity.getX(), targetBoundingBox.maxY, entity.getZ()));
        possibleRotations.add(closestPoint);

        // check all possible rotations until we find
        for (Vec3d possibleRotationVector : possibleRotations) {
            Rotation possibleRotation = RotationUtil.getRotation(possibleRotationVector);
            if (WorldUtil.getMouseOver(possibleRotation, blockReach, range) instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() == entity) {
                return possibleRotationVector;
            }
        }

        return null;
    }
}

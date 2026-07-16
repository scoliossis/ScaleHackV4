package com.scale.scalehackv4.modules.impl.client.options;

import com.scale.scalehackv4.bridge.EntityS2CPacketBridge;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.modules.impl.client.OptionsModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.player.WorldUtil;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public class HideBlinkInboundModule extends OptionsModule {
    @RegisterSubModule(name = "Hide Blink Inbound")
    public static boolean hideBlinkInbound = true;

    public static final HashSet<Class<?>> VISUAL_INBOUND_PACKETS = new HashSet<>(Set.of(
            EntitySetHeadYawS2CPacket.class,
            EntityStatusS2CPacket.class,
            // technically not just visual, waking up from a bed changes ur hitbox.
            EntityAnimationS2CPacket.class,
            EntityDamageS2CPacket.class,

            ChatMessageS2CPacket.class,
            PlaySoundS2CPacket.class,
            ScoreboardDisplayS2CPacket.class,
            ScoreboardObjectiveUpdateS2CPacket.class,
            ScoreboardScoreResetS2CPacket.class
    ));

    public static final HashMap<Class<?>, Predicate<Packet<?>>> POSITION_PACKET_HANDLERS = new HashMap<>(Map.of(
            EntityS2CPacket.MoveRelative.class, (packet) -> {
                handleMovePacket((EntityS2CPacket) packet);
                return true;
            },
            EntityS2CPacket.RotateAndMoveRelative.class, (packet) -> {
                handleMovePacket((EntityS2CPacket) packet);
                return true;
            },
            EntityPositionSyncS2CPacket.class, (packet) -> {
                EntityPositionSyncS2CPacket positionPacket = (EntityPositionSyncS2CPacket) packet;
                int playerID = positionPacket.id();
                Entity entity = C.w().getEntityById(playerID);
                if (entity == null) return false;

                updatePlayerPos(entity, playerID, positionPacket.values().position());

                return true;
            }
    ));

    private static final HashMap<Integer, Integer> LAST_PLAYER_TICK = new HashMap<>();

    private static final HashMap<Integer, Vec3d> LAST_PLAYER_POS = new HashMap<>();
    private static final HashMap<Integer, Vec3d> REAL_PLAYER_POS = new HashMap<>();

    public static boolean handleInboundPacket(Packet<?> packet) {
        if (!HideBlinkInboundModule.hideBlinkInbound) return false;

        Class<?> packetClass = packet.getClass();

        if (HideBlinkInboundModule.VISUAL_INBOUND_PACKETS.contains(packetClass)) return true;

        POSITION_PACKET_HANDLERS.getOrDefault(packetClass, p -> false).test(packet);

        return false;
    }

    public static void handlePlayerRenderPosition(EntityRenderState entityRenderState, float tickProgress) {
        if (!(entityRenderState instanceof PlayerEntityRenderState playerEntityRenderState)) return;

        int playerID = playerEntityRenderState.id;
        Vec3d realPos = REAL_PLAYER_POS.get(playerID);
        if (realPos != null) {
            Vec3d lastPos = LAST_PLAYER_POS.getOrDefault(playerID, realPos);
            int lastTick = LAST_PLAYER_TICK.getOrDefault(playerID, 0);

            // dividing by 3 comes from net.minecraft.entity.PositionInterpolator.PositionInterpolator(net.minecraft.entity.Entity)
            float lerp = Math.min(1, (WorldUtil.tick - lastTick + tickProgress) / 3f);

            Vec3d lerpedPos = MathHelper.lerp(
                    lerp,
                    lastPos,
                    realPos
            );

            playerEntityRenderState.x = lerpedPos.x;
            playerEntityRenderState.y = lerpedPos.y;
            playerEntityRenderState.z = lerpedPos.z;

            playerEntityRenderState.bodyYaw = MathHelper.wrapDegrees(playerEntityRenderState.bodyYaw + playerEntityRenderState.relativeHeadYaw);
            playerEntityRenderState.relativeHeadYaw = 0;
        }
    }

    private static void handleMovePacket(EntityS2CPacket movePacket) {
        int playerID = EntityS2CPacketBridge.from(movePacket).bridge$id();
        Entity entity = C.w().getEntityById(playerID);
        if (entity == null) return;

        Vec3d newPos = REAL_PLAYER_POS.getOrDefault(playerID, entity.getTrackedPosition().getPos())
                .add(unpackDelta(movePacket.getDeltaX()), unpackDelta(movePacket.getDeltaY()), unpackDelta(movePacket.getDeltaZ()));

        updatePlayerPos(entity, playerID, newPos);
    }

    private static void updatePlayerPos(Entity entity, int id, Vec3d newPos) {
        Vec3d lastPos = REAL_PLAYER_POS.getOrDefault(id, entity.getTrackedPosition().getPos());

        LAST_PLAYER_POS.put(id, lastPos);
        REAL_PLAYER_POS.put(id, newPos);

        LAST_PLAYER_TICK.put(id, WorldUtil.tick);
    }

    public static void onDisableBlinkInbound() {
        if (C.w() == null) return;

        for (Entity entity : C.w().getEntities()) {
            Vec3d realPos = HideBlinkInboundModule.REAL_PLAYER_POS.get(entity.getId());
            if (realPos == null) continue;

            entity.updateTrackedPosition(realPos);
        }


        HideBlinkInboundModule.REAL_PLAYER_POS.clear();
        HideBlinkInboundModule.LAST_PLAYER_POS.clear();
        HideBlinkInboundModule.LAST_PLAYER_TICK.clear();
    }

    // used by net.minecraft.client.network.ClientPlayNetworkHandler.onEntity(EntityS2CPacket)
    // net.minecraft.entity.TrackedPosition.unpack
    private static double unpackDelta(long value) {
        if (value == 0) return 0;
        return (double)value / 4096.0;
    }
}

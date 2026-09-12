package com.scale.scalehackv4.modules.impl.client.options;

import com.scale.scalehackv4.bridge.EntityS2CPacketBridge;
import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.player.PacketEvent;
import com.scale.scalehackv4.events.impl.player.PlayerUpdateEvent;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.modules.impl.client.OptionsModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.player.BlinkUtil;
import com.scale.scalehackv4.utils.player.WorldUtil;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// lerp magic stolen from net.minecraft.entity.PositionInterpolator
public class HideBlinkInboundModule extends OptionsModule {
    @RegisterSubModule(name = "Hide Blink Inbound")
    public static boolean hideBlinkInbound = true;

    private static final Map<Integer, BlinkInterpolation> INTERPOLATIONS = new ConcurrentHashMap<>();

    public static final HashSet<Integer> REAL_PLAYER_RENDERERS = new HashSet<>();

    private static class BlinkInterpolation {
        // net.minecraft.entity.PositionInterpolator.DEFAULT_INTERPOLATION_DURATION
        private static final int LERP_DURATION = 3;

        public BlinkInterpolation(Vec3d pos) {
            this.serverPos = this.current = this.previous = pos;
        }

        public void updatePlayerPosition(Vec3d pos) {
            this.serverPos = pos;
            this.steps = LERP_DURATION;
        }

        Vec3d previous, current, serverPos;
        int steps;
    }

    public static final HashSet<Class<?>> VISUAL_INBOUND_PACKETS = new HashSet<>(Set.of(
            EntitySetHeadYawS2CPacket.class,
            EntityStatusS2CPacket.class,
            // technically not just visual, waking up from a bed changes ur hitbox.
            EntityAnimationS2CPacket.class,
            EntityDamageS2CPacket.class,

            ChatMessageS2CPacket.class,
            PlaySoundS2CPacket.class
    ));

    private final Event tickPlayerInterpolation = new PlayerUpdateEvent(null, params -> {
        for (BlinkInterpolation interpolation : INTERPOLATIONS.values().toArray(BlinkInterpolation[]::new)) {
            interpolation.previous = interpolation.current;

            if (interpolation.steps > 0) {
                double t = 1.0 / interpolation.steps--;
                interpolation.current = MathHelper.lerp(t, interpolation.current, interpolation.serverPos);
            }
        }
        return false;
    });

    private final Event trackPlayerPositions = new PacketEvent.Received(null, params -> {
        if (C.w() == null || BlinkUtil.shouldAllowPackets) return false;

        if (params.packet instanceof EntityS2CPacket movePacket)
            handleMovePacket(movePacket);
        else if (params.packet instanceof EntityPositionSyncS2CPacket posPacket)
            handlePositionPacket(posPacket);

        return false;
    }, 0);


    public static void handlePlayerRenderPosition(EntityRenderState entityRenderState) {
        if (!(entityRenderState instanceof PlayerEntityRenderState playerState)) return;

        int playerID = playerState.id;
        Entity player = C.w().getEntityById(playerID);
        if (player == null) return;

        BlinkInterpolation interpolation = INTERPOLATIONS.get(playerID);
        if (interpolation == null) return;

        float tickDelta = C.mc.getRenderTickCounter().getTickProgress(true);
        Vec3d renderPos =  MathHelper.lerp(tickDelta, interpolation.previous, interpolation.current);

        playerState.x = renderPos.x;
        playerState.y = renderPos.y;
        playerState.z = renderPos.z;
    }

    private static void handleMovePacket(EntityS2CPacket movePacket) {
        int playerID = EntityS2CPacketBridge.from(movePacket).bridge$id();

        Entity entity = C.w().getEntityById(playerID);
        if (entity == null) return;

        BlinkInterpolation interpolation = INTERPOLATIONS.computeIfAbsent(playerID, i -> new BlinkInterpolation(entity.getTrackedPosition().getPos()));


        interpolation.updatePlayerPosition(
                interpolation.serverPos.add(
                        unpackDelta(movePacket.getDeltaX()),
                        unpackDelta(movePacket.getDeltaY()),
                        unpackDelta(movePacket.getDeltaZ())
                )
        );
    }

    private static void handlePositionPacket(EntityPositionSyncS2CPacket positionPacket) {
        int playerID = positionPacket.id();
        Entity entity = C.w().getEntityById(playerID);
        if (entity == null) return;

        Vec3d pos = positionPacket.values().position();
        BlinkInterpolation interpolation = INTERPOLATIONS.computeIfAbsent(playerID, i -> new BlinkInterpolation(pos));
        interpolation.updatePlayerPosition(pos);
    }

    // used by net.minecraft.client.network.ClientPlayNetworkHandler.onEntity(EntityS2CPacket)
    // net.minecraft.entity.TrackedPosition.unpack
    private static double unpackDelta(long value) {
        if (value == 0) return 0;
        return (double)value / 4096.0;
    }
}
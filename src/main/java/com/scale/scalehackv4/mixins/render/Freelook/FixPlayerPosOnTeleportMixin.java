package com.scale.scalehackv4.mixins.render.Freelook;

import com.scale.scalehackv4.modules.impl.render.Freelook;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.player.Rotation;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class FixPlayerPosOnTeleportMixin {
    // you recieve this packet somewhere in the render thread, freecam assumes all movement happens during the tick function
    @Inject(method = "onPlayerPositionLook", at = @At("TAIL"))
    public void onPlayerPositionLook(PlayerPositionLookS2CPacket packet, CallbackInfo ci) {
        if (Freelook.isPositionLocked()) {
            Freelook.realPos = packet.change().position();
            if (Freelook.lastRenderPos != null) {
                C.p().lastX = C.p().lastRenderX = Freelook.lastRenderPos.x;
                C.p().lastY = C.p().lastRenderY = Freelook.lastRenderPos.y;
                C.p().lastZ = C.p().lastRenderZ = Freelook.lastRenderPos.z;
                C.p().setPos(Freelook.renderPos.x, Freelook.renderPos.y, Freelook.renderPos.z);
            }
        }
        if (Freelook.isRotationLocked()) {
            Freelook.realRotation = new Rotation(packet.change().yaw(), packet.change().pitch());

            if (Freelook.lastRenderRotation != null) {
                C.p().lastYaw = Freelook.lastRenderRotation.yaw;
                C.p().lastPitch = Freelook.lastRenderRotation.pitch;
                C.p().setYaw(Freelook.renderRotation.yaw);
                C.p().setPitch(Freelook.renderRotation.pitch);
            }
        }
    }
}

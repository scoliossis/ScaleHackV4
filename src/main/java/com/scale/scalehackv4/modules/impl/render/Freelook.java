package com.scale.scalehackv4.modules.impl.render;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.client.PacketEvent;
import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.ModuleUtil;
import com.scale.scalehackv4.utils.player.Rotation;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Vec3d;

@RegisterModule(
        name = "Freelook",
        category = Category.RENDER
)
// todo: impl blocking interation
public class Freelook extends Module {
    @RegisterSubModule(name = "Lock Rotation", hasKeybind = true)
    public static boolean lockRotation = true;

    @RegisterSubModule(name = "Lock Position", hasKeybind = true)
    public static boolean lockPosition = true;

    public static boolean isRotationLocked() {
        return ModuleUtil.isEnabled(Freelook.class) && lockRotation;
    }
    public static boolean isPositionLocked() {
        return ModuleUtil.isEnabled(Freelook.class) && lockPosition;
    }

    public static Vec3d realPos = null;
    public static Vec3d renderPos = null;

    public static Rotation realRotation = null;
    public static Rotation renderRotation = null;

    private final Event acceptPositionPacket = new PacketEvent.Received(this, (params -> {
        if (!C.isInGame()) return false;

        if (isRotationLocked() && realRotation == null) {
            realRotation = new Rotation(C.p().getYaw(), C.p().getPitch());
        }
        if ((!isPositionLocked() && realPos != null) || (!isRotationLocked() && realRotation != null)) {
            onDisable();
        }

        // you recieve these packets somewhere in the render thread, freecam assumes all movement happens during the tick function
        if (params.packet instanceof PlayerPositionLookS2CPacket positionPacket) {
            if (isPositionLocked()) {
                realPos = positionPacket.change().position();
            }
            if (isRotationLocked()) {
                realRotation = new Rotation(positionPacket.change().yaw(), positionPacket.change().pitch());
            }
            return false;
        }

        return false;
    }));

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {
        if (C.isInGame()) {
            if (realPos != null) C.p().setPos(realPos.x, realPos.y, realPos.z);
            if (realRotation != null) {
                C.p().setYaw(realRotation.yaw);
                C.p().setPitch(realRotation.pitch);
            }
        }
        renderPos = realPos = null;
        renderRotation = realRotation = null;
    }
}

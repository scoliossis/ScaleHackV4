package com.scale.scalehackv4.modules.impl.render;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.client.PacketEvent;
import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.ModuleUtil;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Vec3d;

@RegisterModule(
        name = "Freelook",
        category = Category.RENDER
)
// todo: impl locking rotation and blocking interation
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

    private final Event acceptPositionPacket = new PacketEvent.Received(this, (params -> {
        if (params.packet instanceof PlayerPositionLookS2CPacket positionPacket) {
            realPos = positionPacket.change().position();
            return false;
        }

        return false;
    }));

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {
        C.p().setPos(realPos.x, realPos.y, realPos.z);
        renderPos = null;
    }
}

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
import net.minecraft.util.math.Vec3d;

@RegisterModule(
        name = "Freelook",
        category = Category.RENDER
)
// todo: impl blocking interaction
public class Freelook extends Module {
    @RegisterSubModule(name = "Lock Rotation", hasKeybind = true)
    public static boolean lockRotation = true;

    @RegisterSubModule(name = "Lock Position", hasKeybind = true)
    public static boolean lockPosition = true;

    @RegisterSubModule(name = "Horizontal Speed", parent = "Lock Position", min = 0.1, max = 5)
    public static double horizontalSpeed = 1;
    @RegisterSubModule(name = "Vertical Speed", parent = "Lock Position", min = 0.1, max = 5)
    public static double verticalSpeed = 1;

    public static boolean isRotationLocked() {
        return ModuleUtil.isEnabled(Freelook.class) && lockRotation;
    }
    public static boolean isPositionLocked() {
        return ModuleUtil.isEnabled(Freelook.class) && lockPosition;
    }

    public static Vec3d realPos = null;
    public static Vec3d renderPos = null;

    public static Vec3d lastRealPos = null;
    public static Vec3d lastRenderPos = null;


    public static Rotation realRotation = null;
    public static Rotation renderRotation = null;

    public static Rotation lastRealRotation = null;
    public static Rotation lastRenderRotation = null;

    // on the packet event, cause this event is posted to a lot a ig, move later
    private final Event acceptPositionPacket = new PacketEvent.Received(this, (params -> {
        if (!C.isInGame()) return false;

        if (isRotationLocked() && realRotation == null) {
            realRotation = new Rotation(C.p().getYaw(), C.p().getPitch());
        }
        if ((!isPositionLocked() && realPos != null) || (!isRotationLocked() && realRotation != null)) {
            onDisable();
        }

        return false;
    }));

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {
        if (C.isInGame()) {
            if (realPos != null && !isPositionLocked()) {
                if (C.isInGame()) {
                    C.p().setPos(realPos.x, realPos.y, realPos.z);
                }
                renderPos = lastRenderPos = lastRealPos = realPos = null;
            }
            if (realRotation != null && !isRotationLocked()) {
                if (C.isInGame()) {
                    C.p().setYaw(realRotation.yaw);
                    C.p().setPitch(realRotation.pitch);
                }
                renderRotation = lastRenderRotation = lastRealRotation = realRotation = null;
            }
        }
    }
}

package com.scale.scalehackv4.modules.impl.render;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.client.ClientTickEvent;
import com.scale.scalehackv4.events.impl.client.SubModuleToggledEvent;
import com.scale.scalehackv4.events.impl.player.KeyboardInputEvent;
import com.scale.scalehackv4.events.impl.player.PacketEvent;
import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.ModuleUtil;
import com.scale.scalehackv4.utils.player.Rotation;
import net.minecraft.client.option.Perspective;
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

        if (isRotationLocked() && realRotation == null)
            realRotation = new Rotation(C.p().getYaw(), C.p().getPitch());

        return false;
    }));

    private boolean changedLockRotation = false;
    private boolean changedLockPosition = false;

    private final Event onToggleSubmodules = new SubModuleToggledEvent(this, params -> {
        if (params.field.getName().equals("lockRotation")) changedLockRotation = true;
        if (params.field.getName().equals("lockPosition")) changedLockPosition = true;

        return false;
    });

    // smth was wrong w disabling them on SubModuleToggledEvent and idk why, i assume this should fix wtv caused it
    private final Event onPlayerTick = new ClientTickEvent(this, params -> {
        if (changedLockRotation) {
            onSetRotationLock(lockRotation);
            changedLockRotation = false;
        }
        if (changedLockPosition) {
            onSetPositionLock(lockPosition);
            changedLockPosition = false;
        }

        return false;
    });

    private final Event onKeyboardInput = new KeyboardInputEvent(this,params -> {
        if (isPositionLocked()) params.reset();

        return false;
    }, 1);

    @Override
    public void onEnable() {
        onSetPositionLock(lockPosition);
        onSetRotationLock(lockRotation);
    }

    @Override
    public void onDisable() {
        onSetPositionLock(false);
        onSetRotationLock(false);
    }

    private Perspective perspective = Perspective.FIRST_PERSON;

    private void onSetRotationLock(boolean lockRotation) {
        if (!lockRotation) {
            if (C.isInGame() && realRotation != null) {
                C.p().setYaw(realRotation.yaw);
                C.p().setPitch(realRotation.pitch);
            }
            renderRotation = lastRenderRotation = lastRealRotation = realRotation = null;
            if (C.mc.options != null && C.mc.options.getPerspective() == Perspective.THIRD_PERSON_BACK) C.mc.options.setPerspective(perspective);
        }
        else {
            if (C.isInGame()) {
                perspective = C.mc.options.getPerspective();
                C.mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
            }
        }
    }

    private void onSetPositionLock(boolean lockPosition) {
        if (!lockPosition) {
            if (C.isInGame()) C.p().setPos(realPos.x, realPos.y, realPos.z);
            renderPos = lastRenderPos = lastRealPos = realPos = null;
        }
    }
}

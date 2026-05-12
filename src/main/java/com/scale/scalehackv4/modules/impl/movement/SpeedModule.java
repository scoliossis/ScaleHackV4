package com.scale.scalehackv4.modules.impl.movement;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.player.KeyboardInputEvent;
import com.scale.scalehackv4.events.impl.player.PlayerUpdateEvent;
import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.screens.clickgui.ClickGuiScreen;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.ChatUtil;
import com.scale.scalehackv4.utils.player.WorldUtil;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;

@RegisterModule(
        name = "Speed",
        category = Category.MOVEMENT
)
public class SpeedModule extends Module {
    @RegisterSubModule(name = "Sprint")
    public static boolean sprint = true;
    @RegisterSubModule(name = "Jump Delay", min = 1, max = 10)
    public static int jumpDelay = 1;

    @RegisterSubModule(name = "Modify Speed", hasKeybind = true)
    public static boolean modifySpeed = false;
    @RegisterSubModule(name = "Speed Modifier", parent = "Modify Speed")
    public static SpeedModifier speedModifier = SpeedModifier.Vanilla;
    @RegisterSubModule(name = "Horizontal Speed", max = 10, parent = "Speed Modifier", modeParentString = "Vanilla")
    public static float horizontalSpeed = 1;

    @RegisterSubModule(name = "Modify Vertical Speed", hasKeybind = true)
    public static boolean modifyVerticalSpeed = false;

    @RegisterSubModule(name = "Vertical Modifier", parent = "Modify Vertical Speed")
    public static VerticalSpeedModifier verticalSpeedModifier = VerticalSpeedModifier.Fly;
    @RegisterSubModule(name = "Vertical Speed", max = 10, parent = "Vertical Modifier", modeParentString = "Fly")
    public static float verticalSpeed = 1;

    private final Event overrideSprinting = new KeyboardInputEvent(this, params -> {
        params.sprintKey |= sprint;

        if (modifySpeed) speedModifier.handleKeyboardInput(params);

        return false;
    });

    private final Event playerUpdateEvent = new PlayerUpdateEvent(this, params -> {
        if (modifyVerticalSpeed) verticalSpeedModifier.handlePlayerUpdate(params);
        if (modifySpeed) speedModifier.handlePlayerUpdate(params);

        return false;
    });

    public enum SpeedModifier {
        Vanilla {
            @Override public void handleKeyboardInput(KeyboardInputEvent.Params params) {}

            @Override
            public void handlePlayerUpdate(PlayerUpdateEvent.Params params) {
                double yaw = Math.toRadians(C.p().getYaw());
                double forward = (C.mc.options.forwardKey.isPressed() ? horizontalSpeed : 0) + (C.mc.options.backKey.isPressed() ? -horizontalSpeed : 0);
                double sideways = (C.mc.options.leftKey.isPressed() ? horizontalSpeed : 0) + (C.mc.options.rightKey.isPressed() ? -horizontalSpeed : 0);

                C.p().setVelocity(new Vec3d(
                        (Math.sin(-yaw) * forward + Math.cos(-yaw) * sideways),
                        C.p().getVelocity().y,
                        (Math.cos(-yaw) * forward - Math.sin(-yaw) * sideways)
                ));
            }
        },
        Elytra_Hop {
            @Override
            public void handleKeyboardInput(KeyboardInputEvent.Params params) {
                if (C.mc.currentScreen instanceof ClickGuiScreen) return;

                ItemStack chestplate = C.p().getEquippedStack(EquipmentSlot.CHEST);
                boolean hasElytra = chestplate.getItem() == Items.ELYTRA && chestplate.getMaxDamage()-1 != chestplate.getDamage();
                if (!hasElytra) {
                    ChatUtil.prefixMessage("no elytra, or elytra broken, cant elytra hop </3");
                    modifySpeed = false;
                    return;
                }
                if (WorldUtil.airTicks == 1) params.jumpKey = false;
                if ((WorldUtil.airTicks == 2 || C.p().isOnGround()) && params.forwardKey) params.jumpKey = true;
            }

            @Override public void handlePlayerUpdate(PlayerUpdateEvent.Params params) {}
        };

        public abstract void handleKeyboardInput(KeyboardInputEvent.Params params);
        public abstract void handlePlayerUpdate(PlayerUpdateEvent.Params params);
    }

    public enum VerticalSpeedModifier {
        Fly {
            @Override
            public void handlePlayerUpdate(PlayerUpdateEvent.Params params) {
                C.p().setVelocity(
                        C.p().getVelocity().x,
                        verticalSpeed * (C.mc.options.jumpKey.isPressed() ? 1 : 0) + verticalSpeed * (C.mc.options.sneakKey.isPressed() ? -1 : 0)
                        , C.p().getVelocity().z
                );
            }
        };

        public abstract void handlePlayerUpdate(PlayerUpdateEvent.Params params);
    }

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {

    }
}

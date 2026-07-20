package com.scale.scalehackv4.modules.impl.movement.speed;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.player.KeyboardInputEvent;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.modules.impl.movement.SpeedModule;
import com.scale.scalehackv4.screens.clickgui.ClickGuiScreen;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.ChatUtil;
import com.scale.scalehackv4.utils.client.ModuleUtil;
import com.scale.scalehackv4.utils.player.WorldUtil;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;

public class HorizontalSpeedModule extends SpeedModule {
    @RegisterSubModule(name = "Speed Modifier", parent = "Modify Speed")
    public static SpeedModifier speedModifier = SpeedModifier.None;
    @RegisterSubModule(name = "Horizontal Speed", max = 10, parent = "Speed Modifier", modeParentString = "Vanilla")
    public static float horizontalSpeed = 1;

    public enum SpeedModifier {
        None {
            @Override public void handleKeyboardInput(KeyboardInputEvent.Params params) {}
        },
        Vanilla {
            @Override public void handleKeyboardInput(KeyboardInputEvent.Params params) {
                double yaw = Math.toRadians(C.p().getYaw());
                double forward = (params.forwardKey ? horizontalSpeed : 0) + (params.backKey ? -horizontalSpeed : 0);
                double sideways = (params.leftKey ? horizontalSpeed : 0) + (params.rightKey ? -horizontalSpeed : 0);

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
                    speedModifier = SpeedModifier.None;
                    return;
                }
                if (WorldUtil.airTicks == 1) params.jumpKey = false;
                if ((WorldUtil.airTicks == 2 || C.p().isOnGround()) && params.forwardKey) params.jumpKey = true;
            }
        };

        public abstract void handleKeyboardInput(KeyboardInputEvent.Params params);
    }

    private final Event handleKeyboardInputEvent = new KeyboardInputEvent(ModuleUtil.getModule(SpeedModule.class), params -> {
        speedModifier.handleKeyboardInput(params);

        return false;
    });
}

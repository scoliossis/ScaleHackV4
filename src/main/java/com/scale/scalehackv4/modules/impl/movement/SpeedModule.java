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
import com.scale.scalehackv4.utils.client.ModuleUtil;
import com.scale.scalehackv4.utils.client.PacketUtil;
import com.scale.scalehackv4.utils.player.WorldUtil;
import lombok.AllArgsConstructor;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.PlayerInput;
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

    @RegisterSubModule(name = "Speed Modifier", parent = "Modify Speed")
    public static SpeedModifier speedModifier = SpeedModifier.None;
    @RegisterSubModule(name = "Horizontal Speed", max = 10, parent = "Speed Modifier", modeParentString = "Vanilla")
    public static float horizontalSpeed = 1;
    @RegisterSubModule(name = "Vertical Modifier", parent = "Modify Vertical Speed")
    public static VerticalSpeedModifier verticalSpeedModifier = VerticalSpeedModifier.None;
    @RegisterSubModule(name = "Vertical Speed", max = 10, parent = "Vertical Modifier", modeParentString = "Fly")
    public static float verticalSpeed = 1;

    @RegisterSubModule(name = "Keep Sprint", parent = "Modify Speed")
    public static KeepSprint keepSprint = KeepSprint.None;

    public static boolean shouldKeepsprint() {
        return ModuleUtil.isEnabled(SpeedModule.class) && keepSprint != KeepSprint.None && C.p().forwardSpeed > 0;
    }

    private final Event overrideSprinting = new KeyboardInputEvent(this, params -> {
        params.sprintKey |= sprint;

        speedModifier.handleKeyboardInput(params);

        return false;
    });

    private final Event playerUpdateEvent = new PlayerUpdateEvent(this, params -> {
        verticalSpeedModifier.handlePlayerUpdate(params);
        speedModifier.handlePlayerUpdate(params);

        return false;
    });

    public enum SpeedModifier {
        None {
            @Override public void handleKeyboardInput(KeyboardInputEvent.Params params) {}
            @Override public void handlePlayerUpdate(PlayerUpdateEvent.Params params) {}
        },
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
                    speedModifier = SpeedModifier.None;
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
        None {
            @Override public void handlePlayerUpdate(PlayerUpdateEvent.Params params) {}
        },
        Fly {
            @Override
            public void handlePlayerUpdate(PlayerUpdateEvent.Params params) {
                C.p().setVelocity(
                        C.p().getVelocity().x,
                        verticalSpeed * (C.mc.options.jumpKey.isPressed() ? 1 : 0) + verticalSpeed * (C.mc.options.sneakKey.isPressed() ? -1 : 0),
                        C.p().getVelocity().z
                );
            }
        };

        public abstract void handlePlayerUpdate(PlayerUpdateEvent.Params params);
    }

    @AllArgsConstructor
    public enum KeepSprint {
        None(()->{},()->{}),
        Vanilla(()->{},()->{}),
        // thanks badaiim <3
        Hypixel(
                () -> {
                    PacketUtil.sendPacket(new ClientCommandC2SPacket(C.p(), ClientCommandC2SPacket.Mode.STOP_SPRINTING));
                    PacketUtil.sendPacket(new PlayerInputC2SPacket(new PlayerInput(false, false, false, false, false, false, true)));
                },
                () -> {
                    PacketUtil.sendPacket(new ClientCommandC2SPacket(C.p(), ClientCommandC2SPacket.Mode.START_SPRINTING));
                    PacketUtil.sendPacket(new PlayerInputC2SPacket(new PlayerInput(
                            KeyboardInputEvent.lastInput.forwardKey,
                            KeyboardInputEvent.lastInput.backKey,
                            KeyboardInputEvent.lastInput.leftKey,
                            KeyboardInputEvent.lastInput.rightKey,
                            KeyboardInputEvent.lastInput.jumpKey,
                            KeyboardInputEvent.lastInput.sneakKey,
                            true
                    )));
                    C.p().swingHand(Hand.MAIN_HAND);
                }
        );

        public final Runnable preAttack;
        public final Runnable postAttack;
    }

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {

    }
}

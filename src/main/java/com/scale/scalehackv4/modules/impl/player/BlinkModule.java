package com.scale.scalehackv4.modules.impl.player;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.client.ClientTickEvent;
import com.scale.scalehackv4.events.impl.client.SubModuleToggledEvent;
import com.scale.scalehackv4.events.impl.player.PacketEvent;
import com.scale.scalehackv4.events.impl.player.RotationEvent;
import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.ChatUtil;
import com.scale.scalehackv4.utils.client.MultiSelectEnum;
import com.scale.scalehackv4.utils.client.MultiSelectUtil;
import com.scale.scalehackv4.utils.client.PacketUtil;
import com.scale.scalehackv4.utils.player.BlinkUtil;
import com.scale.scalehackv4.utils.player.PlayerUtil;
import com.scale.scalehackv4.utils.player.RotationUtil;
import com.scale.scalehackv4.utils.player.WorldUtil;
import lombok.AllArgsConstructor;
import net.minecraft.item.BlockItem;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.SetCursorItemS2CPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

import java.util.function.Predicate;

@RegisterModule(
        name = "Blink",
        category = Category.PLAYER
)
public class BlinkModule extends Module {
    @RegisterSubModule(name = "Outgoing", hasKeybind = true)
    public boolean outgoing = true;
    @RegisterSubModule(name = "Outgoing Delay", parent = "Outgoing", increment = 10, max = 1000, hasKeybind = true)
    public int outgoingDelay = 0;
    @RegisterSubModule(name = "Disable Outgoing Conditions")
    public static DisableOutgoingConditions conditions = DisableOutgoingConditions.Attack;
    @RegisterSubModule(name = "Re-enable Outgoing Delay", parent = "Outgoing", increment = 10, max = 1000, hasKeybind = true)
    public int renableOutgoingDelay = 500;

    @AllArgsConstructor
    public enum DisableOutgoingConditions implements MultiSelectEnum {
        Swing(packet -> packet instanceof HandSwingC2SPacket
                && C.mc.crosshairTarget instanceof BlockHitResult b
                && b.getType() == HitResult.Type.MISS
        ),
        Chat_Message(packet -> packet instanceof ChatMessageC2SPacket || packet instanceof CommandExecutionC2SPacket),
        Attack(packet -> packet instanceof PlayerInteractEntityC2SPacket),
        Finish_Breaking_Block(packet -> packet instanceof PlayerActionC2SPacket action
                && action.getAction() == PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK),
        In_GUI(packet -> (packet instanceof ClientCommandC2SPacket command && command.getMode() == ClientCommandC2SPacket.Mode.OPEN_INVENTORY)
                || (packet instanceof ClickSlotC2SPacket)
        ),

        Interact_Block(packet -> packet instanceof PlayerInteractBlockC2SPacket interact
                && interact.getBlockHitResult().getType() != HitResult.Type.MISS
                && !(WorldUtil.isBlockHitResultValid(interact.getBlockHitResult()) || !(C.p().getMainHandStack().getItem() instanceof BlockItem))
        ),
        Interact_Item(packet -> packet instanceof PlayerInteractItemC2SPacket interactItem),
        Place_Block(packet -> packet instanceof PlayerInteractBlockC2SPacket interact
                && interact.getBlockHitResult().getType() != HitResult.Type.MISS
                && (WorldUtil.isBlockHitResultValid(interact.getBlockHitResult()) && (C.p().getMainHandStack().getItem() instanceof BlockItem))),

        Crouch(packet -> packet instanceof PlayerInputC2SPacket(net.minecraft.util.PlayerInput input) && input.sneak());

        public static long lastDisableTime = 0;
        public final Predicate<Packet<?>> onPacket;
    }


    @RegisterSubModule(name = "Incoming", hasKeybind = true)
    public boolean incoming = true;
    @RegisterSubModule(name = "Incoming Delay", parent = "Incoming", increment = 10, max = 1000, hasKeybind = true)
    public int incomingDelay = 0;

    public final Event onSettingChange = new SubModuleToggledEvent(this, params -> {
        if (params.field.getName().equals("incoming")) {
            if (!incoming) BlinkUtil.pushBlink(false, true, this.getClass(), null, incomingDelay);
            else BlinkUtil.popBlink(false, true, this.getClass());
        }
        else if (params.field.getName().equals("outgoingDelay")) {
            BlinkUtil.pushBlink(outgoing, false, this.getClass(), null, outgoingDelay);
        }
        else if (params.field.getName().equals("incomingDelay")) {
            BlinkUtil.pushBlink(false, incoming, this.getClass(), null, incomingDelay);
        }
       return false;
    });

    public final Event checkOutgoingConditions = new PacketEvent.Sent(this, params -> {
        if (!C.isInGame()) return false;

        if (!BlinkUtil.isBlinking(true, false, this.getClass())) {
            if (outgoing && System.currentTimeMillis() - DisableOutgoingConditions.lastDisableTime >= renableOutgoingDelay) {
                BlinkUtil.pushBlink(true, false, this.getClass(), null, outgoingDelay);
            }

            return false;
        }

        if (!BlinkUtil.isBlinking(true, false, params.packet)) return false;

        for (DisableOutgoingConditions condition : DisableOutgoingConditions.values()) {
            if (MultiSelectUtil.isEnabled(condition) && condition.onPacket.test(params.packet)) {
                DisableOutgoingConditions.lastDisableTime = System.currentTimeMillis();
                BlinkUtil.popBlink(true, false, this.getClass());

                return false;
            }
        }
        return false;
    });

    @Override
    public void onEnable() {
        BlinkUtil.pushBlink(outgoing, false, this.getClass(), null, outgoingDelay);
        BlinkUtil.pushBlink(false, incoming, this.getClass(), null, incomingDelay);
    }

    @Override
    public void onDisable() {
        BlinkUtil.popBlink(true, true, this.getClass());
    }
}

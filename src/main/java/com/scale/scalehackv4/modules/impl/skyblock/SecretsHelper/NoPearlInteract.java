package com.scale.scalehackv4.modules.impl.skyblock.SecretsHelper;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.player.PacketEvent;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.ModuleUtil;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;

public class NoPearlInteract extends SecretsHelper {
    @RegisterSubModule(name = "Cancel Pearl Interact")
    public static boolean cancelPearlInteract = true;

    private final Event onSendPacket = new PacketEvent.Sent(ModuleUtil.getModule(SecretsHelper.class), params -> {
        return C.isInGame() && cancelPearlInteract && C.p().getMainHandStack().getItem() == Items.ENDER_PEARL && params.packet instanceof PlayerInteractBlockC2SPacket;
    });
}
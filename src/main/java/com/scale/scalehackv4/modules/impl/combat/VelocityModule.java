package com.scale.scalehackv4.modules.impl.combat;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.player.PacketEvent;
import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;

@RegisterModule(
        name = "Velocity",
        category = Category.COMBAT
)
public class VelocityModule extends Module {
    private final Event cancelVelocity = new PacketEvent.Received(this, (params -> {
        if (params.packet instanceof EntityVelocityUpdateS2CPacket velocityPacket) {
            return true;
        }

        return false;
    }));

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {

    }
}

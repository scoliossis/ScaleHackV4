package com.scale.scalehackv4.modules.impl.player;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.client.ClientTickEvent;
import com.scale.scalehackv4.events.impl.client.SubModuleToggledEvent;
import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.player.BlinkUtil;

@RegisterModule(
        name = "Blink",
        category = Category.PLAYER
)
public class BlinkModule extends Module {
    @RegisterSubModule(name = "Outgoing", hasKeybind = true)
    public boolean outgoing = true;
    @RegisterSubModule(name = "Outgoing Delay", min = 0, max = 1000, hasKeybind = true)
    public int outgoingDelay = 0;

    @RegisterSubModule(name = "Incoming", hasKeybind = true)
    public boolean incoming = true;
    @RegisterSubModule(name = "Incoming Delay", min = 0, max = 1000, hasKeybind = true)
    public int incomingDelay = 0;

    public final Event onClientTick = new ClientTickEvent(this, params -> {

       return false;
    });

    @Override
    public void onEnable() {
        BlinkUtil.pushBlink(true, true, this.getClass(), null, 250);
    }

    @Override
    public void onDisable() {
        BlinkUtil.popBlink(true, true, this.getClass());
    }
}

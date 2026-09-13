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
    @RegisterSubModule(name = "Outgoing Delay", parent = "Outgoing", increment = 10, max = 1000, hasKeybind = true)
    public int outgoingDelay = 0;

    @RegisterSubModule(name = "Incoming", hasKeybind = true)
    public boolean incoming = true;
    @RegisterSubModule(name = "Incoming Delay", parent = "Incoming", increment = 10, max = 1000, hasKeybind = true)
    public int incomingDelay = 0;

    public final Event onClientTick = new SubModuleToggledEvent(this, params -> {
        if (params.field.getName().equals("outgoing")) {
            if (!outgoing) BlinkUtil.pushBlink(true, false, this.getClass(), null, outgoingDelay);
            else BlinkUtil.popBlink(true, false, this.getClass());
        }
        else if (params.field.getName().equals("incoming")) {
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

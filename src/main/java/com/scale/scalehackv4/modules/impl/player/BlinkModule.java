package com.scale.scalehackv4.modules.impl.player;

import com.scale.scalehackv4.events.Event;
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
    @RegisterSubModule(name = "Incoming", hasKeybind = true)
    public boolean incoming = true;
    @RegisterSubModule(name = "Send Timer")
    public double sendTimer = 0.5;

    private final Event onToggleBlink = new SubModuleToggledEvent(this, params -> {
        if (params.field.getName().equals("outgoing")) {
            onToggleOutgoing((boolean) params.value);
            return false;
        }
        if (params.field.getName().equals("incoming")) {
            onToggleIncoming((boolean) params.value);
            return false;
        }

        return false;
    });

    private void onToggleOutgoing(boolean outgoing) {
        if (outgoing) BlinkUtil.pushBlink(true, false, this.getClass(), sendTimer);
        else BlinkUtil.popBlink(true, false, this.getClass());
    }
    private void onToggleIncoming(boolean incoming) {
        if (incoming) BlinkUtil.pushBlink(false, true, this.getClass(), sendTimer);
        else BlinkUtil.popBlink(false, true, this.getClass());
    }

    @Override
    public void onEnable() {
        onToggleOutgoing(outgoing);
        onToggleIncoming(incoming);
    }

    @Override
    public void onDisable() {
        BlinkUtil.popBlink(true, true, this.getClass());
    }
}

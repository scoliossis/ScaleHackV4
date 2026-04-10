package com.scale.scalehackv4.modules.impl.player;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.client.SubModuleToggledEvent;
import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.modules.impl.skyblock.SecretsHelper.SecretsHelper;
import com.scale.scalehackv4.utils.client.ModuleUtil;
import com.scale.scalehackv4.utils.player.BlinkUtil;

@RegisterModule(
        name = "Blink",
        category = Category.PLAYER
)
public class BlinkModule extends Module {
    @RegisterSubModule(name = "Outgoing", hasKeybind = true)
    public static boolean outgoing = true;
    @RegisterSubModule(name = "Incoming", hasKeybind = true)
    public static boolean incoming = true;

    private final Event onToggleSecretAura = new SubModuleToggledEvent(ModuleUtil.getModule(SecretsHelper.class), params -> {
        if (params.field.getName().equals("outgoing")) {
            if ((boolean) params.value) BlinkUtil.pushBlink(true, false);
            else BlinkUtil.popBlink(true, false);
            return false;
        }
        if (params.field.getName().equals("incoming")) {
            if ((boolean) params.value) BlinkUtil.pushBlink(false, true);
            else BlinkUtil.popBlink(false, true);
            return false;
        }

        return false;
    });

    @Override
    public void onEnable() {
        BlinkUtil.pushBlink(outgoing, incoming);
    }

    @Override
    public void onDisable() {
        BlinkUtil.popBlink(outgoing, incoming);
    }
}

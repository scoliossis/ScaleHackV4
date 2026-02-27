package com.scale.scalehackv4.modules.impl.skyblock.SecretsHelper;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.player.PlayerUpdateEvent;
import com.scale.scalehackv4.utils.client.ModuleUtil;

public class SecretAura extends SecretsHelper {
    private final Event onPlayerUpdate = new PlayerUpdateEvent(ModuleUtil.getModule(SecretsHelper.class), params -> {

        return false;
    });
}

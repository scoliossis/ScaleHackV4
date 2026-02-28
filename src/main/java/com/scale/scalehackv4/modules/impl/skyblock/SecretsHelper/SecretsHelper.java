package com.scale.scalehackv4.modules.impl.skyblock.SecretsHelper;

import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;

@RegisterModule(
        name = "Secrets Helper",
        category = Category.SKYBLOCK
)
public class SecretsHelper extends Module {
    @Override public void onEnable() {}
    @Override public void onDisable() {
        SecretAura.INTERACTED_POSITIONS.clear();
        AutoRoutes.forceStop();
    }
}
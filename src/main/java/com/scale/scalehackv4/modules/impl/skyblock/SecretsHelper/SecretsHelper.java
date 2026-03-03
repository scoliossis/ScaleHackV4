package com.scale.scalehackv4.modules.impl.skyblock.SecretsHelper;

import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;

@RegisterModule(
        name = "Secrets Helper",
        category = Category.SKYBLOCK
)
public class SecretsHelper extends Module {
    @Override public void onEnable() {
        if (SecretAura.secretAura) SecretAura.onSetSecretAura(true);
    }
    @Override public void onDisable() {
        SecretAura.onSetSecretAura(false);
        AutoRoutes.forceStop();
    }
}
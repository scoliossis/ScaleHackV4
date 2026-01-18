package com.scale.scalehackv4.modules;

import com.scale.scalehackv4.utils.client.ChatUtil;
import lombok.Getter;
import lombok.Setter;

public abstract class Module {
    @Getter @Setter private RegisterModule annotation;

    @Getter private boolean enabled = false;
    public void setEnabled(boolean enabled) {
        if (enabled != this.enabled) {
            if (enabled) {
                // todo: notifications
                ChatUtil.prefixMessage("&aEnabled &f" + this.annotation.name());
                onEnable();
            }
            else {
                ChatUtil.prefixMessage("&cDisabled &f" + this.annotation.name());
                onDisable();
            }
        }
        this.enabled = enabled;
    }

    public abstract void onEnable();
    public abstract void onDisable();
}

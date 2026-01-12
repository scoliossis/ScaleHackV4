package com.scale.scalehackv4.modules;

import lombok.Getter;
import lombok.Setter;

public abstract class Module {
    @Getter private boolean enabled = false;
    public void setEnabled(boolean enabled) {
        if (enabled != this.enabled) {
            if (enabled) onEnable();
            else onDisable();
        }
        this.enabled = enabled;
    }

    public abstract void onEnable();
    public abstract void onDisable();
}

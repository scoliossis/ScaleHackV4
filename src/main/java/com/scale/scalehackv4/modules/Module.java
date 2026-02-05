package com.scale.scalehackv4.modules;

import com.scale.scalehackv4.Main;
import com.scale.scalehackv4.utils.client.ChatUtil;
import com.scale.scalehackv4.utils.client.ModuleUtil;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

public abstract class Module {
    @Getter @Setter private RegisterModule annotation;

    public int x = 0;
    public int y = 0;
    public int width = 0;
    public int height = 0;

    public double scrolledAmount = 0;

    @Getter private final ArrayList<SubModule> children = new ArrayList<>();

    @RegisterSubModule(name = "Enabled", hasKeybind = true)
    @Getter public boolean enabled = false;

    public void setEnabled(boolean enabled) {
        if (enabled != this.enabled) {
            this.enabled = enabled;
            ModuleUtil.saveConfig(Main.BASE_CONFIG);

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
    }

    public String arrayListExtraInfo() {
        return "";
    }

    public abstract void onEnable();
    public abstract void onDisable();
}

package com.scale.scalehackv4.modules;

import com.scale.scalehackv4.Main;
import com.scale.scalehackv4.modules.impl.client.ClickGuiModule;
import lombok.AllArgsConstructor;
import net.minecraft.util.Identifier;

import java.util.HashMap;

@AllArgsConstructor
public enum Category {
    COMBAT,
    MOVEMENT,
    RENDER,
    PLAYER,
    OTHER,
    FUN,
    CLIENT,
    CONFIGS;

    private final HashMap<String, Identifier> icons = new HashMap<>();

    public Identifier getTexture() {
        Identifier icon = icons.get(ClickGuiModule.iconPack.name() + this.name());
        if (icon != null) return icon;

        icon = Identifier.of(Main.MOD_ID,
                "clickgui/icons/"
                        + ClickGuiModule.iconPack.name().toLowerCase()
                        + "/"
                        + this.name().toLowerCase()
                        + ".png"
        );
        icons.put(ClickGuiModule.iconPack.name() + this.name(), icon);
        return icon;
    }
}
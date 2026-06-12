package com.scale.scalehackv4.modules;

import lombok.AllArgsConstructor;
import net.minecraft.util.Identifier;

@AllArgsConstructor
public enum Category {
    COMBAT(Identifier.of("scalehackv4", "clickgui/combat.png")),
    MOVEMENT(Identifier.of("scalehackv4", "clickgui/movement.png")),
    RENDER(Identifier.of("scalehackv4", "clickgui/render.png")),
    PLAYER(Identifier.of("scalehackv4", "clickgui/player.png")),
    OTHER(Identifier.of("scalehackv4", "clickgui/other.png")),
    FUN(Identifier.of("scalehackv4", "clickgui/fun.png")),
    CLIENT(Identifier.of("scalehackv4", "clickgui/client.png")),
    CONFIGS(Identifier.of("scalehackv4", "clickgui/configs.png"));

    public final Identifier texture;
}
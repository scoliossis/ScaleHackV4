package com.scale.scalehackv4.modules;

import lombok.AllArgsConstructor;
import net.minecraft.util.Identifier;

@AllArgsConstructor
public enum Category {
    COMBAT(Identifier.of("scalehackv4", "clickgui/combat.png")),
    MOVEMENT(Identifier.of("scalehackv4", "clickgui/movement.png")),
    RENDER(Identifier.of("scalehackv4", "clickgui/render.png")),
    SKYBLOCK(Identifier.of("scalehackv4", "clickgui/skyblock.png")),
    PLAYER(Identifier.of("scalehackv4", "clickgui/player.png")),
    OTHER(Identifier.of("scalehackv4", "clickgui/other.png")),
    CLIENT(Identifier.of("scalehackv4", "clickgui/client.png"));

    public Identifier texture;
}

package com.scale.scalehackv4.events.impl.render;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.modules.Module;
import lombok.AllArgsConstructor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.text.Text;

import java.util.function.Predicate;

public class RenderScoreboardEvent extends Event {
    @Environment(EnvType.CLIENT)
    public record SidebarEntry(Text name, Text score) {
    }

    public static SidebarEntry[] LAST_SIDEBAR = new SidebarEntry[0];

    @AllArgsConstructor
    public static class Params {
        // spelt wrong by minecraft btw.
        public SidebarEntry[] sidebarEntrys;
    }

    public RenderScoreboardEvent(Module parent, Predicate<Params> predicate) {
        super(parent, predicate);
    }
}

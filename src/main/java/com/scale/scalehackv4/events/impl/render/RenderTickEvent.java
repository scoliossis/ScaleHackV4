package com.scale.scalehackv4.events.impl.render;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.modules.Module;
import lombok.AllArgsConstructor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import java.util.function.Predicate;

public class RenderTickEvent extends Event {
    @AllArgsConstructor
    public static class Params {
        public DrawContext drawContext;
        public RenderTickCounter renderTickCounter;
    }

    public RenderTickEvent(Module module, Predicate<Params> predicate) {
        super(module, predicate);
    }
}

package com.scale.scalehackv4.events.impl.render;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.modules.Module;

import java.util.function.Predicate;

public class RenderWorldEvent extends Event {
    public record Params() {
    }

    public RenderWorldEvent(Module module, Predicate<Params> predicate) {
        super(module, predicate);
    }
}
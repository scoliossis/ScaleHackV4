package com.scale.scalehackv4.events.impl.render;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.modules.Module;
import lombok.AllArgsConstructor;

import java.util.function.Predicate;

public class MouseDraggedEvent extends Event {
    @AllArgsConstructor
    public static class Params {
        public double deltaX, deltaY;
    }

    public MouseDraggedEvent(Module parent, Predicate<Params> predicate, int priority) {
        super(parent, predicate, priority);
    }

    public MouseDraggedEvent(Module parent, Predicate<Params> predicate) {
        super(parent, predicate);
    }
}

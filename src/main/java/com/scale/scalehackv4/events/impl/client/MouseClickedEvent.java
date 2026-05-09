package com.scale.scalehackv4.events.impl.client;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.modules.Module;
import lombok.AllArgsConstructor;
import net.minecraft.client.input.MouseInput;

import java.util.function.Predicate;

public class MouseClickedEvent extends Event {
    @AllArgsConstructor
    public static class Params {
        public long window;
        public MouseInput input;
        public int action;
    }

    public MouseClickedEvent(Module module, Predicate<Params> predicate, int priority) {
        super(module, predicate, priority);
    }
    public MouseClickedEvent(Module module, Predicate<Params> predicate) {
        super(module, predicate);
    }
}

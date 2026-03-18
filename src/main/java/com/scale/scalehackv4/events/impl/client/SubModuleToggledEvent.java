package com.scale.scalehackv4.events.impl.client;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.modules.Module;
import lombok.AllArgsConstructor;

import java.lang.reflect.Field;
import java.util.function.Predicate;

public class SubModuleToggledEvent extends Event {
    @AllArgsConstructor
    public static class Params {
        public Field field;
        public Object value;
    }

    public SubModuleToggledEvent(Module module, Predicate<Params> predicate, int priority) {
        super(module, predicate, priority);
    }

    public SubModuleToggledEvent(Module module, Predicate<Params> predicate) {
        super(module, predicate);
    }
}

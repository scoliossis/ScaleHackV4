package com.scale.scalehackv4.events.impl.client;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.modules.Module;

import java.util.function.Predicate;

public class ClientTickEvent extends Event {
    public ClientTickEvent(Module module, Predicate<?> predicate, int priority) {
        super(module, predicate, priority);
    }

    public ClientTickEvent(Module module, Predicate<?> predicate) {
        super(module, predicate);
    }
}
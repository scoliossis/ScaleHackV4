package com.scale.scalehackv4.events.impl.skyblock;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.modules.Module;

import java.util.function.Predicate;

public class DungeonStartEvent extends Event {
    public static class Params {}

    public DungeonStartEvent(com.scale.scalehackv4.modules.Module parent, Predicate<Params> predicate, int priority) {
        super(parent, predicate, priority);
    }

    public DungeonStartEvent(Module parent, Predicate<Params> predicate) {
        super(parent, predicate);
    }
}
package com.scale.scalehackv4.events.impl.skyblock;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.modules.Module;

import java.util.function.Predicate;

// todo: floor check
public class DungeonJoinEvent extends Event {
    public static class Params {}

    public DungeonJoinEvent(com.scale.scalehackv4.modules.Module parent, Predicate<Params> predicate, int priority) {
        super(parent, predicate, priority);
    }

    public DungeonJoinEvent(Module parent, Predicate<Params> predicate) {
        super(parent, predicate);
    }
}

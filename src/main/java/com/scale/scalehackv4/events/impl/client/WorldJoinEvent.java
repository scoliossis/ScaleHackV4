package com.scale.scalehackv4.events.impl.client;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.modules.Module;
import lombok.AllArgsConstructor;

import java.util.function.Predicate;

public class WorldJoinEvent extends Event {
    @AllArgsConstructor
    public static class Params {
    }

    public WorldJoinEvent(Module parent, Predicate<Params> predicate) {
        super(parent, predicate);
    }
}


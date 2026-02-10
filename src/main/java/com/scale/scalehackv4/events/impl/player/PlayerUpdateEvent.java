package com.scale.scalehackv4.events.impl.player;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.modules.Module;

import java.util.function.Predicate;

public class PlayerUpdateEvent extends Event {
    public static class Params {}

    public PlayerUpdateEvent(Module parent, Predicate<Params> predicate) {
        super(parent, predicate);
    }
}

package com.scale.scalehackv4.events.impl.player;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.modules.Module;

import java.util.function.Predicate;

public class LeftClickedEvent extends Event {
    public LeftClickedEvent(Module parent, Predicate<?> predicate) {
        super(parent, predicate);
    }
}

package com.scale.scalehackv4.events;

import com.scale.scalehackv4.modules.Module;

import java.util.ArrayList;
import java.util.function.Predicate;

// todo: probably a weird way to make an event bus, will probably hate myself later
//  add priority
public abstract class Event {
    public final Module module;
    public final Predicate<Object> predicate;

    public Event(Module module, Predicate<?> predicate) {
        this.module = module;
        this.predicate = (Predicate<Object>) predicate;

        ArrayList<Event> events = Bus.events.get(this.getClass());
        if (events == null) events = new ArrayList<>();

        events.add(this);

        Bus.events.put(this.getClass(), events);
    }
}

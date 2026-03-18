package com.scale.scalehackv4.events;

import com.scale.scalehackv4.modules.Module;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Predicate;

// todo: probably a weird way to make an event bus, will probably hate myself later
public abstract class Event {
    public static final int DEFAULT_PRIORITY = 1000;

    public final Module module;
    public final Predicate<Object> predicate;
    public int priority;

    public Event(Module module, Predicate<?> predicate, int priority) {
        this.module = module;
        this.predicate = (Predicate<Object>) predicate;
        this.priority = priority;

        ArrayList<Event> events = Bus.events.get(this.getClass());
        if (events == null) events = new ArrayList<>();

        events.add(this);
        events.sort(Comparator.comparingInt(e -> e.priority));

        Bus.events.put(this.getClass(), events);
    }

    public Event(Module module, Predicate<?> predicate) {
        this(module, predicate, DEFAULT_PRIORITY);
    }
}

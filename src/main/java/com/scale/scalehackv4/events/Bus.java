package com.scale.scalehackv4.events;

import java.util.ArrayList;
import java.util.HashMap;

public class Bus {
    public static final HashMap<Class<? extends Event>, ArrayList<Event>> events = new HashMap<>();
    private static final HashMap<Class<? extends Event>, Boolean> eventCancelled = new HashMap<>();

    public static boolean isEventCancelled(Class<? extends Event> eventType) {
        return eventCancelled.getOrDefault(eventType, false);
    }

    public static boolean post(Class<? extends Event> eventType, Object params) {
        if (!events.containsKey(eventType)) return false;
        eventCancelled.remove(eventType);

        boolean cancelled = false;

        for (Event event : events.get(eventType)) {
            if (event.module != null && !event.module.isEnabled()) continue;

            cancelled |= event.predicate.test(params);
            if (cancelled) eventCancelled.put(eventType, true);
        }

        return cancelled;
    }
}

package com.scale.scalehackv4.events;

import java.util.ArrayList;
import java.util.HashMap;

public class Bus {
    public static HashMap<Class<? extends Event>, ArrayList<Event>> events = new HashMap<>();

    public static boolean post(Class<?> eventType, Object params) {
        if (!events.containsKey(eventType)) return false;

        boolean cancelled = false;

        for (Event event : events.get(eventType)) {
            if (event.module != null && !event.module.isEnabled()) continue;

            cancelled |= event.predicate.test(params);
        }

        return cancelled;
    }
}

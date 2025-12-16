package com.scale.scalehackv4.events;

import java.util.ArrayList;
import java.util.HashMap;

public class Bus<T> {
    public static HashMap<Class<? extends Event>, ArrayList<Event>> events = new HashMap<>();

    public static void post(Class<?> eventType, Object params) {
        if (!events.containsKey(eventType)) {
            System.out.println("No events of type " + eventType.getName() + " registered!");
            return;
        }

        for (Event e : events.get(eventType)) {
            e.predicate.test(params);
        }
    }
}

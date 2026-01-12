package com.scale.scalehackv4.events;

import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.ModuleManager;

import java.util.ArrayList;
import java.util.HashMap;

public class Bus {
    public static HashMap<Class<? extends Event>, ArrayList<Event>> events = new HashMap<>();

    public static void post(Class<?> eventType, Object params) {
        if (!events.containsKey(eventType)) return;

        for (Event event : events.get(eventType)) {
            if (event.module != null && !event.module.isEnabled()) continue;

            event.predicate.test(params);
        }
    }
}

package com.scale.scalehackv4.modules;

import com.scale.scalehackv4.events.RegisterListeners;
import com.scale.scalehackv4.utils.C;

import java.util.HashMap;

public class ModuleManager {
    private static final HashMap<Class<?>, Module> MODULES = new HashMap<>();

    public static void init() {
        C.REFLECTIONS.getTypesAnnotatedWith(RegisterModule.class).forEach(clazz -> {
            try {
                MODULES.put(clazz, (Module) clazz.getDeclaredConstructor().newInstance());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        C.REFLECTIONS.getTypesAnnotatedWith(RegisterListeners.class).forEach(clazz -> {
            try {
                clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}

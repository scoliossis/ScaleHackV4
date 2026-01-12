package com.scale.scalehackv4.modules;

import com.scale.scalehackv4.events.RegisterListeners;
import com.scale.scalehackv4.utils.C;
import lombok.Getter;

import java.util.HashMap;

public class ModuleManager {
    private static final HashMap<Class<?>, Module> MODULES = new HashMap<>();

    public static void init() {
        C.REFLECTIONS.getTypesAnnotatedWith(RegisterModule.class).forEach(clazz -> {
            try {
                RegisterModule annotation = clazz.getAnnotation(RegisterModule.class);
                Module m = (Module) clazz.getDeclaredConstructor().newInstance();
                m.setEnabled(annotation.enabledByDefault());

                MODULES.put(clazz, m);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        C.REFLECTIONS.getTypesAnnotatedWith(RegisterListeners.class).forEach(clazz -> {
            try {
                // creating a new instance of the class is needed to make the event bus post!
                clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static Module getModule(Class<?> clazz) {
        return MODULES.get(clazz);
    }
}

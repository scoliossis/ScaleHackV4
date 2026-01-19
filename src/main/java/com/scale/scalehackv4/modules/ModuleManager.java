package com.scale.scalehackv4.modules;

import com.scale.scalehackv4.events.RegisterListeners;
import com.scale.scalehackv4.utils.C;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import me.xdrop.fuzzywuzzy.model.ExtractedResult;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;

public class ModuleManager {
    private static final HashMap<Class<?>, Module> MODULES = new HashMap<>();

    public static void init() {
        C.REFLECTIONS.getTypesAnnotatedWith(RegisterModule.class).forEach(clazz -> {
            try {
                RegisterModule annotation = clazz.getAnnotation(RegisterModule.class);
                Module m = (Module) clazz.getDeclaredConstructor().newInstance();
                m.setAnnotation(annotation);
                m.setEnabled(annotation.enabledByDefault());

                for (Field field : clazz.getDeclaredFields()) {
                    RegisterSubModule registerSubModule = field.getAnnotation(RegisterSubModule.class);

                    if (registerSubModule != null) {
                        SubModule subModule = new SubModule(m, field);

                        if (!registerSubModule.parent().isEmpty()) {
                            // todo: children have to be below parents in the file, fix later if needed <3
                            for (SubModule sm : m.getChildren()) {
                                if (registerSubModule.parent().equals(sm.getAnnotation().name())) {
                                    sm.getChildren().add(subModule);
                                    subModule.setParent(sm);
                                    break;
                                }
                            }
                        }
                        m.getChildren().add(subModule);
                    }
                }


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

    public static List<Module> getModules() {
        return ModuleManager.MODULES.values().stream().toList();
    }

    public static List<ExtractedResult> getModules(String text, int closeness) {
        List<String> moduleNames = MODULES.values().stream().map(e -> e.getAnnotation().name()).toList();

        return FuzzySearch.extractSorted(text.toLowerCase(), moduleNames, closeness);
    }

    public static Module getModule(String text, int closeness) {
        List<ExtractedResult> results = getModules(text, closeness);
        return results.getFirst().getIndex() == -1 ? null : getModules().get(results.getFirst().getIndex());
    }


    public static List<Module> getModulesByCategory(Category category) {
        return ModuleManager.MODULES.values().stream().filter(e -> e.getAnnotation().category() == category).toList();
    }
}

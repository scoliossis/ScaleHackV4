package com.scale.scalehackv4.utils.client;

import com.google.gson.internal.LinkedTreeMap;
import com.scale.scalehackv4.Main;
import com.scale.scalehackv4.events.RegisterListeners;
import com.scale.scalehackv4.modules.*;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.screens.clickgui.ClickGuiScreen;
import com.scale.scalehackv4.screens.clickgui.components.ModuleRenderer;
import com.scale.scalehackv4.utils.C;
import lombok.AllArgsConstructor;
import me.xdrop.fuzzywuzzy.FuzzySearch;
import me.xdrop.fuzzywuzzy.model.ExtractedResult;
import org.apache.commons.io.FileUtils;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

public class ModuleUtil {
    private static final HashMap<Class<?>, Module> MODULES = new HashMap<>();

    public static final HashMap<Integer, List<SubModule>> KEYBINDS = new HashMap<>();

    public static void init() {
        C.REFLECTIONS.getTypesAnnotatedWith(RegisterModule.class).forEach(clazz -> {
            try {
                RegisterModule annotation = clazz.getAnnotation(RegisterModule.class);
                Class<? extends Module> moduleClazz = (Class<? extends Module>) clazz;

                if (annotation == null) {
                    annotation = clazz.getSuperclass().getAnnotation(RegisterModule.class);
                    moduleClazz = (Class<? extends Module>) clazz.getSuperclass();
                }

                Module m;
                if (MODULES.containsKey(moduleClazz)) {
                    clazz.getDeclaredConstructor().newInstance();
                    m = MODULES.get(moduleClazz);
                }
                else {
                    m = moduleClazz.getDeclaredConstructor().newInstance();

                    m.setAnnotation(annotation);
                    m.setEnabled(annotation.enabledByDefault());

                    // dumb blehhh, but it isnt broken yet, so i wont fix it.
                    SubModule enabled = new SubModule(m, moduleClazz.getField("enabled"));
                    m.getChildren().add(enabled);
                }

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

                MODULES.put(moduleClazz, m);
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

        loadConfig(Main.BASE_CONFIG);
        loadGuiPositions();
    }

    private static final String GUI_POSITIONS_FILE = "GUI_POSITIONS";

    // gson time!! hip hip hooray!!
    public static void saveGuiPositions() {
        HashMap<String, Rectangle> positions = new HashMap<>();
        for (Module module : getModules()) {
            Rectangle rect = new Rectangle(module.x, module.y, module.width, module.height);
            positions.put(module.getAnnotation().name(), rect);
        }

        saveConfig(Main.CONFIG_PATH, GUI_POSITIONS_FILE, C.gson.toJson(positions));
    }

    public static void loadGuiPositions() {
        String configFileText = readConfig(GUI_POSITIONS_FILE);

        if (configFileText != null) {
            boolean failed = false;
            HashMap<String, LinkedTreeMap<String, Object>> positions = C.gson.fromJson(configFileText, HashMap.class);
            for (Module module : getModules()) {
                LinkedTreeMap<String, Object> rect = positions.get(module.getAnnotation().name());

                if (rect != null) {
                    module.x = getInt("x", rect);
                    module.y = getInt("y", rect);
                    module.width = getInt("width", rect);
                    module.height = getInt("height", rect);
                }
                else {
                    // todo: maybe try slotting it in, instead of giving up and resetting positions
                    failed = true;
                    break;
                }
            }

            if (!failed) return;
        }

        for (Category category : Category.values()) {
            List<Module> categoryModules = getModulesByCategory(category);
            int modules = categoryModules.size();

            if (modules == 0) continue;

            int squareWidth = (int) Math.round(Math.sqrt(modules));
            int squareHeight = (int) Math.ceil(Math.sqrt(modules));

            int moduleWidth = (ModuleRenderer.MODULES_SECTION_WIDTH / squareWidth) - ModuleRenderer.MODULE_X_GAP - (ModuleRenderer.MODULE_X_GAP / squareWidth);
            int moduleHeight = (ClickGuiScreen.HEIGHT / squareHeight) - ModuleRenderer.MODULE_Y_GAP - (ModuleRenderer.MODULE_Y_GAP / squareHeight);

            for (int i = 0; i < modules; i++) {
                Module module = categoryModules.get(i);

                int xOrdinal = i % squareWidth;
                int yOrdinal = i / squareWidth;

                module.x = (xOrdinal * moduleWidth) + ((xOrdinal+1) * ModuleRenderer.MODULE_X_GAP);
                module.y = (yOrdinal * moduleHeight) + ((yOrdinal+1) * ModuleRenderer.MODULE_Y_GAP);

                module.width = moduleWidth;
                module.height = moduleHeight;
            }
        }
    }

    public static boolean isEnabled(Class<?> clazz) {
        return getModule(clazz).isEnabled();
    }

    public static Module getModule(Class<?> clazz) {
        return MODULES.get(clazz);
    }

    public static List<Module> getModules() {
        return new ArrayList<>(ModuleUtil.MODULES.values());
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
        return ModuleUtil.MODULES.values().stream().filter(e -> e.getAnnotation().category() == category).toList();
    }

    // -------------- from https://github.com/scoliossis/ScaleHackOpenSrc ---------------------
    public static void openConfigFolder() {
        try {
            Files.createDirectories(Paths.get(Main.CONFIG_PATH));
            Desktop.getDesktop().open(new File(Main.CONFIG_PATH));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<File> getConfigFiles(String folderPath) {
        return Arrays.asList(Objects.requireNonNull(Paths.get(Main.CONFIG_PATH + folderPath).toFile().listFiles()));
    }


    // god bless https://github.com/google/gson/blob/main/UserGuide.md
    public static void saveConfig(String configName) {
        if (!C.isInGame()) return;

        HashMap<String, LinkedTreeMap<String, Object>> moduleJSON = new HashMap<>();

        List<Module> modules = getModules();

        for (Module m : modules) saveModule(moduleJSON, m);

        saveConfig(Main.CONFIG_PATH, configName, C.gson.toJson(moduleJSON));
    }

    private static void saveModule(HashMap<String, LinkedTreeMap<String, Object>> moduleJSON, Module module) {
        LinkedTreeMap<String, Object> subModules = new LinkedTreeMap<>();

        for (SubModule sm : module.getChildren()) {
            Object value = sm.getField().getType() == boolean.class ? new BooleanModule((Boolean) sm.get(), sm.getKeybind()) : sm.get();
            subModules.put(sm.getAnnotation().name(), value);
        }

        moduleJSON.put(module.getAnnotation().name(), subModules);
    }

    public static String saveModule(Module module) {
        HashMap<String, LinkedTreeMap<String, Object>> moduleJSON = new HashMap<>();

        saveModule(moduleJSON, module);

        return C.gson.toJson(moduleJSON);
    }

    public static void saveModule(Module module, String configName) {
        saveConfig(Main.CONFIG_PATH + module.getAnnotation().name() + "/", configName, saveModule(module));
    }

    public static void saveConfig(String filePath, String fileName, String JSON) {
        try {
            Files.createDirectories(Paths.get(filePath));
            Files.write(Paths.get(filePath + fileName + ".cfg"), JSON.getBytes());
        } catch (IOException e) {
            System.err.println("Failed to save config: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static boolean loadConfig(String configName) {
        String configFileText = readConfig(configName);
        if (configFileText == null) return false;

        for (Module m : MODULES.values()) loadModule(m, configFileText);

        return true;
    }


    public static boolean loadKeybinds(String configName) {
        String configFileText = readConfig(configName);
        if (configFileText == null) return false;

        HashMap<String, LinkedTreeMap<String, Object>> modulesJSON = C.gson.fromJson(configFileText, HashMap.class);

        for (Module m : MODULES.values()) {
            LinkedTreeMap<String, Object> subModules = modulesJSON.get(m.getAnnotation().name());

            for (SubModule sm : m.getChildren()) {
                if (modulesJSON.containsKey(m.getAnnotation().name())) {
                    if (!sm.getAnnotation().hasKeybind()) continue;

                    Object value = subModules.get(sm.getAnnotation().name());
                    LinkedTreeMap<String, Object> booleanMap = (LinkedTreeMap<String, Object>) value;

                    loadKeybind(sm, booleanMap);
                }
            }
        }

        return true;
    }


    public static void loadModule(Module module, String configFileText) {
        HashMap<String, LinkedTreeMap<String, Object>> modulesJSON = C.gson.fromJson(configFileText, HashMap.class);

        if (!modulesJSON.containsKey(module.getAnnotation().name())) {
            System.out.println("Warning: " + module.getAnnotation().name() + " module not found, defaulting to " + module.getAnnotation().enabledByDefault());
            module.setEnabled(module.getAnnotation().enabledByDefault());
        }
        else {
            LinkedTreeMap<String, Object> subModules = modulesJSON.get(module.getAnnotation().name());

            for (SubModule subModule : module.getChildren()) {
                if (!subModules.containsKey(subModule.getAnnotation().name())) {
                    // todo: fix defaulting values
                    System.out.println("Module has no config: " + subModule.getAnnotation().name() + " submodule of " + module.getAnnotation().name() + " not found, defaulting to " + subModule.get());
                    continue;
                }

                Object value = subModules.get(subModule.getAnnotation().name());

                if (subModule.getField().getType().isEnum()) {
                    Enum<?>[] enumConstants = (Enum<?>[]) subModule.getField().getType().getEnumConstants();
                    for (Enum<?> enumConstant : enumConstants)
                        if (enumConstant.name().equals(value)) subModule.set(enumConstant);
                }
                else if (subModule.getField().getType().equals(Color.class)) {
                    LinkedTreeMap<String, Object> colorMap = (LinkedTreeMap<String, Object>) value;
                    Color rgb = new Color(getInt("rgb", colorMap));
                    int alpha = getInt("a", colorMap);

                    subModule.set(new Color(rgb.getRed(), rgb.getGreen(), rgb.getBlue(), alpha));
                }
                else if (subModule.getField().getType() == boolean.class) {
                    LinkedTreeMap<String, Object> booleanMap = (LinkedTreeMap<String, Object>) value;
                    subModule.set(booleanMap.get("enabled").toString().equals("true"));
                    loadKeybind(subModule, booleanMap);
                }
                else {
                    subModule.set(value);
                }
            }
        }
    }

    private static int getInt(String key, LinkedTreeMap<String, Object> map) {
        return (int) Double.parseDouble(map.get(key).toString());
    }

    private static void loadKeybind(SubModule subModule, LinkedTreeMap<String, Object> subModules) {
        subModule.setKeybind(getInt("keybind", subModules));
    }


    public static String readConfig(String configName) {
        try {
            File configFile = new File(Main.CONFIG_PATH + configName + ".cfg");
            if (!Files.exists(configFile.toPath())) {
                ChatUtil.prefixMessage("&cConfig file does not exist: &f" + configName);
                return null;
            }
            return FileUtils.readFileToString(configFile);
        } catch (IOException e) {
            ChatUtil.prefixMessage("&cFailed to read config: " + e.getMessage());
            return null;
        }
    }

    @AllArgsConstructor
    private static class BooleanModule {
        public boolean enabled;
        public int keybind;
    }
}
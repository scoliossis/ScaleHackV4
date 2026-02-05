package com.scale.scalehackv4.modules.impl.client.hud;

import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.client.ModuleUtil;
import com.scale.scalehackv4.utils.render.Draggable;
import com.scale.scalehackv4.utils.render.FontUtil;

import java.awt.*;
import java.util.Comparator;
import java.util.List;

public class ArrayList extends HudModule {
    @RegisterSubModule(
            name = "Array List"
    )
    public static boolean arrayList = true;

    @RegisterSubModule(name = "Font Size", parent = "Array List", min = 5, max = 40)
    public static int fontSize = 10;
    @RegisterSubModule(name = "Gap", parent = "Array List", max = 10)
    public static int yGap = 0;

    @RegisterSubModule(name = "No Capitals", parent = "Array List")
    public static boolean noCapitals = true;
    @RegisterSubModule(name = "No Spaces", parent = "Array List")
    public static boolean noSpaces = true;
    @RegisterSubModule(name = "Show Mode", parent = "Array List")
    public static boolean showMode = true;

    public final Draggable arrayListDraggable = new Draggable(
            "Array List",
            () -> {
                List<Module> modules = ModuleUtil.getModules();
                modules.removeIf(e -> !e.isEnabled());

                if (modules.isEmpty()) return new int[] {0,0};

                modules.sort(Comparator.comparingDouble(e -> -FontUtil.getStringWidth(getModuleName(e), fontSize)));

                int height = FontUtil.getFontHeight(fontSize) + yGap;

                for (int i = 0; i < modules.size(); i++) {
                    Module module = modules.get(i);

                    FontUtil.drawString(getModuleName(module), 0, height*i, fontSize, Color.WHITE, false);
                }

                return new int[] {FontUtil.getStringWidth(getModuleName(modules.getFirst()), fontSize), height*modules.size()};
            },
            e -> ModuleUtil.isEnabled(HudModule.class) && arrayList,
            e -> true
    );

    private static String getModuleName(Module module) {
        String name = module.getAnnotation().name();
        if (noCapitals) name = name.toLowerCase();
        if (noSpaces) name = name.replaceAll(" ", "");
        String arrayListExtraInfo = module.arrayListExtraInfo();
        return name + (arrayListExtraInfo.isEmpty() || !showMode ? "" : " §8[" + arrayListExtraInfo + "]");
    }
}

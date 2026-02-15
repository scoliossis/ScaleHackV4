package com.scale.scalehackv4.modules.impl.client.Hud;

import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.client.ModuleUtil;
import com.scale.scalehackv4.utils.render.Draggable;
import com.scale.scalehackv4.utils.render.FontUtil;
import com.scale.scalehackv4.utils.render.RenderUtil;

import java.util.Comparator;
import java.util.List;

// todo: show keybind submodules
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

                int maxWidth = FontUtil.getStringWidth(getModuleName(modules.getFirst()), fontSize);
                int totalHeight = height*modules.size();

                for (int i = 0; i < modules.size(); i++) {
                    Module module = modules.get(i);

                    int x = isRightToLeft() ? -FontUtil.getStringWidth(getModuleName(module), fontSize) : 0;
                    int y = isBottomToTop() ? -(height*(i+1)) : height*i ;
                    FontUtil.drawString(getModuleName(module), x, y, fontSize, RenderUtil.TRANS[i % RenderUtil.TRANS.length], true);
                }

                return new int[] {isRightToLeft() ? -maxWidth : maxWidth, isBottomToTop() ? -totalHeight : totalHeight};
            },
            e -> ModuleUtil.isEnabled(HudModule.class) && arrayList,
            e -> true
    );

    private boolean isRightToLeft() {
        return arrayListDraggable.x > 0.5;
    }

    private boolean isBottomToTop() {
        return arrayListDraggable.y > 0.5;
    }

    private static String getModuleName(Module module) {
        String name = module.getAnnotation().name();
        if (noCapitals) name = name.toLowerCase();
        if (noSpaces) name = name.replaceAll(" ", "");
        String arrayListExtraInfo = module.arrayListExtraInfo();
        return name + (arrayListExtraInfo.isEmpty() || !showMode ? "" : " §8[" + arrayListExtraInfo + "]");
    }
}

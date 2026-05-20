package com.scale.scalehackv4.modules.impl.client.Hud;

import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.client.ModuleUtil;
import com.scale.scalehackv4.utils.client.MultiSelectEnum;
import com.scale.scalehackv4.utils.client.MultiSelectUtil;
import com.scale.scalehackv4.utils.render.Draggable;
import com.scale.scalehackv4.utils.render.FontUtil;
import com.scale.scalehackv4.utils.render.RenderUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Comparator;
import java.util.List;

// todo: show keybind submodules
public class ArrayListModule extends HudModule {
    @RegisterSubModule(name = "Array List")
    public static boolean arrayList = true;

    @RegisterSubModule(name = "Font Size", parent = "Array List", min = 5, max = 40)
    public static int fontSize = 8;

    @RegisterSubModule(name = "Array List Options", parent = "Array List")
    public static ArrayListOption options = ArrayListOption.No_Capitals;
    public enum ArrayListOption implements MultiSelectEnum {
        No_Capitals,
        No_Spaces,
        Show_Mode
    }

    public final Draggable arrayListDraggable = new Draggable(
            "Array List",
            () -> {
                List<Module> modules = ModuleUtil.getModules();
                modules.removeIf(e -> !e.isEnabled());

                if (modules.isEmpty()) return new int[] {0,0};

                modules.sort(Comparator.comparingDouble(e -> -FontUtil.getStringWidth(getModuleName(e).getString(), fontSize)));

                int height = FontUtil.getFontHeight(fontSize);

                int maxWidth = FontUtil.getStringWidth(getModuleName(modules.getFirst()).getString(), fontSize);
                int totalHeight = height*modules.size();

                for (int i = 0; i < modules.size(); i++) {
                    Module module = modules.get(i);

                    int x = isRightToLeft() ? -FontUtil.getStringWidth(getModuleName(module).getString(), fontSize) : 0;
                    int y = isBottomToTop() ? -(height*(i+1)) : height*i;
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

    private static Text getModuleName(Module module) {
        String name = module.getAnnotation().name();
        if (MultiSelectUtil.isEnabled(ArrayListOption.No_Capitals)) name = name.toLowerCase();
        if (MultiSelectUtil.isEnabled(ArrayListOption.No_Spaces)) name = name.replace(" ", "");
        String arrayListExtraInfo = module.arrayListExtraInfo();
        String arrayListExtraInfoFormatted = arrayListExtraInfo.isEmpty() || !MultiSelectUtil.isEnabled(ArrayListOption.Show_Mode) ? "" : " [" + arrayListExtraInfo + "]";

        return Text.literal(name)
                .append(Text.literal(arrayListExtraInfoFormatted).formatted(Formatting.DARK_GRAY));
    }
}

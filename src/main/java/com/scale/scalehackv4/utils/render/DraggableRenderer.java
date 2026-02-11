package com.scale.scalehackv4.utils.render;

import com.google.gson.JsonSyntaxException;
import com.scale.scalehackv4.Main;
import com.scale.scalehackv4.bridge.MouseBridge;
import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.RegisterListeners;
import com.scale.scalehackv4.events.impl.render.MouseDraggedEvent;
import com.scale.scalehackv4.events.impl.render.RenderTickEvent;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.ChatUtil;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.input.MouseInput;
import org.apache.commons.io.FileUtils;
import org.joml.Matrix3x2fStack;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

@RegisterListeners
public class DraggableRenderer {
    public static ArrayList<Draggable> draggables = new ArrayList<>();

    private static Draggable dragging = null;

    private final static String DRAGGABLES_PATH = Main.OTHER_PATH + "/";
    private final static String draggablesFile = "draggables.cfg";

    private static MouseInput lastInput = null;

    public static boolean overrideDrag = false;

    // more gibberish code i hate everything
    public final Event drawDraggables = new RenderTickEvent(null, (e) -> {
        MouseInput mouseInput = MouseBridge.from(C.mc.mouse).bridge$getActiveButton();

        boolean mousePressed = lastInput == null && mouseInput != null && mouseInput.button() == 0;
        lastInput = mouseInput;

        for (Draggable draggable : draggables) {
            if (!shouldRender(draggable)) continue;

            int renderX = (int) (draggable.x * C.res().getScaledWidth());
            int renderY = (int) (draggable.y * C.res().getScaledHeight());

            Matrix3x2fStack matrices = e.drawContext.getMatrices();
            matrices.pushMatrix();
            matrices.translate(renderX, renderY);

            int[] size = draggable.render();

            if (!overrideDrag && canDrag(mouseInput)) {
                if (dragging == null) {
                    if (MouseUtil.isMouseInBounds(renderX, renderY, size[0], size[1]) && mousePressed) {
                        dragging = draggable;
                    }
                }
                else if (dragging == draggable) {
                    Render2dUtil.drawRect(0, 0, size[0], size[1], new Color(93, 229, 255,100));
                }
            }
            else if (dragging != null) {
                dragging = null;
                saveDraggables();
            }

            matrices.popMatrix();
        }

        overrideDrag = false;
        return false;
    });
    public final Event onDrag = new MouseDraggedEvent(null, (e) -> {
        if (dragging == null) return false;

        dragging.x = Math.clamp(dragging.x + (e.deltaX / C.res().getWidth()), 0, 1);
        dragging.y = Math.clamp(dragging.y + (e.deltaY / C.res().getHeight()), 0, 1);

        return false;
    });

    private static boolean canDrag(MouseInput mouseInput) {
        return C.mc.currentScreen != null && mouseInput != null && mouseInput.button() == 0;
    }

    public static void saveDraggables() {
        HashMap<String, double[]> draggingJSON = new HashMap<>();

        File file = new File(DRAGGABLES_PATH + draggablesFile);
        if (file.exists()) {
            try {
                draggingJSON = C.gson.fromJson(FileUtils.readFileToString(file, Charset.defaultCharset()), HashMap.class);
            } catch (JsonSyntaxException | IOException e) {
                e.printStackTrace();
                ChatUtil.prefixMessage("Failed to read previous positions json");
            }
        }

        if (draggingJSON == null) draggingJSON = new HashMap<>();

        for (Draggable draggable : draggables) {
            draggingJSON.put(draggable.id, new double[] {draggable.x, draggable.y});
        }

        try {
            Files.createDirectories(Paths.get(DRAGGABLES_PATH));
            Files.write(file.toPath(), C.gson.toJson(draggingJSON).getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void loadDraggingPositions() {
        File file = new File(DRAGGABLES_PATH + draggablesFile);
        if (!file.exists()) {
            System.err.println("No draggables config found!");
            return;
        }

        try {
            HashMap<String, ArrayList<Double>>  draggingJSON = C.gson.fromJson(FileUtils.readFileToString(file, Charset.defaultCharset()), HashMap.class);

            for (Draggable draggable : draggables) {
                if (draggingJSON.containsKey(draggable.id)) {
                    ArrayList<Double> positions = draggingJSON.get(draggable.id);
                    draggable.x = Double.parseDouble(String.valueOf(positions.get(0)));
                    draggable.y = Double.parseDouble(String.valueOf(positions.get(1)));
                }
                else {
                    System.err.println("Dragable config for " + draggable.id + " not found!");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            ChatUtil.prefixMessage("Failed to read previous positions json");
        }
    }

    private static boolean shouldRender(Draggable draggable) {
        return (draggable.conditions.test(null) || C.mc.currentScreen instanceof ChatScreen)
                && draggable.canRender.test(null);
    }
}
package com.scale.scalehackv4.utils.alts;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWDropCallback;

import java.nio.file.Path;
import java.nio.file.Paths;

// currently only used for cookies, im sure ill have drag and drop for other types eventually.
public class DroppedFileListener {
    // only has the arguement so i can pass it with DroppedFileListener::init
    public static void init(MinecraftClient client) {
        GLFW.glfwSetDropCallback(client.getWindow().getHandle(), (window, count, names) -> {
            // multiple files can be dropped, how spooky
            for (int i = 0; i < count; i++) {
                // why did they decide to HIDE the names like this.
                String filePath = GLFWDropCallback.getName(names, i);
                System.out.println(filePath);

                Path path = Paths.get(filePath);
                AltManagerUtil.loginWithFile(path);
            }
        });
    }
}

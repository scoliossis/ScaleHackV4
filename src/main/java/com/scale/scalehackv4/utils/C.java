package com.scale.scalehackv4.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.scale.scalehackv4.Main;
import com.scale.scalehackv4.utils.client.ColourAdapter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.Window;
import net.minecraft.client.world.ClientWorld;
import org.reflections.Reflections;

import java.awt.*;

public class C {
    public static final Reflections REFLECTIONS = new Reflections(Main.class.getPackage().getName());
    public static final MinecraftClient mc = MinecraftClient.getInstance();
    public static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .enableComplexMapKeySerialization()
            .registerTypeAdapter(Color.class, new ColourAdapter())
            .create();

    // C.p
    public static ClientPlayerEntity p() {
        return mc.player;
    }

    public static ClientWorld w() {
        return mc.world;
    }

    public static Window res() {
        return mc.getWindow();
    }

    public static boolean isInGame() {
        return w() != null && p() != null;
    }
}
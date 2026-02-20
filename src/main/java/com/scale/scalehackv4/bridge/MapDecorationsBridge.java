package com.scale.scalehackv4.bridge;

import net.minecraft.item.map.MapDecoration;

import java.util.Map;

public interface MapDecorationsBridge {
    static MapDecorationsBridge from(Object instance) {
        return (MapDecorationsBridge) instance;
    }

    Map<String, MapDecoration> bridge$getDecorations();
}
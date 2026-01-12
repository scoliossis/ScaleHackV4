package com.scale.scalehackv4.bridge;

import net.minecraft.client.input.MouseInput;

public interface MouseBridge {
    static MouseBridge from(Object instance) {
        return (MouseBridge) instance;
    }

    MouseInput bridge$getActiveButton();
}
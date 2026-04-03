package com.scale.scalehackv4.bridge;

public interface KeybindingBridge {
    static KeybindingBridge from(Object instance) {
        return (KeybindingBridge) instance;
    }

    int bridge$getTimesPressed();
}

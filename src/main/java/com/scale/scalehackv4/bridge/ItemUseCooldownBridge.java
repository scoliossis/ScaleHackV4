package com.scale.scalehackv4.bridge;

public interface ItemUseCooldownBridge {
    static ItemUseCooldownBridge from(Object instance) {
        return (ItemUseCooldownBridge) instance;
    }

    int bridge$getItemUseCooldown();
}
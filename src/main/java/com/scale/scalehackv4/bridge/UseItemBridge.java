package com.scale.scalehackv4.bridge;

public interface UseItemBridge {
    static UseItemBridge from(Object instance) {
        return (UseItemBridge) instance;
    }

    void bridge$DoItemUse();
    void bridge$doAttack();
}

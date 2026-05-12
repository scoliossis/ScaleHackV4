package com.scale.scalehackv4.bridge;

public interface BreakBlockCooldownBridge {
    static BreakBlockCooldownBridge from(Object instance) {
        return (BreakBlockCooldownBridge) instance;
    }

    int bridge$getBlockBreakingCooldown();
}

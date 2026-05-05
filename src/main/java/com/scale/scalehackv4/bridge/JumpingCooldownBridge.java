package com.scale.scalehackv4.bridge;

public interface JumpingCooldownBridge {
    static JumpingCooldownBridge from(Object instance) {
        return (JumpingCooldownBridge) instance;
    }

    int bridge$getJumpingCooldown();
}

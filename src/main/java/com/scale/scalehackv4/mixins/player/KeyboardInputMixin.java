package com.scale.scalehackv4.mixins.player;

import com.scale.scalehackv4.events.Bus;
import com.scale.scalehackv4.events.impl.player.KeyboardInputEvent;
import com.scale.scalehackv4.modules.impl.render.Freelook;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.option.GameOptions;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// i h8 u docilelm, sry
@Mixin(value = KeyboardInput.class, priority = 1)
public abstract class KeyboardInputMixin extends Input {
    @Shadow
    @Final
    private GameOptions settings;

    @Shadow
    private static float getMovementMultiplier(boolean positive, boolean negative) {
        return 0;
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    public void tick(CallbackInfo ci) {
        KeyboardInputEvent.Params params = new KeyboardInputEvent.Params(
                this.settings.forwardKey.isPressed(),
                this.settings.backKey.isPressed(),
                this.settings.leftKey.isPressed(),
                this.settings.rightKey.isPressed(),
                this.settings.jumpKey.isPressed(),
                this.settings.sneakKey.isPressed(),
                this.settings.sprintKey.isPressed()
        );
        Bus.post(KeyboardInputEvent.class, params);

        this.playerInput = Freelook.isPositionLocked()
                ? PlayerInput.DEFAULT
                : new PlayerInput(params.forwardKey, params.backKey, params.leftKey, params.rightKey, params.jumpKey, params.sneakKey, params.sprintKey);

        float f = getMovementMultiplier(this.playerInput.forward(), this.playerInput.backward());
        float g = getMovementMultiplier(this.playerInput.left(), this.playerInput.right());
        this.movementVector = new Vec2f(g, f).normalize();

        ci.cancel();
    }
}
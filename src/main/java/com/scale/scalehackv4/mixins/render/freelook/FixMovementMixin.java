package com.scale.scalehackv4.mixins.render.freelook;

import com.scale.scalehackv4.modules.impl.render.Freelook;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.option.GameOptions;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(KeyboardInput.class)
public abstract class FixMovementMixin extends Input {
    @Shadow
    @Final
    private GameOptions settings;

    @Shadow
    private static float getMovementMultiplier(boolean positive, boolean negative) {
        return 0;
    }

    /**
     * @author Scale
     * @reason to stop movement when freelook is enabled (ai autofilled this reason and was actually right, ai will take over the world its over)
     */
    @Overwrite
    public void tick() {
        this.playerInput = Freelook.isPositionLocked()
                ? PlayerInput.DEFAULT
                : new PlayerInput(this.settings.forwardKey.isPressed(), this.settings.backKey.isPressed(), this.settings.leftKey.isPressed(), this.settings.rightKey.isPressed(), this.settings.jumpKey.isPressed(), this.settings.sneakKey.isPressed(), this.settings.sprintKey.isPressed());

        float f = getMovementMultiplier(this.playerInput.forward(), this.playerInput.backward());
        float g = getMovementMultiplier(this.playerInput.left(), this.playerInput.right());
        this.movementVector = new Vec2f(g, f).normalize();
    }
}

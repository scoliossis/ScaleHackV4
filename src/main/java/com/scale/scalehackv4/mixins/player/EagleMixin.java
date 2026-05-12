package com.scale.scalehackv4.mixins.player;

import com.scale.scalehackv4.modules.impl.player.ScaffoldModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.MultiSelectUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MovementType;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EagleMixin {
    @Shadow
    protected abstract Vec3d adjustMovementForSneaking(Vec3d movement, MovementType type);

    @Shadow
    public abstract int getId();

    @Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;adjustMovementForSneaking(Lnet/minecraft/util/math/Vec3d;Lnet/minecraft/entity/MovementType;)Lnet/minecraft/util/math/Vec3d;"))
    private void clipAtLedge(MovementType type, Vec3d movement, CallbackInfo ci) {
        if (C.p() == null || this.getId() != C.p().getId() || !ScaffoldModule.shouldEagle()) return;

        PlayerInput prevPlayerInput = C.p().input.playerInput;
        C.p().input.playerInput = new PlayerInput(
                prevPlayerInput.forward(),
                prevPlayerInput.backward(),
                prevPlayerInput.left(),
                prevPlayerInput.right(),
                prevPlayerInput.jump(),
                true,
                prevPlayerInput.sprint()
        );

        Vec3d sneakAdjustedMovement = this.adjustMovementForSneaking(movement, type);

        // if sneaking doesnt stop u, then we arent gonna go over the edge
        if (sneakAdjustedMovement.x == movement.x && sneakAdjustedMovement.z == movement.z) C.p().input.playerInput = new PlayerInput(
                prevPlayerInput.forward(),
                prevPlayerInput.backward(),
                prevPlayerInput.left(),
                prevPlayerInput.right(),
                prevPlayerInput.jump(),
                // if hold crouch mode is on, we dont want to perma sneak
                !MultiSelectUtil.isEnabled(ScaffoldModule.ScaffoldConditions.Crouch_Down) && prevPlayerInput.sneak(),
                prevPlayerInput.sprint()
        );
    }
}

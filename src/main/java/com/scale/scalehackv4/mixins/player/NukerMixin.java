package com.scale.scalehackv4.mixins.player;

import com.scale.scalehackv4.modules.impl.player.NukerModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.player.WorldUtil;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class NukerMixin {
    @Shadow
    private int blockBreakingCooldown;

    @Inject(method = "attackBlock", at = @At("HEAD"))
    public void onAttackBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (!NukerModule.shouldAutotool()) return;

        NukerModule.switchToTool(C.w().getBlockState(pos).getBlock());
    }

    @Inject(method = "updateBlockBreakingProgress", at = @At("HEAD"))
    public void onUpdateBlockBreakProgress(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        int breakDelay = NukerModule.getBreakDelay();
        this.blockBreakingCooldown = Math.min(this.blockBreakingCooldown, breakDelay);

        NukerModule.lastBlockBreakTick = WorldUtil.tick;
    }

    @Redirect(method = "updateBlockBreakingProgress", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;calcBlockBreakingDelta(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;)F"))
    private float onUpdateBreakProgress(BlockState instance, PlayerEntity player, BlockView blockView, BlockPos blockPos) {
        return instance.calcBlockBreakingDelta(C.p(), C.p().getEntityWorld(), blockPos) * NukerModule.getBreakSpeed();
    }
}

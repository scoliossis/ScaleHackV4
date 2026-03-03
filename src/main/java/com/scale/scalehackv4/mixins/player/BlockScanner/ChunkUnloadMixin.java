package com.scale.scalehackv4.mixins.player.BlockScanner;

import com.scale.scalehackv4.utils.player.BlockTracker;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientWorld.class)
public class ChunkUnloadMixin {
    @Inject(at = @At("HEAD"), method = "handleBlockUpdate")
    private void onBlockUpdate(
            BlockPos pos,
            BlockState state,
            int flags,
            CallbackInfo ci
    ) {
        BlockTracker.queueBlock(pos, state.getBlock());
    }

    @Inject(at = @At("HEAD"), method = "onChunkUnload")
    public void onChunkUnload(long sectionPos, CallbackInfo ci) {
        BlockTracker.removeLoadedChunk(new ChunkPos(sectionPos));
    }
}
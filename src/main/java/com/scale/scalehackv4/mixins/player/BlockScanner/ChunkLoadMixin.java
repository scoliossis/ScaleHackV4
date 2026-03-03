package com.scale.scalehackv4.mixins.player.BlockScanner;

import com.scale.scalehackv4.utils.player.BlockTracker;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.PalettesFactory;
import net.minecraft.world.chunk.UpgradeData;
import net.minecraft.world.gen.chunk.BlendingData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// ripped from mushroomclient, no github link, sorry, im sure the src is out there somewhere
@Mixin(Chunk.class)
public class ChunkLoadMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void init(
            ChunkPos pos,
            UpgradeData upgradeData,
            HeightLimitView heightLimitView,
            PalettesFactory palettesFactory,
            long inhabitedTime,
            ChunkSection[] sectionArray,
            BlendingData blendingData,
            CallbackInfo ci
    ) {
        BlockTracker.addLoadedChunk(pos);
    }
}

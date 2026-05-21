package com.scale.scalehackv4.modules.impl.render;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.render.RenderWorldEvent;
import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.player.BlockTracker;
import com.scale.scalehackv4.utils.render.Render3dUtil;
import com.scale.scalehackv4.utils.render.Vec3f;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.BedPart;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.awt.*;
import java.util.List;

@RegisterModule(
        name = "Block ESP",
        category = Category.RENDER
)
// todo: allow ANY block to be picked, not just beds
public class BlockESP extends Module {
    @RegisterSubModule(name = "ESP Colour")
    public static Color espColour = new Color(0xFF00F1);

    // uh huh.
    private final List<Block> BED_COLOURS = List.of(
            Blocks.BLACK_BED,
            Blocks.BLUE_BED,
            Blocks.BROWN_BED,
            Blocks.CYAN_BED,
            Blocks.GRAY_BED,
            Blocks.GREEN_BED,
            Blocks.LIGHT_BLUE_BED,
            Blocks.LIGHT_GRAY_BED,
            Blocks.LIME_BED,
            Blocks.MAGENTA_BED,
            Blocks.ORANGE_BED,
            Blocks.PINK_BED,
            Blocks.PURPLE_BED,
            Blocks.RED_BED,
            Blocks.WHITE_BED,
            Blocks.YELLOW_BED
    );

    public final Event renderWorldEvent = new RenderWorldEvent(this, params -> {
        BED_COLOURS.forEach(bed -> {
            for (BlockPos blockPos : BlockTracker.getBlockPositions(bed)) {
                BlockState blockState = C.w().getBlockState(blockPos);
                if (!(blockState.getBlock() instanceof BedBlock bedBlock) || C.w().getBlockState(blockPos).get(BedBlock.PART) != BedPart.FOOT) continue;

                Direction facing = C.w().getBlockState(blockPos).get(BedBlock.FACING);
                // shrug, https://github.com/scoliossis/ScaleHackV3/blob/master/src/main/java/com/github/scoliossis/modules/impl/render/BedESP.java#L53
                Vec3f size = new Vec3f(1.0F + Math.abs(facing.getOffsetX()), 0.5625F, 1.0F + Math.abs(facing.getOffsetZ()));

                Render3dUtil.drawBox(
                        blockPos.getX() + Math.min(facing.getOffsetX(), 0),
                        blockPos.getY(),
                        blockPos.getZ() + Math.min(facing.getOffsetZ(), 0),
                        size.x,
                        size.y,
                        size.z,
                        espColour,
                        false
                );
            };
        });
        return false;
    });

    @Override
    public void onEnable() {
        BlockTracker.beginTracking(BED_COLOURS, this.getClass());
    }

    @Override
    public void onDisable() {
        BlockTracker.stopTracking(BED_COLOURS, this.getClass());
    }
}
package com.scale.scalehackv4.modules.impl.render;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.render.RenderTickEvent;
import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.InventoryUtil;
import com.scale.scalehackv4.utils.player.BlockTracker;
import com.scale.scalehackv4.utils.player.WorldUtil;
import com.scale.scalehackv4.utils.render.Render2dUtil;
import com.scale.scalehackv4.utils.render.RenderUtil;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.BedPart;
import net.minecraft.client.texture.Sprite;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EmptyBlockView;
import org.joml.Matrix3x2fStack;
import org.joml.Quaternionf;
import org.joml.Vector2i;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

@RegisterModule(
        name = "Block ESP",
        category = Category.RENDER
)
// todo: allow ANY block to be picked, not just beds
public class BlockESP extends Module {
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

    private static final int ESP_SIZE = 16;
    private static final int PADDING = 2;
    private static final int DOWNSCALE = 2;

    private final HashMap<BlockPos, Vector2i> bedPositions  = new HashMap<>();

    public final Event renderTickEvent = new RenderTickEvent(this, params -> {
        BED_COLOURS.forEach(bed -> {
            for (BlockPos blockPos : BlockTracker.getBlockPositions(bed)) {
                BlockState bedBlockState = C.w().getBlockState(blockPos);
                if (!(bedBlockState.getBlock() instanceof BedBlock) || bedBlockState.get(BedBlock.PART) != BedPart.FOOT) continue;

                Direction facing = bedBlockState.get(BedBlock.FACING);

                Vec3d bedMiddle = blockPos.toCenterPos().add(facing.getOffsetX()/2f, 0, facing.getOffsetZ()/2f);

                Vector2i bedMiddlePos = Render2dUtil.projectPoint(bedMiddle);
                if (bedMiddlePos == null) continue;

                bedPositions
                        .compute(blockPos, (k, v) -> (v == null ? bedMiddlePos : v)
                        .add(bedMiddlePos.sub((v == null ? bedMiddlePos : v), new Vector2i()).div(2, new Vector2i())));

                ArrayList<BlockState> outerBlocks = new ArrayList<>();
                outerBlocks.add(bedBlockState);

                // todo: maybe get more than just the immediate block
                BlockState bestBlockState = C.w().getBlockState(WorldUtil.getWeakestSurroundingBlock(blockPos, facing));
                if (InventoryUtil.isFullBlock(bestBlockState)) outerBlocks.add(bestBlockState);

                Matrix3x2fStack matrices = Render2dUtil.getDrawContext().getMatrices();
                matrices.pushMatrix();

                int width = ((ESP_SIZE+PADDING)*outerBlocks.size() + PADDING) / DOWNSCALE;
                int height = (ESP_SIZE + PADDING*2) / DOWNSCALE;

                int x = -width/2;
                int y = -height/2;

                Vector2i pos = bedPositions.get(blockPos);

                Render2dUtil.drawBlur(pos.x+x, pos.y+y, width, height);
                Render2dUtil.drawRect(pos.x+x, pos.y+y, width, height, new Color(20,20,20,100));

                matrices.translate(pos.x+x, pos.y+y);
                matrices.scale(1f/DOWNSCALE);

                matrices.translate(PADDING, PADDING);
                for (BlockState outerBlockState : outerBlocks) {
                    RenderUtil.getDrawContext().drawItem(
                            outerBlockState.getBlock().asItem().getDefaultStack(),
                            0,
                            0
                    );
                    matrices.translate(ESP_SIZE + PADDING, 0);
                }

                matrices.popMatrix();
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
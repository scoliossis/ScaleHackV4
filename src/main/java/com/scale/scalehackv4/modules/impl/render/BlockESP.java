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
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.BedPart;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import org.joml.Quaternionf;

import java.awt.*;
import java.util.List;

@RegisterModule(
        name = "Block ESP",
        category = Category.RENDER
)
// todo: allow ANY block to be picked, not just beds
public class BlockESP extends Module {
    @RegisterSubModule(name = "ESP Colour")
    public static Color espColour = new Color(240, 139, 248,100);

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
                if (!(blockState.getBlock() instanceof BedBlock bedBlock) || blockState.get(BedBlock.PART) != BedPart.FOOT) continue;

                Direction facing = blockState.get(BedBlock.FACING);
                // shrug, https://github.com/scoliossis/ScaleHackV3/blob/master/src/main/java/com/github/scoliossis/modules/impl/render/BedESP.java#L53
                Box box = new Box(
                        Math.min(facing.getOffsetX(), 0), 0, Math.min(facing.getOffsetZ(), 0),
                        1.0F + Math.abs(facing.getOffsetX()), 0.5625F, 1.0F + Math.abs(facing.getOffsetZ())
                );

                Render3dUtil.getMatrices().push();
                Render3dUtil.getMatrices().translate(blockPos.getX(), blockPos.getY(), blockPos.getZ());

                Render3dUtil.drawBox(
                        (float) box.minX, (float) box.minY, (float) box.minZ,
                        (float) box.maxX, (float) box.maxY, (float) box.maxZ,
                        espColour,
                        false
                );

                Render3dUtil.getMatrices().translate(box.minX + 0.5, 1.25, box.minZ + 0.5);
                Render3dUtil.getMatrices().multiply(new Quaternionf().rotationZYX(0f, (float) -Math.toRadians(C.p().getYaw()), (float) Math.toRadians(C.p().getPitch())));
                Sprite sprite = C.mc.getBlockRenderManager().getModel(C.w().getBlockState(blockPos.up())).particleSprite();
                float u0 = sprite.getMinU(), u1 = sprite.getMaxU(), v0 = sprite.getMinV(), v1 = sprite.getMaxV();

                Render3dUtil.drawTexturedQuads(-0.25f, -0.25f, -0.25f, 0.5f, 0.5f, u0, v1, u1, v0, Color.WHITE, sprite.getAtlasId(), false);

                sprite.close();

                Render3dUtil.getMatrices().pop();
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
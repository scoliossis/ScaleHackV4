package com.scale.scalehackv4.modules.impl.render;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.render.RenderWorldEvent;
import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import com.scale.scalehackv4.utils.player.BlockTracker;
import com.scale.scalehackv4.utils.render.Render3dUtil;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

import java.awt.*;
import java.util.List;

// todo: add xray!
@RegisterModule(
        name = "X-Ray",
        category = Category.RENDER
)
public class XRay extends Module {
    public final Event renderWorldEvent = new RenderWorldEvent(this, params -> {
        for (BlockPos blockPos : BlockTracker.getBlockPositions(Blocks.CHEST)) {
            Render3dUtil.drawBox(blockPos.getX(), blockPos.getY(), blockPos.getZ(), 1, 1, 1, new Color(246, 246, 9,20), false);
        }
        return false;
    });

    @Override
    public void onEnable() {
        BlockTracker.beginTracking(List.of(Blocks.CHEST), this.getClass());
    }

    @Override
    public void onDisable() {
        BlockTracker.stopTracking(List.of(Blocks.CHEST), this.getClass());
    }
}

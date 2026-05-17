package com.scale.scalehackv4.commands.impl;

import com.mojang.brigadier.suggestion.Suggestions;
import com.scale.scalehackv4.commands.Command;
import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.client.WorldJoinEvent;
import com.scale.scalehackv4.events.impl.render.RenderWorldEvent;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.ChatUtil;
import com.scale.scalehackv4.utils.player.PathfindUtil;
import com.scale.scalehackv4.utils.render.Render3dUtil;
import net.minecraft.util.math.BlockPos;
import org.joml.Vector3f;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public class PathfindCommand extends Command {
    ArrayList<BlockPos> blocks = null;

    private final Event resetPathfinding = new WorldJoinEvent(null, params -> {
        blocks = null;
        return false;
    });

    private final Event drawPath = new RenderWorldEvent(null, params -> {
        if (blocks == null) return false;

        Vector3f lastPos = null;
        for (BlockPos block : blocks) {
            Vector3f pos = block.toCenterPos().toVector3f().add(0, 0.5f, 0);
            if (lastPos != null) {
                Render3dUtil.drawLine(lastPos, pos, new Color(255, 0, 0), false);
            }
            Render3dUtil.drawBox(pos.x - 0.05f, pos.y - 0.05f, pos.z - 0.05f, 0.1f, 0.1f, 0.1f, new Color(0, 245, 255, 255), false);
            lastPos = pos;
        }

        return false;
    });

    @Override
    public String name() {
        return "pathfind";
    }

    @Override
    public boolean handleCommand(String text) {
        try {
            String[] coords = text.split(",");

            int[] coordsInt = Arrays.stream(coords).mapToInt(e -> Integer.parseInt(e.trim())).toArray();
            blocks = PathfindUtil.findPath(C.p().getBlockPos().down(), new BlockPos(coordsInt[0], coordsInt[1], coordsInt[2]));
        } catch (Exception e) {
            e.printStackTrace();
            ChatUtil.prefixMessage("Invalid coordinates");
            return true;
        }

        if (blocks == null) {
            ChatUtil.prefixMessage("No path found");
        }

        return true;
    }

    @Override
    public void handleTab(String text, int pos, CompletableFuture<Suggestions> pendingSuggestions) {

    }
}

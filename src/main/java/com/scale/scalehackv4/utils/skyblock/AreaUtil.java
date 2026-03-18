package com.scale.scalehackv4.utils.skyblock;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.RegisterListeners;
import com.scale.scalehackv4.events.impl.client.ChatEvent;
import com.scale.scalehackv4.events.impl.client.WorldJoinEvent;
import com.scale.scalehackv4.events.impl.render.RenderScoreboardEvent;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.skyblock.DungeonMap.DungeonScanner;
import com.scale.scalehackv4.utils.skyblock.DungeonMap.Room;
import lombok.Getter;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.util.math.ChunkPos;

@RegisterListeners
public class AreaUtil {
    public final Event onLoadWorld = new WorldJoinEvent(null, params -> {
        RenderScoreboardEvent.LAST_SIDEBAR = new RenderScoreboardEvent.SidebarEntry[0];
        dungeonStarted = false;
        return false;
    });

    public final Event onReceiveChat = new ChatEvent.Received(null, params -> {
        dungeonStarted |= params.message.equals("§e[NPC] §bMort§f: Here, I found this map when I first entered the dungeon.");
        return false;
    });

    public static boolean isInSkyblock() {
        Scoreboard scoreboard = C.w().getScoreboard();
        if (scoreboard == null) return false;

        for (ScoreboardObjective objective : scoreboard.getObjectives()) {
            if (objective.getName().equals("SBScoreboard")) return true;
        }

        return false;
    }

    public static boolean isInDungeons() {
        for (RenderScoreboardEvent.SidebarEntry sidebarEntry : RenderScoreboardEvent.LAST_SIDEBAR) {
            if (sidebarEntry.name().getString().matches("( )+§7⏣ §cThe Catac§[a-zA-Z0-9]§combs §7\\([MFE].+")) {
                return true;
            }
        }

        return false;
    }

    @Getter
    private static boolean dungeonStarted = false;

    // boss is always right of the rightmost chunk
    public static boolean isInBoss() {
        float bossX = -DungeonScanner.MAX_DUNGEON_DIAMETER+DungeonScanner.DUNGEON_BLOCKS_OFFSET + DungeonScanner.CURRENT_DUNGEON_DIAMETER.x;
        float bossZ = -DungeonScanner.MAX_DUNGEON_DIAMETER+DungeonScanner.DUNGEON_BLOCKS_OFFSET + DungeonScanner.CURRENT_DUNGEON_DIAMETER.y;
        return C.p().getX() > bossX && C.p().getZ() > bossZ;
    }

    public static Room getCurrentRoom() {
        if (!isInDungeons()) return null;

        for (Room room : DungeonScanner.ROOMS) {
            for (ChunkPos chunkPos : room.squares) {
                if (C.p().getX() > chunkPos.x * 16 - 7 && C.p().getZ() > chunkPos.z * 16 - 7) {
                    if (C.p().getX() < chunkPos.x * 16 + 23 && C.p().getZ() < chunkPos.z * 16 + 23) {
                        return room;
                    }
                }
            }
        }

        return null;
    }
}
package com.scale.scalehackv4.utils.skyblock;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.RegisterListeners;
import com.scale.scalehackv4.events.impl.client.WorldJoinEvent;
import com.scale.scalehackv4.events.impl.render.RenderScoreboardEvent;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.skyblock.DungeonMap.DungeonScanner;
import com.scale.scalehackv4.utils.skyblock.DungeonMap.Room;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.util.math.ChunkPos;

@RegisterListeners
public class AreaUtil {
    public final Event onLoadWorld = new WorldJoinEvent(null, params -> {
        RenderScoreboardEvent.LAST_SIDEBAR = new RenderScoreboardEvent.SidebarEntry[0];
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
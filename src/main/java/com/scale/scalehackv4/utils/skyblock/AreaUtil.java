package com.scale.scalehackv4.utils.skyblock;

import com.scale.scalehackv4.events.RegisterListeners;
import com.scale.scalehackv4.events.impl.render.RenderScoreboardEvent;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.skyblock.DungeonMap.DungeonScanner;
import com.scale.scalehackv4.utils.skyblock.DungeonMap.Room;
import net.minecraft.util.math.ChunkPos;

@RegisterListeners
public class AreaUtil {
    public static boolean isInDungeons() {
        if (C.w().getScoreboard().getObjectives().isEmpty()) return false;

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
package com.scale.scalehackv4.utils.skyblock;

import com.scale.scalehackv4.events.Bus;
import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.RegisterListeners;
import com.scale.scalehackv4.events.impl.client.ChatEvent;
import com.scale.scalehackv4.events.impl.client.ClientTickEvent;
import com.scale.scalehackv4.events.impl.client.WorldJoinEvent;
import com.scale.scalehackv4.events.impl.player.PacketEvent;
import com.scale.scalehackv4.events.impl.render.RenderScoreboardEvent;
import com.scale.scalehackv4.events.impl.skyblock.DungeonJoinEvent;
import com.scale.scalehackv4.events.impl.skyblock.DungeonStartEvent;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.skyblock.DungeonMap.DungeonScanner;
import com.scale.scalehackv4.utils.skyblock.DungeonMap.Room;
import lombok.Getter;
import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket;
import net.minecraft.network.packet.s2c.play.ScoreboardObjectiveUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.util.math.ChunkPos;

@RegisterListeners
public class AreaUtil {
    @Getter private static boolean inDungeons = false;
    @Getter private static boolean dungeonStarted = false;

    public final Event onLoadWorld = new WorldJoinEvent(null, params -> {
        reset();
        return false;
    });

    private final Event onLeaveGame = new ClientTickEvent(null, params -> {
        if (!C.isInGame()) reset();

        return false;
    });

    private static void reset() {
        RenderScoreboardEvent.LAST_SIDEBAR = new RenderScoreboardEvent.SidebarEntry[0];
        dungeonStarted = false;
        inDungeons = false;
        dungeonStartMs = Integer.MAX_VALUE;
    }

    @Getter private static long dungeonStartMs = Integer.MAX_VALUE;

    // todo: mods block these somehow
    public final Event onReceiveChat = new ChatEvent.Received(null, params -> {
        if (params.message.equals("§e[NPC] §bMort§f: Here, I found this map when I first entered the dungeon.")) {
            dungeonStarted = true;
            dungeonStartMs = Integer.MAX_VALUE;
            Bus.post(DungeonStartEvent.class, null);
        }
        if (inDungeons && params.message.matches("^Starting in [0-5] seconds?\\.$")) {
            dungeonStartMs = Integer.parseInt(params.message.split(" ")[2]) * 1000L;
        }
        return false;
    });

    @Getter private static long serverTick = -1;
    private final Event trackTime = new PacketEvent.Received(null, params -> {
        if (params.packet instanceof WorldTimeUpdateS2CPacket timePacket) {
            serverTick = timePacket.time();
            return false;
        }
        if (params.packet instanceof CommonPingS2CPacket) {
            serverTick++;
            if (dungeonStartMs != Integer.MAX_VALUE) dungeonStartMs -= 50;
            // we went an entire 10 of these without an update on the time, we probably cancelled.
            if (dungeonStartMs % 1000 == 0) dungeonStartMs = Integer.MAX_VALUE;
        }
        return false;
    });

    public final Event onScoreboardUpdate = new PacketEvent.Received(null, params -> {
        if (inDungeons || !(params.packet instanceof ScoreboardObjectiveUpdateS2CPacket)) return false;

        for (RenderScoreboardEvent.SidebarEntry sidebarEntry : RenderScoreboardEvent.LAST_SIDEBAR) {
            if (sidebarEntry.name().getString().matches("( )+§7⏣ §cThe Catac§[a-zA-Z0-9]§combs §7\\([MFE].+")) {
                inDungeons = true;
                Bus.post(DungeonJoinEvent.class, null);
                return false;
            }
        }

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

    // boss is always right of the rightmost chunk
    public static boolean isInBoss() {
        float bossX = -DungeonScanner.MAX_DUNGEON_DIAMETER+DungeonScanner.DUNGEON_BLOCKS_OFFSET + DungeonScanner.CURRENT_DUNGEON_DIAMETER.x;
        float bossZ = -DungeonScanner.MAX_DUNGEON_DIAMETER+DungeonScanner.DUNGEON_BLOCKS_OFFSET + DungeonScanner.CURRENT_DUNGEON_DIAMETER.y;
        return C.p().getX() > bossX && C.p().getZ() > bossZ;
    }

    // todo: make getCurrentRoom faster, maybe use a hashmap?
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
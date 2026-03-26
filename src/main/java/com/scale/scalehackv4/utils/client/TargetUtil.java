package com.scale.scalehackv4.utils.client;

import com.scale.scalehackv4.bridge.antibot.AntiBotBridge;
import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.RegisterListeners;
import com.scale.scalehackv4.events.impl.client.WorldJoinEvent;
import com.scale.scalehackv4.events.impl.player.PlayerUpdateEvent;
import com.scale.scalehackv4.modules.impl.client.Targets;
import com.scale.scalehackv4.utils.C;
import net.minecraft.client.gui.screen.multiplayer.SocialInteractionsPlayerListEntry;
import net.minecraft.client.gui.screen.multiplayer.SocialInteractionsPlayerListWidget;
import net.minecraft.client.gui.screen.multiplayer.SocialInteractionsScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@RegisterListeners
public class TargetUtil {
    public static List<AbstractClientPlayerEntity> getPlayers() {
        return C.w().getPlayers().stream().filter(TargetUtil::isValidTarget).toList();
    }

    private static final HashMap<UUID, Integer> VALID_TARGETS = new HashMap<>();
    private static SocialInteractionsPlayerListWidget PLAYER_LIST_WIDGET;

    private final Event reloadValidTargets = new WorldJoinEvent(null, params -> {
        VALID_TARGETS.clear();
        PLAYER_LIST_WIDGET = new SocialInteractionsPlayerListWidget(new SocialInteractionsScreen(), C.mc, Integer.MAX_VALUE, Integer.MAX_VALUE, 0, 0);

        return false;
    });

    private final Event updateValidEntities = new PlayerUpdateEvent(null, params -> {
        PLAYER_LIST_WIDGET.update(C.p().networkHandler.getPlayerUuids(), 0, true);
        List<SocialInteractionsPlayerListEntry> players = AntiBotBridge.from(PLAYER_LIST_WIDGET).bridge$getPlayers();
        for (SocialInteractionsPlayerListEntry player : players) {
            if (player.isOffline()) VALID_TARGETS.remove(player.getUuid());
            else {
                Integer validTicks = VALID_TARGETS.get(player.getUuid());
                if (validTicks != null) VALID_TARGETS.put(player.getUuid(), validTicks+1);
                else VALID_TARGETS.put(player.getUuid(), 0);
            }
        }

        return false;
    });

    public static boolean isValidTarget(Entity target) {
        if (!Targets.antibot) return true;
        // this is possible for some reason, crash occured with a random skyblock mod, idk why it fed this a null target, wtv.
        if (target == null) return false;

        int ping = -1;
        if (target instanceof PlayerEntity player) {
            PlayerListEntry playerListEntry = C.p().networkHandler.getPlayerListEntry((player).getUuid());
            if (playerListEntry != null) ping = playerListEntry.getLatency();
        }
        Integer validTicks = VALID_TARGETS.get(target.getUuid());
        boolean isValidEntry = !Targets.playerEntry || (validTicks != null && validTicks > 60);
        boolean isValidPing = !Targets.pingCheck || (ping > Targets.botPingMode.ping);
        return (isValidEntry && isValidPing) || target == C.p();
    }
}

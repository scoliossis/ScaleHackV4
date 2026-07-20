package com.scale.scalehackv4.modules.impl.client;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.client.WorldJoinEvent;
import com.scale.scalehackv4.events.impl.player.PlayerUpdateEvent;
import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.MultiSelectEnum;
import com.scale.scalehackv4.utils.client.MultiSelectUtil;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@RegisterModule(
        name = "Targets",
        category = Category.CLIENT,
        enabledByDefault = true
)
public class Targets extends Module {
    @RegisterSubModule(name = "Targets")
    public ValidTargets teams = ValidTargets.Team;

    public enum ValidTargets implements MultiSelectEnum {
        Anti_Bot {
            @Override
            public boolean isInvalid(Entity target, boolean visual) {
                return isBot(target);
            }
            @Override public boolean enabledByDefault() {return true;}
        },
        Team {
            @Override
            public boolean isInvalid(Entity target, boolean visual) {
                return !visual && isTeam(target);
            }
            @Override public boolean enabledByDefault() {return true;}
        };

        public abstract boolean isInvalid(Entity target, boolean visual);
    }

    @RegisterSubModule(name = "Valid Weapons")
    public static Valid_Weapons validWeapons = Valid_Weapons.Sword;

    public enum Valid_Weapons implements MultiSelectEnum {
        Sword {@Override public boolean enabledByDefault() {return true;}},
        Knockback {@Override public boolean enabledByDefault() {return true;}},
        Axe,
        Spear,
        Trident,
        Mace,
        Fist,
        Non_Fist
    }

    public static List<AbstractClientPlayerEntity> getPlayers(boolean visual) {
        return C.w().getPlayers().stream().filter(e -> isValidTarget(e, visual)).toList();
    }

    public static List<Entity> getTargets(double range) {
        return C.w().getOtherEntities(C.p(), C.p().getBoundingBox().expand(range), e -> isValidTarget(e, false));
    }

    private static final HashMap<UUID, Integer> ENTITY_VALID_TICKS = new HashMap<>();

    private final Event reloadValidTargets = new WorldJoinEvent(null, params -> {
        ENTITY_VALID_TICKS.clear();
        return false;
    });

    private static boolean isTeam(Entity target) {
        if (C.p() == null || !(target instanceof PlayerEntity player)) return false;

        return player.getTeamColorValue() == C.p().getTeamColorValue();
    }

    private static boolean isBot(Entity target) {
        return ENTITY_VALID_TICKS.getOrDefault(target.getUuid(), -1) < 5;
    }

    public static boolean isValidTarget(Entity target, boolean visual) {
        if (target == C.p()) return true;
        // this is possible for some reason, crash occured with a random skyblock mod, idk why it fed this a null target, wtv.
        if (target == null) return false;

        for (ValidTargets targetCheck : ValidTargets.values()) {
            if (MultiSelectUtil.isEnabled(targetCheck) && targetCheck.isInvalid(target, visual)) return false;
        }

        return true;
    }

    private final Event updateValidEntities = new PlayerUpdateEvent(null, params -> {
        if (C.mc.getNetworkHandler() == null) return false;
        for (PlayerEntity player : C.w().getPlayers()) {
            if (C.mc.getNetworkHandler().getPlayerListEntry(player.getUuid()) != null) {
                ENTITY_VALID_TICKS.putIfAbsent(player.getUuid(), 0);
                ENTITY_VALID_TICKS.put(player.getUuid(), ENTITY_VALID_TICKS.get(player.getUuid()) + 1);
            }
            else ENTITY_VALID_TICKS.remove(player.getUuid());
        }
        return false;
    });

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
        this.setEnabled(true);
    }
}

package com.scale.scalehackv4.modules.impl.skyblock.SecretsHelper;

import com.scale.scalehackv4.bridge.UseItemBridge;
import com.scale.scalehackv4.events.Bus;
import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.player.CheckClickEvent;
import com.scale.scalehackv4.events.impl.player.PacketEvent;
import com.scale.scalehackv4.events.impl.player.RotationEvent;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.ChatUtil;
import com.scale.scalehackv4.utils.client.ModuleUtil;
import com.scale.scalehackv4.utils.player.RotationUtil;
import com.scale.scalehackv4.utils.skyblock.AreaUtil;
import com.scale.scalehackv4.utils.skyblock.SkyblockItems;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.Optional;

// todo: melee aura
public class BatAura extends SecretsHelper {
    @RegisterSubModule(name = "Bat Aura", hasKeybind = true)
    public static boolean batAura = true;
    @RegisterSubModule(name = "Bat Range", parent = "Bat Aura", increment = 0.1, min = 3, max = 16)
    public static double batRange = 8;
    @RegisterSubModule(name = "Bat Timeout", parent = "Bat Aura", min = 100, max = 3000)
    public static int batTimeout = 500;

    private static boolean shouldKillBat = false;
    private static BlockPos batRotationPos = null;
    private static long lastBatTime = -1;

    private static int prevSlot = -1;

    private static final int SPIRIT_SCEPTRE_EXPLOSION_RADIUS = 6;
    private static final int SECRET_BAT_HEALTH = 100;

    private final Event onRotationEvent = new RotationEvent(ModuleUtil.getModule(SecretsHelper.class), params -> {
        if (Bus.isEventCancelled(RotationEvent.class)) return false;

        if (prevSlot != -1) {
            C.p().getInventory().setSelectedSlot(prevSlot);
            prevSlot = -1;
        }

        if (C.mc.currentScreen != null || !AreaUtil.isInDungeons()) return false;

        if (System.currentTimeMillis() - lastBatTime >= batTimeout) {
            batRotationPos = null;
            lastBatTime = -1;
            shouldKillBat = false;
        }
        if (batRotationPos != null) {
            params.rotation = RotationUtil.getRotation(batRotationPos.toCenterPos());
            return false;
        }

        if (!batAura) return false;

        BatEntity bat = getBat(batRange);
        if (bat == null) return false;

        // look for really close block that is in range of bat
        Optional<BlockPos> closestBlock = BlockPos.findClosest(bat.getBlockPos(), SPIRIT_SCEPTRE_EXPLOSION_RADIUS, SPIRIT_SCEPTRE_EXPLOSION_RADIUS, blockPos -> {
            return C.w().getBlockState(blockPos).isSolidBlock(C.w(), blockPos) && C.p().getEyePos().distanceTo(blockPos.toCenterPos()) <= C.p().getBlockInteractionRange();
        });

        if (closestBlock.isEmpty()) return false;
        Vec3d blockCentre = C.w().getBlockState(closestBlock.get()).getOutlineShape(C.w(), closestBlock.get()).getBoundingBox().getCenter();

        int prevSlot = C.p().getInventory().getSelectedSlot();
        if (!SkyblockItems.SPIRIT_SCEPTRE.switchToItem()) return false;
        BatAura.prevSlot = prevSlot;

        params.rotation = RotationUtil.getRotation(blockCentre);
        shouldKillBat = true;
        lastBatTime = System.currentTimeMillis();
        batRotationPos = closestBlock.get();

        return false;
    });

    private final Event onAllowRightClick = new CheckClickEvent.Right(ModuleUtil.getModule(SecretsHelper.class), params -> {
        if (!shouldKillBat) return false;

        UseItemBridge.from(C.mc).bridge$DoItemUse();
        shouldKillBat = false;
        ChatUtil.prefixMessage("KILLING THE BATMAN");

        return false;
    });

    private final Event handleReceive = new PacketEvent.Received(null, params -> {
        if (params.packet instanceof EntityStatusS2CPacket entityDamageS2CPacket && entityDamageS2CPacket.getStatus() == 3 && entityDamageS2CPacket.getEntity(C.w()) instanceof BatEntity) {
            batRotationPos = null;
            lastBatTime = -1;
            shouldKillBat = false;
        }

        return false;
    });

    private static BatEntity getBat(double radius) {
        return C.w().getOtherEntities(C.p(), Box.from(C.p().getEntityPos()).expand(radius)).stream()
                .filter(e -> e instanceof BatEntity bat && bat.getHealth() == SECRET_BAT_HEALTH && C.p().distanceTo(bat) <= radius)
                .sorted(Comparator.comparingDouble(e -> e.squaredDistanceTo(C.p().getEyePos())))
                .map(e -> (BatEntity) e)
                .findFirst().orElse(null);
    }
}

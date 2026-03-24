package com.scale.scalehackv4.modules.impl.skyblock.DungeonScanner;

import com.scale.scalehackv4.bridge.UseItemBridge;
import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.player.CheckClickEvent;
import com.scale.scalehackv4.events.impl.player.ClickedEvent;
import com.scale.scalehackv4.events.impl.player.PacketEvent;
import com.scale.scalehackv4.events.impl.player.RotationEvent;
import com.scale.scalehackv4.events.impl.skyblock.DungeonJoinEvent;
import com.scale.scalehackv4.events.impl.skyblock.DungeonStartEvent;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.ChatUtil;
import com.scale.scalehackv4.utils.client.ModuleUtil;
import com.scale.scalehackv4.utils.player.BlinkUtil;
import com.scale.scalehackv4.utils.player.Rotation;
import com.scale.scalehackv4.utils.player.RotationUtil;
import com.scale.scalehackv4.utils.player.WorldUtil;
import com.scale.scalehackv4.utils.skyblock.AreaUtil;
import com.scale.scalehackv4.utils.skyblock.DungeonMap.DungeonScanner;
import com.scale.scalehackv4.utils.skyblock.DungeonMap.Room;
import com.scale.scalehackv4.utils.skyblock.SecretsHelper.EtherwarpUtil;
import com.scale.scalehackv4.utils.skyblock.SecretsHelper.Node;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.s2c.common.CommonPingS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.Optional;

// todo: fix other mods conflicting
//  go up early and wait if bb
//  predict dungeon join
//  put pearls in hotbar, take off bonzo mask
//  still somehow fails to pearl
public class BloodBlink extends DungeonScannerModule {
    @RegisterSubModule(name = "Auto Scan")
    public static boolean autoScan = true;
    @RegisterSubModule(name = "Auto Blood")
    public static boolean autoBlood = true;

    public static class Progress {
        private int lastActionTick = -1;
        private int requiredPearls = -1;
        private int pearlsUsed = 0;
        private boolean escapedBox = false;
        private Rotation[] rotationChain = null;
        private Vec3d predictedFinalPos = null;
        private int rotationIndex = -1;
        private boolean goingBlood = false;
        private int pearlsIntoBlood = 0;
        private boolean currentlyBlinking = false;
    }

    public static Progress progress = null;

    private static long getServerTick = -1;
    private static final int REQUIRED_BLOOD_PEARLS = 2;

    private static final int BLOOD_ROOM_MIN_Y = 66;

    public static void enableBB() {
        progress = new Progress();
    }

    private final Event onDungeonJoin = new DungeonJoinEvent(ModuleUtil.getModule(DungeonScannerModule.class), params -> {
        if (!autoScan) return false;

        ChatUtil.prefixMessage("Scanning Dungeon");
        enableBB();
        return false;
    });

    private final Event onDungeonStart = new DungeonStartEvent(ModuleUtil.getModule(DungeonScannerModule.class), params -> {
        if (!autoBlood) return false;

        ChatUtil.prefixMessage("Going Blood");
        enableBB();
        return false;
    });

    /*
    private final Event onReceiveTP = new PacketEvent.Received(ModuleUtil.getModule(DungeonScannerModule.class), params -> {
        if (!(params.packet instanceof PlayerPositionLookS2CPacket posPacket) || !C.isInGame() || !EtherwarpUtil.isEtherwarpItem(C.p().getMainHandStack())) return false;
        float pitch = posPacket.change().pitch() == 0 ? RotationEvent.lastRotation.pitch : posPacket.change().pitch();
        float yaw = posPacket.change().yaw() == 0 ? RotationEvent.lastRotation.yaw : posPacket.change().yaw();
        Vec3d aotePos = EtherwarpUtil.getAotePos(pitch, yaw, C.p().getEntityPos(), 12);

        if (!posPacket.change().position().equals(aotePos)) {
            ChatUtil.prefixMessage("Failed " + aotePos + " " + posPacket.change().position());
        }

        return false;
    });
     */

    private final Event setAppropriateRotation = new RotationEvent(null, params -> {
        if (progress == null) return false;
        if (progress.requiredPearls == -1) {
            progress.requiredPearls = getPearlsRequired();
            if (progress.requiredPearls == -1 || (AreaUtil.isDungeonStarted() && getBloodSquare() == null)) {
                disable();
                ChatUtil.prefixMessage("Invalid position or blood not found");
                return false;
            }
        }

        if (shouldPearl()) params.rotation.pitch = -90;
        else {
            if (backInSpawn()) {
                disable();
                ChatUtil.prefixMessage("we have returned.");
                return false;
            }

            // wow, we made it out, id like to thank everyone, my mum, my sister, alan wood, 112batman, the whole gng yknow
            if (progress.rotationChain == null && getServerTick % OUT_OF_BOUNDS_TICKS >= 20) {
                progress.rotationChain = getRotationChain();
                if (progress.rotationChain == null) {
                    disable();
                    ChatUtil.prefixMessage("Failed to find a valid rotation chain");
                    return false;
                }
                ChatUtil.prefixMessage("blink time...");
                BlinkUtil.pushBlink(true, false);
                progress.currentlyBlinking = true;
            }
            if (progress.currentlyBlinking && getServerTick % OUT_OF_BOUNDS_TICKS < 20) {
                ChatUtil.prefixMessage("we are DISABLING BLINKING!");
                BlinkUtil.popBlink(true, false);
                progress.currentlyBlinking = false;
            }

            if (progress.rotationChain != null) {
                progress.rotationIndex++;
                if (progress.rotationIndex >= progress.rotationChain.length) disable();
                else params.rotation = progress.rotationChain[progress.rotationIndex];
            }
        }

        return false;
    });

    private final Event onAllowRightClick = new CheckClickEvent.Right(null, params -> {
        if (progress == null) return false;

        // we havent yet made it out of the slums. try aote+pearl out, chud
        if (!progress.escapedBox) {
            if (progress.pearlsUsed < progress.requiredPearls) pearlOut();
            return true;
        }

        // once we are out, we just tp to set coordinates n we gud
        if (progress.rotationChain != null) {
            if (progress.goingBlood && progress.rotationIndex >= progress.rotationChain.length-REQUIRED_BLOOD_PEARLS-1 && Node.Type.PEARL.getItem.test(null)) {
                progress.pearlsIntoBlood++;
                if (progress.pearlsIntoBlood >= REQUIRED_BLOOD_PEARLS) disable();
            }
            else if (!Node.Type.ETHERWARP.getItem.test(null)) return false;

            UseItemBridge.from(C.mc).bridge$DoItemUse();
        }

        return true;
    });

    // todo: move, duh
    private final Event trackTime = new PacketEvent.Received(null, params -> {
        if (params.packet instanceof WorldTimeUpdateS2CPacket timePacket) {
            getServerTick = timePacket.time();
            return false;
        }
        if (params.packet instanceof CommonPingS2CPacket) {
            getServerTick++;
        }
        return false;
    });


    private final Event cancelInteracting = new ClickedEvent.Right(null, params -> {
        return progress != null;
    });

    private final Event cancelPearlInteract = new PacketEvent.Sent(null, params -> {
        return C.isInGame() && progress != null && params.packet instanceof PlayerInteractBlockC2SPacket;
    });

    private static final int OUT_OF_BOUNDS_TICKS = 40;
    private static final int EDGE_TELEPORTS = 3;
    private static final int DOWN_TELEPORTS = 8;

    private static Rotation[] getRotationChain() {
        Optional<Room> spawnRoom = DungeonScanner.ROOMS.stream().filter(e -> e.type == Room.Type.SPAWN).findFirst();
        if (spawnRoom.isEmpty()) {
            ChatUtil.chat("Could not find spawn room!");
            return null;
        }
        boolean goingBlood = AreaUtil.isDungeonStarted();
        ChunkPos bloodRoom = getBloodSquare();
        if (goingBlood && bloodRoom == null) {
            ChatUtil.chat("Could not find blood room!");
            return null;
        }

        ChunkPos spawnSquare = spawnRoom.get().squares[0];

        // todo: change this to just find a gap and go for it
        float edgeOfMapYaw;
        if (spawnSquare.x == -12) edgeOfMapYaw = 90;
        else if (spawnSquare.x < spawnSquare.z) edgeOfMapYaw = 0;
        else if (spawnSquare.z == -12) edgeOfMapYaw = 180;
        else edgeOfMapYaw = 270;

        Vec3d targetPos = goingBlood
                ? new Vec3d(bloodRoom.x*16+7, -10, bloodRoom.z*16+7)
                : new Vec3d(-105, 0, -105);

        Vec3d tickPos = C.p().getEntityPos();
        // simulate the teleports to the edge of the map
        for (int i = 0; i < EDGE_TELEPORTS; i++) {
            tickPos = EtherwarpUtil.getAotePos(0, edgeOfMapYaw, tickPos, 12);
        }
        for (int i = 0; i < DOWN_TELEPORTS; i++) {
            tickPos = EtherwarpUtil.getAotePos(90, 0, tickPos, 12);
        }

        Rotation rotationToTarget = new Rotation(RotationUtil.getRotation(RotationUtil.getClientRotation(), tickPos, targetPos).yaw, 5);

        int teleportsToChunk = 0;
        for (int i = 0; i < OUT_OF_BOUNDS_TICKS; i++) {
            if (Math.abs(tickPos.x - targetPos.x) <= 10 && Math.abs(tickPos.z - targetPos.z) <= 10) break;

            // simulate each aote teleport towards the target
            tickPos = EtherwarpUtil.getAotePos(rotationToTarget.pitch, rotationToTarget.yaw, tickPos, 12);
            teleportsToChunk++;
        }

        int requiredUpTeleports = goingBlood ? Math.ceilDiv((int) (BLOOD_ROOM_MIN_Y - tickPos.y), 12)+REQUIRED_BLOOD_PEARLS : 0;
        int requiredTeleports = EDGE_TELEPORTS+DOWN_TELEPORTS+teleportsToChunk+requiredUpTeleports;
        if (requiredTeleports >= OUT_OF_BOUNDS_TICKS) {
            ChatUtil.prefixMessage("too far...?");
            return null;
        }

        if (goingBlood) progress.goingBlood = true;

        progress.predictedFinalPos = tickPos;

        Rotation[] rotations = new Rotation[requiredTeleports];

        for (int i = 0; i < rotations.length; i++) {
            if (i < EDGE_TELEPORTS) rotations[i] = new Rotation(edgeOfMapYaw, 0);
            else if (i < EDGE_TELEPORTS + DOWN_TELEPORTS) rotations[i] = new Rotation(0, 90);
            else if (i < EDGE_TELEPORTS + DOWN_TELEPORTS + teleportsToChunk) rotations[i] = rotationToTarget;
            else rotations[i] = new Rotation(0, -90);
        }

        return rotations;
    }

    private static ChunkPos getBloodSquare() {
        Optional<Room> bloodRoom = DungeonScanner.ROOMS.stream().filter(e -> e.type == Room.Type.BLOOD).findFirst();
        return bloodRoom.map(room -> room.squares[0]).orElse(null);
    }

    private static void pearlOut() {
        // if not below or inside a block, use aote
        if (C.w().getBlockState(C.p().getBlockPos().up(3)).getBlock() == Blocks.AIR && C.w().getBlockState(C.p().getBlockPos().up(1)).getBlock() == Blocks.AIR) {
            // if we have no aote, then stop
            if (!Node.Type.ETHERWARP.getItem.test(null)) {
                progress = null;
                return;
            }
        }
        // if we are looking at a block, find a pearl to use
        else if (!Node.Type.PEARL.getItem.test(null)) {
            progress = null;
            return;
        }
        // we have swapped to a pearl successfully, increment its counter here cause its certainly going to be used
        else progress.pearlsUsed++;

        UseItemBridge.from(C.mc).bridge$DoItemUse();
        progress.lastActionTick = WorldUtil.ticks;
    }

    private static int getPearlsRequired() {
        int pearlsRequired = -1;
        BlockPos pos = C.p().getBlockPos().up(2);
        // find the first block
        while (C.w().getBlockState(pos).getBlock() == Blocks.AIR) {
            pos = pos.up();
            if (pos.getY() == C.w().getHeight()) return -1;
        }
        // iterate up until there is no air
        while (C.w().getBlockState(pos.up(2)).getBlock() != Blocks.AIR) {
            pearlsRequired++;
            // you pearl up 2 blocks at a time
            pos = pos.up(2);
        }

        // if u finish pearling and end outside of a block, pos is invalid, gg go next.
        // AND ensure there are no more blocks above us, cant nobody hold me down, cant nobody break my stride
        if (isOpenAir(pos.up())) {
            // 1 more pearl to get in, and 1 more pearl to get out.
            return pearlsRequired + 2;
        }

        return -1;
    }

    private static boolean shouldPearl() {
        if (!progress.escapedBox) {
            boolean escaped = isOpenAir(C.p().getBlockPos().up());

            if (!escaped && WorldUtil.ticks - progress.lastActionTick > 5) {
                progress.pearlsUsed = 0;
                progress.requiredPearls = getPearlsRequired();
            }
            if (escaped) progress.escapedBox = true;
        }

        return !progress.escapedBox;
    }

    private static boolean isOpenAir(BlockPos pos) {
        while (C.w().getBlockState(pos).getBlock() == Blocks.AIR) {
            pos = pos.up();
            if (pos.getY() == C.w().getHeight()) return true;
        }

        return false;
    }

    private static boolean backInSpawn() {
        // if there are blocks above and below us, we have returned ig bro
        return C.w().raycast(new RaycastContext(
                C.p().getBlockPos().toBottomCenterPos(),
                new Vec3d(C.p().getX(), C.w().getBottomY(), C.p().getZ()),
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                C.p()
        )).getType() != HitResult.Type.MISS
                &&
                C.w().raycast(new RaycastContext(
                        C.p().getBlockPos().toBottomCenterPos().offset(Direction.UP, 1),
                        new Vec3d(C.p().getX(), C.w().getHeight(), C.p().getZ()),
                        RaycastContext.ShapeType.COLLIDER,
                        RaycastContext.FluidHandling.NONE,
                        C.p()
                )).getType() != HitResult.Type.MISS;
    }

    private static void disable() {
        if (progress == null) return;
        if (progress.currentlyBlinking) {
            ChatUtil.prefixMessage("we are DISABLING BLINKING!");
            BlinkUtil.popBlink(true, false);
        }
        progress = null;
    }
}
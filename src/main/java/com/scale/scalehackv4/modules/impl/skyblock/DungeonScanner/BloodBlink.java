package com.scale.scalehackv4.modules.impl.skyblock.DungeonScanner;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.client.ChatEvent;
import com.scale.scalehackv4.events.impl.client.WorldJoinEvent;
import com.scale.scalehackv4.events.impl.player.ClickedEvent;
import com.scale.scalehackv4.events.impl.player.KeyboardInputEvent;
import com.scale.scalehackv4.events.impl.player.RotationEvent;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.ChatUtil;
import com.scale.scalehackv4.utils.client.ModuleUtil;
import com.scale.scalehackv4.utils.player.Rotation;
import com.scale.scalehackv4.utils.player.RotationUtil;
import com.scale.scalehackv4.utils.skyblock.AreaUtil;
import com.scale.scalehackv4.utils.skyblock.ChainUtil;
import com.scale.scalehackv4.utils.skyblock.DungeonMap.DungeonScanner;
import com.scale.scalehackv4.utils.skyblock.DungeonMap.Room;
import com.scale.scalehackv4.utils.skyblock.SecretsHelper.EtherwarpUtil;
import com.scale.scalehackv4.utils.skyblock.SkyblockItems;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

// todo: fix chat hiding mods conflicting
public class BloodBlink extends DungeonScannerModule {
    @RegisterSubModule(name = "Auto Scan")
    public static boolean autoScan = true;
    @RegisterSubModule(name = "Auto Blood", parent = "Auto Scan")
    public static boolean autoBlood = true;

    // todo: ig this could be configable
    //@RegisterSubModule(name = "Timeout Delay", min = 500, max = 10000, increment = 100, parent = "Auto Scan")
    //public static int timeoutDelay = 1500;

    private static final int BLOOD_ROOM_MIN_Y = 66;
    private static final int MAX_OUT_BOUNDS_TICKS = 40;

    private static boolean attemptedScanning = false;

    private static long lastDungeonJoinMessage = 0;
    private static long lastWorldJoinTime = 0;

    private static final int DUNGEON_JOIN_TIMEOUT = 10000;
    private static final int SCAN_START_TIMEOUT = 2000;

    private static boolean canBB() {
        return SkyblockItems.ETHERWARP.getItemStack() != -1 && SkyblockItems.PEARL.getItemStack() != -1;
    }

    private final Event onAttemptJoinDungeon = new ChatEvent.Received(null, params -> {
        // regex ig bro...
        if (Formatting.strip(params.message).matches("-+\\n\\s*[^\\n]*? entered (?:MM )?The Catacombs, Floor (?:I|II|III|IV|V|VI|VII)!\\n-+")) {
            lastDungeonJoinMessage = System.currentTimeMillis();
        }

        return false;
    });

    public static void enableBB(boolean blood, boolean scan) {
        // todo: force aote and pearls into hotbar
        if (!canBB()) {
            ChatUtil.prefixMessage("hey god, you need AOTE and Pearls in your hotbar to use bb, dont dm me to make it auto put them in hotbar, im working on it!");
            return;
        }

        boolean hasEscapedBox = getCeilingY() == -1;
        Vec3d clipOutPos = C.p().getEntityPos();

        ChunkPos bloodRoom = blood ? getBloodSquare() : null;
        if (blood && bloodRoom == null && !scan) {
            ChatUtil.prefixMessage("Could not find blood room!");
            return;
        }

        Vec3d targetPos = bloodRoom == null ? new Vec3d(-105, BLOOD_ROOM_MIN_Y, -105) : new Vec3d(bloodRoom.x * 16 + 7, BLOOD_ROOM_MIN_Y, bloodRoom.z * 16 + 7);

        if (hasEscapedBox || (clipOutPos = queueClipOutChain(C.p().getEntityPos())) != null) {
            getTargetChain(clipOutPos, targetPos, bloodRoom != null);
        }
    }

    private final Event onWorldJoin = new WorldJoinEvent(null, params -> {
        attemptedScanning = false;
        lastWorldJoinTime = System.currentTimeMillis();

        return false;
    });

    private static boolean shouldBlockInputs() {
        return canBB() && autoScan && System.currentTimeMillis() - lastWorldJoinTime < SCAN_START_TIMEOUT && System.currentTimeMillis() - lastDungeonJoinMessage < DUNGEON_JOIN_TIMEOUT;
    }

    private final Event overrideKeyInputs = new KeyboardInputEvent(null, params -> {
        if (shouldBlockInputs()) {
            params.reset();
        }

        return false;
    }, 2000); // higher priority, to stop macros or wtv overthrowing it

    private final Event stopClicking = new ClickedEvent.Right(null, params -> {
        return shouldBlockInputs();
    });

    private static boolean shouldAutoClipOut() {
        if (!autoScan) return false;

        if (!ChainUtil.INTERACTION_CHAINS.isEmpty()
                || !ModuleUtil.isEnabled(DungeonScannerModule.class)
                || !insideRoom()
                || AreaUtil.isDungeonStarted()
        ) return false;

        boolean canScan = !attemptedScanning;
        boolean canBB = autoBlood && getBloodSquare() != null;

        if (!canScan && !canBB) return false;

        Room currentRoom = AreaUtil.getCurrentRoom();
        return currentRoom != null && currentRoom.type == Room.Type.SPAWN;
    }

    private final Event handleQueuing = new RotationEvent(null, params -> {
        if (shouldAutoClipOut()) {
            Optional<Room> spawnRoom = DungeonScanner.ROOMS.stream().filter(e -> e.type == Room.Type.SPAWN).findFirst();
            if (spawnRoom.isEmpty()) return false;

            enableBB(autoBlood, autoScan);
            lastDungeonJoinMessage = 0;
            attemptedScanning = true;
        }

        return false;
    });

    private static final int MAX_PACKETS = 60;
    private static final int EDGE_TELEPORTS = 3;
    private static final int DOWN_TELEPORTS = 8;

    private static Vec3d queueClipOutChain(Vec3d tickPos) {
        int ceilingY = getCeilingY();
        if (ceilingY != -1) {
            // teleport out of spawn
            ChainUtil.UpdatedInteractionChain teleportUp = new ChainUtil.UpdatedInteractionChain(
                    new ArrayList<>(),
                    getItem -> SkyblockItems.ETHERWARP.switchToItem(),
                    shouldSend -> true,
                    shouldCancel -> false,
                    false,
                    cancelBlink -> true,
                    packet -> packet instanceof PlayerPositionLookS2CPacket,
                    false
            );

            while (teleportUp.action().size() < MAX_PACKETS) {
                if (tickPos.y >= ceilingY-4) break;

                tickPos = EtherwarpUtil.getAotePos(-90, 0, tickPos, 12);
                teleportUp.queueInteract(0, -90);
            }

            // pearl through ceiling
            ChainUtil.UpdatedInteractionChain pearlOut = new ChainUtil.UpdatedInteractionChain(
                    new ArrayList<>(),
                    getItem -> SkyblockItems.PEARL.switchToItem(),
                    shouldSend -> ChainUtil.EXPECTED_PACKETS.isEmpty(),
                    shouldCancel -> false,
                    false,
                    cancelBlink -> true,
                    packet -> packet instanceof PlayerPositionLookS2CPacket,
                    false
            );

            int pearlsRequired = getPearlsRequired();
            if (pearlsRequired == -1) {
                ChatUtil.prefixMessage("Invalid position, there should be an even amount of blocks above you.");
                return null;
            }
            for (int i = 0; i < pearlsRequired; i++) {
                tickPos = tickPos.offset(Direction.UP, 2);
                pearlOut.queueInteract(0, -90);
            }

            ChainUtil.INTERACTION_CHAINS.add(teleportUp);
            ChainUtil.INTERACTION_CHAINS.add(pearlOut);

            return new Vec3d(tickPos.x, getCeilingY() + (getPearlsRequired()-1)*2, tickPos.z);
        }

        return null;
    }

    private static boolean getTargetChain(Vec3d tickPos, Vec3d targetPos, boolean clipUp) {
        Optional<Room> spawnRoom = DungeonScanner.ROOMS.stream().filter(e -> e.type == Room.Type.SPAWN).findFirst();
        if (spawnRoom.isEmpty()) {
            ChatUtil.prefixMessage("Could not find spawn room??");
            return false;
        }

        ChunkPos spawnSquare = spawnRoom.get().squares[0];

        // todo: change this to just find a gap and go for it
        float edgeOfMapYaw;
        if (spawnSquare.x == -12) edgeOfMapYaw = 90;
        else if (spawnSquare.x < spawnSquare.z) edgeOfMapYaw = 0;
        else if (spawnSquare.z == -12) edgeOfMapYaw = 180;
        else edgeOfMapYaw = 270;

        Vec3d finalTickPos = tickPos;
        ChainUtil.UpdatedInteractionChain teleportBelowBlood = new ChainUtil.UpdatedInteractionChain(
                new ArrayList<>(),
                getItem -> SkyblockItems.ETHERWARP.switchToItem(),
                shouldSend -> !clipUp || AreaUtil.getDungeonStartMs() < 2000 || AreaUtil.isDungeonStarted(),
                shouldCancel -> (C.p().getEntityPos().distanceTo(finalTickPos) < 0.5f && ChainUtil.packetsSent == 0 && ChainUtil.EXPECTED_PACKETS.isEmpty()) || (!clipUp && AreaUtil.isDungeonStarted()),
                true,
                cancelBlink -> ChainUtil.INTERACTION_CHAINS.getFirst().action().isEmpty() && AreaUtil.getServerTick() % MAX_OUT_BOUNDS_TICKS <= 20 && (!clipUp || AreaUtil.isDungeonStarted()),
                packet -> !clipUp || packet instanceof PlayerPositionLookS2CPacket,
                false
        );

        // fun fact: hypixel still allows multiple tp's a tick if they have the same yaw and pitch

        // teleport to the edge of the map
        teleportBelowBlood.queueInteracts(EDGE_TELEPORTS, edgeOfMapYaw, 0);
        for (int i = 0; i < EDGE_TELEPORTS; i++) tickPos = EtherwarpUtil.getAotePos(0, edgeOfMapYaw, tickPos, 12);

        // teleport down to the bottom of the map
        teleportBelowBlood.queueInteracts(DOWN_TELEPORTS, 0, 90);
        for (int i = 0; i < DOWN_TELEPORTS; i++) tickPos = EtherwarpUtil.getAotePos(90, 0, tickPos, 12);

        Rotation rotationToTarget = new Rotation(RotationUtil.getRotation(RotationUtil.getClientRotation(), tickPos, targetPos).yaw, 0);

        // try and tp to below blood
        int packetsToBelowBlood = 0;
        while (teleportBelowBlood.action().size() < MAX_PACKETS) {
            int maxDistance = 16;
            double xDistance = Math.abs(tickPos.x - targetPos.x);
            double zDistance = Math.abs(tickPos.z - targetPos.z);

            if (xDistance+zDistance <= maxDistance) break;

            tickPos = EtherwarpUtil.getAotePos(rotationToTarget.pitch, rotationToTarget.yaw, tickPos, 12);
            packetsToBelowBlood++;
        }
        teleportBelowBlood.queueInteracts(packetsToBelowBlood, rotationToTarget.yaw, rotationToTarget.pitch);

        Rotation bloodFloorRotation = RotationUtil.getRotation(RotationUtil.getClientRotation(), tickPos, targetPos);
        int packetsUpToBloodFloor = 0;
        // aote up to blood floor
        while (teleportBelowBlood.action().size() < MAX_PACKETS) {
            if (tickPos.y >= targetPos.y-4 || packetsUpToBloodFloor >= 10) break;

            tickPos = EtherwarpUtil.getAotePos(bloodFloorRotation.pitch, bloodFloorRotation.yaw, tickPos, 12);
            packetsUpToBloodFloor++;
        }
        teleportBelowBlood.queueInteracts(packetsUpToBloodFloor, bloodFloorRotation.yaw, bloodFloorRotation.pitch);

        if (teleportBelowBlood.action().size() >= MAX_PACKETS) {
            ChatUtil.prefixMessage("Too many packets required, aborting.");
            return false;
        }

        ChainUtil.INTERACTION_CHAINS.add(teleportBelowBlood);

        if (clipUp) {
            // 2 pearls up into blood
            ChainUtil.UpdatedInteractionChain pearlIntoBlood = new ChainUtil.UpdatedInteractionChain(
                    new ArrayList<>(),
                    getItem -> SkyblockItems.PEARL.switchToItem(),
                    shouldSend -> ChainUtil.EXPECTED_PACKETS.isEmpty(),
                    shouldCancel -> false,
                    false,
                    cancelBlink -> true,
                    packet -> packet instanceof PlayerPositionLookS2CPacket,
                    false
            );

            // 2 pearls up, into blood!
            pearlIntoBlood.queueInteract(0, -90);
            pearlIntoBlood.queueInteract(0, -90);
            // 2 pearls DOWN, to make sure we above the cobblestone
            pearlIntoBlood.queueInteract(0, 90);
            pearlIntoBlood.queueInteract(0, 90);
            ChainUtil.INTERACTION_CHAINS.add(pearlIntoBlood);
        }

        return true;
    }

    private static ChunkPos getBloodSquare() {
        for (int x : DungeonScanner.MAP_DIVISIONS.keySet()) {
            for (int z : DungeonScanner.MAP_DIVISIONS.get(x).keySet()) {
                DungeonScanner.MapDivision mapDivision = DungeonScanner.MAP_DIVISIONS.get(x).get(z);
                if (mapDivision.doorType() != DungeonScanner.DoorType.BLOOD) continue;

                ArrayList<Room> connectedRooms = DungeonScanner.getConnectedRooms(x, z);
                if (connectedRooms.isEmpty()) return null;
                // blood already found ig bro
                Room room = connectedRooms.stream().filter(e -> e.type == Room.Type.BLOOD).findFirst().orElse(null);
                if (room != null) return room.squares[0];

                ChunkPos oppositeSquare = Arrays.stream(connectedRooms.getFirst().squares)
                        .filter(e -> DungeonScanner.isChunkConnected(e, x, z))
                        .findFirst()
                        .orElse(null);

                // shouldnt happen but wtv
                if (oppositeSquare == null) return null;

                // get opposite of the connected square offset.
                return new ChunkPos((int) (x + Math.signum(x-oppositeSquare.x)), (int) (z + Math.signum(z-oppositeSquare.z)));
            }
        }

        return null;
    }

    private static int getPearlsRequired() {
        int pearlsRequired = -1;
        int ceilingY = getCeilingY();

        if (ceilingY == -1) return -1;
        BlockPos pos = new BlockPos(C.p().getBlockPos().getX(), getCeilingY(), C.p().getBlockPos().getZ());

        // iterate up until there is no air
        while (C.w().getBlockState(pos.up(2)).getBlock() != Blocks.AIR && pos.getY() < C.w().getHeight()) {
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

    private static boolean isOpenAir(BlockPos pos) {
        while (C.w().getBlockState(pos).getBlock() == Blocks.AIR) {
            pos = pos.up();
            if (pos.getY() >= C.w().getHeight()) return true;
        }

        return false;
    }

    private static int getCeilingY() {
        BlockHitResult aboveBlock = C.w().raycast(new RaycastContext(
                C.p().getBlockPos().toBottomCenterPos().offset(Direction.UP, 1),
                new Vec3d(C.p().getX(), C.w().getHeight(), C.p().getZ()),
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                C.p()
        ));

        if (aboveBlock.getType() == HitResult.Type.MISS) return -1;
        return aboveBlock.getBlockPos().getY();
    }

    private static boolean insideRoom() {
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
}
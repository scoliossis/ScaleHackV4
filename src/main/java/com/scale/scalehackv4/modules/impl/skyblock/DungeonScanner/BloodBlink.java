package com.scale.scalehackv4.modules.impl.skyblock.DungeonScanner;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.client.ChatEvent;
import com.scale.scalehackv4.events.impl.client.WorldJoinEvent;
import com.scale.scalehackv4.events.impl.player.*;
import com.scale.scalehackv4.events.impl.skyblock.DungeonJoinEvent;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.ChatUtil;
import com.scale.scalehackv4.utils.client.ModuleUtil;
import com.scale.scalehackv4.utils.client.PacketUtil;
import com.scale.scalehackv4.utils.player.Rotation;
import com.scale.scalehackv4.utils.player.RotationUtil;
import com.scale.scalehackv4.utils.skyblock.AreaUtil;
import com.scale.scalehackv4.utils.skyblock.ChainUtil;
import com.scale.scalehackv4.utils.skyblock.DungeonMap.DungeonScanner;
import com.scale.scalehackv4.utils.skyblock.DungeonMap.Room;
import com.scale.scalehackv4.utils.skyblock.SecretsHelper.EtherwarpUtil;
import com.scale.scalehackv4.utils.skyblock.SecretsHelper.Node;
import com.scale.scalehackv4.utils.skyblock.SkyblockItems;
import net.minecraft.block.Blocks;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
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
//  bring back legit mode
//  pearls drop now...?
public class BloodBlink extends DungeonScannerModule {
    @RegisterSubModule(name = "Auto Scan")
    public static boolean autoScan = true;
    @RegisterSubModule(name = "Auto Blood")
    public static boolean autoBlood = true;

    @RegisterSubModule(name = "Timeout Delay", min = 500, max = 10000, increment = 100)
    public static int timeoutDelay = 1500;

    private static final int BLOOD_ROOM_MIN_Y = 66;
    private static final int MAX_OUT_BOUNDS_TICKS = 40;

    private static ChainUtil.InteractionChain interactionChain;
    private static ChainUtil.QueuedInteractionChain queuedInteractionChain;

    private static int interactionChainIndex = 0;

    private static final ArrayList<Long> expectedTeleports = new ArrayList<>();
    private static boolean pearlResponded = true;

    private static int clipAttempts = 0;

    private static boolean attemptedScanning = false;

    private static long lastDungeonJoinMessage = 0;
    private static boolean joiningDungeon = false;

    private static final int DUNGEON_JOIN_TIMEOUT = 10000;

    private final ArrayList<ChainUtil.InteractionChainNode> pendingInteractions = new ArrayList<>();

    public static Vec3d getDefaultTarget() {
        if (!AreaUtil.isDungeonStarted()) return getBestScanningPos();

        ChunkPos bloodRoom = getBloodSquare();
        if (bloodRoom == null) {
            ChatUtil.prefixMessage("Could not find blood room!");
            return null;
        }

        return new Vec3d(bloodRoom.x * 16 + 7, BLOOD_ROOM_MIN_Y, bloodRoom.z * 16 + 7);
    }

    private static Vec3d getBestScanningPos() {
        Room currentRoom = AreaUtil.getCurrentRoom();
        double middleX = -105;
        double middleZ = -105;

        if (currentRoom == null || !autoBlood) return new Vec3d(middleX, 0, middleZ);
        ChunkPos currentSquare = currentRoom.squares[0];

        // since we are INSIDE the room, the chunk offset is 1 extra
        int bottomChunk = -(DungeonScanner.DUNGEON_CHUNK_OFFSET+1);

        // go an extra chunk forward if ur on a corner, because that whole row (SHOULD) have been fully scanned
        if (currentSquare.x == -DungeonScanner.MAX_DUNGEON_CHUNKS) middleX += 32;
        else if (currentSquare.x == bottomChunk) middleX -= 32;

        if (currentSquare.z == -DungeonScanner.MAX_DUNGEON_CHUNKS) middleZ += 32;
        else if (currentSquare.z == bottomChunk) middleZ -= 32;

        return new Vec3d(middleX, 0, middleZ);
    }

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

    public static void enableBB(boolean blood, boolean scan, boolean clipUp) {
        // todo: force aote and pearls into hotbar
        if (!canBB()) {
            ChatUtil.prefixMessage("hey god, you need AOTE and Pearls in your hotbar to use bb, dont dm me to make it auto put them in hotbar, im working on it!");
            return;
        }

        boolean hasEscapedBox = getCeilingY() == -1;
        interactionChain = hasEscapedBox ? getTargetChain(C.p().getEntityPos(), getDefaultTarget(), clipUp) : getClipOutChain(C.p().getEntityPos());
        interactionChainIndex = 0;
        pearlResponded = true;

        if (interactionChain == null || hasEscapedBox) return;

        if (clipAttempts == 0 && scan && !AreaUtil.isDungeonStarted()) {
            queuedInteractionChain = new ChainUtil.QueuedInteractionChain(
                    getTargetChain(interactionChain.endPos(), getBestScanningPos(), false),
                    e -> AreaUtil.getServerTick() % MAX_OUT_BOUNDS_TICKS <= 5 && AreaUtil.getDungeonStartMs() > 2000,
                    e -> AreaUtil.isDungeonStarted()
            );
        }
        else if (blood) {
            ChunkPos bloodRoom = getBloodSquare();
            if (bloodRoom == null) {
                interactionChain = null;
                ChatUtil.prefixMessage("Could not find blood room!");
                return;
            }

            queuedInteractionChain = new ChainUtil.QueuedInteractionChain(
                    getTargetChain(interactionChain.endPos(), new Vec3d(bloodRoom.x * 16 + 7, BLOOD_ROOM_MIN_Y, bloodRoom.z * 16 + 7), true),
                    e -> AreaUtil.isDungeonStarted() && AreaUtil.getServerTick() % MAX_OUT_BOUNDS_TICKS <= MAX_OUT_BOUNDS_TICKS/2,
                    e -> getBloodSquare() == null && insideRoom()
            );
        }

        clipAttempts++;
    }

    private final Event onDungeonJoin = new DungeonJoinEvent(null, params -> {
        clipAttempts = 0;
        return false;
    });

    private final Event onWorldJoin = new WorldJoinEvent(null, params -> {
        clipAttempts = 0;
        attemptedScanning = false;
        disable();
        if (System.currentTimeMillis() - lastDungeonJoinMessage < DUNGEON_JOIN_TIMEOUT) {
            joiningDungeon = autoScan && canBB() && ModuleUtil.isEnabled(DungeonScannerModule.class);
        }
        return false;
    });

    private final Event overrideKeyInputs = new KeyboardInputEvent(null, params -> {
        if (queuedInteractionChain != null || interactionChain != null || joiningDungeon) {
            params.reset();
        }

        return false;
    }, 2000); // higher priority, to stop macros or wtv overthrowing it

    private final Event stopClicking = new ClickedEvent.Right(null, params -> {
        return queuedInteractionChain != null || interactionChain != null || joiningDungeon;
    });

    private static boolean shouldAutoClipOut() {
        if (queuedInteractionChain != null
                || interactionChain != null
                || !ModuleUtil.isEnabled(DungeonScannerModule.class)
                || !insideRoom()
                || AreaUtil.isDungeonStarted()
        ) return false;

        boolean canScan = autoScan && !attemptedScanning;
        boolean canBB = autoBlood && getBloodSquare() != null;

        if (!canScan && !canBB) return false;

        Room currentRoom = AreaUtil.getCurrentRoom();
        return currentRoom != null && currentRoom.type == Room.Type.SPAWN;
    }

    private final Event handleHeldItem = new RotationEvent(null, params -> {
        if (System.currentTimeMillis() - lastDungeonJoinMessage > DUNGEON_JOIN_TIMEOUT) {
            joiningDungeon = false;
        }

        // stop it ig bro
        if ((!canBB() && (joiningDungeon || interactionChain != null || queuedInteractionChain != null)) || (interactionChain == null && queuedInteractionChain != null && queuedInteractionChain.deactivationPredicate().test(null))) {
            disable();
            return false;
        }

        // forget failed teleports
        while (!expectedTeleports.isEmpty() && System.currentTimeMillis() - expectedTeleports.getFirst() > timeoutDelay) {
            expectedTeleports.removeFirst();
        }
        if (expectedTeleports.isEmpty() && !pearlResponded) {
            ChatUtil.prefixMessage("Failed to teleport, disabling.");
            disable();
            return false;
        }

        if (shouldAutoClipOut()) {
            Optional<Room> spawnRoom = DungeonScanner.ROOMS.stream().filter(e -> e.type == Room.Type.SPAWN).findFirst();
            if (spawnRoom.isEmpty()) return false;

            enableBB(autoBlood, autoScan, false);
            lastDungeonJoinMessage = 0;
            attemptedScanning = true;
        }

        if (interactionChain == null) {
            if (queuedInteractionChain != null && queuedInteractionChain.activationPredicate().test(null)) {
                interactionChain = queuedInteractionChain.interactionChain();
                queuedInteractionChain = null;
            }
            else return false;
        }

        Node.Type nodeItem = interactionChain.interactionChainNodes()[interactionChainIndex].itemType();
        if (!nodeItem.getItem.test(null)) return false;

        // i never do for loops like this, how cool
        for (; interactionChainIndex < interactionChain.interactionChainNodes().length; interactionChainIndex++) {
            ChainUtil.InteractionChainNode node = interactionChain.interactionChainNodes()[interactionChainIndex];
            // take a tick gap between switching items / using pearls
            if (node.itemType() != nodeItem || !pearlResponded) return false;
            if (nodeItem == Node.Type.PEARL) pearlResponded = false;

            pendingInteractions.add(node);
            expectedTeleports.add(System.currentTimeMillis());
        }

        finishInteract();

        return false;
    });

    private final Event onReceiveTP = new PacketEvent.Received(null, params -> {
        if (!(params.packet instanceof PlayerPositionLookS2CPacket) || expectedTeleports.isEmpty()) return false;

        expectedTeleports.removeFirst();
        pearlResponded |= expectedTeleports.isEmpty();

        return false;
    });

    private final Event executeTeleports = new PlayerUpdateEvent(null, params -> {
        while (!pendingInteractions.isEmpty()) {
            Rotation nodeItem = pendingInteractions.removeFirst().rotation();
            PacketUtil.sendSequencedPacket(sequence -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, sequence, nodeItem.yaw, nodeItem.pitch));
        }

        return false;
    });

    private static final int MAX_PACKETS = 60;
    private static final int EDGE_TELEPORTS = 3;
    private static final int DOWN_TELEPORTS = 9;

    private static ChainUtil.InteractionChain getClipOutChain(Vec3d tickPos) {
        ArrayList<ChainUtil.InteractionChainNode> interactionChain = new ArrayList<>();

        int ceilingY = getCeilingY();
        if (ceilingY != -1) {
            tickPos = new Vec3d(Math.floor(tickPos.x)+0.5, tickPos.y, Math.floor(tickPos.z)+0.5);
            // teleport out of spawn
            while (interactionChain.size() < MAX_PACKETS) {
                if (tickPos.y >= ceilingY-2) break;

                tickPos = EtherwarpUtil.getAotePos(-90, 0, tickPos, 12);
                interactionChain.add(new ChainUtil.InteractionChainNode(new Rotation(0, -90), BlockPos.ofFloored(tickPos), Node.Type.ETHERWARP));
            }

            // pearl through ceiling
            int pearlsRequired = getPearlsRequired();
            if (pearlsRequired == -1) {
                ChatUtil.prefixMessage("Invalid position, there should be an even amount of blocks above you.");
                return null;
            }
            for (int i = 0; i < pearlsRequired; i++) {
                tickPos = tickPos.offset(Direction.UP, 2);
                interactionChain.add(new ChainUtil.InteractionChainNode(new Rotation(0, -90), BlockPos.ofFloored(tickPos), Node.Type.PEARL));
            }
            return new ChainUtil.InteractionChain(interactionChain.toArray(ChainUtil.InteractionChainNode[]::new), tickPos);
        }

        return null;
    }

    private static ChainUtil.InteractionChain getTargetChain(Vec3d tickPos, Vec3d targetPos, boolean clipUp) {
        Optional<Room> spawnRoom = DungeonScanner.ROOMS.stream().filter(e -> e.type == Room.Type.SPAWN).findFirst();
        if (spawnRoom.isEmpty()) {
            ChatUtil.prefixMessage("Could not find spawn room??");
            return null;
        }

        ArrayList<ChainUtil.InteractionChainNode> interactionChain = new ArrayList<>();

        ChunkPos spawnSquare = spawnRoom.get().squares[0];

        // todo: change this to just find a gap and go for it
        float edgeOfMapYaw;
        if (spawnSquare.x == -12) edgeOfMapYaw = 90;
        else if (spawnSquare.x < spawnSquare.z) edgeOfMapYaw = 0;
        else if (spawnSquare.z == -12) edgeOfMapYaw = 180;
        else edgeOfMapYaw = 270;

        // teleport to the edge of the map
        for (int i = 0; i < EDGE_TELEPORTS; i++) {
            tickPos = EtherwarpUtil.getAotePos(0, edgeOfMapYaw, tickPos, 12);
            interactionChain.add(new ChainUtil.InteractionChainNode(new Rotation(edgeOfMapYaw, 0), BlockPos.ofFloored(tickPos), Node.Type.ETHERWARP));
        }
        // teleport down to the bottom of the map
        for (int i = 0; i < DOWN_TELEPORTS; i++) {
            tickPos = EtherwarpUtil.getAotePos(90, 0, tickPos, 12);
            interactionChain.add(new ChainUtil.InteractionChainNode(new Rotation(0, 90), BlockPos.ofFloored(tickPos), Node.Type.ETHERWARP));
        }

        Rotation rotationToTarget = new Rotation(RotationUtil.getRotation(RotationUtil.getClientRotation(), tickPos, targetPos).yaw, -5);

        // try and tp to below blood
        while (interactionChain.size() < MAX_PACKETS) {
            int maxDistance = 12;
            double xDistance = Math.abs(tickPos.x - targetPos.x);
            double zDistance = Math.abs(tickPos.z - targetPos.z);

            if (xDistance+zDistance <= maxDistance) break;

            tickPos = EtherwarpUtil.getAotePos(rotationToTarget.pitch, rotationToTarget.yaw, tickPos, 12);
            interactionChain.add(new ChainUtil.InteractionChainNode(rotationToTarget, BlockPos.ofFloored(tickPos), Node.Type.ETHERWARP));
        }

        Rotation bloodFloorRotation = RotationUtil.getRotation(RotationUtil.getClientRotation(), tickPos, targetPos);
        // aote up to blood floor
        while (interactionChain.size() < MAX_PACKETS) {
            // todo: requires 1 less aote than expected, find out why...
            if (tickPos.y >= targetPos.y-14) break;

            tickPos = EtherwarpUtil.getAotePos(bloodFloorRotation.pitch, bloodFloorRotation.yaw, tickPos, 12);
            interactionChain.add(new ChainUtil.InteractionChainNode(bloodFloorRotation, BlockPos.ofFloored(tickPos), Node.Type.ETHERWARP));
        }

        if (clipUp) {
            // 2 pearls up into blood
            interactionChain.add(new ChainUtil.InteractionChainNode(new Rotation(0, -90), BlockPos.ofFloored(tickPos), Node.Type.PEARL));
            interactionChain.add(new ChainUtil.InteractionChainNode(new Rotation(0, -90), BlockPos.ofFloored(tickPos), Node.Type.PEARL));
            interactionChain.add(new ChainUtil.InteractionChainNode(new Rotation(0, 90), BlockPos.ofFloored(tickPos), Node.Type.PEARL));
            tickPos = tickPos.offset(Direction.UP, 2);
        }
        if (interactionChain.size() >= MAX_PACKETS) {
            ChatUtil.prefixMessage("Too many packets required, aborting.");
            return null;
        }

        return new ChainUtil.InteractionChain(interactionChain.toArray(ChainUtil.InteractionChainNode[]::new), tickPos);
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

    public static void disable() {
        pearlResponded = true;
        queuedInteractionChain = null;
        expectedTeleports.clear();

        finishInteract();
    }

    public static void finishInteract() {
        interactionChain = null;
        interactionChainIndex = 0;
    }
}
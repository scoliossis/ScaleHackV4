package com.scale.scalehackv4.modules.impl.skyblock.SecretsHelper;

import com.scale.scalehackv4.bridge.UseItemBridge;
import com.scale.scalehackv4.events.Bus;
import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.client.ChatEvent;
import com.scale.scalehackv4.events.impl.client.SubModuleToggledEvent;
import com.scale.scalehackv4.events.impl.client.WorldJoinEvent;
import com.scale.scalehackv4.events.impl.player.CheckClickEvent;
import com.scale.scalehackv4.events.impl.player.ClickedEvent;
import com.scale.scalehackv4.events.impl.player.RotationEvent;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.ChatUtil;
import com.scale.scalehackv4.utils.client.ModuleUtil;
import com.scale.scalehackv4.utils.player.BlockTracker;
import com.scale.scalehackv4.utils.player.RotationUtil;
import com.scale.scalehackv4.utils.skyblock.AreaUtil;
import com.scale.scalehackv4.utils.skyblock.DungeonMap.Room;
import com.scale.scalehackv4.utils.skyblock.SkyblockItems;
import lombok.AllArgsConstructor;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeverBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

// todo: add option to select whatever blocks
//  check if the pos is blacklisted in the room
//  locked chest check
//  check surroundings, no lever (idk why i choose that block, look into it) should be touching eachother (u should be touching me tho)
//  check the room the SECRET is in, not the guy.
//  other players using spirit sceptre fricks up bat aura
public class SecretAura extends SecretsHelper {
    @RegisterSubModule(name = "Secret Aura", hasKeybind = true)
    public static boolean secretAura = true;

    @RegisterSubModule(name = "Rotate", parent = "Secret Aura")
    public static boolean rotate = true;
    @RegisterSubModule(name = "Swap To Stonk", parent = "Secret Aura")
    public static boolean swapStonk = true;
    @RegisterSubModule(name = "Full Block Range Check", parent = "Secret Aura", hasKeybind = true)
    public static boolean fullBlockRangeCheck = true;
    @RegisterSubModule(name = "Dungeons Only", parent = "Secret Aura")
    public static boolean dungeonsOnly = true;
    @RegisterSubModule(name = "Reach", parent = "Secret Aura", increment = 0.1, min = 1, max = 6)
    public static double reach = 4.5;
    @RegisterSubModule(name = "Bat Aura", parent = "Secret Aura", hasKeybind = true)
    public static boolean batAura = true;

    @RegisterSubModule(name = "Retry Delay", parent = "Secret Aura", increment = 50, min = 100, max = 5000)
    public static long delay = 1000;

    protected static final ConcurrentHashMap<BlockPos, Long> INTERACTED_POSITIONS = new ConcurrentHashMap<>();

    private static boolean hasRedstoneKey = false;
    private static CurrentTarget currentTarget = null;
    private record CurrentTarget(BlockPos pos, AuraBlock block, BlockHitResult hitResult) {}
    private static int prevSlot = -1;

    private static final ArrayList<Long> expectedBats = new ArrayList<>();
    private static boolean batTime = false;
    private static final long MAX_BAT_TIME = 1500;
    private static final int SPIRIT_SCEPTRE_EXPLOSION_RADIUS = 4;
    private static final int possibleBatRadius = 8;
    private static final int batScanRadius = 15;

    private static final List<Integer> IGNORE_CHEST_ROOM_HASHES = List.of(
            -949494214, // three weirdos
            -550739784, // ice path iirc
            -637013673, // tp maze
            -160860033 // waterboard
    );

    @AllArgsConstructor
    private enum AuraBlock {
        SKULL(
                new Block[]{Blocks.PLAYER_HEAD, Blocks.PLAYER_WALL_HEAD},
                pos -> {
                    BlockEntity blockEntity = C.w().getBlockEntity(pos);
                    if (!(blockEntity instanceof SkullBlockEntity skullBlockEntity)) return false;

                    ProfileComponent profileComponent = skullBlockEntity.getOwner();
                    if (profileComponent == null) return false;

                    UUID skinID = skullBlockEntity.getOwner().getGameProfile().id();

                    boolean isWitherEssence = skinID.toString().equals("e0f3e929-869e-3dca-9504-54c666ee6f23");
                    if (isWitherEssence) return true;

                    boolean isRedstoneSkull = skinID.toString().equals("fed95410-aba1-39df-9b95-1d4f361eb66e");
                    // only skulls we want to click are wither essence and redstone skulls
                    if (!isRedstoneSkull) return false;

                    // ensure its not touching a redstone block
                    for (Direction direction : Direction.values()) {
                        BlockPos neighbourPos = pos.offset(direction);
                        // evil redstone dtc, stop
                        if (C.w().getBlockState(neighbourPos).getBlock() == Blocks.REDSTONE_BLOCK) return false;
                    }

                    return true;
                }
        ),
        LEVER(
                new Block[]{Blocks.LEVER},
                pos -> {
                    Room currentRoom = AreaUtil.getCurrentRoom();
                    if (AreaUtil.isInBoss() || (currentRoom != null && currentRoom.name.equals("Water Board"))) return false;

                    BlockState blockState = C.w().getBlockState(pos);
                    return blockState.getBlock() instanceof LeverBlock && !blockState.get(LeverBlock.POWERED);
                }
        ),
        REDSTONE_BLOCK(
                new Block[]{Blocks.REDSTONE_BLOCK},
                pos -> {
                    for (Direction direction : Direction.values()) {
                        BlockPos neighbourPos = pos.offset(direction);
                        if (C.w().getBlockState(neighbourPos).getBlock() == Blocks.REDSTONE_BLOCK) return false;
                    }
                    return hasRedstoneKey && C.w().getBlockState(pos).getBlock() == Blocks.REDSTONE_BLOCK;
                }
        ),
        CHEST(
                new Block[]{Blocks.CHEST, Blocks.TRAPPED_CHEST},
                pos -> {
                    Room currentRoom = AreaUtil.getCurrentRoom();
                    if (currentRoom != null && IGNORE_CHEST_ROOM_HASHES.contains(currentRoom.hash)) return false;

                    BlockEntity blockEntity = C.w().getBlockEntity(pos);
                    if (blockEntity instanceof ChestBlockEntity chestBlockEntity) {
                        return chestBlockEntity.getAnimationProgress(0) == 0;
                    }
                    return false;
                }
        );

        private final Block[] validBlocks;
        private final Predicate<BlockPos> condition;
    }

    private final Event onRotationEvent = new RotationEvent(ModuleUtil.getModule(SecretsHelper.class), params -> {
        if (prevSlot != -1) {
            C.p().getInventory().setSelectedSlot(prevSlot);
            prevSlot = -1;
        }

        if (!secretAura || C.mc.currentScreen != null || (dungeonsOnly && !AreaUtil.isInDungeons())) return false;

        for (BlockPos blockPos : INTERACTED_POSITIONS.keySet()) {
            long interactedTime = INTERACTED_POSITIONS.get(blockPos);
            if (System.currentTimeMillis() - interactedTime < delay) continue;

            INTERACTED_POSITIONS.remove(blockPos);
        }

        // get rid of bats
        while (!expectedBats.isEmpty() && System.currentTimeMillis() - expectedBats.getFirst() > MAX_BAT_TIME) expectedBats.removeFirst();

        for (AuraBlock auraBlock : AuraBlock.values()) {
            for (Block block : auraBlock.validBlocks) {
                for (BlockPos pos : BlockTracker.getBlockPositions(block)) {
                    if (pos == null || INTERACTED_POSITIONS.containsKey(pos)) continue;

                    // basic reach check + condition check done after cause its more expensive on the cpu than the distance check
                    if (Math.sqrt(pos.getSquaredDistance(C.p().getEyePos()))-1 > reach || !auraBlock.condition.test(pos)) continue;

                    BlockState blockState = C.w().getBlockState(pos);
                    Vec3d targetMiddle = new Vec3d(pos.getX(), pos.getY(), pos.getZ()).add(blockState.getOutlineShape(C.w(), pos).getBoundingBox().getCenter());

                    BlockHitResult blockHitResult = C.w().raycastBlock(
                            C.p().getEyePos(),
                            targetMiddle,
                            pos,
                            blockState.getOutlineShape(C.w(), pos),
                            blockState
                    );

                    // make CERTAIN we are UP
                    if (blockHitResult == null) continue;

                    double distance = fullBlockRangeCheck
                            ? Math.sqrt(C.p().getEyePos().squaredDistanceTo(pos.toCenterPos())) - 0.5
                            : Math.sqrt(C.p().getEyePos().squaredDistanceTo(blockHitResult.getPos()));
                    if (blockHitResult.getType() == HitResult.Type.MISS || distance > reach) continue;

                    currentTarget = new CurrentTarget(pos, auraBlock, blockHitResult);
                    if (rotate) params.rotation = RotationUtil.getRotation(blockHitResult.getPos());
                    if (swapStonk) {
                        int prevSlot = C.p().getInventory().getSelectedSlot();
                        if (SkyblockItems.DUNGEON_BREAKER.switchToItem()) SecretAura.prevSlot = prevSlot;
                    }

                    return false;
                }
            }
        }

        // ignore any spirit sceptre bats
        int minBats = expectedBats.isEmpty() ? 0 : expectedBats.size();
        if (shouldBatAura(minBats)) {
            List<BatEntity> batsPositions = getBatsInRange(possibleBatRadius);

            // look for really close block that is in range of bat
            Optional<BlockPos> closestBlock = BlockPos.findClosest(C.p().getBlockPos().up(2), 3, 3, blockPos -> {
                BatEntity closestBat = batsPositions.stream().min(Comparator.comparingDouble(bat -> blockPos.getSquaredDistance(bat.getEntityPos()))).orElse(null);
                if (closestBat == null) return false;

                double closestBatDistance = Math.sqrt(blockPos.getSquaredDistance(closestBat.getEntityPos()));
                return C.w().getBlockState(blockPos).isSolidBlock(C.w(), blockPos) && closestBatDistance <= SPIRIT_SCEPTRE_EXPLOSION_RADIUS;
            });

            if (closestBlock.isEmpty()) return false;
            Vec3d blockCentre = C.w().getBlockState(closestBlock.get()).getOutlineShape(C.w(), closestBlock.get()).getBoundingBox().getCenter();

            int prevSlot = C.p().getInventory().getSelectedSlot();
            if (!SkyblockItems.SPIRIT_SCEPTRE.switchToItem()) return false;
            SecretAura.prevSlot = prevSlot;

            params.rotation = RotationUtil.getRotation(params.rotation, C.p().getEyePos(), blockCentre);
            batTime = true;
            // ignore spirit sceptre bat AND the one we about to kill
            expectedBats.add(System.currentTimeMillis());
            expectedBats.add(System.currentTimeMillis());
        }

        return false;
    });

    private final Event onRightClickSpiritSceptre = new ClickedEvent.Right(ModuleUtil.getModule(SecretsHelper.class), params -> {
        if (Bus.isEventCancelled(ClickedEvent.Right.class)) return false;
        if (SkyblockItems.SPIRIT_SCEPTRE.isItem.test(C.p().getMainHandStack())) {
            expectedBats.add(System.currentTimeMillis());
        }

        return false;
    }, 2000);


    private static boolean shouldBatAura(int minBats) {
        if (!batAura) return false;

        long totalBatCount = getBatsInRange(batScanRadius).size();
        // if there is 4 bats, thats too many lwk, doors open using bats for some reason
        if (totalBatCount <= minBats || totalBatCount >= 4) return false;

        // check that we are actually killing a bat...
        return !getBatsInRange(possibleBatRadius).isEmpty();
    }

    private static List<BatEntity> getBatsInRange(double radius) {
        return C.w().getOtherEntities(C.p(), Box.from(C.p().getEntityPos()).expand(radius)).stream().filter(e -> e instanceof BatEntity).map(e -> (BatEntity) e).toList();
    }

    private final Event onAllowRightClick = new CheckClickEvent.Right(ModuleUtil.getModule(SecretsHelper.class), params -> {
        if (batTime) {
            UseItemBridge.from(C.mc).bridge$DoItemUse();
            batTime = false;
            ChatUtil.prefixMessage("KILLING THE BATMAN");
            return false;
        }

        if (secretAura && C.mc.currentScreen == null && currentTarget != null) {
            // ideally we should use packets, but they are scary
            C.im().interactBlock(C.p(), Hand.MAIN_HAND, currentTarget.hitResult);
            ChatUtil.prefixMessage("&agot secret!! cheer and rejoice");

            INTERACTED_POSITIONS.put(currentTarget.pos, System.currentTimeMillis());
        }

        currentTarget = null;

        return false;
    });

    private final Event onRedstoneKeyPickup = new ChatEvent.Received(null, params -> {
        if (Formatting.strip(params.message).equals("You found a Secret Redstone Key!")) {
            hasRedstoneKey = true;
        }
        if (Formatting.strip(params.message).equals("You hear something open...")) {
            hasRedstoneKey = false;
        }
        return false;
    });

    private final Event onWorldJoin = new WorldJoinEvent(null, params -> {
        INTERACTED_POSITIONS.clear();
        hasRedstoneKey = false;

        return false;
    });

    private final Event onToggleSecretAura = new SubModuleToggledEvent(ModuleUtil.getModule(SecretsHelper.class), params -> {
        if (!params.field.getName().equals("secretAura")) return false;

        onSetSecretAura((boolean) params.value);
        return false;
    });

    public static void onSetSecretAura(boolean secretAura) {
        ArrayList<Block> blocksToTrack = new ArrayList<>();

        for (AuraBlock auraBlock : AuraBlock.values()) {
            blocksToTrack.addAll(Arrays.asList(auraBlock.validBlocks));
        }

        if (secretAura) BlockTracker.beginTracking(blocksToTrack, SecretAura.class);
        else {
            INTERACTED_POSITIONS.clear();
            BlockTracker.stopTracking(blocksToTrack, SecretAura.class);
        }
    }

    /*
    // todo: maybe make a secrets esp
    public final Event renderWorldEvent = new RenderWorldEvent(ModuleUtil.getModule(SecretsHelper.class), params -> {
        if (!secretAura || (dungeonsOnly && !AreaUtil.isInDungeons())) return false;

        for (AuraBlock auraBlock : AuraBlock.values()) {
            for (Block block : auraBlock.validBlocks) {
                for (BlockPos blockPos : BlockTracker.getBlockPositions(block)) {
                    if (INTERACTED_POSITIONS.containsKey(blockPos) || !auraBlock.condition.test(blockPos)) continue;

                    VoxelShape shape = C.w().getBlockState(blockPos).getOutlineShape(C.w(), blockPos);
                    shape.forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> {
                        Render3dUtil.drawBox(
                                (float) (blockPos.getX()+minX),
                                (float) (blockPos.getY()+minY),
                                (float) (blockPos.getZ()+minZ),
                                (float) (maxX - minX),
                                (float) (maxY - minY),
                                (float) (maxZ - minZ),
                                new Color(60, 248, 226, 47),
                                false
                        );
                    });
                }
            }
        }
        return false;
    });
     */
}

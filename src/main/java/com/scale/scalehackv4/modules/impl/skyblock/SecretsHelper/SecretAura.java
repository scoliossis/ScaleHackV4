package com.scale.scalehackv4.modules.impl.skyblock.SecretsHelper;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.client.ChatEvent;
import com.scale.scalehackv4.events.impl.client.WorldJoinEvent;
import com.scale.scalehackv4.events.impl.player.CheckClickEvent;
import com.scale.scalehackv4.events.impl.player.RotationEvent;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.ChatUtil;
import com.scale.scalehackv4.utils.client.ModuleUtil;
import com.scale.scalehackv4.utils.player.RotationUtil;
import com.scale.scalehackv4.utils.skyblock.AreaUtil;
import com.scale.scalehackv4.utils.skyblock.DungeonMap.Room;
import lombok.AllArgsConstructor;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.DoubleBlockProperties;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.HashMap;
import java.util.UUID;
import java.util.function.Predicate;

// todo: add option to select whatever blocks
public class SecretAura extends SecretsHelper {
    @RegisterSubModule(name = "Secret Aura", hasKeybind = true)
    public static boolean secretAura = true;

    @RegisterSubModule(name = "Rotate", parent = "Secret Aura")
    public static boolean rotate = true;
    @RegisterSubModule(name = "Dungeons Only", parent = "Secret Aura")
    public static boolean dungeonsOnly = true;
    @RegisterSubModule(name = "Reach", parent = "Secret Aura", increment = 0.1, min = 1, max = 6)
    public static double reach = 4.5;

    protected static HashMap<BlockPos, Boolean> INTERACTED_POSITIONS = new HashMap<>();

    private static boolean hasRedstoneKey = false;
    private static CurrentTarget currentTarget = null;
    private record CurrentTarget(BlockPos pos, AuraBlock block, BlockHitResult hitResult) {}

    @AllArgsConstructor
    private enum AuraBlock {
        SKULL(
                pos -> {
                    BlockEntity blockEntity = C.w().getBlockEntity(pos);
                    if (!(blockEntity instanceof SkullBlockEntity skullBlockEntity)) return false;

                    ProfileComponent profileComponent = skullBlockEntity.getOwner();
                    if (profileComponent == null) return false;

                    UUID skinID = skullBlockEntity.getOwner().getGameProfile().id();

                    // only click wither essence or redstone key
                    return skinID.toString().equals("e0f3e929-869e-3dca-9504-54c666ee6f23") || skinID.toString().equals("fed95410-aba1-39df-9b95-1d4f361eb66e");
                },
                pos -> true
        ),
        LEVER(
                pos -> {
                    Room currentRoom = AreaUtil.getCurrentRoom();
                    if (currentRoom == null || currentRoom.name.equals("Water Board")) return false;

                    return C.w().getBlockState(pos).getBlock() == Blocks.LEVER;
                },
                pos -> true
        ),
        REDSTONE_BLOCK(
                pos -> hasRedstoneKey && C.w().getBlockState(pos).getBlock() == Blocks.REDSTONE_BLOCK,
                pos -> hasRedstoneKey = false
        ),
        CHEST(
                pos -> {
                    Room currentRoom = AreaUtil.getCurrentRoom();
                    if (currentRoom == null || currentRoom.name.equals("Three Weirdos")) return false;

                    BlockEntity blockEntity = C.w().getBlockEntity(pos);
                    if (blockEntity instanceof ChestBlockEntity chestBlockEntity) {
                        return chestBlockEntity.getAnimationProgress(0) == 0;
                    }
                    return false;
                },
                pos -> {
                    BlockState blockState = C.w().getBlockState(pos);

                    DoubleBlockProperties.Type doubleChestType = ChestBlock.getDoubleBlockType(blockState);

                    if (doubleChestType == DoubleBlockProperties.Type.SINGLE) return false;
                    Direction facing = ChestBlock.getFacing(blockState);
                    boolean first = doubleChestType == DoubleBlockProperties.Type.FIRST;

                    INTERACTED_POSITIONS.put(pos.offset(first ? facing : facing.getOpposite()), true);
                    return false;
                }
        );

        private final Predicate<BlockPos> condition;
        private final Predicate<BlockPos> postClick;

        public static AuraBlock getBlock(BlockPos pos) {
            for (AuraBlock auraBlock : values()) {
                if (auraBlock.condition.test(pos)) return auraBlock;
            }

            return null;
        }
    }

    private final Event onRotationEvent = new RotationEvent(ModuleUtil.getModule(SecretsHelper.class), params -> {
        if (!secretAura || C.mc.currentScreen != null || (dungeonsOnly && !AreaUtil.isInDungeons())) return false;

        // todo: Goddamn, fella, don't just say all that concernin' shit, Besides, you should know, none of this is permanent
        // yeah, ive complained before about people doing these loops, im no better ig </3
        int scanningDistance = (int) Math.ceil(reach);
        for (int x = -scanningDistance; x < scanningDistance; x++) {
            for (int y = -scanningDistance; y < scanningDistance; y++) {
                for (int z = -scanningDistance; z < scanningDistance; z++) {
                    BlockPos pos = C.p().getBlockPos().add(x, y, z);
                    if (INTERACTED_POSITIONS.containsKey(pos)) continue;

                    AuraBlock auraBlock = AuraBlock.getBlock(pos);
                    if (auraBlock == null || !auraBlock.condition.test(pos)) continue;

                    // basic reach check
                    double distance = Math.sqrt(pos.getSquaredDistance(C.p().getEyePos()));
                    if (distance > reach) continue;

                    BlockState blockState = C.w().getBlockState(pos);
                    BlockHitResult blockHitResult = C.w().raycastBlock(
                            C.p().getEyePos(),
                            pos.toBottomCenterPos(),
                            pos,
                            blockState.getOutlineShape(C.w(), pos),
                            blockState
                    );

                    // make CERTAIN we are UP
                    if (blockHitResult == null || blockHitResult.getType() == HitResult.Type.MISS || C.p().squaredDistanceTo(blockHitResult.getPos()) > reach * reach) continue;

                    currentTarget = new CurrentTarget(pos, auraBlock, blockHitResult);
                    if (rotate) params.rotation = RotationUtil.getRotation(blockHitResult.getPos());

                    break;
                }
            }
        }

        return false;
    });

    private final Event onAllowRightClick = new CheckClickEvent.Right(ModuleUtil.getModule(SecretsHelper.class), params -> {
        if (secretAura && C.mc.currentScreen == null && currentTarget != null) {
            // ideally we should use packets, but they are scary
            C.im().interactBlock(C.p(), Hand.MAIN_HAND, currentTarget.hitResult);
            ChatUtil.prefixMessage("&agot secret!! cheer and rejoice");

            INTERACTED_POSITIONS.put(currentTarget.pos, true);

            currentTarget.block.postClick.test(currentTarget.pos);
        }

        currentTarget = null;

        return false;
    });

    private final Event onRedstoneKeyPickup = new ChatEvent.Received(ModuleUtil.getModule(SecretsHelper.class), params -> {
        if (Formatting.strip(params.message).equals("You found a Secret Redstone Key!")) {
            hasRedstoneKey = true;
            ChatUtil.prefixMessage("REDSTONE KEY FOUND!");
        }
        return false;
    });

    private final Event onWorldJoin = new WorldJoinEvent(ModuleUtil.getModule(SecretsHelper.class), params -> {
        INTERACTED_POSITIONS.clear();

        return false;
    });
}

package com.scale.scalehackv4.modules.impl.player;

import com.scale.scalehackv4.bridge.UseItemBridge;
import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.player.CheckClickEvent;
import com.scale.scalehackv4.events.impl.player.RotationEvent;
import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.InventoryUtil;
import com.scale.scalehackv4.utils.client.ModuleUtil;
import com.scale.scalehackv4.utils.client.MultiSelectEnum;
import com.scale.scalehackv4.utils.client.MultiSelectUtil;
import com.scale.scalehackv4.utils.player.Rotation;
import com.scale.scalehackv4.utils.player.WorldUtil;
import com.scale.scalehackv4.utils.render.MouseUtil;
import net.minecraft.item.BlockItem;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Optional;

@RegisterModule(
        name = "Scaffold",
        category = Category.PLAYER
)
// todo: telly scaffold ig bro...
//  better on edge check
public class ScaffoldModule extends Module {
    @RegisterSubModule(name = "Fast Place", hasKeybind = true)
    public static boolean fastPlace = true;
    // in theory, the "fast place" option isnt needed, only place delay.
    @RegisterSubModule(name = "Fast Place Speed", parent = "Fast Place", min = 1, max = 3)
    public static int placeDelay = 1;

    @RegisterSubModule(name = "Safewalk", hasKeybind = true)
    public static boolean crouchOnEdge = true;
    @RegisterSubModule(name = "Autoplace", hasKeybind = true)
    public static boolean autoplace = true;

    @RegisterSubModule(name = "Conditions")
    public static ScaffoldConditions conditions = ScaffoldConditions.Holding_Block;
    public enum ScaffoldConditions implements MultiSelectEnum {
        Holding_Block,
        Looking_Down,
        Right_Click_Down,
        Crouch_Down,
        Holding_S;
    }

    @RegisterSubModule(name = "Rotation Mode")
    public static RotationMode rotationMode = RotationMode.God_Bridge;

    private boolean rotationSuccessful = false;
    private static boolean wasActive = false;

    public enum RotationMode {
        None {@Override public boolean rotate(RotationEvent.Params params, BlockPos closestBlock) {return false;}},
        Assist {
            @Override
            public boolean rotate(RotationEvent.Params params, BlockPos closestBlock) {
                if (!onEdgeOfBlock()) {
                    params.rotation.pitch = RotationEvent.getLastRotation().pitch;
                    return false;
                }

                for (int i = 90; i >= 45; i--) {
                    HitResult hitResult = WorldUtil.getMouseOver(new Rotation(RotationEvent.clientRotation.yaw, i));
                    if (hitResult instanceof BlockHitResult blockHitResult
                            && WorldUtil.isBlockHitResultValid(blockHitResult)
                            && blockHitResult.getBlockPos().equals(closestBlock)
                            && blockHitResult.getBlockPos().offset(blockHitResult.getSide()).getY() <= C.p().getY()-1
                    ) {
                        params.rotation.yaw = RotationEvent.clientRotation.yaw;
                        params.rotation.pitch = i;
                        return true;
                    }
                }

                return false;
            }
        },
        Snap {
            @Override
            public boolean rotate(RotationEvent.Params params, BlockPos closestBlock) {
                if (!onEdgeOfBlock()) return false;
                return God_Bridge.rotate(params, closestBlock);
            }
        },
        God_Bridge {
            @Override
            public boolean rotate(RotationEvent.Params params, BlockPos closestBlock) {
                if (!onEdgeOfBlock()) {
                    params.rotation = RotationEvent.getLastRotation();
                    return false;
                }

                Rotation bestRotation = null;
                Rotation lastRotation = RotationEvent.getLastRotation();

                for (float yaw = -180; yaw <= 180; yaw++) {
                    for (float pitch = 90; pitch >= 60; pitch--) {
                        // Math.random() to stop https://github.com/GrimAnticheat/Grim/blob/2.0/common/src/main/java/ac/grim/grimac/checks/impl/scaffolding/DuplicateRotPlace.java
                        Rotation rotation = new Rotation((float) (lastRotation.yaw+yaw + Math.random()), pitch);
                        HitResult hitResult = WorldUtil.getMouseOver(rotation);
                        if (!(hitResult instanceof BlockHitResult blockHitResult)
                                || !WorldUtil.isBlockHitResultValid(blockHitResult)
                                || !blockHitResult.getBlockPos().equals(closestBlock)
                                || blockHitResult.getBlockPos().offset(blockHitResult.getSide()).getY() > C.p().getY()-1
                                || getRotationScore(rotation) > getRotationScore(bestRotation)) continue;

                        bestRotation = rotation;
                    }
                }

                if (bestRotation == null) return false;

                params.rotation = bestRotation;

                return true;
            }
        };

        public abstract boolean rotate(RotationEvent.Params params, BlockPos closestBlock);
    }

    // the ideal rotation is:
    //  - a multiple of 45 away from client rotation (so movefix is less noticable)
    //  - as close to the last rotation as possible
    private static float getRotationScore(Rotation rotation) {
        if (rotation == null) return Float.MAX_VALUE;
        return Math.abs(rotation.yaw - RotationEvent.getLastRotation().yaw) - ((RotationEvent.clientRotation.yaw-rotation.yaw) % 45)*2;
    }

    private BlockPos closestBlock;
    public Event onRotationEvent = new RotationEvent(this, params -> {
        if (!InventoryUtil.isItemValidBlock(C.p().getMainHandStack().getItem()) && (!MultiSelectUtil.isEnabled(ScaffoldConditions.Holding_Block) || wasActive)) {
            InventoryUtil.swapToBestBlocks();
        }

        rotationSuccessful = false;
        closestBlock = findClosestBlock();
        if (!(wasActive = shouldScaffold())) return false;

        if (closestBlock == null || !C.p().canInteractWithBlockAt(closestBlock, 0)) return false;

        return rotationSuccessful = rotationMode.rotate(params, closestBlock);
    });

    public Event onAllowRightClick = new CheckClickEvent.Right(this, params -> {
        HitResult hitResult = WorldUtil.getMouseOver(RotationEvent.getLastRotation());
        if (!autoplace
                || !shouldScaffold()
                || !(hitResult instanceof BlockHitResult blockHitResult)
                || !WorldUtil.isBlockHitResultValid(blockHitResult)
                || !blockHitResult.getBlockPos().equals(closestBlock)
        ) return false;

        UseItemBridge.from(C.mc).bridge$DoItemUse();
        return true;
    });

    public static int getPlaceDelay() {
        boolean isPlacingBlock = C.mc.crosshairTarget instanceof BlockHitResult blockHitResult
                && blockHitResult.getType() == HitResult.Type.BLOCK
                && (C.p().isSneaking() || !InventoryUtil.isBlockInteractable(C.w().getBlockState(blockHitResult.getBlockPos()).getBlock()));
        boolean isDoingNothing = C.mc.crosshairTarget == null || C.mc.crosshairTarget.getType() == HitResult.Type.MISS;

        return (isPlacingBlock || isDoingNothing)
                && C.p().getMainHandStack().getItem() instanceof BlockItem
                && ModuleUtil.isEnabled(ScaffoldModule.class) && fastPlace
                ? placeDelay
                : 4;
    }

    public static boolean shouldEagle() {
        return crouchOnEdge && ModuleUtil.isEnabled(ScaffoldModule.class) && wasActive;
    }

    private static boolean shouldScaffold() {
        return (!MultiSelectUtil.isEnabled(ScaffoldConditions.Holding_Block) || InventoryUtil.isItemValidBlock(C.p().getMainHandStack().getItem()))
                && (!MultiSelectUtil.isEnabled(ScaffoldConditions.Looking_Down) || C.p().getPitch() > 45)
                && (!MultiSelectUtil.isEnabled(ScaffoldConditions.Holding_S) || C.mc.options.backKey.isPressed())
                && (!MultiSelectUtil.isEnabled(ScaffoldConditions.Right_Click_Down) || MouseUtil.isButtonDown(1))
                && (!MultiSelectUtil.isEnabled(ScaffoldConditions.Crouch_Down) || C.mc.options.sneakKey.isPressed());
    }

    // function from unreleased injectable mushroom!
    private BlockPos findClosestBlock() {
        // loops in a 5x5x5 radius and checks if:
        // checks if the block is below the player
        // checks if the block is NOT air
        // returns the closest block that meets those conditions
        Optional<BlockPos> closestBlock = BlockPos.findClosest(
                blockBelowPlayer(),
                5,
                5,
                // i dont think i knew what a predicate was at the time so i typed "predicate" and predicate.not came up
                blockPos -> blockPos.getY() <= blockBelowPlayer().getY()
                        && !C.w().getBlockState(blockPos).isAir()
        );

        return closestBlock.orElse(null);
    }

    private BlockPos blockBelowPlayer() {
        return C.p().getBlockPos().down();
    }

    private static boolean onEdgeOfBlock() {
        return C.w().getBlockState(C.p().getBlockPos().offset(Direction.DOWN)).isAir();
    }

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {

    }
}
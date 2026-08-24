package com.scale.scalehackv4.modules.impl.player;

import com.scale.scalehackv4.bridge.UseItemBridge;
import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.player.CheckClickEvent;
import com.scale.scalehackv4.events.impl.player.KeyboardInputEvent;
import com.scale.scalehackv4.events.impl.player.RotationEvent;
import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.*;
import com.scale.scalehackv4.utils.player.MovementUtil;
import com.scale.scalehackv4.utils.player.Rotation;
import com.scale.scalehackv4.utils.player.RotationUtil;
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
    @RegisterSubModule(name = "Keep Y", hasKeybind = true)
    public static boolean keepY = false;

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
    @RegisterSubModule(name = "Easing Mode", parent = "Rotation Mode", modeParentString = "Telly")
    public static EasingUtil.EasingFunctions easingMode = EasingUtil.EasingFunctions.Ease_In_Back;
    @RegisterSubModule(name = "Easing Ticks", min = 1, max = 5, parent = "Rotation Mode", modeParentString = "Telly")
    public static int easingCycle = 10;

    private static boolean wasActive = false;

    private static Rotation startEasingRotation = null;
    private static int easingTicks = 0;

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

                Rotation bestRotation = getBestRotation(closestBlock);
                if (bestRotation == null) return false;

                params.rotation = bestRotation;

                return true;
            }
        },
        Telly {
            @Override
            public boolean rotate(RotationEvent.Params params, BlockPos closestBlock) {
                if (C.p().isOnGround() && MovementUtil.willJump()) {
                    easingTicks = 0;
                    return false;
                }

                Rotation bestRotation;
                if (!onEdgeOfBlock() || (bestRotation = getBestRotation(closestBlock)) == null) {
                    params.rotation = RotationEvent.getLastRotation();
                    return false;
                }

                if (easingTicks == 0) startEasingRotation = RotationEvent.getLastRotation();
                easingTicks++;

                params.rotation = RotationUtil.getEasedRotation(startEasingRotation, bestRotation, easingMode, (double) Math.min(easingTicks, easingCycle) / easingCycle);
                return true;
            }
        };

        public abstract boolean rotate(RotationEvent.Params params, BlockPos closestBlock);
    }

    private static Rotation getBestRotation(BlockPos closestBlock) {
        Rotation bestRotation = null;
        Rotation lastRotation = RotationEvent.getLastRotation();

        for (float yaw = -180; yaw <= 180; yaw++) {
            for (float pitch = 90; pitch >= 60; pitch--) {
                // Math.random() to stop https://github.com/GrimAnticheat/Grim/blob/2.0/common/src/main/java/ac/grim/grimac/checks/impl/scaffolding/DuplicateRotPlace.java
                Rotation rotation = new Rotation((float) (lastRotation.yaw+yaw + Math.random()), (float) (pitch + Math.random()));
                if (getRotationScore(rotation) > getRotationScore(bestRotation) || !isValidPlacement(rotation, closestBlock)) continue;

                bestRotation = rotation;
            }
        }

        return bestRotation;
    }

    // the ideal rotation is:
    //  - a multiple of 45 away from client rotation (so movefix is less noticable)
    //      - only matters on ground
    //  - a low a pitch as possible so it doesnt climb too quick when in air
    //  - as close to the last rotation as possible
    private static float getRotationScore(Rotation rotation) {
        if (rotation == null) return Float.MAX_VALUE;
        return Math.abs(rotation.yaw - RotationEvent.getLastRotation().yaw) -
                ((RotationEvent.clientRotation.yaw-rotation.yaw) % 45)*2 -
                (C.p().isOnGround() ? 0 : (rotation.pitch-60)/4);
    }

    private static boolean isValidPlacement(Rotation rotation, BlockPos closestBlock) {
        HitResult hitResult = WorldUtil.getMouseOver(rotation);
        return hitResult instanceof BlockHitResult blockHitResult
                && WorldUtil.isBlockHitResultValid(blockHitResult)
                && blockHitResult.getBlockPos().equals(closestBlock)
                && (!keepY || blockHitResult.getSide() != Direction.UP)
                && blockHitResult.getBlockPos().offset(blockHitResult.getSide()).getY() <= C.p().getY() - 1;
    }

    private BlockPos closestBlock;
    public Event onRotationEvent = new RotationEvent(this, params -> {
        // queue swapping to blocks for next tick, i think it helps bypahh
        if (!InventoryUtil.isItemValidBlock(C.p().getMainHandStack().getItem()) && (!MultiSelectUtil.isEnabled(ScaffoldConditions.Holding_Block) || wasActive))
            InventoryUtil.swapToBestBlocks();

        closestBlock = findClosestBlock();
        if (!(wasActive = shouldScaffold())) {
            easingTicks = 0;
            return false;
        }

        if (closestBlock == null || !C.p().canInteractWithBlockAt(closestBlock, 0)) return false;

        return rotationMode.rotate(params, closestBlock);
    });

    public Event onAllowRightClick = new CheckClickEvent.Right(this, params -> {
        if (!shouldScaffold()) return false;

        if (!InventoryUtil.isItemValidBlock(C.p().getMainHandStack().getItem())
                || !isValidPlacement(RotationEvent.getLastRotation(), closestBlock)) return true;

        if (!autoplace) return false;

        UseItemBridge.from(C.mc).bridge$DoItemUse();
        return true;
    });


    public Event onKeyboardInputEvent = new KeyboardInputEvent(this, params -> {
        if (!shouldScaffold() || rotationMode != RotationMode.Telly || !C.mc.options.jumpKey.isPressed()) return false;

        params.jumpKey = true;
        params.sneakKey = false;
        params.sprintKey = true;

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
        return crouchOnEdge
                && ModuleUtil.isEnabled(ScaffoldModule.class)
                && wasActive
                && (rotationMode != RotationMode.Telly || !C.mc.options.jumpKey.isPressed());
    }

    private static boolean shouldScaffold() {
        return (!MultiSelectUtil.isEnabled(ScaffoldConditions.Holding_Block) || InventoryUtil.isItemValidBlock(C.p().getMainHandStack().getItem()))
                && (!MultiSelectUtil.isEnabled(ScaffoldConditions.Looking_Down) || C.p().getPitch() > 20)
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
                        && (blockPos.getY() >= blockBelowPlayer().getY() - 1 || rotationMode != RotationMode.Telly)
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
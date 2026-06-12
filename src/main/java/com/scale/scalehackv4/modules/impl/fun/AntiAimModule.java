package com.scale.scalehackv4.modules.impl.fun;

import com.scale.scalehackv4.bridge.BreakBlockCooldownBridge;
import com.scale.scalehackv4.bridge.ItemUseCooldownBridge;
import com.scale.scalehackv4.bridge.KeybindingBridge;
import com.scale.scalehackv4.events.Bus;
import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.player.ClickedEvent;
import com.scale.scalehackv4.events.impl.player.RotationEvent;
import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.InventoryUtil;
import com.scale.scalehackv4.utils.client.MultiSelectEnum;
import com.scale.scalehackv4.utils.client.MultiSelectUtil;
import com.scale.scalehackv4.utils.player.MovementUtil;
import com.scale.scalehackv4.utils.player.WorldUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.BlockItem;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

@RegisterModule(
        name = "Anti Aim",
        category = Category.FUN
)
// todo: new modes, and maybe actually dodging arrows...
public class AntiAimModule extends Module {
    @RegisterSubModule(name = "Disable Conditions")
    public DisableConditions disableConditions = DisableConditions.Jump;

    public enum DisableConditions implements MultiSelectEnum {
        Jump,
        Interact,
        Attack,
        Break_Block
    }

    private boolean clientRotationInteracting = false;

    public final Event GOD_BYPASS_TECH_20XX = new RotationEvent(this, params -> {
        clientRotationInteracting = isInteracting();

        if (clientRotationInteracting) {
            C.mc.crosshairTarget = WorldUtil.getMouseOver(params.rotation);
            return false;
        }

        params.rotation.pitch += (float) ((Math.random() * 180) - 90);

        if (MovementUtil.willJump() && MultiSelectUtil.isEnabled(DisableConditions.Jump)) return false;
        params.rotation.yaw += (float) ((Math.random() * 360) - 180);

        return false;
        // lowest priority, sadly.
    }, 1);

    private final Event stopRightClick = new ClickedEvent.Right(this, this::shouldBlockRightClick);
    //private final Event stopLeftClick = new ClickedEvent.Left(this, this::shouldBlockLeftClick);

    private boolean isInteracting() {
        return (willPlaceBlock() && MultiSelectUtil.isEnabled(DisableConditions.Interact))
                || (willAttack() && MultiSelectUtil.isEnabled(DisableConditions.Attack))
                || (willBreakBlock() && MultiSelectUtil.isEnabled(DisableConditions.Break_Block));
    }

    private boolean willPlaceBlock() {
        boolean isRightClickingThisTick = KeybindingBridge.from(C.mc.options.useKey).bridge$getTimesPressed() > 0
                || (ItemUseCooldownBridge.from(C.mc).bridge$getItemUseCooldown() <= 1 && C.mc.options.useKey.isPressed());
        boolean clickingBlock = C.mc.crosshairTarget instanceof BlockHitResult blockHitResult
                && ((C.p().getMainHandStack().getItem() instanceof BlockItem && WorldUtil.isBlockHitResultValid(blockHitResult))
                || InventoryUtil.isBlockInteractable(C.w().getBlockState(blockHitResult.getBlockPos()).getBlock()));
        boolean clickingEntity = C.mc.crosshairTarget instanceof EntityHitResult;

        return !C.p().isUsingItem() && !C.im().isBreakingBlock()
                && isRightClickingThisTick && (clickingBlock || clickingEntity);
    }

    private boolean shouldBlockRightClick(Object params) {
        return (Bus.isEventCancelled(RotationEvent.class) ? !isInteracting() : !clientRotationInteracting) && C.p().getMainHandStack().getItem() instanceof BlockItem;
    }

    private boolean willAttack() {
        return KeybindingBridge.from(C.mc.options.attackKey).bridge$getTimesPressed() > 0
                && C.mc.crosshairTarget instanceof EntityHitResult
                && !C.p().isUsingItem();
    }

    // todo: anticheats only check if its ur first/last tick breaking a block
    private boolean willBreakBlock() {
        // first line is the "breaking" boolean passed to "net.minecraft.client.MinecraftClient.handleBlockBreaking"
        // the rest of the lines are the separate if statements inside of that method
        return C.mc.options.attackKey.isPressed() && C.mc.currentScreen == null && C.mc.mouse.isCursorLocked()
                && C.mc.attackCooldown <= 0 && !C.p().isUsingItem()
                && !C.p().getMainHandStack().contains(DataComponentTypes.PIERCING_WEAPON)
                && C.mc.crosshairTarget != null && C.mc.crosshairTarget.getType() == HitResult.Type.BLOCK
                && C.mc.crosshairTarget instanceof BlockHitResult blockHitResult && !C.w().getBlockState(blockHitResult.getBlockPos()).isAir()
                && BreakBlockCooldownBridge.from(C.im()).bridge$getBlockBreakingCooldown() <= 0;
    }

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {

    }
}

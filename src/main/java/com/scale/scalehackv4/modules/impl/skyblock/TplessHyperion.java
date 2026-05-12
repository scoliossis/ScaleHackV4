package com.scale.scalehackv4.modules.impl.skyblock;

import com.scale.scalehackv4.bridge.UseItemBridge;
import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.client.ChatEvent;
import com.scale.scalehackv4.events.impl.player.ClickedEvent;
import com.scale.scalehackv4.events.impl.player.RotationEvent;
import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.PacketUtil;
import com.scale.scalehackv4.utils.player.Rotation;
import com.scale.scalehackv4.utils.player.WorldUtil;
import com.scale.scalehackv4.utils.skyblock.AreaUtil;
import com.scale.scalehackv4.utils.skyblock.SkyblockItems;
import net.minecraft.block.BlockState;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;

@RegisterModule(
        name = "Tpless Hyperion",
        category = Category.SKYBLOCK
)
// todo: works looking at ceilings and walls too
public class TplessHyperion extends Module {
    @RegisterSubModule(name = "Dungeons Only")
    public static boolean dungeonsOnly = true;
    @RegisterSubModule(name = "Disable In Boss")
    public static boolean disableInBoss = true;
    @RegisterSubModule(name = "Block Air Clicks")
    public static boolean blockAirClicks = true;
    @RegisterSubModule(name = "Left Click Hyperion")
    public static boolean leftClickHyp = true;

    private static int sentTeleports = 0;

    private boolean shouldTpless() {
        return SkyblockItems.HYPERION.isItem.test(C.p().getMainHandStack())
                && (AreaUtil.isInDungeons() || !dungeonsOnly)
                && (!AreaUtil.isInBoss() || !disableInBoss);
    }
    private boolean shouldLookDown() {
        return !isInAir() || blockAirClicks;
    }
    private boolean shouldCancelClick() {
        return isInAir() && blockAirClicks && shouldTpless() && shouldLookDown();
    }

    private boolean isInAir() {
        BlockPos raytracePos = WorldUtil.raytrace(C.p().getEntityPos(), 4.5, new Rotation(RotationEvent.getLastRotation().yaw, 90)).getBlockPos();
        BlockState blockState = C.w().getBlockState(raytracePos);
        VoxelShape outlineShape = blockState.getOutlineShape(C.w(), raytracePos);
        if (outlineShape.isEmpty()) return true;

        Box outlineBox = blockState.getOutlineShape(C.w(), raytracePos).getBoundingBox();
        double blockY = C.p().getBlockPos().toBottomCenterPos().add(0, outlineBox.minY + outlineBox.getLengthY(), 0).getY();
        if (blockY % 1 == 0) blockY--;
        return C.p().getY() != blockY || !C.p().isOnGround() || !C.w().getBlockState(C.p().getBlockPos().down()).isSolid();
    }

    private final Event onRightClick = new ClickedEvent.Right(this, params -> {
        if (shouldCancelClick()) return true;

        if (!shouldTpless() || !shouldLookDown()) return false;

        PacketUtil.sendSequencedPacket(sequence -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, sequence, RotationEvent.getLastRotation().yaw, 90));
        sentTeleports++;

        return false;
    });

    private final Event onLeftClick = new ClickedEvent.Left(this, params -> {
        if (!leftClickHyp || !SkyblockItems.HYPERION.isItem.test(C.p().getMainHandStack()) || !shouldTpless()) return false;

        UseItemBridge.from(C.mc).bridge$DoItemUse();
        return true;
    });

    private final Event hideBypahhMessages = new ChatEvent.Received(this, params -> {
        if (params.message.equals("There are blocks in the way!") && sentTeleports > 0) {
            sentTeleports--;
            return true;
        }

        return false;
    });


    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {

    }
}
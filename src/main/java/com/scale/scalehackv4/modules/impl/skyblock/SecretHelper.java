package com.scale.scalehackv4.modules.impl.skyblock;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.player.PlayerUpdateEvent;
import com.scale.scalehackv4.events.impl.render.RenderWorldEvent;
import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.render.Render3dUtil;
import com.scale.scalehackv4.utils.skyblock.AreaUtil;
import com.scale.scalehackv4.utils.skyblock.DungeonMap.Room;
import com.scale.scalehackv4.utils.skyblock.SecretsHelper.EtherwarpUtil;
import com.scale.scalehackv4.utils.skyblock.SecretsHelper.NodeUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

import java.awt.*;

@RegisterModule(
        name = "Secret Helper",
        category = Category.SKYBLOCK
)
public class SecretHelper extends Module {
    @RegisterSubModule(name = "Etherwarp Overlay")
    public static boolean etherwarpOverlay = true;
    @RegisterSubModule(name = "Etherwarp Colour", parent = "Etherwarp Overlay")
    public static Color etherwarpColour = new Color(162, 164, 238);

    Vec3i etherwarpPos = null;

    private final Event getEtherwarpPos = new PlayerUpdateEvent(this, params -> {
        if (!etherwarpOverlay || !C.p().isSneaking() || !EtherwarpUtil.isEtherwarpItem(C.p().getMainHandStack())) {
            etherwarpPos = null;
            return false;
        }

        etherwarpPos = EtherwarpUtil.getEtherwarpBlockSuccess(C.p().getPitch(), C.p().getYaw(), C.p().getEntityPos(), 57);
        return false;
    });

    private final Event renderWorldEvent = new RenderWorldEvent(this, params -> {
        renderEtherwarpPos();
        renderAutoroutes();

        return false;
    });

    private void renderEtherwarpPos() {
        if (etherwarpPos != null) {
            Render3dUtil.drawBox(etherwarpPos.getX(), etherwarpPos.getY(), etherwarpPos.getZ(), 1, 1, 1, etherwarpColour, false);
        }
    }

    private static void renderAutoroutes() {
        Room currentRoom = AreaUtil.getCurrentRoom();
        if (currentRoom == null) return;

        for (BlockPos relativeNodePos : currentRoom.nodes) {
            BlockPos nodePos = NodeUtil.getNodePos(relativeNodePos, currentRoom);
            Render3dUtil.drawBox(nodePos.getX(), nodePos.getY(), nodePos.getZ(), 1, 1, 1, new Color(135, 148, 255), false);
        }

    }

    @Override public void onEnable() {}
    @Override public void onDisable() {}
}

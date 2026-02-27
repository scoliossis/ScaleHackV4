package com.scale.scalehackv4.modules.impl.skyblock.SecretsHelper;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.player.PlayerUpdateEvent;
import com.scale.scalehackv4.events.impl.render.RenderWorldEvent;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.ModuleUtil;
import com.scale.scalehackv4.utils.render.Render3dUtil;
import com.scale.scalehackv4.utils.skyblock.SecretsHelper.EtherwarpUtil;
import net.minecraft.util.math.Vec3i;

import java.awt.*;

public class EtherwarpOverlay extends SecretsHelper {
    @RegisterSubModule(name = "Etherwarp Overlay")
    public static boolean etherwarpOverlay = true;
    @RegisterSubModule(name = "Etherwarp Colour", parent = "Etherwarp Overlay")
    public static Color etherwarpColour = new Color(188, 0, 250, 90);

    private Vec3i etherwarpPos = null;

    private final Event onPlayerUpdate = new PlayerUpdateEvent(ModuleUtil.getModule(SecretsHelper.class), params -> {
        handleEtherwarpOverlay();

        return false;
    });

    private final Event renderWorldEvent = new RenderWorldEvent(ModuleUtil.getModule(SecretsHelper.class), params -> {
        if (etherwarpPos != null) {
            Render3dUtil.drawBox(etherwarpPos.getX(), etherwarpPos.getY(), etherwarpPos.getZ(), 1, 1, 1, etherwarpColour, false);
        }

        return false;
    });

    private void handleEtherwarpOverlay() {
        if (!etherwarpOverlay || !C.p().isSneaking() || !EtherwarpUtil.isEtherwarpItem(C.p().getMainHandStack())) {
            etherwarpPos = null;
            return;
        }

        etherwarpPos = EtherwarpUtil.getEtherwarpBlockSuccess(C.p().getPitch(), C.p().getYaw(), C.p().getEntityPos(), 57);
    }
}

package com.scale.scalehackv4.modules.client;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.render.RenderTickEvent;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.util.Identifier;

import java.awt.*;

@RegisterModule(name = "HUD")
public class HUD extends Module {
    public final Event onDrawScreen = new RenderTickEvent(this, (e) -> {
        e.drawContext.drawTexture(RenderPipelines.GUI_TEXTURED, Identifier.of("scalehackv4", "icon.png"), 100, 100, 0, 0, 200, 200, 200, 200, 0x1F00FF1F);
        /*
        e.drawContext.state.addSimpleElement(new ColoredQuadGuiElementRenderState(
                RenderPipelines.GUI,
                TextureSetup.empty(),
                new Matrix3x2f(e.drawContext.getMatrices()),
                0,
                0,
                100,
                100,
                0xFF0000FF,
                0x00000000,
                e.drawContext.scissorStack.peekLast()
        ));
         */
        return false;
    });
}

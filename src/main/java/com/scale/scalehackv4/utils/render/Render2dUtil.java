package com.scale.scalehackv4.utils.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.scale.scalehackv4.Main;
import com.scale.scalehackv4.events.impl.render.RenderTickEvent;
import com.scale.scalehackv4.utils.C;
import lombok.Setter;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.util.HashMap;
import java.util.function.Predicate;

public class Render2dUtil {
    @Setter protected static DrawContext drawContext;

    private static final HashMap<VertexFormat, HashMap<VertexFormat.DrawMode, RenderPipeline>> PIPELINES = new HashMap<>();

    private static RenderPipeline getRenderPipeline(VertexFormat vertexFormat, VertexFormat.DrawMode drawMode) {
        HashMap<VertexFormat.DrawMode, RenderPipeline> pipelineMap = PIPELINES.get(vertexFormat);

        if (pipelineMap == null || !pipelineMap.containsKey(drawMode)) {
            pipelineMap = new HashMap<>();

            RenderPipeline pipeline = RenderPipelines.register(
                    RenderPipeline.builder(getRenderPipelineSnippet(vertexFormat))
                            .withVertexFormat(vertexFormat, drawMode)
                            .withLocation(Main.MOD_ID + PIPELINES.size())
                            .build()
            );

            PIPELINES.put(vertexFormat, pipelineMap);
            return pipeline;
        }

        return pipelineMap.get(drawMode);
    }

    private static RenderPipeline.Snippet getRenderPipelineSnippet(VertexFormat vertexFormat) {
        RenderPipeline.Snippet base = RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .buildSnippet();

        if (vertexFormat == VertexFormats.POSITION_COLOR) return RenderPipeline.builder(base)
                .withVertexShader("core/position_color")
                .withFragmentShader("core/position_color")
                .buildSnippet();

        else if (vertexFormat == VertexFormats.POSITION_TEXTURE_COLOR) return RenderPipeline.builder(base)
                .withVertexShader("core/position_tex_color")
                .withFragmentShader("core/position_tex_color")
                .withSampler("Sampler0")
                .buildSnippet();

        return RenderPipeline.builder(base)
                .withVertexShader("core/gui")
                .withFragmentShader("core/gui")
                .buildSnippet();
    }

    private static Matrix3x2f getMatrices() {
        return new Matrix3x2f(drawContext.getMatrices());
    }

    // courtesy of Lnet/minecraft/client/gui/DrawContext;drawTexturedQuad(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIIIFFFFI)V
    private static TextureSetup getTexture(Identifier texture) {
        AbstractTexture abstractTexture = C.mc.getTextureManager().getTexture(texture);
        return TextureSetup.of(abstractTexture.getGlTextureView(), abstractTexture.getSampler());
    }

    private static void drawVertexes(VertexFormat vertexFormat, VertexFormat.DrawMode drawMode, Predicate<VertexConsumer> callable) {
        drawVertexes(vertexFormat, drawMode, TextureSetup.empty(), callable);
    }

    // idea hit me when i saw "net.minecraft.client.gui.render.state.ColoredQuadGuiElementRenderState.ColoredQuadGuiElementRenderState"
    private static void drawVertexes(VertexFormat vertexFormat, VertexFormat.DrawMode drawMode, TextureSetup textureSetup, Predicate<VertexConsumer> callable) {
        drawContext.state.addSimpleElement(new SimpleGuiElementRenderState() {
            @Override
            public void setupVertices(VertexConsumer vertices) {
                callable.test(vertices);
            }

            @Override
            public RenderPipeline pipeline() {
                return getRenderPipeline(vertexFormat, drawMode);
            }

            @Override
            public TextureSetup textureSetup() {
                return textureSetup;
            }

            @Override
            public @Nullable ScreenRect scissorArea() {
                return drawContext.scissorStack.peekLast();
            }

            @Override
            public ScreenRect bounds() {
                return new ScreenRect(0, 1, C.mc.getWindow().getScaledWidth(), C.mc.getWindow().getScaledHeight());
            }
        });
    }


    public static void drawRect(int x, int y, int width, int height, Color colour) {
        // as far as im aware, theres no way to set the colour before drawing,
        // minecraft themselves use position_colour in net.minecraft.client.gui.DrawContext.fill
        drawGradient(x, y, width, height, colour, colour, colour, colour);
    }

    public static void drawGradientAcross(int x, int y, int width, int height, Color colour1, Color colour2) {
        drawGradient(x, y, width, height, colour1, colour1, colour2, colour2);
    }

    public static void drawGradientDown(int x, int y, int width, int height, Color colour1, Color colour2) {
        drawGradient(x, y, width, height, colour1, colour2, colour2, colour1);
    }

    public static void drawGradient(int x, int y, int width, int height, Color tl, Color bl, Color br, Color tr) {
        Matrix3x2f matrices = getMatrices();

        drawVertexes(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS, (vertices) -> {
            vertices.vertex(matrices, x, y).color(tl.getRGB());
            vertices.vertex(matrices, x, y+height).color(bl.getRGB());
            vertices.vertex(matrices, x+width, y+height).color(br.getRGB());
            vertices.vertex(matrices, x+width, y).color(tr.getRGB());

            return true;
        });
    }

    public static void drawTexturedRect(Identifier image, int x, int y, int width, int height) {
        drawTexturedRect(image, x, y, width, height, Color.WHITE);
    }

    public static void drawTexturedRect(Identifier image, int x, int y, int width, int height, Color color) {
        Matrix3x2f matrices = getMatrices();

        drawVertexes(VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS, getTexture(image), (vertices) -> {
            vertices.vertex(matrices, x, y).texture(0, 0).color(color.getRGB());
            vertices.vertex(matrices, x, y+height).texture(0, 1).color(color.getRGB());
            vertices.vertex(matrices, x+width, y+height).texture(1, 1).color(color.getRGB());
            vertices.vertex(matrices, x+width, y).texture(1, 0).color(color.getRGB());

            return true;
        });
    }
}

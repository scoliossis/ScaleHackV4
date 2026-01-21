package com.scale.scalehackv4.utils.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.scale.scalehackv4.Main;
import com.scale.scalehackv4.utils.C;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;
import org.jspecify.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;

public class RenderUtil {
    private static final HashMap<VertexFormat, HashMap<VertexFormat.DrawMode, RenderPipeline>> PIPELINES = new HashMap<>();

    @Setter @Getter protected static DrawContext drawContext;

    private static final Queue<SimpleGuiElementRenderState> RENDER_QUEUE = new ConcurrentLinkedQueue<>();

    protected static RenderPipeline getRenderPipeline(VertexFormat vertexFormat, VertexFormat.DrawMode drawMode) {
        HashMap<VertexFormat.DrawMode, RenderPipeline> pipelineMap = PIPELINES.get(vertexFormat);

        if (pipelineMap == null || !pipelineMap.containsKey(drawMode)) {
            pipelineMap = new HashMap<>();

            RenderPipeline pipeline = RenderPipelines.register(
                    RenderPipeline.builder(getRenderPipelineSnippet(vertexFormat))
                            .withVertexFormat(vertexFormat, drawMode)
                            .withLocation(Main.MOD_ID + "2d" + PIPELINES.size())
                            .build()
            );

            pipelineMap.put(drawMode, pipeline);
            PIPELINES.put(vertexFormat, pipelineMap);
            return pipeline;
        }

        return pipelineMap.get(drawMode);
    }

    protected static RenderPipeline.Snippet getRenderPipelineSnippet(VertexFormat vertexFormat) {
        RenderPipeline.Snippet base = RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
                .withCull(false)
                .withBlend(BlendFunction.TRANSLUCENT)
                .buildSnippet();

        if (vertexFormat == VertexFormats.POSITION_COLOR) return RenderPipeline.builder(base)
                .withVertexShader("core/position_color")
                .withFragmentShader("core/position_color")
                .buildSnippet();

        else if (vertexFormat == VertexFormats.POSITION_TEXTURE_COLOR_NORMAL) return RenderPipeline.builder(base)
                .withVertexShader("core/position_texture_color_normal")
                .withFragmentShader("core/position_texture_color_normal")
                .withSampler("Sampler0")
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

    // courtesy of Lnet/minecraft/client/gui/DrawContext;drawTexturedQuad(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIIIFFFFI)V
    protected static TextureSetup getTexture(Identifier texture) {
        return getTexture(C.mc.getTextureManager().getTexture(texture));
    }

    protected static TextureSetup getTexture(BufferedImage bufferedImage, String name) {
        return getTexture(createTexture(bufferedImage, name));
    }

    protected static TextureSetup getTexture(AbstractTexture abstractTexture) {
        return TextureSetup.of(abstractTexture.getGlTextureView(), abstractTexture.getSampler());
    }


    public static AbstractTexture createTexture(BufferedImage bufferedImage, String name) {
        NativeImage nativeImage = new NativeImage(bufferedImage.getWidth(), bufferedImage.getHeight(), false);
        for (int x = 0; x < bufferedImage.getWidth(); x++) {
            for (int y = 0; y < bufferedImage.getHeight(); y++) {
                int color = bufferedImage.getRGB(x, y);
                nativeImage.fillRect(x, y, 1, 1, color);
            }
        }

        return new NativeImageBackedTexture(() -> name, nativeImage);
    }

    public static Vector2f projectPos(Vector2f pos, Matrix3x2f matrices) {
        return matrices.transformPosition(pos);
    }

    protected static void drawVertexes(VertexFormat vertexFormat, VertexFormat.DrawMode drawMode, Predicate<VertexConsumer> callable) {
        drawVertexes(vertexFormat, drawMode, callable, false);
    }

    protected static void drawVertexes(VertexFormat vertexFormat, VertexFormat.DrawMode drawMode, Predicate<VertexConsumer> callable, boolean queue) {
        drawVertexes(vertexFormat, drawMode, TextureSetup.empty(), callable, queue);
    }

    protected static void drawVertexes(VertexFormat vertexFormat, VertexFormat.DrawMode drawMode, TextureSetup textureSetup, Predicate<VertexConsumer> callable) {
        drawVertexes(vertexFormat, drawMode, textureSetup, callable, false);
    }

    // idea hit me when i saw "net.minecraft.client.gui.render.state.ColoredQuadGuiElementRenderState.ColoredQuadGuiElementRenderState"
    protected static void drawVertexes(VertexFormat vertexFormat, VertexFormat.DrawMode drawMode, TextureSetup textureSetup, Predicate<VertexConsumer> callable, boolean queue) {
        ScreenRect scissor = drawContext.scissorStack.peekLast();

        SimpleGuiElementRenderState guiElementRenderState = new SimpleGuiElementRenderState() {
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
                return scissor;
            }

            @Override
            public ScreenRect bounds() {
                return new ScreenRect(0, 1, C.mc.getWindow().getScaledWidth(), C.mc.getWindow().getScaledHeight());
            }
        };

        if (queue) RENDER_QUEUE.add(guiElementRenderState);
        else drawContext.state.addSimpleElement(guiElementRenderState);
    }

    public static void renderQueued() {
        while (!RENDER_QUEUE.isEmpty()) {
            drawContext.state.addSimpleElement(RENDER_QUEUE.poll());
        }
    }
}

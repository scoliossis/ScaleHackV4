package com.scale.scalehackv4.utils.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.scale.scalehackv4.Main;
import it.unimi.dsi.fastutil.floats.FloatUnaryOperator;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.*;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.awt.*;
import java.util.HashMap;

public class Render3dUtil extends RenderUtil {
    @Getter private static MatrixStack matrices;
    private static VertexConsumerProvider.Immediate vertexConsumers = VertexConsumerProvider.immediate(BufferAllocator.fixedSized(0));

    @Setter @Getter private static RenderTickCounter tickCounter = new RenderTickCounter.Dynamic(0,0, FloatUnaryOperator.negation());

    private static final HashMap<VertexFormat, HashMap<VertexFormat.DrawMode, RenderLayer>> LAYERS = new HashMap<>();
    private static final HashMap<VertexFormat, HashMap<VertexFormat.DrawMode, HashMap<Identifier, RenderLayer>>> TEXTURED_LAYERS = new HashMap<>();

    public static void tickRender(MatrixStack matrices, VertexConsumerProvider.Immediate vertexConsumers, CameraRenderState camera) {
        matrices.push();

        Render3dUtil.matrices = matrices;
        if (vertexConsumers != null) Render3dUtil.vertexConsumers = vertexConsumers;
        if (camera != null) matrices.translate(-camera.pos.getX(), -camera.pos.getY(), -camera.pos.getZ());
    }

    public static void finishTickRender() {
        matrices.pop();
    }

    private static MatrixStack.Entry getEntry() {
        return matrices.peek().copy();
    }

    protected static RenderLayer getRenderLayer(VertexFormat vertexFormat, VertexFormat.DrawMode drawMode) {
        HashMap<VertexFormat.DrawMode, RenderLayer> layersMap = LAYERS.get(vertexFormat);

        if (layersMap == null || !layersMap.containsKey(drawMode)) {
            layersMap = new HashMap<>();

            RenderLayer layer = RenderLayer.of(
                    Main.MOD_NAME + "3d" + LAYERS.size(),
                    RenderSetup.builder(getRenderPipeline(vertexFormat, drawMode))
                            .layeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING_FORWARD)
                            .build()
            );

            layersMap.put(drawMode, layer);
            LAYERS.put(vertexFormat, layersMap);

            return layer;
        }

        return layersMap.get(drawMode);
    }

    protected static RenderLayer getRenderLayerTexture(VertexFormat vertexFormat, VertexFormat.DrawMode drawMode, Identifier texture) {
        HashMap<VertexFormat.DrawMode, HashMap<Identifier, RenderLayer>> texturesMap = TEXTURED_LAYERS.get(vertexFormat);
        HashMap<Identifier, RenderLayer> layersMap = texturesMap == null ? null : texturesMap.get(drawMode);

        if (layersMap == null || !layersMap.containsKey(texture)) {
            texturesMap = new HashMap<>();
            layersMap = new HashMap<>();

            RenderLayer layer = RenderLayer.of(
                    Main.MOD_NAME + "textured3d" + TEXTURED_LAYERS.size(),
                    RenderSetup.builder(getRenderPipeline(vertexFormat, drawMode))
                            .layeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING_FORWARD)
                            .texture("Sampler0", texture)
                            .build()
            );

            layersMap.put(texture, layer);
            texturesMap.put(drawMode, layersMap);

            TEXTURED_LAYERS.put(vertexFormat, texturesMap);

            return layer;
        }

        return layersMap.get(texture);
    }

    // todo: auto generate cull and non cull versions
    private static final RenderLayer QUADS_NO_CULL = RenderLayer.of(
            Main.MOD_ID + "glowpipeline3d",
            RenderSetup.builder(
                            RenderPipelines.register(
                                    RenderPipeline.builder(RenderUtil.getRenderPipelineSnippet(VertexFormats.POSITION_COLOR))
                                            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
                                            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                                            .withLocation(Main.MOD_ID + "quadsnocull")
                                            .build()
                            )
                    )
                    .layeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING_FORWARD)
                    .build()
    );

    public static void drawSquare(float x, float y, float z, float width, float height, float depth, Color colour) {
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(getRenderLayer(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS));

        MatrixStack.Entry entry = getEntry();

        addSquare(x, y, z, width, height, depth, colour, vertexConsumer, entry);

        vertexConsumers.drawCurrentLayer();
    }

    public static void drawBox(float x, float y, float z, float width, float height, float depth, Color colour, boolean cull) {
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(cull ? getRenderLayer(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS) : QUADS_NO_CULL);

        MatrixStack.Entry entry = getEntry();

        addSquare(x, y, z, width, height, 0, colour, vertexConsumer, entry);
        addSquare(x, y, z+depth, width, height, 0, colour, vertexConsumer, entry);

        addSquare(x, y, z, 0, height, depth, colour, vertexConsumer, entry);
        addSquare(x+width, y, z, 0, height, depth, colour, vertexConsumer, entry);

        addSquare(x, y, z, width, 0, depth, colour, vertexConsumer, entry);
        addSquare(x, y+height, z, width, 0, depth, colour, vertexConsumer, entry);

        vertexConsumers.drawCurrentLayer();
    }

    private static void addSquare(float x, float y, float z, float width, float height, float depth, Color colour, VertexConsumer vertexConsumer, MatrixStack.Entry entry) {
        if (depth == 0) {
            vertexConsumer.vertex(entry, x, y, z).color(colour.getRGB());
            vertexConsumer.vertex(entry, x, y+height, z).color(colour.getRGB());
            vertexConsumer.vertex(entry, x+width, y+height, z).color(colour.getRGB());
            vertexConsumer.vertex(entry, x+width, y, z).color(colour.getRGB());
        }
        else {
            vertexConsumer.vertex(entry, x, y, z+depth).color(colour.getRGB());
            vertexConsumer.vertex(entry, x+width, y+height, z+depth).color(colour.getRGB());
            vertexConsumer.vertex(entry, x+width, y+height, z).color(colour.getRGB());
            vertexConsumer.vertex(entry, x, y, z).color(colour.getRGB());
        }
    }

    public static void drawModel(float x, float y, float z, ModelUtil.Model model, Identifier texture, Color colour) {
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(getRenderLayerTexture(VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.TRIANGLES, texture));

        MatrixStack.Entry entry = getEntry();
        entry.translate(x, y, z);

        for (ModelUtil.ModelFace face : model.faces) {
            // the v coordinates are inverted..?
            vertexConsumer
                    .vertex(entry, face.vertex().vertex1().x, face.vertex().vertex1().y, face.vertex().vertex1().z)
                    .texture(face.textureCoordinates().vertex1().x, 1-face.textureCoordinates().vertex1().y)
                    .color(colour.getRGB());

            vertexConsumer
                    .vertex(entry, face.vertex().vertex2().x, face.vertex().vertex2().y, face.vertex().vertex2().z)
                    .texture(face.textureCoordinates().vertex2().x, 1-face.textureCoordinates().vertex2().y)
                    .color(colour.getRGB());

            vertexConsumer
                    .vertex(entry, face.vertex().vertex3().x, face.vertex().vertex3().y, face.vertex().vertex3().z)
                    .texture(face.textureCoordinates().vertex3().x, 1-face.textureCoordinates().vertex3().y)
                    .color(colour.getRGB());
        }

        vertexConsumers.drawCurrentLayer();
    }

    // blehhh, similar to the one above, but just not quite >.<
    public static void drawModel(float x, float y, float z, ModelUtil.Model model, Color colour) {
        drawModel(x, y, z, model, colour, getRenderLayer(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.TRIANGLES));
    }

    // ditto
    public static void drawModel(float x, float y, float z, ModelUtil.Model model, Color colour, RenderLayer layer) {
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(layer);

        MatrixStack.Entry entry = getEntry();
        entry.translate(x, y, z);

        for (ModelUtil.ModelFace face : model.faces) {
            vertexConsumer
                    .vertex(entry, face.vertex().vertex1().x, face.vertex().vertex1().y, face.vertex().vertex1().z)
                    .color(colour.getRGB());

            vertexConsumer
                    .vertex(entry, face.vertex().vertex2().x, face.vertex().vertex2().y, face.vertex().vertex2().z)
                    .color(colour.getRGB());

            vertexConsumer
                    .vertex(entry, face.vertex().vertex3().x, face.vertex().vertex3().y, face.vertex().vertex3().z)
                    .color(colour.getRGB());
        }

        vertexConsumers.drawCurrentLayer();
    }

    public static double lerp(double prev, double current) {
        return prev + (current - prev) * tickCounter.getTickProgress(true);
    }

    public static Vec3d lerpPos(Vec3d prevPos, Vec3d pos) {
        return new Vec3d(
                lerp(prevPos.x, pos.x),
                lerp(prevPos.y, pos.y),
                lerp(prevPos.z, pos.z)
        );
    }
}
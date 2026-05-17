package com.scale.scalehackv4.utils.render;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.scale.scalehackv4.Main;
import it.unimi.dsi.fastutil.floats.FloatUnaryOperator;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.render.*;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

import java.awt.*;
import java.util.HashMap;
import java.util.function.Predicate;

public class Render3dUtil extends RenderUtil {
    @Getter private static MatrixStack matrices;
    @Getter private static VertexConsumerProvider.Immediate vertexConsumers = VertexConsumerProvider.immediate(BufferAllocator.fixedSized(0));
    @Getter private static VertexConsumerProvider vertexConsumerProvider = VertexConsumerProvider.immediate(BufferAllocator.fixedSized(0));

    @Setter @Getter private static RenderTickCounter tickCounter = new RenderTickCounter.Dynamic(0,0, FloatUnaryOperator.negation());

    private static final HashMap<PipelineIdentifier, RenderLayer> LAYERS = new HashMap<>();

    public static void tickRender(MatrixStack matrices, VertexConsumerProvider vertexConsumerProvider, CameraRenderState camera) {
        matrices.push();

        Render3dUtil.matrices = matrices;
        if (vertexConsumerProvider != null) {
            Render3dUtil.vertexConsumerProvider = vertexConsumerProvider;
            vertexConsumers = (VertexConsumerProvider.Immediate) vertexConsumerProvider;
        }
        if (camera != null) matrices.translate(-camera.pos.getX(), -camera.pos.getY(), -camera.pos.getZ());
    }

    public static void finishTickRender() {
        matrices.pop();
    }

    private static MatrixStack.Entry getEntry() {
        return matrices.peek().copy();
    }

    protected static RenderLayer getRenderLayer(VertexFormat vertexFormat, VertexFormat.DrawMode drawMode, boolean cull) {
        return getRenderLayer(vertexFormat, drawMode, cull, null);
    }

    protected static RenderLayer getRenderLayer(VertexFormat vertexFormat, VertexFormat.DrawMode drawMode, boolean cull, Identifier texture) {
        PipelineIdentifier pipelineIdentifier = new PipelineIdentifier(vertexFormat, drawMode, cull, texture);
        RenderLayer layer = LAYERS.get(pipelineIdentifier);
        if (layer != null) return layer;

        RenderSetup.Builder base = RenderSetup.builder(getRenderPipeline(vertexFormat, drawMode, cull))
                .layeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING_FORWARD);
        if (texture != null) base = base.texture("Sampler0", texture);

        layer = RenderLayer.of(Main.MOD_NAME + "3d" + LAYERS.size(), base.build());
        LAYERS.put(pipelineIdentifier, layer);

        return layer;
    }

    public static void drawSquare(float x, float y, float z, float width, float height, float depth, Color colour) {
        drawSquare(x, y, z, width, height, depth, colour, true, false);
    }

    public static void drawSquare(float x, float y, float z, float width, float height, float depth, Color colour, boolean cull, boolean outline) {
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(
                        outline
                                ? getRenderLayer(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.DEBUG_LINES, cull)
                                : getRenderLayer(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS, cull)
        );

        MatrixStack.Entry entry = getEntry();

        addSquare(x, y, z, width, height, depth, colour, vertexConsumer, entry, outline);

        vertexConsumers.drawCurrentLayer();
    }

    public static void drawTexturedQuads(Predicate<VertexConsumer> drawPredicate, Identifier texture, boolean cull) {
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(getRenderLayer(VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS, cull, texture));

        drawPredicate.test(vertexConsumer);

        vertexConsumers.drawCurrentLayer();
    }

    public static void drawBox(float x, float y, float z, float width, float height, float depth, Color colour, boolean cull) {
        drawBox(x, y, z, width, height, depth, colour, cull, false);
    }

    public static void drawBox(float x, float y, float z, float width, float height, float depth, Color colour, boolean cull, boolean outline) {
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(
                outline
                        ? getRenderLayer(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.DEBUG_LINES, cull)
                        : getRenderLayer(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS, cull)
        );

        MatrixStack.Entry entry = getEntry();

        addSquare(x, y, z, width, height, 0, colour, vertexConsumer, entry, outline);
        addSquare(x, y, z+depth, width, height, 0, colour, vertexConsumer, entry, outline);

        addSquare(x, y, z, 0, height, depth, colour, vertexConsumer, entry, outline);
        addSquare(x+width, y, z, 0, height, depth, colour, vertexConsumer, entry, outline);

        addSquare(x, y, z, width, 0, depth, colour, vertexConsumer, entry, outline);
        addSquare(x, y+height, z, width, 0, depth, colour, vertexConsumer, entry, outline);

        vertexConsumers.drawCurrentLayer();
    }

    public static void drawLine(Vector3f one, Vector3f two, Color colour, boolean cull) {
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(getRenderLayer(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.DEBUG_LINES, cull));

        MatrixStack.Entry entry = getEntry();

        vertexConsumer.vertex(entry, one).color(colour.getRGB());
        vertexConsumer.vertex(entry, two).color(colour.getRGB());

        vertexConsumers.drawCurrentLayer();
    }

    private static void addSquare(float x, float y, float z, float width, float height, float depth, Color colour, VertexConsumer vertexConsumer, MatrixStack.Entry entry, boolean lines) {
        if (depth == 0) {
            vertexConsumer.vertex(entry, x, y, z).color(colour.getRGB());
            vertexConsumer.vertex(entry, x, y+height, z).color(colour.getRGB());
            vertexConsumer.vertex(entry, x+width, y+height, z).color(colour.getRGB());
            vertexConsumer.vertex(entry, x+width, y, z).color(colour.getRGB());
            if (lines) vertexConsumer.vertex(entry, x, y, z).color(colour.getRGB());
        }
        else {
            vertexConsumer.vertex(entry, x, y, z+depth).color(colour.getRGB());
            vertexConsumer.vertex(entry, x+width, y+height, z+depth).color(colour.getRGB());
            vertexConsumer.vertex(entry, x+width, y+height, z).color(colour.getRGB());
            vertexConsumer.vertex(entry, x, y, z).color(colour.getRGB());
            if (lines) vertexConsumer.vertex(entry, x, y, z+depth).color(colour.getRGB());
        }
    }

    public static void drawModel(float x, float y, float z, ModelUtil.Model model, Identifier texture, Color colour, boolean cull) {
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(getRenderLayer(VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.TRIANGLES, cull, texture));

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
    public static void drawModel(float x, float y, float z, ModelUtil.Model model, Color colour, boolean cull) {
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(getRenderLayer(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.TRIANGLES, cull));

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
package com.scale.scalehackv4.utils.render;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.scale.scalehackv4.Main;
import com.scale.scalehackv4.utils.C;
import it.unimi.dsi.fastutil.floats.FloatUnaryOperator;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.render.*;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector2i;
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

    @Setter private static Matrix4f lastProjectionMatrix = new Matrix4f();
    private static Matrix4f lastPosMatrix = new Matrix4f();
    private static Vec3d cameraPos = new Vec3d(0, 0, 0);

    // this was relatively simple, the tricky part was figuring out that you need "C.mc.gameRenderer.getBasicProjectionMatrix(C.mc.options.getFov().getValue())"
    // i hate you mojang
    public static Vector2i projectPoint(Vec3d pos) {
        int[] viewport = new int[]{0, 0, C.res().getScaledWidth(), C.res().getScaledHeight()};
        Vector3f relativePos = pos.subtract(cameraPos).toVector3f();
        // order actually matters for the multiplication!
        Vector3f screenPos = new Matrix4f(lastProjectionMatrix)
                .mul(lastPosMatrix)
                .project(relativePos, viewport, new Vector3f());

        if (screenPos.z < 0 || screenPos.z > 1
                || screenPos.x < 0 || screenPos.x > C.res().getScaledWidth()
                || screenPos.y < 0 || viewport[3] - (int) screenPos.y > C.res().getScaledHeight()) return null;

        return new Vector2i((int) screenPos.x, viewport[3] - (int) screenPos.y);
    }

    public static void tickRender(MatrixStack matrices, VertexConsumerProvider vertexConsumerProvider, CameraRenderState camera, Matrix4f posMatrix) {
        matrices.push();

        Render3dUtil.matrices = matrices;
        lastPosMatrix = new Matrix4f(posMatrix);

        if (vertexConsumerProvider != null) {
            Render3dUtil.vertexConsumerProvider = vertexConsumerProvider;
            vertexConsumers = (VertexConsumerProvider.Immediate) vertexConsumerProvider;
        }
        if (camera != null) {
            cameraPos = camera.pos;
            matrices.translate(-camera.pos.getX(), -camera.pos.getY(), -camera.pos.getZ());
        }
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

    public static void drawTexturedQuads(float x, float y, float z, float w, float h, Color colour, Identifier texture, boolean cull) {
        drawTexturedQuads(x, y, z, w, h, 0, 0, 1, 1, colour, texture, cull);
    }

    public static void drawTexturedQuads(float x, float y, float z, float w, float h, float u0, float v0, float u1, float v1, Color colour, Identifier texture, boolean cull) {
        VertexConsumer vertexConsumer = vertexConsumers.getBuffer(getRenderLayer(VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS, cull, texture));

        MatrixStack.Entry entry = getEntry();

        vertexConsumer.vertex(entry, x, y+h, 0).color(colour.getRGB()).texture(u0, v1);
        vertexConsumer.vertex(entry, x + w, y+h, 0).color(colour.getRGB()).texture(u1, v1);
        vertexConsumer.vertex(entry, x + w, y, 0).color(colour.getRGB()).texture(u1, v0);
        vertexConsumer.vertex(entry, x, y, 0).color(colour.getRGB()).texture(u0, v0);

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
package com.scale.scalehackv4.modules.impl.render;

import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.ModuleUtil;
import com.scale.scalehackv4.utils.client.TargetUtil;
import com.scale.scalehackv4.utils.render.ModelUtil;
import com.scale.scalehackv4.utils.render.Render3dUtil;
import com.scale.scalehackv4.utils.render.Vec3f;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelPartNames;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.awt.*;

@RegisterModule(
        name = "Model Modifier",
        enabledByDefault = true,
        category = Category.RENDER
)
// todo: hide capes
public class ModelModifierModule extends Module {
    @RegisterSubModule(name = "Self")
    public static boolean self = true;
    @RegisterSubModule(name = "Change Hand", parent = "Self")
    public static boolean changeHand = true;

    @RegisterSubModule(name = "Others")
    public static boolean others = true;

    @RegisterSubModule(name = "Hide Armor")
    public static boolean hideArmor = true;
    @RegisterSubModule(name = "Hide Helmet", parent = "Hide Armor")
    public static boolean hideHelmet = false;

    public static boolean shouldHideHelmet() {
        return shouldChangeModel() && hideHelmet && hideArmor;
    }
    public static boolean shouldHideArmor() {
        return shouldChangeModel() && hideArmor;
    }

    public static boolean shouldChangeHand() {
        return shouldChangeModel() && changeHand;
    }

    public static boolean shouldChangeModel() {
        return ModuleUtil.getModule(ModelModifierModule.class).isEnabled();
    }

    public static boolean isValidTarget(int entityId) {
        PlayerEntity player = (PlayerEntity) C.w().getEntityById(entityId);

        if ((entityId == C.p().getId() && !self) || (entityId != C.p().getId() && !others)) return false;

        return TargetUtil.isValidTarget(player);
    }

    // part of the reason i like 1.21 is that it allows me to use more than java 8 with the official forge version
    private record Body(ModelUtil.Model model, Identifier texture, ModelUtil.Model head, ModelUtil.Model body, ModelUtil.Model leftArm, ModelUtil.Model rightArm, ModelUtil.Model leftLeg, ModelUtil.Model rightLeg) {
    }

    private static final Body RAYMAN = new Body(
            ModelUtil.getModel("/assets/scalehackv4/models/rayman/texture.obj"),
            Identifier.of("scalehackv4", "models/rayman/texturemap.png"),
            ModelUtil.getModel("/assets/scalehackv4/models/rayman/head.obj"),
            ModelUtil.getModel("/assets/scalehackv4/models/rayman/body.obj"),
            ModelUtil.getModel("/assets/scalehackv4/models/rayman/lefthand.obj"),
            ModelUtil.getModel("/assets/scalehackv4/models/rayman/righthand.obj"),
            ModelUtil.getModel("/assets/scalehackv4/models/rayman/leftfoot.obj"),
            ModelUtil.getModel("/assets/scalehackv4/models/rayman/rightfoot.obj")
    );

    private static Body currentModel = RAYMAN;

    // todo: figure out where this number is from, idk how i came to this
    private static final float SCALE = 2;

    public static void drawRayman(EntityModel<?> model, Color color) {
        MatrixStack matrices = Render3dUtil.getMatrices();
        matrices.push();

        ModelModifierModule.entityModel = model;
        ModelModifierModule.matrices = matrices;
        ModelModifierModule.color = color;

        doTranslations();

        drawPart(EntityModelPartNames.HEAD, currentModel.head, Vec3f.middle(currentModel.head.box));
        drawPart(EntityModelPartNames.BODY, currentModel.body, Vec3f.middle(currentModel.body.box));

        drawPart(EntityModelPartNames.LEFT_ARM, currentModel.rightArm, Vec3f.lerpBox(RAYMAN.body.box, 1, 1, 0.5f));
        drawPart(EntityModelPartNames.RIGHT_ARM, currentModel.leftArm, Vec3f.lerpBox(RAYMAN.body.box, 0, 1, 0.5f));

        drawPart(EntityModelPartNames.LEFT_LEG, currentModel.rightLeg, Vec3f.lerpBox(RAYMAN.rightLeg.box, 1, 1, 0.5f));
        drawPart(EntityModelPartNames.RIGHT_LEG, currentModel.leftLeg, Vec3f.lerpBox(RAYMAN.leftLeg.box, 0, 1, 0.5f));

        matrices.pop();
    }

    private static void doTranslations() {
        // LivingEntityRenderer.render() line 93 does this, we have to reverse it...
        matrices.translate(0.0F, 1.501F, 0.0F);

        matrices.scale(SCALE, SCALE, SCALE);

        // line 91 of LivingEntityRenderer.render() scales X and Y by -1...?
        matrices.multiply(new Quaternionf().rotateAxis((float) Math.toRadians(180), 1, 0, 0));
    }

    private static EntityModel<?> entityModel;
    private static MatrixStack matrices;
    private static Color color;

    private static void drawPart(String part, ModelUtil.Model objModel, Vec3f pivot) {
        ModelPart modelPart = entityModel.getChildPart(part);

        matrices.push();
        matrices.multiply(new Quaternionf().rotationZYX(-modelPart.roll, -modelPart.yaw, modelPart.pitch), pivot.x, pivot.y, pivot.z);
        Render3dUtil.drawModel(0, 0, 0, objModel, currentModel.texture, color);
        matrices.pop();
    }

    public static void drawItem(EntityModel<?> model, ItemRenderState itemState, Arm arm, MatrixStack matrices, OrderedRenderCommandQueue queue, int light, int outlineColor) {
        if (!itemState.isEmpty()) {
            matrices.push();

            ModelPart armModelPart = model.getChildPart(arm.equals(Arm.RIGHT) ? EntityModelPartNames.RIGHT_ARM : EntityModelPartNames.LEFT_ARM);
            ModelUtil.Model armModel = arm.equals(Arm.LEFT) ? RAYMAN.rightArm : RAYMAN.leftArm;

            Vector3f pivot = new Vector3f(
                    (float) (armModel.box.minX + armModel.box.maxX) / 2,
                    (float) (armModel.box.maxY),
                    (float) (armModel.box.minZ + armModel.box.maxZ) / 2
            );


            new Quaternionf().rotationZYX(armModelPart.roll, armModelPart.yaw, armModelPart.pitch).transform(pivot).add(pivot);

            matrices.translate(pivot.x, pivot.y + 0.125f, pivot.z);
            matrices.multiply(new Quaternionf().rotationZYX(0, (float) Math.toRadians(180), (float) Math.toRadians(80)));
            matrices.multiply(new Quaternionf().rotationZYX(-armModelPart.roll, armModelPart.yaw, -armModelPart.pitch));

            itemState.render(matrices, queue, light, OverlayTexture.DEFAULT_UV, outlineColor);
            matrices.pop();
        }
    }

    public static void drawHand() {
        MatrixStack matrices = Render3dUtil.getMatrices();
        matrices.push();

        matrices.scale(SCALE,SCALE,SCALE);
        matrices.multiply(new Quaternionf().rotateAxis((float) Math.toRadians(180), 0.7f, -0.2f, 0.1f), -0.3f, 0.35f, -0.1f);
        Render3dUtil.drawModel(0, 0, 0, RAYMAN.leftArm, RAYMAN.texture, Color.WHITE);
        matrices.pop();
    }

    @Override
    public void onEnable() {}

    @Override
    public void onDisable() {}
}
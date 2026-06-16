package com.scale.scalehackv4.mixins.render;

import com.scale.scalehackv4.modules.impl.render.EspModule;
import com.scale.scalehackv4.utils.client.ModuleUtil;
import com.scale.scalehackv4.utils.client.TargetUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class EspMixin extends Entity {
    public EspMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(method = "isGlowing", at = @At("HEAD"), cancellable = true)
    private void isGlowing(CallbackInfoReturnable<Boolean> cir) {
        if (ModuleUtil.isEnabled(EspModule.class) && EspModule.glow && TargetUtil.isValidTarget(this, true)) {
            cir.setReturnValue(true);
        }
    }
}

package com.scale.scalehackv4.mixins.render.NoRender;

import com.scale.scalehackv4.modules.impl.render.NoRender;
import net.minecraft.client.particle.ExplosionLargeParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ExplosionLargeParticle.Factory.class)
public abstract class ExplosionParticleHiderMixin {
    @Inject(method = "createParticle(Lnet/minecraft/particle/SimpleParticleType;Lnet/minecraft/client/world/ClientWorld;DDDDDDLnet/minecraft/util/math/random/Random;)Lnet/minecraft/client/particle/Particle;", at = @At("HEAD"), cancellable = true)
    public void tick(SimpleParticleType simpleParticleType, ClientWorld clientWorld, double d, double e, double f, double g, double h, double i, Random random, CallbackInfoReturnable<Particle> cir) {
        if (NoRender.shouldHideExplosions()) cir.setReturnValue(null);
    }
}

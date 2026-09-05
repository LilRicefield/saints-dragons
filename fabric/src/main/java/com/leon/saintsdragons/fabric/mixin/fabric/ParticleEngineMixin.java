package com.leon.saintsdragons.fabric.mixin.fabric;

import com.leon.saintsdragons.client.particle.DragonParticleRenderTypes;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleRenderType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {
    @Shadow
    @Final
    @Mutable
    private static List<ParticleRenderType> RENDER_ORDER;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void saintsdragons$registerNoDepthParticleLayer(CallbackInfo ci) {
        List<ParticleRenderType> extendedOrder = new ArrayList<>(RENDER_ORDER.size() + 1);
        for (ParticleRenderType renderType : RENDER_ORDER) {
            if (renderType == ParticleRenderType.CUSTOM) {
                extendedOrder.add(DragonParticleRenderTypes.TRANSLUCENT_NO_DEPTH_WRITE);
            }
            extendedOrder.add(renderType);
        }
        RENDER_ORDER = List.copyOf(extendedOrder);
    }
}

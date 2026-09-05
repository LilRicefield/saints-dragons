package com.leon.saintsdragons.client.particle;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;

public final class DragonParticleShaders {
    public static final ResourceLocation IMPACT_GLOW = SaintsDragonsCommon.rl("impact_glow");

    private static ShaderInstance impactGlowShader;

    private DragonParticleShaders() {
    }

    public static void setImpactGlowShader(ShaderInstance shader) {
        impactGlowShader = shader;
    }

    public static ShaderInstance getImpactGlowShader() {
        ShaderInstance shader = impactGlowShader;
        return shader != null ? shader : GameRenderer.getParticleShader();
    }
}

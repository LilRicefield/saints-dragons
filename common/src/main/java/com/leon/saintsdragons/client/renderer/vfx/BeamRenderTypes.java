package com.leon.saintsdragons.client.renderer.vfx;

import com.leon.saintsdragons.client.renderer.ShaderPassCompatibility;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/** Unlit alpha blending in vanilla, retaining the existing entity pass for shader packs. */
public abstract class BeamRenderTypes extends RenderType {
    private static final Map<ResourceLocation, RenderType> UNLIT = new HashMap<>();

    private BeamRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize,
                            boolean crumbling, boolean sorting, Runnable setup, Runnable clear) {
        super(name, format, mode, bufferSize, crumbling, sorting, setup, clear);
    }

    public static RenderType translucent(ResourceLocation texture) {
        if (ShaderPassCompatibility.isShaderPackInUse()) {
            return RenderType.entityTranslucent(texture);
        }
        return UNLIT.computeIfAbsent(texture, BeamRenderTypes::createUnlit);
    }

    private static RenderType createUnlit(ResourceLocation texture) {
        return create("saintsdragons_unlit_beam", DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS, 256, false, true,
                CompositeState.builder()
                        .setShaderState(RENDERTYPE_BEACON_BEAM_SHADER)
                        .setTextureState(new TextureStateShard(texture, false, false))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                        .setCullState(NO_CULL)
                        .setDepthTestState(LEQUAL_DEPTH_TEST)
                        .setWriteMaskState(COLOR_WRITE)
                        .createCompositeState(false));
    }
}

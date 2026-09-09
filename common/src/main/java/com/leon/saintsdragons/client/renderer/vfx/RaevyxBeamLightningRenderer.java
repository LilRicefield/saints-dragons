package com.leon.saintsdragons.client.renderer.vfx;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class RaevyxBeamLightningRenderer {
    // Each full texture spans half the current beam, with overlapping edges for scrolling.
    private static final float RIBBON_TEXTURE_CYCLES = 2.0F;
    private static final float OBSERVER_RIBBON_WIDTH_SCALE = 1.5F;
    private static final ResourceLocation LIGHTNING_BEAM_TEXTURE =
            SaintsDragonsCommon.rl("textures/particle/lightning_beam.png");
    private static final ResourceLocation LIGHTNING_BEAM_AURA_TEXTURE =
            SaintsDragonsCommon.rl("textures/particle/lightning_beam_aura.png");
    private static final ResourceLocation LIGHTNING_BEAM_SWIRL_TEXTURE =
            SaintsDragonsCommon.rl("textures/particle/lightning_beam_swirl.png");
    private static final ResourceLocation LIGHTNING_BEAM_SECOND_SWIRL_TEXTURE =
            SaintsDragonsCommon.rl("textures/particle/lightning_beam_second_swirl.png");
    private static final BeamRibbonRenderer.Style LIGHTNING_BEAM_STYLE =
            new BeamRibbonRenderer.Style(
                    0.75F,
                    RIBBON_TEXTURE_CYCLES,
                    0.075F,
                    0.12F,
                    0.28F,
                    1.0F,
                    true);
    private static final BeamRibbonRenderer.Style LIGHTNING_BEAM_AURA_STYLE =
            new BeamRibbonRenderer.Style(
                    0.92F,
                    RIBBON_TEXTURE_CYCLES,
                    0.11F,
                    0.12F,
                    0.28F,
                    0.72F,
                    true);
    private static final BeamRibbonRenderer.Style LIGHTNING_BEAM_FAINT_AURA_STYLE =
            new BeamRibbonRenderer.Style(
                    1.16F,
                    RIBBON_TEXTURE_CYCLES,
                    0.14F,
                    0.12F,
                    0.28F,
                    0.05F,
                    true);
    private static final BeamRibbonRenderer.Style LIGHTNING_BEAM_SWIRL_STYLE =
            new BeamRibbonRenderer.Style(
                    0.88F,
                    RIBBON_TEXTURE_CYCLES,
                    0.09F,
                    0.5F,
                    0.28F,
                    0.85F,
                    true);
    private static final BeamRibbonRenderer.Style LIGHTNING_BEAM_SECOND_SWIRL_STYLE =
            new BeamRibbonRenderer.Style(
                    0.62F,
                    RIBBON_TEXTURE_CYCLES,
                    0.10F,
                    0.5F,
                    0.28F,
                    0.60F,
                    true);
    private static final ResourceLocation[] FAST_LINE_TEXTURES = new ResourceLocation[16];
    private static final BeamGrainRenderer.RibbonEmitterStyle FAST_LINE_EMITTER_STYLE =
            new BeamGrainRenderer.RibbonEmitterStyle(
                    32, 8.0F, 16.0F,
                    3.0F, 3.0F,
                    0.5F, 0.5F, 3.0F,
                    0.5F, 0.25F, 0.75F, 0.85F,
                    0.75F, 0.25F);
    private static final long FAST_LINE_SEED_SALT = 0xD1B54A32D192ED03L;
    private static final ResourceLocation STAR_TEXTURE =
            SaintsDragonsCommon.rl("textures/particle/star.png");
    private static final BeamStarFlashRenderer.Style STAR_STYLE =
            new BeamStarFlashRenderer.Style(
                    16,
                    4.0F, 8.0F,
                    1.0F, 4.0F,
                    1.25F, 0.8F, 0.25F,
                    0.5F, 0.75F,
                    0.15F, 0.45F,
                    0.95F, 1.0F,
                    0.3F, 0.6F);
    private static final long STAR_SEED_SALT = 0x6A09E667F3BCC909L;
    private static final ResourceLocation[] LIGHTNING_ZAP_TEXTURES = new ResourceLocation[16];
    private static final float LIGHTNING_ZAP_FRAME_TICKS = 0.5F;
    private static final BeamStarFlashRenderer.Style LIGHTNING_ZAP_STYLE =
            new BeamStarFlashRenderer.Style(
                    8,
                    4.0F, 6.0F,
                    2.0F, 6.0F,
                    1.0F, 0.15F, 0.15F,
                    0.9F, 1.15F,
                    0.0F, 0.0F,
                    0.95F, 0.85F,
                    0.3F, 0.6F);
    private static final long LIGHTNING_ZAP_SEED_SALT = 0xBB67AE8584CAA73BL;

    static {
        for (int frame = 0; frame < FAST_LINE_TEXTURES.length; frame++) {
            FAST_LINE_TEXTURES[frame] = SaintsDragonsCommon.rl(
                    "textures/particle/fast_lines" + frame + ".png");
        }
        for (int frame = 0; frame < LIGHTNING_ZAP_TEXTURES.length; frame++) {
            LIGHTNING_ZAP_TEXTURES[frame] = SaintsDragonsCommon.rl(
                    "textures/particle/lightning_zap" + frame + ".png");
        }
    }

    private RaevyxBeamLightningRenderer() {
    }

    public static void render(Raevyx raevyx, PoseStack poseStack, MultiBufferSource bufferSource,
                              float beamLength, float visibility, float ageInTicks,
                              Vec3 beamStartWorld, Vec3 beamEndWorld,
                              boolean firstPersonView) {
        if (raevyx == null || beamLength <= 0.05F || visibility <= 0.01F) {
            return;
        }

        long entitySeed = raevyx.getUUID().getMostSignificantBits()
                ^ raevyx.getUUID().getLeastSignificantBits();
        boolean gold = raevyx.getTextureVariant() == Raevyx.VARIANT_NIGHT_GOLD;
        float green = gold ? 0.75F : 0.0F;
        float blue = gold ? 0.15F : 0.0F;
        float ribbonWidthScale = firstPersonView ? 1.0F : OBSERVER_RIBBON_WIDTH_SCALE;
        BeamRibbonRenderer.render(poseStack, bufferSource, LIGHTNING_BEAM_TEXTURE,
                beamLength, visibility, ageInTicks, beamStartWorld, beamEndWorld,
                LIGHTNING_BEAM_STYLE,
                1.0F, green, blue, ribbonWidthScale, firstPersonView);
        BeamRibbonRenderer.render(poseStack, bufferSource, LIGHTNING_BEAM_AURA_TEXTURE,
                beamLength, visibility, ageInTicks, beamStartWorld, beamEndWorld,
                LIGHTNING_BEAM_AURA_STYLE,
                1.0F, green, blue, ribbonWidthScale, firstPersonView);
        BeamRibbonRenderer.render(poseStack, bufferSource, LIGHTNING_BEAM_AURA_TEXTURE,
                beamLength, visibility, ageInTicks, beamStartWorld, beamEndWorld,
                LIGHTNING_BEAM_FAINT_AURA_STYLE,
                0.05F, green * 0.05F, blue * 0.05F, ribbonWidthScale, firstPersonView);
        BeamRibbonRenderer.render(poseStack, bufferSource, LIGHTNING_BEAM_SWIRL_TEXTURE,
                beamLength, visibility, ageInTicks, beamStartWorld, beamEndWorld,
                LIGHTNING_BEAM_SWIRL_STYLE,
                1.0F, green, blue, ribbonWidthScale, firstPersonView);
        BeamRibbonRenderer.render(poseStack, bufferSource, LIGHTNING_BEAM_SECOND_SWIRL_TEXTURE,
                beamLength, visibility, ageInTicks, beamStartWorld, beamEndWorld,
                LIGHTNING_BEAM_SECOND_SWIRL_STYLE,
                0.0F, 0.0F, 0.0F, ribbonWidthScale, firstPersonView);
        BeamGrainRenderer.renderEmitter(poseStack, bufferSource, FAST_LINE_TEXTURES,
                beamLength, visibility, ageInTicks, entitySeed ^ FAST_LINE_SEED_SALT,
                FAST_LINE_EMITTER_STYLE, beamStartWorld, beamEndWorld,
                0.0F, 0.0F, 0.0F, firstPersonView);
        BeamStarFlashRenderer.renderAnimated(poseStack, bufferSource,
                LIGHTNING_ZAP_TEXTURES, LIGHTNING_ZAP_FRAME_TICKS,
                beamLength, visibility, ageInTicks, entitySeed ^ LIGHTNING_ZAP_SEED_SALT,
                LIGHTNING_ZAP_STYLE, beamStartWorld, beamEndWorld,
                1.0F, green, blue, firstPersonView);
        ProceduralBeamLightningRenderer.render(poseStack, bufferSource,
                beamLength, visibility, ageInTicks, entitySeed, 1.0F, green, blue);
    }

    /** Called after the model pose is restored so stars face the camera independently of the beam. */
    public static void renderStars(Raevyx raevyx, PoseStack entityPose, MultiBufferSource bufferSource,
                                   Matrix4f beamToEntity, float beamLength, float visibility, float ageInTicks) {
        long entitySeed = raevyx.getUUID().getMostSignificantBits()
                ^ raevyx.getUUID().getLeastSignificantBits();
        boolean gold = raevyx.getTextureVariant() == Raevyx.VARIANT_NIGHT_GOLD;
        BeamStarFlashRenderer.renderBillboards(entityPose, bufferSource, STAR_TEXTURE,
                beamLength, visibility, ageInTicks, entitySeed ^ STAR_SEED_SALT, STAR_STYLE,
                beamToEntity, Raevyx.MODEL_SCALE,
                1.0F, gold ? 0.75F : 0.0F, gold ? 0.15F : 0.0F);
    }
}

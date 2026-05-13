package com.leon.saintsdragons.client.renderer.raevyx;

import com.leon.saintsdragons.client.model.raevyx.RaevyxModel;
import com.leon.saintsdragons.client.renderer.DragonGeoEntityRenderer;
import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.NotNull;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import com.leon.saintsdragons.client.renderer.layer.raevyx.RaevyxLightningBeamLayer;
import com.leon.saintsdragons.client.renderer.layer.raevyx.RaevyxGlowLayer;

@Environment(EnvType.CLIENT)
public class RaevyxRenderer extends DragonGeoEntityRenderer<Raevyx> {
    private static final ResourceLocation TEXTURE_MALE = SaintsDragonsCommon.rl("textures/entity/raevyx/raevyx.png");
    private static final ResourceLocation TEXTURE_FEMALE = SaintsDragonsCommon.rl("textures/entity/raevyx/raevyx_female.png");
    private static final ResourceLocation TEXTURE_NIGHT_GOLD_MALE = SaintsDragonsCommon.rl("textures/entity/raevyx/raevyx_night_gold.png");
    private static final ResourceLocation TEXTURE_NIGHT_GOLD_FEMALE = SaintsDragonsCommon.rl("textures/entity/raevyx/raevyx_night_gold_female.png");
    private static final ResourceLocation TEXTURE_BABY_MALE = SaintsDragonsCommon.rl("textures/entity/raevyx/baby_raevyx.png");
    private static final ResourceLocation TEXTURE_BABY_FEMALE = SaintsDragonsCommon.rl("textures/entity/raevyx/baby_raevyx_female.png");
    private static final String PASSENGER_BONE = "passengerBone";
    private static final String BEAM_BONE = "beamBone";
    private static final float PASSENGER_X = 0.0f;
    private static final float PASSENGER_Y = -3.0f;
    private static final float PASSENGER_Z = 0.0f;

    public RaevyxRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new RaevyxModel());
        this.addRenderLayer(new RaevyxGlowLayer(this));
        this.addRenderLayer(new RaevyxLightningBeamLayer());
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull Raevyx entity) {
        if (entity.isBaby()) {
            return entity.isFemale() ? TEXTURE_BABY_FEMALE : TEXTURE_BABY_MALE;
        }
        if (entity.getTextureVariant() == Raevyx.VARIANT_NIGHT_GOLD) {
            return entity.isFemale() ? TEXTURE_NIGHT_GOLD_FEMALE : TEXTURE_NIGHT_GOLD_MALE;
        }
        return entity.isFemale() ? TEXTURE_FEMALE : TEXTURE_MALE;
    }

    @Override
    protected float getBabyShadowRadius(Raevyx entity) {
        return 1.25F;
    }

    @Override
    protected float getAdultShadowRadius(Raevyx entity) {
        return 3.0f;
    }

    @Override
    protected String[] trackedBoneNames() {
        return new String[] {PASSENGER_BONE, BEAM_BONE};
    }

    @Override
    protected LocatorSpec[] locatorSpecs(Raevyx entity) {
        return new LocatorSpec[] {
                new LocatorSpec(PASSENGER_BONE, PASSENGER_X, PASSENGER_Y, PASSENGER_Z,
                        "passengerLocator", "passengerSeat0"),
                new LocatorSpec(BEAM_BONE, 0.0f, 0.0f, 0.0f, "beamBoneOrigin")
        };
    }
}

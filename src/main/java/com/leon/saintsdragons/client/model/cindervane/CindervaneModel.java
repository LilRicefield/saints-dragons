package com.leon.saintsdragons.client.model.cindervane;

import com.leon.saintsdragons.SaintsDragons;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;

public class CindervaneModel extends DefaultedEntityGeoModel<Cindervane> {
    public CindervaneModel() {
        super(SaintsDragons.rl("cindervane"), "headController");
    }

    private static final ResourceLocation MALE_TEXTURE = ResourceLocation.fromNamespaceAndPath("saintsdragons", "textures/entity/cindervane/cindervane.png");
    private static final ResourceLocation FEMALE_TEXTURE = ResourceLocation.fromNamespaceAndPath("saintsdragons", "textures/entity/cindervane/cindervane_female.png");


    @Override
    public void setCustomAnimations(Cindervane entity, long instanceId, AnimationState<Cindervane> animationState) {
        super.setCustomAnimations(entity, instanceId, animationState);

        if (entity.isAlive()) {
            applyBankingRoll(entity, animationState);
        }
    }

    @Override
    public ResourceLocation getTextureResource(Cindervane entity) {
        // TODO: Add baby texture variant
        return entity.isFemale() ? FEMALE_TEXTURE : MALE_TEXTURE;
    }

    /**
     * Apply smoothed banking roll straight to the body bone based on mouse drag.
     * Only applies when being ridden - wild Cindervanes don't bank.
     * FIXED: Always calculate from initialSnapshot to prevent cross-entity sync bleeding.
     */
    private void applyBankingRoll(Cindervane entity, AnimationState<Cindervane> state) {
        var bodyOpt = getBone("body");
        if (bodyOpt.isEmpty()) {
            return;
        }

        GeoBone body = bodyOpt.get();
        var snap = body.getInitialSnapshot();

        float partialTick = state.getPartialTick();
        float bankAngleDeg = entity.getBankAngleDegrees(partialTick);
        // Banking right rotates negative around Z, hence the inversion.
        float bankAngleRad = Mth.clamp(-bankAngleDeg * Mth.DEG_TO_RAD, -Mth.HALF_PI, Mth.HALF_PI);

        // Set directly from snapshot + bank angle (no lerp with previous frame's bone rotation)
        // This prevents sync bleeding between multiple dragons rendering in the same frame
        body.setRotZ(snap.getRotZ() + bankAngleRad);
    }
}


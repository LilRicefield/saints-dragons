package com.leon.saintsdragons.client.model.armor;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.item.DragonlordArmorItem;
import com.leon.saintsdragons.common.item.DragonlordArmorSetBonus;
import com.leon.saintsdragons.server.flight.DragonFlightVisuals;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

import java.util.Map;
import java.util.WeakHashMap;

public class DragonlordArmorModel extends GeoModel<DragonlordArmorItem> {
    private static final float DEG_TO_RAD = Mth.DEG_TO_RAD;
    private static final ResourceLocation MODEL = SaintsDragonsCommon.rl("geo/armor/dragonlord_armor.geo.json");
    private static final ResourceLocation TEXTURE = SaintsDragonsCommon.rl("textures/armor/dragonlord_armor.png");
    private static final ResourceLocation ANIMATION = SaintsDragonsCommon.rl("animations/armor/dragonlord_armor.animation.json");
    private final Map<LivingEntity, DivePoseTracker> divePoseTrackers = new WeakHashMap<>();

    @Override
    public void setCustomAnimations(DragonlordArmorItem animatable, long instanceId,
                                    AnimationState<DragonlordArmorItem> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        Entity wearer = animationState.getData(DataTickets.ENTITY);
        if (!(wearer instanceof LivingEntity living)
                || !living.isFallFlying()
                || !DragonlordArmorSetBonus.isWearingFullSet(living)) {
            if (wearer instanceof LivingEntity livingEntity) {
                divePoseTrackers.remove(livingEntity);
            }
            return;
        }

        float blend = getDivePose(living, animationState.getPartialTick());
        if (blend <= 0.001F) {
            return;
        }

        applyDiveRotation("leftwing", blend, -4.49F, -3.48F, 22.68F);
        applyDiveRotation("leftforewing", blend, -0.08F, 62.5F, -0.7F);
        applyDiveRotation("leftfinger1", blend, 3.5F, -57.66F, 7.3F);
        applyDiveRotation("leftfinger2", blend, 0.0F, -25.0F, 0.0F);
        applyDiveRotation("leftfinger3", blend, 0.0F, -27.5F, 0.0F);

        applyDiveRotation("rightwing", blend, -4.49F, 3.48F, -22.68F);
        applyDiveRotation("rightforewing", blend, -0.08F, -62.5F, 0.7F);
        applyDiveRotation("rightfinger1", blend, 3.5F, 57.66F, -7.3F);
        applyDiveRotation("rightfinger2", blend, 0.0F, 25.0F, 0.0F);
        applyDiveRotation("rightfinger3", blend, 0.0F, 27.5F, 0.0F);
    }

    private float getDivePose(LivingEntity living, float partialTick) {
        DivePoseTracker tracker = divePoseTrackers.computeIfAbsent(
                living,
                ignored -> new DivePoseTracker(living.tickCount - 1)
        );

        int elapsedTicks = living.tickCount - tracker.lastTick;
        if (elapsedTicks < 0) {
            tracker.pose = new DragonFlightVisuals.DivePoseState();
            elapsedTicks = 1;
        }

        int updates = Mth.clamp(elapsedTicks, 0, 5);
        for (int i = 0; i < updates; i++) {
            DragonFlightVisuals.tickDivePose(tracker.pose, true, living.getDeltaMovement());
        }
        tracker.lastTick = living.tickCount;

        return Mth.clamp(DragonFlightVisuals.getDivePose(tracker.pose, partialTick), 0.0F, 1.0F);
    }

    private void applyDiveRotation(String boneName, float blend,
                                   float xDegrees, float yDegrees, float zDegrees) {
        getBone(boneName).ifPresent(bone -> {
            // GeckoLib converts Blockbench rotations with inverted X/Y axes.
            bone.setRotX(bone.getRotX() - xDegrees * DEG_TO_RAD * blend);
            bone.setRotY(bone.getRotY() - yDegrees * DEG_TO_RAD * blend);
            bone.setRotZ(bone.getRotZ() + zDegrees * DEG_TO_RAD * blend);
        });
    }

    @Override
    public ResourceLocation getModelResource(DragonlordArmorItem animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(DragonlordArmorItem animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(DragonlordArmorItem animatable) {
        return ANIMATION;
    }

    private static final class DivePoseTracker {
        private DragonFlightVisuals.DivePoseState pose = new DragonFlightVisuals.DivePoseState();
        private int lastTick;

        private DivePoseTracker(int lastTick) {
            this.lastTick = lastTick;
        }
    }
}

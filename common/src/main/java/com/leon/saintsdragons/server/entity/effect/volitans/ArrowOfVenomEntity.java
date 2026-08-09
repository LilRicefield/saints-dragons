package com.leon.saintsdragons.server.entity.effect.volitans;

import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import com.leon.saintsdragons.server.entity.npc.IvyTheDragonMerchant;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class ArrowOfVenomEntity extends AbstractArrow implements GeoEntity {
    private static final EntityDataAccessor<Boolean> IMPACTED =
            SynchedEntityData.defineId(ArrowOfVenomEntity.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation IMPACT = RawAnimation.begin().thenPlay("impact");
    private static final int POISON_DURATION_TICKS = 30 * 20;
    private static final int BLINDNESS_DURATION_TICKS = 5 * 20;
    private static final int VOLITANS_VENOM_NEUTRALIZE_TICKS = 30 * 20;
    private static final float VELOCITY_MULTIPLIER = 1.35F;
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    public ArrowOfVenomEntity(EntityType<? extends ArrowOfVenomEntity> type, Level level) {
        super(type, level);
    }

    public ArrowOfVenomEntity(Level level, LivingEntity owner) {
        super(ModEntities.ARROW_OF_VENOM.get(), owner, level);
    }

    @Override
    public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
        super.shoot(x, y, z, velocity * VELOCITY_MULTIPLIER, inaccuracy);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(IMPACTED, false);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (shouldIgnoreFriendlyHit(result)) {
            discard();
            return;
        }
        markImpacted();
        super.onHitEntity(result);
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        markImpacted();
        super.onHitBlock(result);
    }

    @Override
    protected void doPostHurtEffects(LivingEntity target) {
        super.doPostHurtEffects(target);
        target.invulnerableTime = 0;
        if (target instanceof Volitans volitans) {
            volitans.neutralizeVenom(VOLITANS_VENOM_NEUTRALIZE_TICKS);
        }
        target.addEffect(new MobEffectInstance(MobEffects.POISON, POISON_DURATION_TICKS, 1));
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BLINDNESS_DURATION_TICKS, 0));
    }

    @Override
    protected float getWaterInertia() {
        return 1.0F;
    }

    @Override
    protected ItemStack getPickupItem() {
        return new ItemStack(ModItems.ARROW_OF_VENOM.get());
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::animationPredicate));
    }

    private <E extends GeoEntity> PlayState animationPredicate(AnimationState<E> state) {
        if (entityData.get(IMPACTED)) {
            state.getController().setAnimation(IMPACT);
            return PlayState.CONTINUE;
        }
        return PlayState.STOP;
    }

    private void markImpacted() {
        entityData.set(IMPACTED, true);
    }

    private boolean shouldIgnoreFriendlyHit(EntityHitResult result) {
        if (!(result.getEntity() instanceof LivingEntity target)) {
            return false;
        }
        Entity owner = getOwner();
        return owner instanceof IvyTheDragonMerchant ivy && !ivy.canTargetForCombat(target);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}

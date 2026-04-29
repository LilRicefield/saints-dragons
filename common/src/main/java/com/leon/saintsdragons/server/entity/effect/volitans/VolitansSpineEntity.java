package com.leon.saintsdragons.server.entity.effect.volitans;

import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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

public class VolitansSpineEntity extends AbstractArrow implements GeoEntity {
    private static final int LIFETIME_TICKS = 40;
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("volitans_spine");
    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private float impactDamage = 0.0F;
    private int poisonDurationTicks = 0;
    private int poisonAmplifier = 0;

    public VolitansSpineEntity(EntityType<? extends VolitansSpineEntity> type, Level level) {
        super(type, level);
        this.pickup = Pickup.DISALLOWED;
        this.setBaseDamage(0.0D);
    }

    public VolitansSpineEntity(Level level, LivingEntity owner) {
        this(ModEntities.VOLITANS_SPINE.get(), level);
        this.setOwner(owner);
        this.pickup = Pickup.DISALLOWED;
        this.setBaseDamage(0.0D);
    }

    public void setImpactEffects(float damage, int poisonDurationTicks, int poisonAmplifier) {
        this.impactDamage = Math.max(0.0F, damage);
        this.poisonDurationTicks = Math.max(0, poisonDurationTicks);
        this.poisonAmplifier = Math.max(0, poisonAmplifier);
        this.setBaseDamage(this.impactDamage);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && this.tickCount >= LIFETIME_TICKS) {
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (!level().isClientSide) {
            if (impactDamage > 0.0F && result.getEntity() instanceof LivingEntity target) {
                LivingEntity owner = this.getOwner() instanceof LivingEntity living ? living : null;
                boolean validTarget = owner == null || (target != owner && !isAlliedTarget(owner, target));
                if (validTarget) {
                    if (owner != null) {
                        target.hurt(this.damageSources().mobAttack(owner), impactDamage);
                    } else {
                        target.hurt(this.damageSources().magic(), impactDamage);
                    }
                    if (poisonDurationTicks > 0) {
                        target.addEffect(new MobEffectInstance(MobEffects.POISON, poisonDurationTicks, poisonAmplifier));
                    }
                    discard();
                }
            } else {
                discard();
            }
        }
    }

    @Override
    protected boolean canHitEntity(net.minecraft.world.entity.Entity target) {
        if (!super.canHitEntity(target)) {
            return false;
        }
        if (target == getOwner()) {
            return false;
        }
        if (target instanceof LivingEntity living && this.getOwner() instanceof LivingEntity owner) {
            if (isAlliedTarget(owner, living)) {
                return false;
            }
        }
        return true;
    }

    private boolean isAlliedTarget(LivingEntity owner, LivingEntity target) {
        if (owner == null || target == null) {
            return false;
        }
        if (owner.isAlliedTo(target)) {
            return true;
        }
        if (owner instanceof DragonEntity dragon) {
            return dragon.isAlly(target);
        }
        return false;
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!level().isClientSide) {
            discard();
        }
    }

    @Override
    protected ItemStack getPickupItem() {
        return ItemStack.EMPTY;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::animationPredicate));
    }

    private <E extends GeoEntity> PlayState animationPredicate(AnimationState<E> state) {
        state.getController().setAnimation(IDLE);
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
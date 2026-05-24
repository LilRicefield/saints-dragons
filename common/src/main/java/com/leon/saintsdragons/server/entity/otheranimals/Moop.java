//pee pee poo poo meep meep moop moop :>
package com.leon.saintsdragons.server.entity.otheranimals;

import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.server.ai.goals.base.DirectSwimWanderGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class Moop extends AbstractFish implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.moop.idle");
    private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("animation.moop.swim");
    private static final RawAnimation SWIM_FAST = RawAnimation.begin().thenLoop("animation.moop.swim_fast");
    private static final RawAnimation ON_LAND = RawAnimation.begin().thenLoop("animation.moop.on_land");

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);

    public Moop(EntityType<? extends AbstractFish> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.@NotNull Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 3.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.8D);
    }

    public static boolean canSpawnHere(EntityType<Moop> type,
                                       LevelAccessor level,
                                       MobSpawnType spawnType,
                                       BlockPos pos,
                                       RandomSource random) {
        return WaterAnimal.checkSurfaceWaterAnimalSpawnRules(type, level, spawnType, pos, random);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new DirectSwimWanderGoal(this, 15.0F, 0.10, 30));
        this.goalSelector.addGoal(2, new PanicGoal(this, 1.25F));
    }

    @Override
    public void triggerAnim(@Nullable String controllerName, String animName) {
        GeoEntity.super.triggerAnim(controllerName, animName);
    }

    @Override
    protected @NotNull SoundEvent getFlopSound() {
        return SoundEvents.COD_FLOP;
    }

    @Override
    public @NotNull ItemStack getBucketItemStack() {
        return new ItemStack(Items.WATER_BUCKET);
    }

    @Override
    protected void dropCustomDeathLoot(@NotNull DamageSource damageSource, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(damageSource, looting, recentlyHit);
        this.spawnAtLocation(this.isOnFire() || this.getRemainingFireTicks() > 0
                ? ModItems.COOKED_MOOP.get()
                : ModItems.RAW_MOOP.get());
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 4, state -> {
            if (!isInWaterOrBubble()) {
                state.setAndContinue(ON_LAND);
                return PlayState.CONTINUE;
            }

            double speed = getDeltaMovement().horizontalDistanceSqr();
            if (speed > 0.01D) {
                state.setAndContinue(SWIM_FAST);
            } else if (speed > 0.0004D) {
                state.setAndContinue(SWIM);
            } else {
                state.setAndContinue(IDLE);
            }

            return PlayState.CONTINUE;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animationCache;
    }
}
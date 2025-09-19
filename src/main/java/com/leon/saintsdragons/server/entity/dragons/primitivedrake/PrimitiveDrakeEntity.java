package com.leon.saintsdragons.server.entity.dragons.primitivedrake;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.dragons.primitivedrake.handler.PrimitiveDrakeAnimationHandler;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.common.registry.ModEntities;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Primitive Drake - A simple ground drake that wanders around and runs away from players.
 * No complex abilities, just basic AI and cute behavior.
 */
public class PrimitiveDrakeEntity extends DragonEntity {
    
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final PrimitiveDrakeAnimationHandler animationController = new PrimitiveDrakeAnimationHandler(this);
    
    public PrimitiveDrakeEntity(EntityType<? extends PrimitiveDrakeEntity> entityType, Level level) {
        super(entityType, level);
        // Initialize animation state
        animationController.initializeAnimation();
    }
    
    @Override
    protected void registerGoals() {
        // Basic AI goals - simple and cute!
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new com.leon.saintsdragons.server.ai.goals.primitivedrake.PrimitiveDrakeGroundWanderGoal(this, 0.35D, 120));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 16.0D);
    }
    
    // Ground drake, no flying!
    public boolean isFlying() {
        return false;
    }

    
    public boolean isTameable() {
        return true; // Can be tamed like other dragons
    }
    
    @Override
    public boolean isFood(net.minecraft.world.item.ItemStack stack) {
        // Simple food - maybe raw meat or fish?
        return stack.is(net.minecraft.world.item.Items.BEEF) || 
               stack.is(net.minecraft.world.item.Items.PORKCHOP) ||
               stack.is(net.minecraft.world.item.Items.CHICKEN) ||
               stack.is(net.minecraft.world.item.Items.MUTTON) ||
               stack.is(net.minecraft.world.item.Items.COD) ||
               stack.is(net.minecraft.world.item.Items.SALMON);
    }
    
    @Override
    public Vec3 getHeadPosition() {
        // Use eye position - more reliable than bone positions
        return this.getEyePosition();
    }
    
    @Override
    public Vec3 getMouthPosition() {
        // Simple mouth position - just the head area
        return this.position().add(0, this.getBbHeight() * 0.8, 0);
    }
    
    @Override
    public DragonAbilityType<?, ?> getPrimaryAttackAbility() {
        // Simple drake has no abilities - just runs away!
        return null;
    }
    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Use the new smooth animation controller
        controllers.add(new AnimationController<>(this, "movement", 1, animationController::handleMovementAnimation));
    }
    
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
    
    @Override
    public AgeableMob getBreedOffspring(@NotNull ServerLevel level, @NotNull AgeableMob other) {
        // Simple breeding - just spawn a new Primitive Drake
        return ModEntities.PRIMITIVE_DRAKE.get().create(level);
    }
    
    public static boolean canSpawnHere(EntityType<? extends PrimitiveDrakeEntity> entityType, LevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        // Use vanilla animal spawn rules for better compatibility
        return net.minecraft.world.entity.animal.Animal.checkAnimalSpawnRules(entityType, level, spawnType, pos, random);
    }
}
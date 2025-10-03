//FOR FUTURE USE CASEs LIKE NEW DRAGONS??? more zap van dinks or some fire dragon named Lava Tickler, idk

package com.leon.saintsdragons.server.entity.base;

import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.handler.DragonCombatHandler;
import com.leon.saintsdragons.server.entity.handler.DragonAllyManager;
import com.leon.saintsdragons.common.network.DragonAnimTickets;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;

/**
 * Base class for all dragon entities in the Lightning Dragon mod.
 * Provides common GeckoLib integration, ability management, and basic dragon functionality.
 */
public abstract class DragonEntity extends TamableAnimal implements GeoEntity {
    // Shared command synced data for all dragons
    protected static final EntityDataAccessor<Integer> DATA_COMMAND =
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.INT);
    
    // Shared sit progress data for all dragons
    public static final EntityDataAccessor<Float> DATA_SIT_PROGRESS =
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.FLOAT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    
    // Dragon ability system (lightweight base – no global cooldown here)
    private DragonAbility<?> activeAbility = null;
    
    // Combat manager for handling abilities and cooldowns
    public final DragonCombatHandler combatManager;
    
    // Ally manager for handling dragon allies
    public final DragonAllyManager allyManager;
    
    // Sit progress fields
    public float sitProgress = 0f;
    public float prevSitProgress = 0f;
    
    protected DragonEntity(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
        this.combatManager = new DragonCombatHandler(this);
        this.allyManager = new DragonAllyManager(this);
    }

    protected void setRideable(boolean rideable) {
        this.isRideable = rideable;
    }

    public boolean isRideableDragon() {
        return this.isRideable;
    }

    private boolean isRideable = false;

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_COMMAND, 0); // 0=Follow, 1=Sit, 2=Wander (default Follow)
        this.entityData.define(DATA_SIT_PROGRESS, 0.0f); // Sit progress for smooth animations
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public abstract void registerControllers(AnimatableManager.ControllerRegistrar controllers);

    public void syncAnimState(int groundState, int flightMode) {
        if (level().isClientSide) {
            return;
        }
        this.setAnimData(DragonAnimTickets.GROUND_STATE, groundState);
        this.setAnimData(DragonAnimTickets.FLIGHT_MODE, flightMode);
    }

    // ===== DRAGON ABILITY SYSTEM =====
    /**
     * Get the currently active Dragon ability, if any
     */
    @SuppressWarnings("unchecked")
    public <T extends DragonEntity> DragonAbility<T> getActiveAbility() {
        return (DragonAbility<T>) activeAbility;
    }

    /**
     * Set the active Dragon ability
     */
    public void setActiveAbility(DragonAbility<?> ability) {
        this.activeAbility = ability;
    }

    /**
     * Check if dragon can use abilities (not on cooldown, not already using one)
     */
    public boolean canUseAbility() {
        return combatManager.canUseAbility();
    }

    /**
     * Try to activate a Dragon ability
     */
    public <T extends DragonEntity> void tryActivateAbility(DragonAbilityType<T, ?> abilityType) {
        if (abilityType == null || level().isClientSide) {
            return;
        }
        combatManager.tryUseAbility(abilityType);
    }

    protected DragonAbilityType<?, ?> getHurtAbilityType() {
        return null;
    }

    protected void onSuccessfulDamage(DamageSource source, float amount) {
        if (level().isClientSide || isDying()) {
            return;
        }
        DragonAbilityType<?, ?> hurtAbility = getHurtAbilityType();
        if (hurtAbility != null) {
            combatManager.tryUseAbility(hurtAbility);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);
        if (result) {
            onSuccessfulDamage(source, amount);
        }
        return result;
    }

    /**
     * Get the primary attack ability for this dragon type.
     * Override in subclasses to define dragon-specific attack abilities.
     */
    public abstract DragonAbilityType<?, ?> getPrimaryAttackAbility();

    /**
     * Get the roar ability for this dragon type (if any)
     */
    public DragonAbilityType<?, ?> getRoarAbility() {
        return null; // Default: no roar ability
    }

    /**
     * Get the summon storm ability for this dragon type (if any)
     */
    public DragonAbilityType<?, ?> getSummonStormAbility() {
        return null; // Default: no summon storm ability
    }

    // ===== DRAGON STATE METHODS =====
    // These methods should be implemented by dragon subclasses
    // Default implementations return false/null for basic functionality

    /**
     * Check if the dragon is currently dying
     */
    public boolean isDying() {
        return false;
    }

    /**
     * Check if the dragon is in a muted state (sitting/staying)
     */
    public boolean isStayOrSitMuted() {
        return false;
    }

    /**
     * Check if the dragon is transitioning between sleep states
     */
    public boolean isSleepTransitioning() {
        return false;
    }

    /**
     * Check if the dragon is flying
     */
    public boolean isFlying() {
        return false;
    }

    /**
     * Check if the dragon is running
     */
    public boolean isRunning() {
        return false;
    }

    /**
     * Check if the dragon is walking
     */
    public boolean isWalking() {
        return false;
    }

    /**
     * Check if the dragon is actually running (not just flagged as running)
     */
    public boolean isActuallyRunning() {
        return false;
    }

    /**
     * Get cached horizontal speed for performance
     */
    public double getCachedHorizontalSpeed() {
        return 0.0;
    }

    /**
     * Check if rider controls are locked
     */
    public boolean areRiderControlsLocked() {
        return false;
    }

    /**
     * Get client-side locator position for sound effects
     */
    public Vec3 getClientLocatorPosition(String locator) {
        return null;
    }

    /**
     * Get the maximum sit progress ticks for smooth sitting animation
     */
    public float maxSitTicks() {
        return 15.0F; // Default: 15 ticks to fully sit (about 0.75 seconds)
    }

    /**
     * Get the current sit progress
     */
    public float getSitProgress() {
        return this.entityData.get(DATA_SIT_PROGRESS);
    }

    /**
     * Check if the dragon is going up (for riding controls)
     */
    public boolean isGoingUp() {
        return false; // Default implementation - override in subclasses
    }

    /**
     * Set if the dragon is going up (for riding controls)
     */
    public void setGoingUp(boolean goingUp) {
        // Default implementation - override in subclasses
    }

    /**
     * Check if the dragon is going down (for riding controls)
     */
    public boolean isGoingDown() {
        return false; // Default implementation - override in subclasses
    }

    /**
     * Set if the dragon is going down (for riding controls)
     */
    public void setGoingDown(boolean goingDown) {
        // Default implementation - override in subclasses
    }

    /**
     * Get the riding player if any
     */
    public net.minecraft.world.entity.player.Player getRidingPlayer() {
        if (this.getControllingPassenger() instanceof net.minecraft.world.entity.player.Player player) {
            return player;
        }
        return null;
    }
    /**
     * Tick Dragon ability system
     */
    protected void tickAbilities() {
        if (!level().isClientSide) {
            combatManager.tick();
        }
    }

    @Override
    public void tick() {
        super.tick();
        tickAbilities();
    }

    // ===== COMMAND SYSTEM (shared) =====

    /**
     * Current owner command: 0=Follow, 1=Sit, 2=Wander (extend as needed)
     */
    public int getCommand() {
        return this.entityData.get(DATA_COMMAND);
    }

    /**
     * Sets owner command and applies base behaviors (e.g., sit toggle).
     */
    public void setCommand(int command) {
        this.entityData.set(DATA_COMMAND, command);
        // Only sit via command if tamed; untamed dragons ignore owner commands
        if (this.isTame()) {
            this.setOrderedToSit(command == 1);
        }
    }

    /**
     * Base owner-command gesture: crouching owner with empty hand by default.
     * Subclasses can override to change how commands are issued.
     */
    public boolean canOwnerCommand(Player player) {
        return player != null && player.isCrouching();
    }

    /**
     * Base mounting permission; subclasses may override with stricter logic.
     */
    public boolean canOwnerMount(Player player) {
        return !this.isBaby();
    }
    
    /**
     * Check if an entity is an ally of this dragon.
     * Includes owner, other dragons owned by the same player, and manually set allies.
     */
    public boolean isAlly(net.minecraft.world.entity.Entity entity) {
        if (entity == null) return false;
        
        // Owner is always an ally
        if (entity instanceof Player player && this.isTame() && this.isOwnedBy(player)) {
            return true;
        }
        
        // Other dragons owned by the same player or allied players are allies
        if (entity instanceof DragonEntity otherDragon && this.isTame() && otherDragon.isTame()) {
            LivingEntity owner = this.getOwner();
            if (owner instanceof Player ownerPlayer) {
                if (otherDragon.isOwnedBy(ownerPlayer)) {
                    return true;
                }

                LivingEntity otherOwner = otherDragon.getOwner();
                if (otherOwner instanceof Player otherPlayer) {
                    return allyManager.isAlly(otherPlayer);
                }
            }
            return false;
        }

        
        // Check manually set allies
        if (entity instanceof Player player) {
            return allyManager.isAlly(player);
        }
        
        // Check if entity is owned by an ally (including the dragon's owner)
        if (entity instanceof net.minecraft.world.entity.TamableAnimal tamable && tamable.isTame()) {
            net.minecraft.world.entity.LivingEntity owner = tamable.getOwner();
            if (owner instanceof Player playerOwner) {
                // Check if the pet's owner is the dragon's owner OR a manually set ally
                return (this.isTame() && this.isOwnedBy(playerOwner)) || allyManager.isAlly(playerOwner);
            }
        }
        
        if (entity instanceof OwnableEntity ownable) {
            LivingEntity owner = ownable.getOwner();
            if (owner instanceof Player playerOwner) {
                return (this.isTame() && this.isOwnedBy(playerOwner)) || allyManager.isAlly(playerOwner);
            }
        }
        return false;
    }
    
    /**
     * Check if an entity should be considered a valid target for this dragon.
     * This prevents targeting allies even in retaliation scenarios.
     */
    public boolean canTarget(net.minecraft.world.entity.Entity entity) {
        if (entity == null) return false;
        
        // Never target allies
        if (isAlly(entity)) {
            return false;
        }
        
        // Additional check: Never target tamed animals owned by the same player
        // This prevents targeting pets even if they're not explicitly marked as allies
        if (entity instanceof net.minecraft.world.entity.TamableAnimal tamable && tamable.isTame()) {
            net.minecraft.world.entity.LivingEntity owner = tamable.getOwner();
            if (owner instanceof Player playerOwner && this.isTame() && this.isOwnedBy(playerOwner)) {
                return false; // Never target pets owned by the same player
            }
        }
        
        if (entity instanceof OwnableEntity ownable) {
            LivingEntity owner = ownable.getOwner();
            if (owner instanceof Player playerOwner && this.isTame() && this.isOwnedBy(playerOwner)) {
                return false; // Never target entities owned by the same player
            }
        }
        
        return true;
    }

    /**
     * Default interaction handling for command cycling (Shift + empty hand).
     * Subclasses overriding mobInteract should call super to retain this behavior.
     */
    @Override
    public @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        if (this.isTame() && this.isOwnedBy(player)) {
            if (canOwnerCommand(player) && hand == InteractionHand.MAIN_HAND && player.getItemInHand(hand).isEmpty()) {
                int next = (getCommand() + 1) % 3; // 0,1,2 wrap
                setCommand(next);
                return InteractionResult.SUCCESS;
            }
        }
        return super.mobInteract(player, hand);
    }

    /**
     * Persist base command state to NBT.
     */
    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Command", getCommand());
        allyManager.saveToNBT(tag);
    }

    /**
     * Load base command state from NBT.
     */
    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Command")) {
            setCommand(tag.getInt("Command"));
        }
        allyManager.loadFromNBT(tag);
    }

    /**
     * Halt movement when ordered to sit and not ridden. Subclasses can call super and then apply their own logic.
     */
    @Override
    public void travel(@NotNull Vec3 travelVector) {
        if (this.isOrderedToSit() && !this.isVehicle() && !this.isPassenger()) {
            this.setDeltaMovement(Vec3.ZERO);
            super.travel(Vec3.ZERO);
            return;
        }
        super.travel(travelVector);
    }

    // ===== ABSTRACT METHODS =====
    /**
     * Get head position for targeting and ability positioning.
     * Override this in subclasses to provide accurate head positioning.
     */
    public abstract Vec3 getHeadPosition();

    /**
     * Get mouth position for beam/breath attacks.
     * Override this in subclasses to provide accurate mouth positioning.
     */
    public abstract Vec3 getMouthPosition();
}


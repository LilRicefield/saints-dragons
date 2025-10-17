//FOR FUTURE USE CASEs LIKE NEW DRAGONS??? more zap van dinks or some fire wyvern named Lava Tickler, idk

package com.leon.saintsdragons.server.entity.base;

import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.handler.DragonCombatHandler;
import com.leon.saintsdragons.server.entity.interfaces.DragonSoundProfile;
import com.leon.saintsdragons.server.entity.handler.DragonAllyManager;
import com.leon.saintsdragons.common.network.DragonAnimTickets;
import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Base class for all wyvern entities in the Lightning Dragon mod.
 * Provides common GeckoLib integration, ability management, and basic wyvern functionality.
 */
public abstract class DragonEntity extends TamableAnimal implements GeoEntity {
    // Shared command synced data for all dragons
    protected static final EntityDataAccessor<Integer> DATA_COMMAND =
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.INT);
    
    // Shared sit progress data for all dragons
    public static final EntityDataAccessor<Float> DATA_SIT_PROGRESS =
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.FLOAT);

    // Shared gender flag for all dragons (0=male,1=female)
    private static final EntityDataAccessor<Byte> DATA_GENDER =
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.BYTE);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    
    // Dragon ability system (lightweight base – no global cooldown here)
    private DragonAbility<?> activeAbility = null;
    
    // Combat manager for handling abilities and cooldowns
    public final DragonCombatHandler combatManager;
    
    // Ally manager for handling wyvern allies
    public final DragonAllyManager allyManager;
    
    // Sit progress fields
    public float sitProgress = 0f;
    public float prevSitProgress = 0f;
    
    // Death sequence management
    private boolean dying = false;
    private boolean genderInitialized = false;

    protected DragonEntity(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
        this.combatManager = new DragonCombatHandler(this);
        this.allyManager = new DragonAllyManager(this);
    }

    protected void setRideable() {
        this.isRideable = true;
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
        this.entityData.define(DATA_GENDER, DragonGender.MALE.getId());
    }

    public DragonGender getGender() {
        return DragonGender.fromId(this.entityData.get(DATA_GENDER));
    }

    public void setGender(@Nullable DragonGender gender) {
        DragonGender resolved = gender == null ? DragonGender.MALE : gender;
        this.entityData.set(DATA_GENDER, resolved.getId());
        this.genderInitialized = true;
    }

    public boolean isFemale() {
        return getGender() == DragonGender.FEMALE;
    }

    public void setFemale(boolean female) {
        setGender(female ? DragonGender.FEMALE : DragonGender.MALE);
    }

    public boolean hasGender() {
        return this.genderInitialized;
    }

    protected void ensureGenderInitialized() {
        Level level = level();
        if (level != null && level.isClientSide) {
            return;
        }
        if (!this.genderInitialized) {
            setGender(this.random.nextBoolean() ? DragonGender.FEMALE : DragonGender.MALE);
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public abstract void registerControllers(AnimatableManager.ControllerRegistrar controllers);

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        if (!level().isClientSide) {
            ensureGenderInitialized();
        }
    }

    public void syncAnimState(int groundState, int flightMode) {
        if (level().isClientSide) {
            return;
        }
        this.setAnimData(DragonAnimTickets.GROUND_STATE, groundState);
        this.setAnimData(DragonAnimTickets.FLIGHT_MODE, flightMode);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor levelAccessor, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData spawnData, @Nullable CompoundTag spawnTag) {
        SpawnGroupData data = super.finalizeSpawn(levelAccessor, difficulty, reason, spawnData, spawnTag);
        ensureGenderInitialized();
        return data;
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
     * Check if wyvern can use abilities (not on cooldown, not already using one)
     */
    public boolean canUseAbility() {
        return combatManager.canUseAbility();
    }

    public boolean areAbilitiesLocked() {
        return combatManager.isGlobalCooldownActive();
    }

    public void lockAbilities(int ticks) {
        combatManager.lockGlobalCooldown(ticks);
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

    public Map<String, VocalEntry> getVocalEntries() {
        return Collections.emptyMap();
    }

    /**
     * Provides dragon-specific sound configuration. Subclasses should override as needed.
     */
    public DragonSoundProfile getSoundProfile() {
        return DragonSoundProfile.EMPTY;
    }

    public record VocalEntry(String controllerId, String animationId, Supplier<SoundEvent> soundSupplier,
                                  float volume, float basePitch, float pitchVariance, boolean allowWhenSitting,
                                  boolean allowDuringSleep, boolean preventOverlap) {
    }


    /**
     * Builder for creating VocalEntry maps with less boilerplate.
     * Use this to define wyvern vocals instead of manual Map.ofEntries bookkeeping.
     */
    public static final class VocalEntryBuilder {
        private final Map<String, VocalEntry> entries = new java.util.HashMap<>();

        /**
         * Add a vocal entry with full control over all parameters.
         */
        public VocalEntryBuilder add(String key, String controller, String animation,
                                     Supplier<SoundEvent> sound, float volume,
                                     float basePitch, float variance,
                                     boolean allowWhenSitting, boolean allowDuringSleep,
                                     boolean preventOverlap) {
            entries.put(key, new VocalEntry(controller, animation, sound,
                    volume, basePitch, variance, allowWhenSitting,
                    allowDuringSleep, preventOverlap));
            return this;
        }

        /**
         * Convenience overload for common case: no variance, trigger ok everywhere.
         */
        public VocalEntryBuilder add(String key, String controller, String animation,
                                     Supplier<SoundEvent> sound) {
            return add(key, controller, animation, sound,
                    1.0f, 1.0f, 0.0f, false, false, false);
        }

        /**
         * Build the immutable map of vocal entries.
         */
        public Map<String, VocalEntry> build() {
            return Map.copyOf(entries);
        }
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
    public boolean hurt(@NotNull DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);
        if (result) {
            onSuccessfulDamage(source, amount);
        }
        return result;
    }

    /**
     * Get the primary attack ability for this wyvern type.
     * Override in subclasses to define wyvern-specific attack abilities.
     */
    public abstract DragonAbilityType<?, ?> getPrimaryAttackAbility();

    /**
     * Get the roar ability for this wyvern type (if any)
     */
    public DragonAbilityType<?, ?> getRoaringAbility() {
        return null; // Default: no roar ability
    }

    /**
     * Get the summon storm ability for this wyvern type (if any)
     */
    public DragonAbilityType<?, ?> getChannelingAbility() {
        return null;
    }

    // ===== DRAGON STATE METHODS =====
    // These methods should be implemented by wyvern subclasses
    // Default implementations return false/null for basic functionality

    /**
     * Check if the wyvern is currently dying
     */
    public boolean isDying() {
        return dying;
    }

    /**
     * Hook invoked when the shared death ability starts playing.
     * Subclasses can override to update custom state.
     */
    public void onDeathAbilityStarted() {
        // Default no-op
    }

    /**
     * Duration (in ticks) that the shared death ability should run before the kill pulse.
     * Subclasses override to match their custom death animation length.
     */
    public int getDeathAnimationDurationTicks() {
        return 62;
    }


    // ===== DEATH SEQUENCE HELPERS =====

    /**
     * Begin the standard death sequence for this wyvern.
     * Sets the wyvern invulnerable, marks it as dying, and starts the death ability.
     * 
     * @param deathAbility The death ability type to activate
     */
    protected final void beginStandardDeathSequence(DragonAbilityType<?, ?> deathAbility) {
        if (level().isClientSide || dying) {
            return;
        }

        setInvulnerable(true);
        dying = true;

        if (!canUseAbility()) {
            combatManager.forceEndActiveAbility();
        }

        tryActivateAbilityUnchecked(deathAbility);
    }

    /**
     * Complete the standard death sequence.
     * Resets the dying flag and invulnerability.
     * Call this when the death ability finishes if you need cleanup.
     */
    protected final void completeStandardDeathSequence() {
        dying = false;
        setInvulnerable(false);
    }

    /**
     * Helper to activate an ability without type-checking (used internally for death abilities).
     */
    @SuppressWarnings("unchecked")
    private void tryActivateAbilityUnchecked(DragonAbilityType<?, ?> type) {
        combatManager.tryUseAbility((DragonAbilityType<DragonEntity, ?>) type);
    }

    /**
     * Handle lethal damage by starting the death sequence.
     * Call this from your wyvern's hurt() override to intercept lethal damage.
     * 
     * @param source The damage source
     * @param amount The damage amount
     * @param deathAbility The death ability to play
     * @return true if lethal damage was intercepted and death sequence started
     */
    protected final boolean handleLethalDamage(DamageSource source, float amount,
                                               DragonAbilityType<?, ?> deathAbility) {
        if (level().isClientSide || dying) {
            return false;
        }

        if (getHealth() - amount > 0.0f) {
            return false;
        }

        beginStandardDeathSequence(deathAbility);
        // If the death ability actually started, swallow the damage event.
        boolean abilityStarted = combatManager.isAbilityActive(deathAbility);
        if (!abilityStarted) {
            // Ability failed to start - unwind the invulnerable and dying flags
            completeStandardDeathSequence();
            return false;
        }
        return true;
    }

    /**
     * Register a bite sound key for animation controllers.
     * Convenience method to avoid repeating the same pattern in every wyvern.
     * 
     * @param controller The animation controller to register the sound key with
     */
    protected final void registerBiteSoundKey(software.bernie.geckolib.core.animation.AnimationController<?> controller, String speciesId) {
        controller.triggerableAnim("bite", software.bernie.geckolib.core.animation.RawAnimation.begin().thenPlay("animation." + speciesId + ".bite"));
    }


    /**
     * Check if the wyvern is in a muted state (sitting/staying)
     */
    public boolean isStayOrSitMuted() {
        return false;
    }

    /**
     * Check if the wyvern is transitioning between sleep states
     */
    public boolean isSleepTransitioning() {
        return false;
    }

    /**
     * Check if the wyvern is flying
     */
    public boolean isFlying() {
        return false;
    }

    /**
     * Check if the wyvern is running
     */
    public boolean isRunning() {
        return false;
    }

    /**
     * Check if the wyvern is walking
     */
    public boolean isWalking() {
        return false;
    }

    /**
     * Check if the wyvern is actually running (not just flagged as running)
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
     * Check if the wyvern is going up (for riding controls)
     */
    public boolean isGoingUp() {
        return false; // Default implementation - override in subclasses
    }

    /**
     * Set if the wyvern is going up (for riding controls)
     */
    public void setGoingUp(boolean goingUp) {
        // Default implementation - override in subclasses
    }

    /**
     * Check if the wyvern is going down (for riding controls)
     */
    public boolean isGoingDown() {
        return false; // Default implementation - override in subclasses
    }

    /**
     * Set if the wyvern is going down (for riding controls)
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
     * Check if an entity is an ally of this wyvern.
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
        
        // Check if entity is owned by an ally (including the wyvern's owner)
        if (entity instanceof net.minecraft.world.entity.TamableAnimal tamable && tamable.isTame()) {
            net.minecraft.world.entity.LivingEntity owner = tamable.getOwner();
            if (owner instanceof Player playerOwner) {
                // Check if the pet's owner is the wyvern's owner OR a manually set ally
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
     * Check if an entity should be considered a valid target for this wyvern.
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
        tag.putByte("Gender", getGender().getId());
        tag.putBoolean("IsFemale", isFemale());
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
        if (tag.contains("Gender", Tag.TAG_BYTE)) {
            setGender(DragonGender.fromId(tag.getByte("Gender")));
        } else if (tag.contains("IsFemale")) {
            setFemale(tag.getBoolean("IsFemale"));
        } else {
            this.genderInitialized = false;
            ensureGenderInitialized();
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


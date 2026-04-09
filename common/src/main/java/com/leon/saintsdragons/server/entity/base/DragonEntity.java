//FOR FUTURE USE CASEs LIKE NEW DRAGONS??? more zap van dinks or some fire wyvern named Lava Tickler, idk

package com.leon.saintsdragons.server.entity.base;

import com.leon.saintsdragons.common.registry.DragonType;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.component.DragonAnimationSyncComponent;
import com.leon.saintsdragons.server.entity.component.DragonAiCombatPacingComponent;
import com.leon.saintsdragons.server.entity.component.DragonBabyComponent;
import com.leon.saintsdragons.server.entity.component.DragonCommandComponent;
import com.leon.saintsdragons.server.entity.component.DragonGenderComponent;
import com.leon.saintsdragons.server.entity.component.DragonGroomingComponent;
import com.leon.saintsdragons.server.entity.component.DragonHappinessComponent;
import com.leon.saintsdragons.server.entity.component.DragonHungerComponent;
import com.leon.saintsdragons.server.entity.component.DragonRecoveryComponent;
import com.leon.saintsdragons.server.entity.component.DragonSitComponent;
import com.leon.saintsdragons.server.entity.component.DragonSleepComponent;
import com.leon.saintsdragons.server.entity.controller.BodyControl;
import com.leon.saintsdragons.server.entity.handler.DragonCombatHandler;
import com.leon.saintsdragons.server.entity.interfaces.DragonSoundProfile;
import com.leon.saintsdragons.server.entity.handler.DragonAllyManager;
import com.leon.saintsdragons.common.network.DragonAnimTickets;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animation.AnimatableManager;
import com.leon.saintsdragons.server.data.DragonCodexSavedData;
import java.util.List;
import java.util.UUID;

public abstract class DragonEntity extends TamableAnimal implements GeoEntity {
    protected static final EntityDataAccessor<Integer> DATA_COMMAND =
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Float> DATA_SIT_PROGRESS =
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Byte> DATA_GENDER =
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> DATA_HAPPINESS =
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_SLEEPING =
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SLEEPING_ENTERING =
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SLEEPING_EXITING =
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_BODY_DEVIATION =
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_PITCH_DEVIATION =
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_YAW_VELOCITY =
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_TEXTURE_VARIANT =
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_PENDING_ADULT_TEXTURE_VARIANT =
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.INT);
    public static final int HUNGER_MAX = DragonHungerComponent.HUNGER_MAX;
    public static final int HAPPINESS_MAX = DragonHappinessComponent.HAPPINESS_MAX;
    private DragonAbility<?> activeAbility = null;
    public final DragonCombatHandler combatManager;
    public final DragonAllyManager allyManager;
    @Nullable
    private final DragonAnimationSyncComponent animationSyncComponent;
    @Nullable
    private final DragonCommandComponent commandComponent;
    @Nullable
    private final DragonGenderComponent genderComponent;
    @Nullable
    private final DragonHungerComponent hungerComponent;
    @Nullable
    private final DragonHappinessComponent happinessComponent;
    @Nullable
    private final DragonGroomingComponent groomingComponent;
    @Nullable
    private final DragonSitComponent sitComponent;
    @Nullable
    private final DragonSleepComponent sleepComponent;
    @Nullable
    private final DragonRecoveryComponent recoveryComponent;
    @Nullable
    private final DragonBabyComponent babyComponent;
    private final DragonAiCombatPacingComponent aiCombatPacing = new DragonAiCombatPacingComponent();

    private final com.leon.saintsdragons.util.math.SmoothValue fallbackBodyRotDeviation =
            com.leon.saintsdragons.util.math.SmoothValue.rotation(0.0);
    private final com.leon.saintsdragons.util.math.SmoothValue fallbackPitchDeviation =
            com.leon.saintsdragons.util.math.SmoothValue.rotation(0.0);
    private final com.leon.saintsdragons.util.math.SmoothValue fallbackYawVelocity =
            com.leon.saintsdragons.util.math.SmoothValue.value(0.0);
    @Nullable
    private DragonType cachedDragonType;
    private boolean dying = false;
    @Nullable
    private LivingEntity lastDamager;
    private int lastDamagerTimestamp;
    private DamageSource killDataCause;
    private int killDataRecentlyHit;
    private Player killDataAttackingPlayer;
    private boolean isRespawning = false;
    public int skipRespawnTicks = 0;
    private BodyControl dragonBodyControl;
    private boolean boundInBinder = false;
    @Nullable
    private UUID assignedParentUuid;

    protected DragonEntity(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
        this.combatManager = new DragonCombatHandler(this);
        this.allyManager = new DragonAllyManager(this);
        this.commandComponent = createCommandComponent();
        this.genderComponent = createGenderComponent();
        this.hungerComponent = createHungerComponent();
        this.happinessComponent = createHappinessComponent();
        this.groomingComponent = createGroomingComponent();
        this.sleepComponent = createSleepComponent();
        this.recoveryComponent = createRecoveryComponent();
        this.animationSyncComponent = createAnimationSyncComponent();
        this.sitComponent = createSitComponent();
        this.babyComponent = createBabyComponent();
        this.lookControl = new com.leon.saintsdragons.server.entity.controller.DragonLookControl<>(this);
    }

    @Nullable
    protected DragonCommandComponent createCommandComponent() {
        return new DragonCommandComponent(this, DATA_COMMAND);
    }

    @Nullable
    protected DragonGenderComponent createGenderComponent() {
        return new DragonGenderComponent(this, DATA_GENDER);
    }

    @Nullable
    protected DragonHungerComponent createHungerComponent() {
        return new DragonHungerComponent(this);
    }

    @Nullable
    protected DragonHappinessComponent createHappinessComponent() {
        return new DragonHappinessComponent(this, DATA_HAPPINESS);
    }

    @Nullable
    protected DragonGroomingComponent createGroomingComponent() {
        return new DragonGroomingComponent(this);
    }

    @Nullable
    protected DragonSleepComponent createSleepComponent() {
        return new DragonSleepComponent(this, DATA_SLEEPING, DATA_SLEEPING_ENTERING, DATA_SLEEPING_EXITING);
    }

    @Nullable
    protected DragonRecoveryComponent createRecoveryComponent() {
        return new DragonRecoveryComponent(this);
    }

    @Nullable
    protected DragonAnimationSyncComponent createAnimationSyncComponent() {
        return new DragonAnimationSyncComponent(this, DATA_BODY_DEVIATION, DATA_PITCH_DEVIATION, DATA_YAW_VELOCITY);
    }

    @Nullable
    protected DragonSitComponent createSitComponent() {
        return new DragonSitComponent(this, DATA_SIT_PROGRESS);
    }

    @Nullable
    protected DragonBabyComponent createBabyComponent() {
        return new DragonBabyComponent(this);
    }

    @Nullable
    public DragonBabyComponent getBabyComponent() {
        return babyComponent;
    }

    public DragonAiCombatPacingComponent getAiCombatPacing() {
        return aiCombatPacing;
    }

    @Override
    protected net.minecraft.world.entity.ai.control.@NotNull BodyRotationControl createBodyControl() {
        this.dragonBodyControl = new BodyControl(this, getBodyTurnSpeed());
        return this.dragonBodyControl;
    }

    protected float getBodyTurnSpeed() {
        return 0.6f; // Default for most dragons
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
        this.entityData.define(DATA_HAPPINESS, HAPPINESS_MAX);
        this.entityData.define(DATA_SLEEPING, false);
        this.entityData.define(DATA_SLEEPING_ENTERING, false);
        this.entityData.define(DATA_SLEEPING_EXITING, false);
        this.entityData.define(DATA_BODY_DEVIATION, 0.0f);
        this.entityData.define(DATA_PITCH_DEVIATION, 0.0f);
        this.entityData.define(DATA_YAW_VELOCITY, 0.0f);
        this.entityData.define(DATA_TEXTURE_VARIANT, 0);
        this.entityData.define(DATA_PENDING_ADULT_TEXTURE_VARIANT, -1);
    }

    public float smoothTailDragVelocity(float targetDegrees) {
        if (animationSyncComponent == null) {
            return targetDegrees;
        }
        return animationSyncComponent.smoothTailDragVelocity(targetDegrees);
    }


    public void resetTailDragVelocity() {
        if (animationSyncComponent != null) {
            animationSyncComponent.resetTailDragVelocity();
        }
    }

    public com.leon.saintsdragons.util.math.SmoothValue getBodyRotDeviation() {
        if (animationSyncComponent == null) {
            return fallbackBodyRotDeviation;
        }
        return animationSyncComponent.getBodyRotDeviation();
    }

    public com.leon.saintsdragons.util.math.SmoothValue getPitchDeviation() {
        if (animationSyncComponent == null) {
            return fallbackPitchDeviation;
        }
        return animationSyncComponent.getPitchDeviation();
    }

    public com.leon.saintsdragons.util.math.SmoothValue getYawVelocity() {
        if (animationSyncComponent == null) {
            return fallbackYawVelocity;
        }
        return animationSyncComponent.getYawVelocity();
    }

    public DragonGender getGender() {
        if (genderComponent == null) {
            return DragonGender.MALE;
        }
        return genderComponent.getGender();
    }

    public void setGender(@Nullable DragonGender gender) {
        if (genderComponent != null) {
            genderComponent.setGender(gender);
        }
    }

    public boolean isFemale() {
        return genderComponent != null && genderComponent.isFemale();
    }

    public void setFemale(boolean female) {
        if (genderComponent != null) {
            genderComponent.setFemale(female);
        }
    }

    public int getHunger() {
        if (hungerComponent == null) {
            return HUNGER_MAX;
        }
        return hungerComponent.getHunger();
    }

    public int getMaxHunger() {
        if (hungerComponent == null) {
            return HUNGER_MAX;
        }
        return hungerComponent.getMaxHunger();
    }

    public boolean isHungry() {
        return hungerComponent != null && hungerComponent.isHungry();
    }

    public int getHappiness() {
        if (happinessComponent == null) {
            return HAPPINESS_MAX;
        }
        return happinessComponent.getHappiness();
    }

    public int getMaxHappiness() {
        if (happinessComponent == null) {
            return HAPPINESS_MAX;
        }
        return happinessComponent.getMaxHappiness();
    }

    public void setHappiness(int happiness) {
        if (happinessComponent != null) {
            happinessComponent.setHappiness(happiness);
        }
    }

    public void setHunger(int hunger) {
        if (hungerComponent != null) {
            hungerComponent.setHunger(hunger);
        }
    }

    public boolean applyFeedingHunger(boolean heartyMeal) {
        boolean wasHungry = hungerComponent != null && hungerComponent.isHungry();
        if (hungerComponent != null) {
            hungerComponent.applyFeeding(heartyMeal);
        }
        applyFeedingHappiness(heartyMeal);
        return wasHungry;
    }

    public void applyFeedingHappiness(boolean heartyMeal) {
        if (happinessComponent != null) {
            happinessComponent.applyFeeding(heartyMeal);
        }
    }

    public float getHappinessSpeedMultiplier() {
        if (happinessComponent == null) {
            return 1.0f;
        }
        return happinessComponent.getSpeedMultiplier();
    }

    public float getHungerMeleeDamageMultiplier() {
        if (hungerComponent == null) {
            return 1.0f;
        }
        return hungerComponent.getMeleeDamageMultiplier();
    }

    public boolean hasGender() {
        return genderComponent != null && genderComponent.hasGender();
    }

    @Nullable
    public UUID getAssignedParentUuid() {
        return assignedParentUuid;
    }

    public void setAssignedParentUuid(@Nullable UUID assignedParentUuid) {
        this.assignedParentUuid = assignedParentUuid;
    }

    public void clearAssignedParentUuid() {
        this.assignedParentUuid = null;
    }

    public <T extends DragonEntity> List<T> getNearbyAssignedBabies(Class<T> dragonClass) {
        return this.level().getEntitiesOfClass(
                dragonClass,
                this.getBoundingBox().inflate(16.0D),
                baby -> baby != null
                        && baby.isBaby()
                        && baby.isAlive()
                        && this.getUUID().equals(baby.getAssignedParentUuid())
        );
    }

    public <T extends DragonEntity> boolean hasNearbyAssignedBabies(Class<T> dragonClass) {
        return !getNearbyAssignedBabies(dragonClass).isEmpty();
    }

    protected void assignMotherToBaby(DragonEntity baby, @Nullable AgeableMob otherParent) {
        DragonEntity mother = this.isFemale()
                ? this
                : (otherParent instanceof DragonEntity otherDragon && otherDragon.isFemale() ? otherDragon : null);
        if (mother != null) {
            baby.setAssignedParentUuid(mother.getUUID());
        } else {
            baby.clearAssignedParentUuid();
        }
    }

    // ===== TEXTURE VARIANT SYSTEM =====

    public int getTextureVariant() {
        return this.entityData.get(DATA_TEXTURE_VARIANT);
    }

    public void setTextureVariant(int variant) {
        int clamped = Math.max(0, Math.min(getMaxTextureVariant(), variant));
        this.entityData.set(DATA_TEXTURE_VARIANT, clamped);
    }

    protected boolean shouldPersistAdultTextureVariantOnBabies() {
        return getMaxTextureVariant() > 0;
    }

    protected int chooseAdultTextureVariant() {
        return rollRandomTextureVariant();
    }

    public int getPendingAdultTextureVariant() {
        return this.entityData.get(DATA_PENDING_ADULT_TEXTURE_VARIANT);
    }

    public void setPendingAdultTextureVariant(int variant) {
        int clamped = Math.max(-1, Math.min(getMaxTextureVariant(), variant));
        this.entityData.set(DATA_PENDING_ADULT_TEXTURE_VARIANT, clamped);
    }

    protected void ensurePendingAdultTextureVariant() {
        if (!shouldPersistAdultTextureVariantOnBabies()) {
            return;
        }
        if (getPendingAdultTextureVariant() < 0) {
            setPendingAdultTextureVariant(chooseAdultTextureVariant());
        }
    }

    protected void applyPendingAdultTextureVariant() {
        if (!shouldPersistAdultTextureVariantOnBabies()) {
            return;
        }
        int pending = getPendingAdultTextureVariant();
        if (pending < 0) {
            pending = chooseAdultTextureVariant();
        }
        setTextureVariant(pending);
        setPendingAdultTextureVariant(-1);
    }

    public int getCodexTextureVariant() {
        if (this.isBaby() && shouldPersistAdultTextureVariantOnBabies()) {
            int pending = getPendingAdultTextureVariant();
            if (pending >= 0) {
                return pending;
            }
        }
        return getTextureVariant();
    }

    protected int getMaxTextureVariant() {
        return 0;
    }


    public Map<String, Integer> getTextureVariantNameMap() {
        return Map.of("default", 0);
    }

    /**
     * Resolves a variant id to its configured command/display name.
     */
    public String getTextureVariantName(int variantId) {
        int clamped = Math.max(0, Math.min(getMaxTextureVariant(), variantId));
        for (Map.Entry<String, Integer> entry : getTextureVariantNameMap().entrySet()) {
            if (entry.getValue() == clamped) {
                return entry.getKey();
            }
        }
        return "default";
    }

    public String getTextureVariantTranslationKey(int variantId) {
        return "saintsdragons.variant." + getTextureVariantName(variantId);
    }

    /**
     * Rolls a random valid texture variant index using this dragon's configured range.
     */
    protected int rollRandomTextureVariant() {
        int maxVariant = getMaxTextureVariant();
        if (maxVariant <= 0) {
            return 0;
        }
        return this.getRandom().nextInt(maxVariant + 1);
    }

    /**
     * Hook for spawn-time variant selection.
     * By default, picks a random variant from 0..maxVariant.
     */
    protected int chooseSpawnTextureVariant(@NotNull ServerLevelAccessor levelAccessor,
                                            @NotNull DifficultyInstance difficulty,
                                            @NotNull MobSpawnType reason,
                                            @Nullable SpawnGroupData spawnData,
                                            @Nullable CompoundTag spawnTag) {
        return chooseAdultTextureVariant();
    }

    public boolean tryBrush(Player player, net.minecraft.world.item.ItemStack brushStack) {
        return groomingComponent != null && player != null && brushStack != null
                && groomingComponent.tryBrush(player, brushStack);
    }

    public boolean isBoundInBinder() {
        return this.boundInBinder;
    }

    public void setBoundInBinder(boolean boundInBinder) {
        this.boundInBinder = boundInBinder;
    }

    protected void ensureGenderInitialized() {
        if (genderComponent != null) {
            genderComponent.ensureInitialized();
        }
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

    @Override
    public @NotNull SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor levelAccessor, @NotNull DifficultyInstance difficulty, MobSpawnType reason,
                                                 @Nullable SpawnGroupData spawnData, @Nullable CompoundTag spawnTag) {
        SpawnGroupData data = super.finalizeSpawn(levelAccessor, difficulty, reason, spawnData, spawnTag);
        ensureGenderInitialized();
        int chosenVariant = chooseSpawnTextureVariant(levelAccessor, difficulty, reason, spawnData, spawnTag);
        if (this.isBaby() && shouldPersistAdultTextureVariantOnBabies()) {
            setPendingAdultTextureVariant(chosenVariant);
            setTextureVariant(0);
        } else {
            setTextureVariant(chosenVariant);
        }

        // If baby spawned from spawn egg, reposition on ground to prevent falling from sky
        if (this.isBaby() && reason == MobSpawnType.SPAWN_EGG) {
            net.minecraft.core.BlockPos safePos = findSafeBabySpawnPos(levelAccessor, this.blockPosition());
            if (safePos != null && safePos.getY() < this.getY()) {
                this.moveTo(this.getX(), safePos.getY(), this.getZ(), this.getYRot(), this.getXRot());
            }
        }
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

    /**
     * Get the death ability type for this dragon.
     * Override this to specify which death ability to use (e.g., baby vs adult death animations).
     */
    protected DragonAbilityType<?, ?> getDeathAbilityType() {
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
    protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {

    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(double x, double y, double z) {
    }

    @Override
    public void knockback(double strength, double x, double z) {
    }

    @Override
    public boolean isInvulnerableTo(@NotNull DamageSource source) {
        DragonType dragonType = getDragonType();
        if (dragonType != null && dragonType.getElementalProfile().isImmuneTo(source)) {
            return true;
        }
        return super.isInvulnerableTo(source);
    }

    @Override
    public boolean fireImmune() {
        return super.fireImmune();
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (isDamageFromCurrentRider(source) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }

        // Apply elemental damage modifiers based on dragon type
        DragonType dragonType = getDragonType();
        if (dragonType != null) {
            float multiplier = dragonType.getElementalProfile().getDamageMultiplier(source);
            amount *= multiplier;

            // If immune (multiplier = 0), don't take damage
            if (multiplier == 0.0f) {
                return false;
            }
        }

        boolean result = super.hurt(source, amount);
        if (result) {
            LivingEntity attacker = null;
            if (source.getEntity() instanceof LivingEntity living) {
                attacker = living;
            } else if (source.getDirectEntity() instanceof LivingEntity living) {
                attacker = living;
            } else if (source.getDirectEntity() instanceof Projectile projectile
                    && projectile.getOwner() instanceof LivingEntity living) {
                attacker = living;
            }

            if (attacker != null) {
                this.setLastHurtByMob(attacker);
                this.lastDamager = attacker;
                this.lastDamagerTimestamp = this.tickCount;
            }
            onSuccessfulDamage(source, amount);
        }
        if (result && !level().isClientSide && this.isTame() && this.getOwnerUUID() != null) {
            net.minecraft.server.level.ServerLevel serverLevel = (net.minecraft.server.level.ServerLevel) level();
            DragonCodexSavedData.get(serverLevel).updateDragonStats(this.getOwnerUUID(), this);
            applyHappinessHitPenalty(serverLevel);
        }
        return result;
    }

    private boolean isDamageFromCurrentRider(@NotNull DamageSource source) {
        Entity directAttacker = source.getEntity();
        if (directAttacker instanceof Player player && player.getVehicle() == this) {
            return true;
        }
        if (directAttacker != null && directAttacker == this.getControllingPassenger()) {
            return true;
        }

        Entity projectileEntity = source.getDirectEntity();
        if (projectileEntity instanceof Projectile projectile) {
            Entity projectileOwner = projectile.getOwner();
            if (projectileOwner instanceof Player player && player.getVehicle() == this) {
                return true;
            }
            return projectileOwner != null && projectileOwner == this.getControllingPassenger();
        }

        return false;
    }

    @Nullable
    public LivingEntity getLastDamager() {
        return lastDamager;
    }

    public int getLastDamagerTimestamp() {
        return lastDamagerTimestamp;
    }

    @Override
    public void die(@NotNull DamageSource cause) {
        if (!this.dead) {
            killDataCause = cause;
            killDataRecentlyHit = this.lastHurtByPlayerTime;
            killDataAttackingPlayer = this.lastHurtByPlayer;

            dying = true;

            DragonAbilityType<?, ?> deathAbility = getDeathAbilityType();
            if (deathAbility != null && !level().isClientSide) {
                combatManager.forceUseAbility(deathAbility);
            }
        }
        if (!level().isClientSide && this.isTame() && this.getOwnerUUID() != null) {
            net.minecraft.server.level.ServerLevel serverLevel = (net.minecraft.server.level.ServerLevel) level();
            DragonCodexSavedData.get(serverLevel).removeDragon(this.getOwnerUUID(), this.getUUID());
        }
        super.die(cause);
    }

    @Override
    public void tame(@NotNull Player player) {
        super.tame(player);
        if (!level().isClientSide && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            DragonCodexSavedData.get(serverPlayer.serverLevel()).addDragon(serverPlayer, this);
        }
    }

    @Override
    public void setCustomName(@Nullable net.minecraft.network.chat.Component name) {
        super.setCustomName(name);
        if (!level().isClientSide && this.isTame() && this.getOwnerUUID() != null) {
            net.minecraft.server.level.ServerLevel serverLevel = (net.minecraft.server.level.ServerLevel) level();
            DragonCodexSavedData.get(serverLevel).updateDragonName(this.getOwnerUUID(), this.getUUID(), this.getName().getString());
            DragonCodexSavedData.get(serverLevel).updateDragonStats(this.getOwnerUUID(), this);
        }
    }

    @Override
    public void heal(float amount) {
        super.heal(amount);
        if (!level().isClientSide && this.isTame() && this.getOwnerUUID() != null) {
            net.minecraft.server.level.ServerLevel serverLevel = (net.minecraft.server.level.ServerLevel) level();
            DragonCodexSavedData.get(serverLevel).updateDragonStats(this.getOwnerUUID(), this);
        }
    }


    @Override
    protected void tickDeath() {
        ++this.deathTime;
        int deathDuration = getDeathAnimationDurationTicks();

        if (this.deathTime >= deathDuration && !this.level().isClientSide()) {

            this.lastHurtByPlayer = killDataAttackingPlayer;
            this.lastHurtByPlayerTime = killDataRecentlyHit;

            if (killDataCause != null) {
                this.dropAllDeathLoot(killDataCause);
            }
            this.level().broadcastEntityEvent(this, (byte)60);
            this.remove(Entity.RemovalReason.KILLED);
        }
    }

    @Override
    protected void dropAllDeathLoot(@NotNull DamageSource source) {
        if (deathTime < getDeathAnimationDurationTicks()) {
            return;
        }
        super.dropAllDeathLoot(source);
    }

    @Nullable
    public DragonType getDragonType() {
        if (cachedDragonType == null) {
            cachedDragonType = DragonType.fromEntity(this);
        }
        return cachedDragonType;
    }

    public abstract DragonAbilityType<?, ?> getPrimaryAttackAbility();
    public DragonAbilityType<?, ?> getRoaringAbility() {
        return null; // Default: no roar ability
    }
    public DragonAbilityType<?, ?> getChannelingAbility() {
        return null;
    }

    @Nullable
    public BlockState getEggBlockState() {
        return null;
    }

    public void configureEggBlockEntity(BlockEntity blockEntity, @Nullable DragonEntity partner) {
    }

    @Nullable
    protected java.util.UUID resolveEggOwnerUUID(@Nullable DragonEntity partner) {
        if (babyComponent != null) {
            return babyComponent.resolveEggOwnerUUID(partner);
        }
        if (this.isTame() && this.getOwnerUUID() != null) return this.getOwnerUUID();
        if (partner != null && partner.isTame() && partner.getOwnerUUID() != null) return partner.getOwnerUUID();
        return null;
    }

    protected void registerToOwnerCodex(@Nullable DragonEntity dragon, @Nullable net.minecraft.server.level.ServerLevel level) {
        if (babyComponent != null) {
            babyComponent.registerToOwnerCodex(dragon, level);
            return;
        }
        if (dragon == null || level == null || level.isClientSide) {
            return;
        }
        if (dragon.isTame() && dragon.getOwnerUUID() != null) {
            DragonCodexSavedData.get(level).addDragon(dragon.getOwnerUUID(), dragon);
        }
    }

    // ===== DRAGON STATE METHODS =====
    // These methods should be implemented by subclasses
    // Default implementations return false/null for basic functionality


    public boolean isDying() {
        return dying;
    }

    public void onDeathAbilityStarted() {

    }

    public int getDeathAnimationDurationTicks() {
        return 62;
    }

    public boolean isStayOrSitMuted() {
        return false;
    }

    public boolean supportsSleep() {
        return false;
    }

    public boolean isSleepTransitioning() {
        return sleepComponent != null && sleepComponent.isSleepTransitioning();
    }

    public boolean isSleeping() {
        return sleepComponent != null && sleepComponent.isSleeping();
    }

    public boolean isSleepingEntering() {
        return sleepComponent != null && sleepComponent.isSleepingEntering();
    }

    public boolean isSleepingExiting() {
        return sleepComponent != null && sleepComponent.isSleepingExiting();
    }

    public boolean isSleepLocked() {
        return sleepComponent != null && sleepComponent.isSleepLocked();
    }

    public void startSleepEnter() {
        if (sleepComponent != null) {
            sleepComponent.startSleepEnter();
        }
    }
    public void startSleepExit() {
        if (sleepComponent != null) {
            sleepComponent.startSleepExit();
        }
    }

    public void wakeUpImmediately() {
        if (sleepComponent != null) {
            sleepComponent.wakeUpImmediately();
        }
    }

    public void suppressSleep(int ticks) {
        if (sleepComponent != null) {
            sleepComponent.suppressSleep(ticks);
        }
    }

    public boolean isSleepSuppressed() {
        return sleepComponent != null && sleepComponent.isSleepSuppressed();
    }

    public int getSleepAmbientCooldownTicks() {
        if (sleepComponent == null) {
            return 0;
        }
        return sleepComponent.getAmbientCooldownTicks();
    }

    protected void clearSleepCooldowns() {
        if (sleepComponent != null) {
            sleepComponent.clearCooldowns();
        }
    }

    protected boolean useSleepSitDownTimer() {
        return false;
    }

    protected boolean requireSeatedBeforeFallAsleep() {
        return false;
    }

    protected boolean sleepForceSitDownOnEnter() {
        return false;
    }

    protected boolean useSleepSitUpAfterWake() {
        return true;
    }

    protected int getSleepSitDownDuration() {
        return 0;
    }

    protected int getSleepFallAsleepDuration() {
        return 0;
    }

    protected int getSleepWakeUpDuration() {
        return 0;
    }

    protected int getSleepSitUpDuration() {
        return 0;
    }

    protected int getSleepLoopLeadTicks() {
        return 0;
    }

    protected int getSleepExitSuppressionTicks() {
        return 40;
    }

    protected int getSleepWakeUpSuppressionTicks() {
        return 40;
    }

    protected boolean isAlreadySeatedForSleep() {
        return this.isOrderedToSit() || this.getSitProgress() >= this.maxSitTicks();
    }

    protected boolean shouldStaySeatedAfterWake() {
        return this.getCommand() == 1 || this.isOrderedToSit();
    }

    protected void onSleepLockCommand(int snapshot) {
        if (this.getCommand() != 1) {
            this.setCommand(1);
            this.setOrderedToSit(true);
        }
    }

    protected void onSleepUnlockCommand(int desired) {
        if (desired >= 0 && desired != this.getCommand()) {
            this.setCommand(desired);
            this.setOrderedToSit(desired == 1);
        }
    }

    protected void onSleepFreezeTick() {
    }

    protected void onSleepSitDownAnimation() {
    }

    protected void onSleepFallAsleepAnimation() {
    }

    protected void onSleepLoopAnimation() {
    }

    protected void onSleepWakeUpAnimation() {
    }

    protected void onSleepSitUpAnimation() {
    }

    protected void onSleepEntered() {
        this.setOrderedToSit(true);
    }

    protected void onSleepExitStarted() {
        this.setOrderedToSit(true);
    }

    protected void onSleepExitSeated() {
        this.setOrderedToSit(true);
    }

    protected void onSleepWakeUpImmediate() {
        this.setOrderedToSit(false);
    }

    public final boolean sleepUseSitDownTimer() {
        return useSleepSitDownTimer();
    }

    public final boolean sleepShouldForceSitDownOnEnter() {
        return sleepForceSitDownOnEnter();
    }

    public final boolean sleepUseSitUpAfterWake() {
        return useSleepSitUpAfterWake();
    }

    public final boolean sleepRequireSeatedBeforeFallAsleep() {
        return requireSeatedBeforeFallAsleep();
    }

    public final int sleepGetSitDownDuration() {
        return getSleepSitDownDuration();
    }

    public final int sleepGetFallAsleepDuration() {
        return getSleepFallAsleepDuration();
    }

    public final int sleepGetWakeUpDuration() {
        return getSleepWakeUpDuration();
    }

    public final int sleepGetSitUpDuration() {
        return getSleepSitUpDuration();
    }

    public final int sleepGetLoopLeadTicks() {
        return getSleepLoopLeadTicks();
    }

    public final int sleepGetExitSuppressionTicks() {
        return getSleepExitSuppressionTicks();
    }

    public final int sleepGetWakeUpSuppressionTicks() {
        return getSleepWakeUpSuppressionTicks();
    }

    public final boolean sleepIsAlreadySeatedForSleep() {
        return isAlreadySeatedForSleep();
    }

    public final boolean sleepShouldStaySeatedAfterWake() {
        return shouldStaySeatedAfterWake();
    }

    public final void sleepOnLockCommand(int snapshot) {
        onSleepLockCommand(snapshot);
    }

    public final void sleepOnUnlockCommand(int desired) {
        onSleepUnlockCommand(desired);
    }

    public final void sleepOnFreezeTick() {
        onSleepFreezeTick();
    }

    public final void sleepOnSitDownAnimation() {
        onSleepSitDownAnimation();
    }

    public final void sleepOnFallAsleepAnimation() {
        onSleepFallAsleepAnimation();
    }

    public final void sleepOnLoopAnimation() {
        onSleepLoopAnimation();
    }

    public final void sleepOnWakeUpAnimation() {
        onSleepWakeUpAnimation();
    }

    public final void sleepOnSitUpAnimation() {
        onSleepSitUpAnimation();
    }

    public final void sleepOnEntered() {
        onSleepEntered();
    }

    public final void sleepOnExitStarted() {
        onSleepExitStarted();
    }

    public final void sleepOnExitSeated() {
        onSleepExitSeated();
    }

    public final void sleepOnWakeUpImmediate() {
        onSleepWakeUpImmediate();
    }

    public DragonSleepPreferences getSleepPreferences() {
        return DragonSleepPreferences.FLEXIBLE();
    }

    public boolean canSleepNow() {
        return true; // Override for custom logic
    }

    public boolean canSleepInWater() {
        return false;
    }

    public record DragonSleepPreferences(
            boolean canSleepAtNight,
            boolean canSleepDuringDay,
            boolean avoidsThunderstorms
    ) {
        public boolean canSleepDuringConditions(net.minecraft.world.level.Level level) {
            if (avoidsThunderstorms && level.isThundering()) return false;
            boolean isDay = level.isDay();
            if (isDay && !canSleepDuringDay) return false;
            if (!isDay && !canSleepAtNight) return false;
            return true;
        }

        public static DragonSleepPreferences DIURNAL() {
            return new DragonSleepPreferences(false, true, true);
        }

        public static DragonSleepPreferences NOCTURNAL() {
            return new DragonSleepPreferences(true, false, true);
        }

        public static DragonSleepPreferences FLEXIBLE() {
            return new DragonSleepPreferences(true, true, true);
        }
    }

    public boolean isFlying() {
        return false;
    }
    public boolean isRunning() {
        return false;
    }
    public boolean isWalking() {
        return false;
    }

    public boolean isActuallyRunning() {
        return false;
    }
    public double getCachedHorizontalSpeed() {
        return 0.0;
    }
    public boolean areRiderControlsLocked() {
        return false;
    }
    public Vec3 getClientLocatorPosition(String locator) {
        return null;
    }
    public float maxSitTicks() {
        return 15.0F; // Default: 15 ticks to fully sit (about 0.75 seconds)
    }

    public float getSitProgress() {
        if (sitComponent == null) {
            return 0f;
        }
        return sitComponent.getSitProgress();
    }

    public float getPrevSitProgress() {
        if (sitComponent == null) {
            return 0f;
        }
        return sitComponent.getPrevSitProgress();
    }

    protected void setSitProgress(float value) {
        if (sitComponent != null) {
            sitComponent.setSitProgress(value);
        }
    }

    protected void setPrevSitProgress(float value) {
        if (sitComponent != null) {
            sitComponent.setPrevSitProgress(value);
        }
    }

    public void clearSitProgress() {
        if (sitComponent != null) {
            sitComponent.clearSitProgress();
        }
    }

    public void forceSitProgress(float value) {
        if (sitComponent != null) {
            sitComponent.forceSitProgress(value);
        }
    }

    protected void syncClientSitProgress() {
        if (sitComponent != null) {
            sitComponent.syncClientProgress();
        }
    }

    protected void saveSitProgress(CompoundTag tag) {
        if (sitComponent != null) {
            sitComponent.saveToNBT(tag);
        }
    }

    protected void loadSitProgress(CompoundTag tag, boolean orderedToSit) {
        if (sitComponent != null) {
            sitComponent.loadFromNBT(tag, orderedToSit);
        }
    }

    public boolean isGoingUp() {
        return false;

    }
    public void setGoingUp(boolean goingUp) {
    }

    public boolean isGoingDown() {
        return false;
    }

    public void setGoingDown(boolean goingDown) {
    }

    public net.minecraft.world.entity.player.Player getRidingPlayer() {
        if (this.getControllingPassenger() instanceof net.minecraft.world.entity.player.Player player) {
            return player;
        }
        return null;
    }

    protected void tickAbilities() {
        if (!level().isClientSide) {
            combatManager.tick();
        }
    }

    @Override
    public void tick() {
        super.tick();

        // Decrement skip respawn counter
        if (skipRespawnTicks > 0) {
            skipRespawnTicks--;
        }
        tickAbilities();
        if (!level().isClientSide) {
            aiCombatPacing.tick();
            if (sleepComponent != null) {
                sleepComponent.tick();
            }
            if (this.isTame() && !this.isBoundInBinder()) {
                if (hungerComponent != null) {
                    hungerComponent.tick();
                }
                if (happinessComponent != null) {
                    int currentHunger = hungerComponent != null ? hungerComponent.getHunger() : HUNGER_MAX;
                    happinessComponent.tick(currentHunger);
                    happinessComponent.updateSpeedModifiers();
                }
            } else if (happinessComponent != null) {
                happinessComponent.clearSpeedModifiers();
            }
            if (recoveryComponent != null) {
                recoveryComponent.tick();
            }
        }

        if (!level().isClientSide && this.dragonBodyControl != null) {
            this.dragonBodyControl.serverTick();
        }
        if (level().isClientSide) {
            syncClientSitProgress();
            if (animationSyncComponent != null) {
                animationSyncComponent.tickClientRotationDeviations();
            }
        } else if (animationSyncComponent != null) {
            animationSyncComponent.tickServerRotationTargets();
        }
    }

    // ===== COMMAND SYSTEM (shared) =====

    public int getCommand() {
        if (commandComponent == null) {
            return this.entityData.get(DATA_COMMAND);
        }
        return commandComponent.getCommand();
    }


    public void setCommand(int command) {
        if (commandComponent == null) {
            this.entityData.set(DATA_COMMAND, command);
            if (this.isTame()) {
                this.setOrderedToSit(command == 1);
            }
            return;
        }
        commandComponent.setCommand(command);
    }
    public boolean canOwnerCommand(Player player) {
        return player != null && player.isCrouching();
    }

    public boolean canOwnerMount(Player player) {
        if (this.isBaby()) {
            return false;
        }
        if (this.getHappiness() < 30) {
            if (!this.level().isClientSide && this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        net.minecraft.core.particles.ParticleTypes.ANGRY_VILLAGER,
                        this.getX(),
                        this.getY() + this.getBbHeight() + 0.3,
                        this.getZ(),
                        6,
                        0.3,
                        0.2,
                        0.3,
                        0.0
                );
            }
            return false;
        }
        return true;
    }

    public boolean ignoresLeashPull() {
        return false;
    }

    public float getLeashBreakDistance() {
        return 10.0F;
    }

    public boolean isAlly(net.minecraft.world.entity.Entity entity) {
        if (entity == null) return false;

        if (entity instanceof Player player) {
            return isPlayerAlly(player);
        }

        // Dragons and other owned companions inherit protection from their player owner.
        if (entity instanceof DragonEntity otherDragon && otherDragon.isTame()) {
            LivingEntity otherOwner = otherDragon.getOwner();
            return otherOwner instanceof Player otherPlayer && isPlayerAlly(otherPlayer);
        }

        if (entity instanceof net.minecraft.world.entity.TamableAnimal tamable && tamable.isTame()) {
            net.minecraft.world.entity.LivingEntity owner = tamable.getOwner();
            return owner instanceof Player playerOwner && isPlayerAlly(playerOwner);
        }

        if (entity instanceof OwnableEntity ownable) {
            LivingEntity owner = ownable.getOwner();
            return owner instanceof Player playerOwner && isPlayerAlly(playerOwner);
        }
        return false;
    }

    private boolean isPlayerAlly(Player player) {
        return player != null && ((this.isTame() && this.isOwnedBy(player)) || allyManager.isAlly(player));
    }

    public boolean canTarget(net.minecraft.world.entity.Entity entity) {
        if (entity == null) return false;

        if (entity instanceof LivingEntity living && !isTargetValid(living)) {
            return false;
        }

        if (isAlly(entity)) {
            return false;
        }

        if (entity instanceof net.minecraft.world.entity.TamableAnimal tamable && tamable.isTame()) {
            net.minecraft.world.entity.LivingEntity owner = tamable.getOwner();
            if (owner instanceof Player playerOwner && this.isTame() && this.isOwnedBy(playerOwner)) {
                return false;
            }
        }

        if (entity instanceof OwnableEntity ownable) {
            LivingEntity owner = ownable.getOwner();
            if (owner instanceof Player playerOwner && this.isTame() && this.isOwnedBy(playerOwner)) {
                return false;
            }
        }

        return true;
    }

    public boolean isTargetValid(@Nullable LivingEntity target) {
        if (target == null) return false;
        if (!target.isAlive() || target.isRemoved()) return false;
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) return false;
        return !isIafMobDead(target);
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
            super.setTarget(null);
            return;
        }
        super.setTarget(target);
    }

    private static final ConcurrentHashMap<Class<?>, Optional<Method>> IAF_DEAD_METHODS =
            new ConcurrentHashMap<>();

    private static boolean isIafMobDead(LivingEntity target) {
        String className = target.getClass().getName();
        if (!className.startsWith("com.github.alexthe666.iceandfire.")
                && !className.startsWith("com.iafenvoy.iceandfire.")) {
            return false;
        }

        Optional<Method> method = IAF_DEAD_METHODS.computeIfAbsent(
                target.getClass(),
                DragonEntity::resolveIafDeadMethod
        );

        if (method.isEmpty()) {
            return false;
        }

        try {
            Object result = method.get().invoke(target);
            return result instanceof Boolean && (Boolean) result;
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    private static Optional<Method> resolveIafDeadMethod(Class<?> type) {
        Method method = findNoArgBoolean(type, "isMobDead");
        if (method == null) {
            method = findNoArgBoolean(type, "isModelDead");
        }
        return Optional.ofNullable(method);
    }

    private static Method findNoArgBoolean(Class<?> type, String name) {
        try {
            Method method = type.getMethod(name);
            if (method.getReturnType() == boolean.class) {
                return method;
            }
        } catch (NoSuchMethodException ignored) {
            return null;
        }
        return null;
    }

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

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (commandComponent != null) {
            commandComponent.saveToNBT(tag);
        }
        if (hungerComponent != null) {
            hungerComponent.saveToNBT(tag);
        }
        if (happinessComponent != null) {
            happinessComponent.saveToNBT(tag);
        }
        if (groomingComponent != null) {
            groomingComponent.saveToNBT(tag);
        }
        if (sleepComponent != null) {
            sleepComponent.saveToNBT(tag);
        }
        if (genderComponent != null) {
            genderComponent.saveToNBT(tag);
        }
        tag.putInt("TextureVariant", getTextureVariant());
        tag.putInt("PendingAdultTextureVariant", getPendingAdultTextureVariant());
        tag.putBoolean("BoundInBinder", this.boundInBinder);
        if (assignedParentUuid != null) {
            tag.putUUID("AssignedParentUuid", assignedParentUuid);
        }

        allyManager.saveToNBT(tag);
    }


    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {

        if (tag.contains("IsRespawning")) {
            this.isRespawning = tag.getBoolean("IsRespawning");
        }

        super.readAdditionalSaveData(tag);

        if (commandComponent != null) {
            commandComponent.loadFromNBT(tag);
        }
        if (genderComponent != null) {
            genderComponent.loadFromNBT(tag);
        }
        if (hungerComponent != null) {
            hungerComponent.loadFromNBT(tag);
        }
        if (happinessComponent != null) {
            happinessComponent.loadFromNBT(tag);
        }
        if (groomingComponent != null) {
            groomingComponent.loadFromNBT(tag);
        }
        if (sleepComponent != null) {
            sleepComponent.loadFromNBT(tag);
        }
        if (tag.contains("TextureVariant")) {
            setTextureVariant(tag.getInt("TextureVariant"));
        }
        if (tag.contains("PendingAdultTextureVariant")) {
            setPendingAdultTextureVariant(tag.getInt("PendingAdultTextureVariant"));
        }
        this.assignedParentUuid = tag.hasUUID("AssignedParentUuid") ? tag.getUUID("AssignedParentUuid") : null;
        this.boundInBinder = tag.getBoolean("BoundInBinder");
        allyManager.loadFromNBT(tag);
    }

    private void applyHappinessHitPenalty(net.minecraft.server.level.ServerLevel serverLevel) {
        if (happinessComponent != null) {
            happinessComponent.applyHitPenalty(serverLevel);
        }
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

    // ===== BABY/BREEDING SYSTEM =====
    @Override
    public void setAge(int age) {
        boolean wasBaby = this.isBaby();
        super.setAge(age);
        boolean isNowBaby = this.isBaby();

        // Detect baby->adult or adult->baby transition
        // Skip if:
        // - Already in the middle of a respawn (prevents infinite recursion)
        // - Skip flag is set (newly spawned babies from spawn eggs/breeding)
        // - Entity has never been added to world (getId() returns -1 for entities not yet added)
        // - Entity is being initially loaded (wasBaby is false and age is going negative - this is world load, not actual aging)
        boolean isInitialLoad = !wasBaby && age < 0 && this.tickCount == 0;

        if (wasBaby != isNowBaby && !level().isClientSide && !isRespawning && this.getId() != -1 && skipRespawnTicks == 0 && !isInitialLoad) {
            if (wasBaby && !isNowBaby) {
                applyPendingAdultTextureVariant();
            }

            // Set flag to prevent re-entrancy when newEntity.load() calls setAge()
            isRespawning = true;

            // NUCLEAR OPTION: GeckoLib caches animations on the client renderer, and there's
            // no clean way to invalidate that cache. So we force the entity to "respawn"
            // by saving its state, removing it, and spawning a fresh copy.

            // Cache the world reference BEFORE any modifications (important!)
            Level world = this.level();

            // Cache position, rotation, and UUID before saving NBT
            double posX = this.getX();
            double posY = this.getY();
            double posZ = this.getZ();
            float yaw = this.getYRot();
            float pitch = this.getXRot();
            java.util.UUID oldUUID = this.getUUID();

            // Save current state
            CompoundTag nbt = new CompoundTag();
            this.saveWithoutId(nbt);

            // Mark in NBT that we're currently respawning - the new entity will inherit this flag
            nbt.putBoolean("IsRespawning", true);

            // Update age in the saved data
            nbt.putInt("Age", age);

            // CRITICAL: Preserve gender initialization state across respawn
            // Without this, the new entity will have genderInitialized=false and may randomize gender
            nbt.putBoolean("GenderInitialized", this.genderComponent != null && this.genderComponent.isInitialized());

            // Create fresh entity with updated data
            @SuppressWarnings("unchecked")
            DragonEntity newEntity = (DragonEntity) this.getType().create(world);
            if (newEntity != null) {
                newEntity.load(nbt);  // Reads IsRespawning=true, preventing another respawn when setAge() is called

                // Force set the cached position and rotation (overrides any bad NBT data)
                newEntity.setPos(posX, posY, posZ);
                newEntity.setYRot(yaw);
                newEntity.setXRot(pitch);

                // Clear the respawning flag now that the process is complete
                newEntity.isRespawning = false;

                // CRITICAL: Remove old entity FIRST to free up the UUID
                this.discard();

                // Preserve UUID for both tamed and untamed dragons.
                // This keeps external references stable across baby/adult visual respawn.
                newEntity.setUUID(oldUUID);

                // Finally, add the new entity to the world
                world.addFreshEntity(newEntity);

                // Visual/sound feedback for the transformation
                world.broadcastEntityEvent(newEntity, (byte) 7); // Hearts
            }

            // Note: We don't reset the flag on old entity because it's about to be discarded anyway
        }
    }

    @Override
    public void setBaby(boolean baby) {
        super.setBaby(baby);

        if (level().isClientSide || !shouldPersistAdultTextureVariantOnBabies()) {
            return;
        }

        if (baby) {
            ensurePendingAdultTextureVariant();
            setTextureVariant(0);
        } else {
            applyPendingAdultTextureVariant();
        }
    }


    @Override
    public void spawnChildFromBreeding(net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.animal.Animal otherParent) {
        net.minecraft.world.entity.AgeableMob baby = this.getBreedOffspring(level, otherParent);
        if (baby != null) {
            net.minecraft.core.BlockPos safePos = findSafeBabySpawnPos(level, this.blockPosition());
            baby.setBaby(true);
            baby.moveTo(this.getX(), safePos != null ? safePos.getY() : this.getY(), this.getZ(), 0.0F, 0.0F);
            level.addFreshEntityWithPassengers(baby);
        }
    }

    @Nullable
    protected net.minecraft.core.BlockPos findSafeBabySpawnPos(net.minecraft.world.level.LevelAccessor level, net.minecraft.core.BlockPos start) {
        if (level == null || start == null) return null;
        net.minecraft.core.BlockPos.MutableBlockPos cursor = start.mutable();
        int minY = level.getMinBuildHeight();

        while (cursor.getY() >= minY) {
            net.minecraft.world.level.block.state.BlockState state = level.getBlockState(cursor);
            if (isStableBabyLandingSurface(level, cursor, state)) {
                net.minecraft.core.BlockPos above = cursor.above();
                net.minecraft.world.level.block.state.BlockState aboveState = level.getBlockState(above);
                if (aboveState.getCollisionShape(level, above).isEmpty() && aboveState.getFluidState().isEmpty()) {
                    return above;
                }
            }
            cursor.move(net.minecraft.core.Direction.DOWN);
        }
        return null;
    }

    private boolean isStableBabyLandingSurface(net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos,
                                               net.minecraft.world.level.block.state.BlockState state) {
        if (state.isAir() || !state.getFluidState().isEmpty()) {
            return false;
        }
        return state.isSolidRender(level, pos) || state.isFaceSturdy(level, pos, net.minecraft.core.Direction.UP);
    }

    public abstract Vec3 getHeadPosition();

    public abstract Vec3 getMouthPosition();
}

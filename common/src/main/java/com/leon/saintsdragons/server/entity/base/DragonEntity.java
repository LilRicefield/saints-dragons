package com.leon.saintsdragons.server.entity.base;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.block.AbstractDragonEggBlockEntity;
import com.leon.saintsdragons.common.registry.Dragons;
import com.leon.saintsdragons.common.config.SaintsDragonsConfig;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
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
import com.leon.saintsdragons.server.entity.component.ScreenShakeComponent;
import com.leon.saintsdragons.server.entity.controller.BodyControl;
import com.leon.saintsdragons.server.entity.controller.DragonLookControl;
import com.leon.saintsdragons.server.entity.handler.DragonCombatHandler;
import com.leon.saintsdragons.server.entity.handler.DragonSoundHandler;
import com.leon.saintsdragons.server.entity.interfaces.DragonSoundProfile;
import com.leon.saintsdragons.server.entity.interfaces.DragonMovementCapable;
import com.leon.saintsdragons.server.entity.interfaces.DragonMovementCapability;
import com.leon.saintsdragons.server.entity.interfaces.SoundHandledDragon;
import com.leon.saintsdragons.server.entity.handler.DragonAllyManager;
import com.leon.saintsdragons.server.entity.dragons.util.DragonBreedingRules;
import com.leon.saintsdragons.server.entity.variant.SaintsDragonVariantRegistry;
import com.leon.saintsdragons.common.network.DragonAnimTickets;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import com.leon.saintsdragons.util.math.SmoothValue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animation.AnimatableManager;
import com.leon.saintsdragons.server.data.DragonCodexSavedData;
import java.util.List;
import java.util.UUID;
import java.util.EnumSet;

public abstract class DragonEntity extends TamableAnimal implements GeoEntity, SoundHandledDragon, DragonMovementCapable {
    protected static final int DAMAGE_SLEEP_SUPPRESSION_TICKS = 20 * 30;
    private static final DragonVariantSet DEFAULT_VARIANTS = DragonVariantSet.of(
            DragonVariant.of(0, "default", 1)
    );
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
    private static final EntityDataAccessor<Integer> DATA_TEXTURE_VARIANT =
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_PENDING_ADULT_TEXTURE_VARIANT =
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> DATA_TEXTURE_VARIANT_ID =
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DATA_PENDING_ADULT_TEXTURE_VARIANT_ID =
            SynchedEntityData.defineId(DragonEntity.class, EntityDataSerializers.STRING);
    public static final int HUNGER_MAX = DragonHungerComponent.HUNGER_MAX;
    public static final int HAPPINESS_MAX = DragonHappinessComponent.HAPPINESS_MAX;
    private DragonAbility<?> activeAbility = null;
    public final DragonCombatHandler combatManager;
    public final DragonAllyManager allyManager;
    private final DragonSoundHandler soundHandler = new DragonSoundHandler(this);
    private int ambientSoundTimer;
    private int nextAmbientSoundDelay;
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

    private final SmoothValue fallbackBodyRotDeviation = SmoothValue.rotation(0.0);
    private final SmoothValue fallbackPitchDeviation = SmoothValue.rotation(0.0);
    private final SmoothValue fallbackYawVelocity = SmoothValue.value(0.0);
    private float clientTailDragVelocity = 0.0f;
    @Nullable
    private Dragons cachedDragonType;
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
    private boolean growthStunted = false;
    @Nullable
    private UUID assignedParentUuid;
    private boolean familySpawnPending = false;
    private int pendingFamilyBabyCount = 0;

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
        this.sitComponent = createSitComponent();
        this.babyComponent = createBabyComponent();
        this.lookControl = new DragonLookControl<>(this);
    }

    @Override
    public DragonSoundHandler getSoundHandler() {
        return soundHandler;
    }

    @Override
    public EnumSet<DragonMovementCapability> movementCapabilities() {
        return EnumSet.of(DragonMovementCapability.WALK);
    }

    protected void handleAnimationSound(String soundKey) {
        if (soundKey == null || soundKey.isEmpty()) {
            return;
        }
        DragonSoundProfile profile = getSoundProfile();
        if (profile == null || !profile.handleAnimationSound(getSoundHandler(), this, soundKey, null)) {
            getSoundHandler().playVocal(soundKey);
        }
    }

    protected void resetAmbientSoundTimer(int minDelayTicks, int maxDelayTicks) {
        ambientSoundTimer = 0;
        int min = Math.max(1, minDelayTicks);
        int max = Math.max(min, maxDelayTicks);
        nextAmbientSoundDelay = min + getRandom().nextInt(max - min + 1);
    }

    protected void seedAmbientSoundTimer(int minDelayTicks, int maxDelayTicks, int initialTimerBound) {
        ambientSoundTimer = initialTimerBound > 0 ? getRandom().nextInt(initialTimerBound) : 0;
        int min = Math.max(1, minDelayTicks);
        int max = Math.max(min, maxDelayTicks);
        nextAmbientSoundDelay = min + getRandom().nextInt(max - min + 1);
    }

    protected void tickAmbientVocalSounds(int minDelayTicks, int maxDelayTicks, Supplier<String> vocalSelector) {
        if (nextAmbientSoundDelay <= 0) {
            resetAmbientSoundTimer(minDelayTicks, maxDelayTicks);
        }
        if (++ambientSoundTimer < nextAmbientSoundDelay) {
            return;
        }

        String vocalKey = vocalSelector != null ? vocalSelector.get() : null;
        if (vocalKey != null && !vocalKey.isEmpty()) {
            getSoundHandler().playVocal(vocalKey);
        }
        resetAmbientSoundTimer(minDelayTicks, maxDelayTicks);
    }

    protected String selectWeightedAmbientVocal(String first, float firstChance, String second, float secondChance, String fallback) {
        float roll = getRandom().nextFloat();
        if (roll < firstChance) {
            return first;
        }
        if (roll < secondChance) {
            return second;
        }
        return fallback;
    }

    protected void playGroundStepLoopSound(SoundEvent walkSound, SoundEvent runSound,
                                           int walkDurationTicks, int runDurationTicks,
                                           long walkReplayIntervalTicks, long runReplayIntervalTicks,
                                           boolean running, float volume, float pitch) {
        if (level().isClientSide || isBaby()) {
            return;
        }

        long now = level().getGameTime();
        long minIntervalTicks = running ? runReplayIntervalTicks : walkReplayIntervalTicks;
        if (now - getSoundHandler().getLastStepTick() < minIntervalTicks) {
            return;
        }
        getSoundHandler().setLastStepTick(now);
        getSoundHandler().playMovingEntitySound(
                running ? runSound : walkSound,
                volume,
                pitch,
                running ? runDurationTicks : walkDurationTicks
        );
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
    protected @NotNull BodyRotationControl createBodyControl() {
        this.dragonBodyControl = new BodyControl(this, getBodyTurnSpeed());
        return this.dragonBodyControl;
    }

    protected float getBodyTurnSpeed() {
        return 0.6f;
    }


    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_COMMAND, 0);
        this.entityData.define(DATA_SIT_PROGRESS, 0.0f);
        this.entityData.define(DATA_GENDER, DragonGender.MALE.getId());
        this.entityData.define(DATA_HAPPINESS, HAPPINESS_MAX);
        this.entityData.define(DATA_SLEEPING, false);
        this.entityData.define(DATA_SLEEPING_ENTERING, false);
        this.entityData.define(DATA_SLEEPING_EXITING, false);
        this.entityData.define(DATA_TEXTURE_VARIANT, 0);
        this.entityData.define(DATA_PENDING_ADULT_TEXTURE_VARIANT, -1);
        this.entityData.define(DATA_TEXTURE_VARIANT_ID, SaintsDragonVariantRegistry.DEFAULT_VARIANT_ID.toString());
        this.entityData.define(DATA_PENDING_ADULT_TEXTURE_VARIANT_ID, "");
    }

    public float smoothTailDragVelocity(float targetDegrees) {
        clientTailDragVelocity = Mth.lerp(0.15f, clientTailDragVelocity, targetDegrees);
        return clientTailDragVelocity;
    }

    public SmoothValue getBodyRotDeviation() {
        return fallbackBodyRotDeviation;
    }

    public SmoothValue getPitchDeviation() {
        return fallbackPitchDeviation;
    }

    public SmoothValue getYawVelocity() {
        return fallbackYawVelocity;
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

    @Override
    public boolean canMate(@NotNull Animal otherAnimal) {
        if (!DragonBreedingRules.isEnabled()) {
            return false;
        }
        if (otherAnimal == this || otherAnimal.getClass() != this.getClass()) {
            return false;
        }
        if (!(otherAnimal instanceof DragonEntity otherDragon)) {
            return false;
        }
        return !this.isBaby()
                && !otherDragon.isBaby()
                && this.getAge() == 0
                && otherDragon.getAge() == 0
                && this.isInLove()
                && otherDragon.isInLove()
                && this.isFemale() != otherDragon.isFemale();
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

    protected void scheduleFamilyBabies(int count) {
        this.pendingFamilyBabyCount = Math.max(0, count);
        this.familySpawnPending = this.pendingFamilyBabyCount > 0;
    }

    protected boolean hasPendingFamilyBabies() {
        return familySpawnPending && pendingFamilyBabyCount > 0;
    }

    protected <T extends DragonEntity> void spawnPendingFamilyBabies(EntityType<T> babyType, Consumer<T> configureBaby) {
        if (!hasPendingFamilyBabies()) {
            return;
        }

        this.familySpawnPending = false;

        if (!(level() instanceof ServerLevel serverLevel)) {
            this.pendingFamilyBabyCount = 0;
            return;
        }

        int spawnCount = this.pendingFamilyBabyCount;
        this.pendingFamilyBabyCount = 0;

        serverLevel.getServer().execute(() -> {
            if (this.isRemoved()) {
                return;
            }

            for (int i = 0; i < spawnCount; i++) {
                T baby = babyType.create(serverLevel);
                if (baby == null) {
                    continue;
                }

                baby.setGender(this.random.nextBoolean() ? DragonGender.FEMALE : DragonGender.MALE);
                assignMotherToBaby(baby, null);
                baby.skipRespawnTicks = 5;
                baby.setBaby(true);
                baby.setAge(-24000);
                configureBaby.accept(baby);
                baby.setHealth(baby.getMaxHealth());

                double angle = (Math.PI * 2.0 * i) / spawnCount;
                double distance = 1.0 + this.random.nextDouble() * 0.5;
                double offsetX = Math.cos(angle) * distance;
                double offsetZ = Math.sin(angle) * distance;
                baby.moveTo(
                        this.getX() + offsetX,
                        this.getY(),
                        this.getZ() + offsetZ,
                        this.random.nextFloat() * 360.0F,
                        0.0F
                );
                serverLevel.addFreshEntity(baby);
            }
        });
    }

    protected <T extends DragonEntity> T createBreedOffspring(ServerLevel level, AgeableMob otherParent, EntityType<T> babyType, Consumer<T> configureBaby) {
        T baby = babyType.create(level);
        if (baby == null) {
            return null;
        }

        baby.setGender(this.random.nextBoolean() ? DragonGender.FEMALE : DragonGender.MALE);
        assignMotherToBaby(baby, otherParent);
        UUID ownerId = this.getOwnerUUID();
        if (ownerId != null) {
            baby.setOwnerUUID(ownerId);
            baby.setTame(true);
        }

        baby.skipRespawnTicks = 5;
        baby.setAge(-24000);
        baby.setBaby(true);
        configureBaby.accept(baby);
        baby.setHealth(baby.getMaxHealth());

        BlockPos safePos = findSafeBabySpawnPos(level, this.blockPosition());
        double spawnY = safePos != null ? safePos.getY() : this.getY();
        baby.moveTo(this.getX(), spawnY, this.getZ(), this.getYRot(), 0.0F);
        registerToOwnerCodex(baby, level);
        return baby;
    }

    public int getTextureVariant() {
        return SaintsDragonVariantRegistry.variantIdToLegacy(getDragonVariantTypeId(), getTextureVariantId());
    }

    public void setTextureVariant(int variant) {
        setTextureVariantId(SaintsDragonVariantRegistry.legacyToVariantId(getDragonVariantTypeId(), variant));
    }

    public ResourceLocation getTextureVariantId() {
        return parseVariantId(this.entityData.get(DATA_TEXTURE_VARIANT_ID), SaintsDragonVariantRegistry.defaultVariantId(getDragonVariantTypeId()));
    }

    public void setTextureVariantId(ResourceLocation variantId) {
        ResourceLocation dragonId = getDragonVariantTypeId();
        ResourceLocation normalized = SaintsDragonVariantRegistry.normalize(dragonId, variantId);
        this.entityData.set(DATA_TEXTURE_VARIANT_ID, normalized.toString());
        this.entityData.set(DATA_TEXTURE_VARIANT, SaintsDragonVariantRegistry.variantIdToLegacy(dragonId, normalized));
    }

    protected boolean shouldPersistAdultTextureVariantOnBabies() {
        return SaintsDragonVariantRegistry.getVariants(getDragonVariantTypeId()).size() > 1;
    }

    protected int chooseAdultTextureVariant() {
        return SaintsDragonVariantRegistry.variantIdToLegacy(getDragonVariantTypeId(), chooseAdultTextureVariantId());
    }

    protected ResourceLocation chooseAdultTextureVariantId() {
        if (this.level() instanceof ServerLevelAccessor serverLevelAccessor) {
            return SaintsDragonVariantRegistry.chooseSpawnVariant(serverLevelAccessor, this);
        }
        return SaintsDragonVariantRegistry.legacyToVariantId(getDragonVariantTypeId(), getVariantSet().roll(this.getRandom()));
    }

    public int getPendingAdultTextureVariant() {
        ResourceLocation pending = getPendingAdultTextureVariantId();
        return pending == null ? -1 : SaintsDragonVariantRegistry.variantIdToLegacy(getDragonVariantTypeId(), pending);
    }

    public void setPendingAdultTextureVariant(int variant) {
        if (variant < 0) {
            setPendingAdultTextureVariantId(null);
            return;
        }
        setPendingAdultTextureVariantId(SaintsDragonVariantRegistry.legacyToVariantId(getDragonVariantTypeId(), variant));
    }

    @Nullable
    public ResourceLocation getPendingAdultTextureVariantId() {
        String value = this.entityData.get(DATA_PENDING_ADULT_TEXTURE_VARIANT_ID);
        if (value == null || value.isBlank()) {
            return null;
        }
        return parseVariantId(value, null);
    }

    public void setPendingAdultTextureVariantId(@Nullable ResourceLocation variantId) {
        ResourceLocation dragonId = getDragonVariantTypeId();
        if (variantId == null) {
            this.entityData.set(DATA_PENDING_ADULT_TEXTURE_VARIANT_ID, "");
            this.entityData.set(DATA_PENDING_ADULT_TEXTURE_VARIANT, -1);
            return;
        }
        ResourceLocation normalized = SaintsDragonVariantRegistry.normalize(dragonId, variantId);
        this.entityData.set(DATA_PENDING_ADULT_TEXTURE_VARIANT_ID, normalized.toString());
        this.entityData.set(DATA_PENDING_ADULT_TEXTURE_VARIANT, SaintsDragonVariantRegistry.variantIdToLegacy(dragonId, normalized));
    }

    protected void ensurePendingAdultTextureVariant() {
        if (!shouldPersistAdultTextureVariantOnBabies()) {
            return;
        }
        if (getPendingAdultTextureVariantId() == null) {
            setPendingAdultTextureVariantId(chooseAdultTextureVariantId());
        }
    }

    protected void applyPendingAdultTextureVariant() {
        if (!shouldPersistAdultTextureVariantOnBabies()) {
            return;
        }
        ResourceLocation pending = getPendingAdultTextureVariantId();
        if (pending == null) {
            pending = chooseAdultTextureVariantId();
        }
        setTextureVariantId(pending);
        setPendingAdultTextureVariantId(null);
    }

    public int getCodexTextureVariant() {
        return SaintsDragonVariantRegistry.variantIdToLegacy(getDragonVariantTypeId(), getCodexTextureVariantId());
    }

    public ResourceLocation getCodexTextureVariantId() {
        if (this.isBaby() && shouldPersistAdultTextureVariantOnBabies()) {
            ResourceLocation pending = getPendingAdultTextureVariantId();
            if (pending != null) {
                return pending;
            }
        }
        return getTextureVariantId();
    }

    protected int getMaxTextureVariant() {
        return getVariantSet().maxId();
    }


    public Map<String, Integer> getTextureVariantNameMap() {
        return SaintsDragonVariantRegistry.legacyNameMap(getDragonVariantTypeId());
    }

    public Map<String, ResourceLocation> getTextureVariantIdNameMap() {
        return SaintsDragonVariantRegistry.variantNameMap(getDragonVariantTypeId());
    }

    public List<String> getTextureVariantCommandSuggestions() {
        return SaintsDragonVariantRegistry.commandSuggestions(getDragonVariantTypeId());
    }

    protected DragonVariantSet getVariantSet() {
        return DEFAULT_VARIANTS;
    }

    public String getTextureVariantName(int variantId) {
        return getTextureVariantName(SaintsDragonVariantRegistry.legacyToVariantId(getDragonVariantTypeId(), variantId));
    }

    public String getTextureVariantName(ResourceLocation variantId) {
        ResourceLocation normalized = SaintsDragonVariantRegistry.normalize(getDragonVariantTypeId(), variantId);
        var definition = SaintsDragonVariantRegistry.get(getDragonVariantTypeId(), normalized);
        return definition != null ? definition.name() : normalized.getPath();
    }

    public String getTextureVariantTranslationKey(int variantId) {
        return getTextureVariantTranslationKey(SaintsDragonVariantRegistry.legacyToVariantId(getDragonVariantTypeId(), variantId));
    }

    public String getTextureVariantTranslationKey(ResourceLocation variantId) {
        ResourceLocation normalized = SaintsDragonVariantRegistry.normalize(getDragonVariantTypeId(), variantId);
        var definition = SaintsDragonVariantRegistry.get(getDragonVariantTypeId(), normalized);
        return definition != null ? definition.translationKey() : "saintsdragons.variant." + normalized.getNamespace() + "." + normalized.getPath().replace('/', '.');
    }

    protected int chooseSpawnTextureVariant(@NotNull ServerLevelAccessor levelAccessor,
                                            @NotNull DifficultyInstance difficulty,
                                            @NotNull MobSpawnType reason,
                                            @Nullable SpawnGroupData spawnData,
                                            @Nullable CompoundTag spawnTag) {
        return SaintsDragonVariantRegistry.variantIdToLegacy(getDragonVariantTypeId(),
                chooseSpawnTextureVariantId(levelAccessor, difficulty, reason, spawnData, spawnTag));
    }

    protected ResourceLocation chooseSpawnTextureVariantId(@NotNull ServerLevelAccessor levelAccessor,
                                                          @NotNull DifficultyInstance difficulty,
                                                          @NotNull MobSpawnType reason,
                                                          @Nullable SpawnGroupData spawnData,
                                                          @Nullable CompoundTag spawnTag) {
        return SaintsDragonVariantRegistry.chooseSpawnVariant(levelAccessor, this);
    }

    public boolean hasCustomTextureVariant() {
        ResourceLocation variantId = getTextureVariantId();
        return !SaintsDragonVariantRegistry.isLegacyVariant(getDragonVariantTypeId(), variantId);
    }

    public ResourceLocation getCustomAdultTextureResource(boolean female) {
        return SaintsDragonVariantRegistry.adultTexture(getDragonVariantTypeId(), getTextureVariantId(), female);
    }

    protected ResourceLocation getDragonVariantTypeId() {
        return SaintsDragonVariantRegistry.dragonId(this);
    }

    @Nullable
    private static ResourceLocation parseVariantId(@Nullable String value, @Nullable ResourceLocation fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return new ResourceLocation(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    public boolean tryBrush(Player player, ItemStack brushStack) {
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
        this.setAnimData(DragonAnimTickets.GROUND_MODE, groundState);
        this.setAnimData(DragonAnimTickets.FLIGHT_MODE, flightMode);
    }

    @Override
    public @NotNull SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor levelAccessor, @NotNull DifficultyInstance difficulty, @NotNull MobSpawnType reason,
                                                 @Nullable SpawnGroupData spawnData, @Nullable CompoundTag spawnTag) {
        SpawnGroupData data = super.finalizeSpawn(levelAccessor, difficulty, reason, spawnData, spawnTag);
        ensureGenderInitialized();
        ResourceLocation chosenVariant = chooseSpawnTextureVariantId(levelAccessor, difficulty, reason, spawnData, spawnTag);
        if (this.isBaby() && shouldPersistAdultTextureVariantOnBabies()) {
            setPendingAdultTextureVariantId(chosenVariant);
            setTextureVariant(0);
        } else {
            setTextureVariantId(chosenVariant);
        }

        if (this.isBaby() && reason == MobSpawnType.SPAWN_EGG) {
            BlockPos safePos = findSafeBabySpawnPos(levelAccessor, this.blockPosition());
            if (safePos != null && safePos.getY() < this.getY()) {
                this.moveTo(this.getX(), safePos.getY(), this.getZ(), this.getYRot(), this.getXRot());
            }
        }
        return data;
    }

    @SuppressWarnings("unchecked")
    public <T extends DragonEntity> DragonAbility<T> getActiveAbility() {
        return (DragonAbility<T>) activeAbility;
    }

    public void setActiveAbility(DragonAbility<?> ability) {
        this.activeAbility = ability;
    }

    public void lockAbilities(int ticks) {
        combatManager.lockGlobalCooldown(ticks);
    }

    public <T extends DragonEntity> void tryActivateAbility(DragonAbilityType<T, ?> abilityType) {
        if (abilityType == null || level().isClientSide) {
            return;
        }
        combatManager.tryUseAbility(abilityType);
    }

    public boolean isAbilityActive(DragonAbilityType<?, ?> abilityType) {
        return combatManager.isAbilityActive(abilityType);
    }

    public void forceEndAbility(DragonAbilityType<?, ?> abilityType) {
        combatManager.forceEndAbility(abilityType);
    }

    public void forceEndActiveAbility() {
        combatManager.forceEndActiveAbility();
    }

    @Nullable
    protected ScreenShakeComponent getScreenShakeComponent() {
        return null;
    }

    public float getScreenShakeAmount(float partialTicks) {
        ScreenShakeComponent screenShake = getScreenShakeComponent();
        return screenShake != null ? screenShake.getAmount(partialTicks) : 0.0F;
    }

    public boolean canFeelShake(Entity player) {
        return true;
    }

    public void triggerScreenShake(float intensity) {
        ScreenShakeComponent screenShake = getScreenShakeComponent();
        if (screenShake != null) {
            if (!SaintsDragonsConfig.SCREEN_SHAKE_ENABLED.get()) {
                screenShake.clear();
                return;
            }
            screenShake.trigger(intensity);
        }
    }

    public void triggerScreenShake(float intensity, int durationTicks) {
        ScreenShakeComponent screenShake = getScreenShakeComponent();
        if (screenShake != null) {
            if (!SaintsDragonsConfig.SCREEN_SHAKE_ENABLED.get()) {
                screenShake.clear();
                return;
            }
            screenShake.hold(intensity, durationTicks);
        }
    }

    public void clearScreenShake() {
        ScreenShakeComponent screenShake = getScreenShakeComponent();
        if (screenShake != null) {
            screenShake.clear();
        }
    }

    public Map<String, VocalEntry> getVocalEntries() {
        return Collections.emptyMap();
    }

    public DragonSoundProfile getSoundProfile() {
        return DragonSoundProfile.EMPTY;
    }

    public record VocalEntry(String controllerId, String animationId, Supplier<SoundEvent> soundSupplier,
                             float volume, float basePitch, float pitchVariance, boolean allowWhenSitting,
                             boolean allowDuringSleep, boolean preventOverlap) {
    }

    public static final class VocalEntryBuilder {
        private final Map<String, VocalEntry> entries = new java.util.HashMap<>();

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

        public VocalEntryBuilder add(String key, String controller, String animation,
                                     Supplier<SoundEvent> sound) {
            return add(key, controller, animation, sound,
                    1.0f, 1.0f, 0.0f, false, false, false);
        }

        public Map<String, VocalEntry> build() {
            return Map.copyOf(entries);
        }
    }

    protected DragonAbilityType<?, ?> getHurtAbilityType() {
        return null;
    }

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
    protected void playStepSound(@NotNull BlockPos pos, @NotNull BlockState state) {
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
    public boolean fireImmune() {
        return super.fireImmune();
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (isDamageFromCurrentRider(source) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }

        boolean result = super.hurt(source, amount);
        if (result) {
            if (isSleeping() || isSleepingEntering() || isSleepingExiting() || isSleepTransitioning()) {
                wakeUpImmediately();
            }
            suppressSleep(DAMAGE_SLEEP_SUPPRESSION_TICKS);

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
            ServerLevel serverLevel = (ServerLevel) level();
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
            ServerLevel serverLevel = (ServerLevel) level();
            DragonCodexSavedData.get(serverLevel).removeDragon(this.getOwnerUUID(), this.getUUID());
        }
        super.die(cause);
    }

    @Override
    public void tame(@NotNull Player player) {
        super.tame(player);
        if (!level().isClientSide && player instanceof ServerPlayer serverPlayer) {
            DragonCodexSavedData.get(serverPlayer.serverLevel()).addDragon(serverPlayer, this);
        }
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        if (!level().isClientSide && this.isTame() && this.getOwnerUUID() != null) {
           ServerLevel serverLevel = (ServerLevel) level();
            DragonCodexSavedData.get(serverLevel).updateDragonName(this.getOwnerUUID(), this.getUUID(), this.getName().getString());
            DragonCodexSavedData.get(serverLevel).updateDragonStats(this.getOwnerUUID(), this);
        }
    }

    @Override
    public void heal(float amount) {
        super.heal(amount);
        if (!level().isClientSide && this.isTame() && this.getOwnerUUID() != null) {
            ServerLevel serverLevel = (ServerLevel) level();
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
        dropAdditionalDeathLootAfterBase(source);
    }

    protected void dropAdditionalDeathLootAfterBase(@NotNull DamageSource source) {
    }

    @Override
    public @NotNull AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling().inflate(
                getCullingInflateX(),
                getCullingInflateY(),
                getCullingInflateZ());
    }

    protected double getCullingInflateX() {
        return 0.0D;
    }

    protected double getCullingInflateY() {
        return 0.0D;
    }

    protected double getCullingInflateZ() {
        return 0.0D;
    }

    @Nullable
    public Dragons getDragonType() {
        if (cachedDragonType == null) {
            cachedDragonType = Dragons.fromEntity(this);
        }
        return cachedDragonType;
    }

    public abstract DragonAbilityType<?, ?> getPrimaryAttackAbility();

    public DragonAbilityType<?, ?> getRoaringAbility() {
        return null;
    }
    public DragonAbilityType<?, ?> getChannelingAbility() {
        return null;
    }

    @Nullable
    public BlockState getEggBlockState() {
        Supplier<? extends Block> eggBlock = getEggBlock();
        return eggBlock == null ? null : eggBlock.get().defaultBlockState();
    }

    @Nullable
    protected Supplier<? extends Block> getEggBlock() {
        return null;
    }

    public void configureEggBlockEntity(BlockEntity blockEntity, @Nullable DragonEntity partner) {
        if (!(blockEntity instanceof AbstractDragonEggBlockEntity eggEntity)) {
            return;
        }

        java.util.UUID ownerUUID = resolveEggOwnerUUID(partner);
        if (ownerUUID != null) {
            eggEntity.setOwnerUUID(ownerUUID);
        }

        eggEntity.setBabyGender(this.getRandom().nextBoolean() ? DragonGender.FEMALE : DragonGender.MALE);
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

    protected void registerToOwnerCodex(@Nullable DragonEntity dragon, @Nullable ServerLevel level) {
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

    public boolean isDying() {
        return dying;
    }

    public void onDeathAbilityStarted() {

    }

    public int getDeathAnimationDurationTicks() {
        return 62;
    }

    public boolean isStayOrSitMuted() {
        return this.isOrderedToSit() || this.isInSittingPose();
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
        this.getNavigation().stop();
        if (shouldClearTargetOnSleepFreeze()) {
            this.setTarget(null);
        }
        this.setDeltaMovement(getSleepFreezeDeltaMovement());
    }

    protected boolean shouldClearTargetOnSleepFreeze() {
        return true;
    }

    protected Vec3 getSleepFreezeDeltaMovement() {
        return Vec3.ZERO;
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
            boolean canSleepDuringDay
    ) {
        public boolean canSleepDuringConditions(Level level) {
            boolean isDay = isNaturalDay(level);
            if (isDay && !canSleepDuringDay) return false;
            if (!isDay && !canSleepAtNight) return false;
            return true;
        }

        public static DragonSleepPreferences DIURNAL() {
            return new DragonSleepPreferences(false, true);
        }

        public static DragonSleepPreferences NOCTURNAL() {
            return new DragonSleepPreferences(true, false);
        }

        public static DragonSleepPreferences FLEXIBLE() {
            return new DragonSleepPreferences(true, true);
        }

        public static boolean isNaturalDay(Level level) {
            long dayTime = level.getDayTime() % 24000L;
            return dayTime < 12000L;
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

    protected abstract ResourceLocation getDragonAttributesId();

    public DragonAttributeConfig getConfiguredDragonAttributes() {
        return DragonAttributeConfigLoader.getInstance().getConfig(getDragonAttributesId());
    }

    public float getConfiguredAbilityDamage(String key, float fallback) {
        return (float) getConfiguredDragonAttributes().abilityDamage(key, fallback);
    }

    public double getConfiguredExtra(String key, double fallback) {
        return getConfiguredDragonAttributes().extraDouble(key, fallback);
    }

    public boolean getConfiguredExtraBoolean(String key, boolean fallback) {
        return getConfiguredDragonAttributes().extraBoolean(key, fallback);
    }

    protected void setAttributeBase(Attribute attribute, double value) {
        AttributeInstance instance = this.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    protected double configuredMaxHealth(DragonAttributeConfig config, double babyMaxHealth) {
        return isBaby() ? babyMaxHealth : config.maxHealth();
    }

    protected double configuredArmor(DragonAttributeConfig config, double babyArmor) {
        return isBaby() ? babyArmor : config.armor();
    }

    protected void applyConfiguredHealthAndArmor(DragonAttributeConfig config, double babyMaxHealth, double babyArmor) {
        setAttributeBase(Attributes.MAX_HEALTH, configuredMaxHealth(config, babyMaxHealth));
        setAttributeBase(Attributes.ARMOR, configuredArmor(config, babyArmor));
    }

    protected void applyConfiguredFlyingHealthAndArmor(DragonAttributeConfig config, double babyMaxHealth, double babyArmor) {
        applyConfiguredFlyingHealthAndArmor(config, babyMaxHealth, babyArmor, 0.0D);
    }

    protected void applyConfiguredFlyingHealthAndArmor(DragonAttributeConfig config, double babyMaxHealth, double babyArmor, double babyFlyingSpeed) {
        applyConfiguredHealthAndArmor(config, babyMaxHealth, babyArmor);
        setAttributeBase(Attributes.FLYING_SPEED, isBaby() ? babyFlyingSpeed : config.flyingSpeed());
    }

    protected void clampHealthToMax() {
        if (this.getHealth() > this.getMaxHealth()) {
            this.setHealth(this.getMaxHealth());
        }
    }

    protected float getBabyHitboxScale() {
        return 1.0F;
    }

    @Override
    public @NotNull EntityDimensions getDimensions(@NotNull Pose pose) {
        EntityDimensions baseDimensions = super.getDimensions(pose);
        float babyScale = getBabyHitboxScale();
        return isBaby() && babyScale != 1.0F ? baseDimensions.scale(babyScale) : baseDimensions;
    }

    public boolean areRiderControlsLocked() {
        return false;
    }
    public Vec3 getClientLocatorPosition(String locator) {
        return null;
    }
    public float maxSitTicks() {
        return 15.0F;
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

    public Player getRidingPlayer() {
        if (this.getControllingPassenger() instanceof Player player) {
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
        this.soundHandler.tick();
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
            tickClientRotationAnimationState();
        }
    }

    private void tickClientRotationAnimationState() {
        double bodyYawDelta = Mth.wrapDegrees(this.yBodyRot - this.yBodyRotO) * 2.0;
        fallbackYawVelocity.setTo(bodyYawDelta);
        fallbackYawVelocity.update(0.25f);

        if (this.isVehicle()) {
            fallbackBodyRotDeviation.setTo(0.0);
            fallbackBodyRotDeviation.update(0.25f);
            fallbackPitchDeviation.setTo(0.0);
            fallbackPitchDeviation.update(0.25f);
            return;
        }

        double headToBody = Mth.wrapDegrees(this.yHeadRot - this.yBodyRot) * 0.25;
        double pitchDelta = (this.getXRot() - this.xRotO) * 0.5f;

        fallbackBodyRotDeviation.setTo(headToBody);
        fallbackBodyRotDeviation.update(0.25f);

        fallbackPitchDeviation.setTo(pitchDelta);
        fallbackPitchDeviation.update(0.25f);
    }

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
                this.applyCommandState(command);
            }
            return;
        }
        commandComponent.setCommand(command);
    }

    public int getNextCommand() {
        int nextCommand = (getCommand() + 1) % 3;
        if (nextCommand == 1 && !canAcceptSitCommand()) {
            nextCommand = (nextCommand + 1) % 3;
        }
        return nextCommand;
    }

    public boolean canAcceptSitCommand() {
        return !isAirborneForSitCommandProtection();
    }

    protected boolean isAirborneForSitCommandProtection() {
        return !onGround() && !isInWaterOrBubble();
    }

    public void applyCommandState(int command) {
        switch (command) {
            case 0, 2 -> this.setOrderedToSit(false);
            case 1 -> this.setOrderedToSit(true);
            default -> {
            }
        }
    }

    public void refreshCommandState() {
        applyCommandState(getCommand());
    }

    public boolean canOwnerCommand(Player player) {
        return player != null && player.isCrouching() && this.isOwnedBy(player);
    }

    public boolean canOwnerMount(Player player) {
        if (this.isBaby()) {
            return false;
        }
        if (this.getHappiness() <= 30) {
            if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        ParticleTypes.ANGRY_VILLAGER,
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

    public boolean isAlly(Entity entity) {
        if (entity == null) return false;

        if (entity instanceof Player player) {
            return isPlayerAlly(player);
        }

        if (entity instanceof DragonEntity otherDragon && otherDragon.isTame()) {
            LivingEntity otherOwner = otherDragon.getOwner();
            return otherOwner instanceof Player otherPlayer && isPlayerAlly(otherPlayer);
        }

        if (entity instanceof TamableAnimal tamable && tamable.isTame()) {
            LivingEntity owner = tamable.getOwner();
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

    public boolean canTarget(Entity entity) {
        if (entity == null) return false;

        if (entity instanceof LivingEntity living && !isTargetValid(living)) {
            return false;
        }

        if (isAlly(entity)) {
            return false;
        }

        if (entity instanceof TamableAnimal tamable && tamable.isTame()) {
            LivingEntity owner = tamable.getOwner();
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
                int next = getNextCommand();
                setCommand(next);
                return InteractionResult.SUCCESS;
            }
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void handleEntityEvent(byte eventId) {
        if (eventId == 6) {
            spawnClientEventParticles(ParticleTypes.SMOKE);
            return;
        }
        if (eventId == 7) {
            spawnClientEventParticles(ParticleTypes.HEART);
            return;
        }
        super.handleEntityEvent(eventId);
    }

    private void spawnClientEventParticles(ParticleOptions particle) {
        if (!level().isClientSide) {
            return;
        }
        for (int i = 0; i < 7; ++i) {
            double d0 = this.random.nextGaussian() * 0.02D;
            double d1 = this.random.nextGaussian() * 0.02D;
            double d2 = this.random.nextGaussian() * 0.02D;
            this.level().addParticle(
                    particle,
                    this.getRandomX(1.0D),
                    this.getRandomY() + 0.5D,
                    this.getRandomZ(1.0D),
                    d0,
                    d1,
                    d2
            );
        }
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
        tag.putString("TextureVariantId", getTextureVariantId().toString());
        ResourceLocation pendingAdultVariantId = getPendingAdultTextureVariantId();
        if (pendingAdultVariantId != null) {
            tag.putString("PendingAdultTextureVariantId", pendingAdultVariantId.toString());
        }
        tag.putBoolean("BoundInBinder", this.boundInBinder);
        tag.putBoolean("GrowthStunted", this.growthStunted);
        if (assignedParentUuid != null) {
            tag.putUUID("AssignedParentUuid", assignedParentUuid);
        }
        if (hasPendingFamilyBabies()) {
            tag.putBoolean("FamilySpawnPending", true);
            tag.putInt("FamilySpawnCount", pendingFamilyBabyCount);
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
        if (tag.contains("TextureVariantId")) {
            setTextureVariantId(parseVariantId(tag.getString("TextureVariantId"), SaintsDragonVariantRegistry.defaultVariantId(getDragonVariantTypeId())));
        } else if (tag.contains("TextureVariant")) {
            setTextureVariant(tag.getInt("TextureVariant"));
        }
        if (tag.contains("PendingAdultTextureVariantId")) {
            setPendingAdultTextureVariantId(parseVariantId(tag.getString("PendingAdultTextureVariantId"), null));
        } else if (tag.contains("PendingAdultTextureVariant")) {
            setPendingAdultTextureVariant(tag.getInt("PendingAdultTextureVariant"));
        }
        this.assignedParentUuid = tag.hasUUID("AssignedParentUuid") ? tag.getUUID("AssignedParentUuid") : null;
        this.boundInBinder = tag.getBoolean("BoundInBinder");
        this.growthStunted = tag.getBoolean("GrowthStunted");
        if (this.growthStunted && !this.isBaby()) {
            this.setAge(-24000);
            this.setBaby(true);
        }
        if (tag.contains("FamilySpawnPending")) {
            this.familySpawnPending = tag.getBoolean("FamilySpawnPending");
            this.pendingFamilyBabyCount = Math.max(0, tag.getInt("FamilySpawnCount"));
        } else {
            this.familySpawnPending = false;
            this.pendingFamilyBabyCount = 0;
        }
        allyManager.loadFromNBT(tag);
    }

    private void applyHappinessHitPenalty(ServerLevel serverLevel) {
        if (happinessComponent != null) {
            happinessComponent.applyHitPenalty(serverLevel);
        }
    }


    @Override
    public void travel(@NotNull Vec3 travelVector) {
        if (this.isOrderedToSit() && !this.isVehicle() && !this.isPassenger()) {
            this.setDeltaMovement(Vec3.ZERO);
            super.travel(Vec3.ZERO);
            return;
        }
        super.travel(travelVector);
    }

    @Override
    public void setAge(int age) {
        if (this.growthStunted) {
            int currentAge = this.getAge();
            if (!this.isBaby() || currentAge >= 0) {
                age = Math.min(age, -24000);
            } else if (age > currentAge) {
                age = currentAge;
            }
        }

        boolean wasBaby = this.isBaby();
        super.setAge(age);
        boolean isNowBaby = this.isBaby();
        boolean isInitialLoad = !wasBaby && age < 0 && this.tickCount == 0;

        if (wasBaby != isNowBaby && !level().isClientSide && !isRespawning && this.getId() != -1 && skipRespawnTicks == 0 && !isInitialLoad) {
            if (wasBaby && !isNowBaby) {
                applyPendingAdultTextureVariant();
            }
            isRespawning = true;
            Level world = this.level();

            double posX = this.getX();
            double posY = this.getY();
            double posZ = this.getZ();
            float yaw = this.getYRot();
            float pitch = this.getXRot();
            java.util.UUID oldUUID = this.getUUID();
            CompoundTag nbt = new CompoundTag();
            this.saveWithoutId(nbt);
            nbt.putBoolean("IsRespawning", true);
            nbt.putInt("Age", age);
            nbt.putBoolean("GenderInitialized", this.genderComponent != null && this.genderComponent.isInitialized());
            @SuppressWarnings("unchecked")
            DragonEntity newEntity = (DragonEntity) this.getType().create(world);
            if (newEntity != null) {
                newEntity.load(nbt);
                newEntity.setPos(posX, posY, posZ);
                newEntity.setYRot(yaw);
                newEntity.setXRot(pitch);
                newEntity.isRespawning = false;
                this.discard();
                newEntity.setUUID(oldUUID);
                world.addFreshEntity(newEntity);
                world.broadcastEntityEvent(newEntity, (byte) 7);
            }
        }
    }

    @Override
    public void setBaby(boolean baby) {
        if (this.growthStunted && !baby) {
            return;
        }
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

    public boolean isGrowthStunted() {
        return growthStunted;
    }

    public void setGrowthStunted(boolean growthStunted) {
        if (this.growthStunted == growthStunted) {
            return;
        }
        this.growthStunted = growthStunted;
        if (growthStunted) {
            this.setAge(-24000);
            this.setBaby(true);
        }
    }


    @Override
    public void spawnChildFromBreeding(@NotNull ServerLevel level, Animal otherParent) {
        AgeableMob baby = this.getBreedOffspring(level, otherParent);
        if (baby != null) {
           BlockPos safePos = findSafeBabySpawnPos(level, this.blockPosition());
            baby.setBaby(true);
            baby.moveTo(this.getX(), safePos != null ? safePos.getY() : this.getY(), this.getZ(), 0.0F, 0.0F);
            level.addFreshEntityWithPassengers(baby);
        }
    }

    @Nullable
    protected BlockPos findSafeBabySpawnPos(LevelAccessor level, BlockPos start) {
        if (level == null || start == null) return null;
        BlockPos.MutableBlockPos cursor = start.mutable();
        int minY = level.getMinBuildHeight();

        while (cursor.getY() >= minY) {
            BlockState state = level.getBlockState(cursor);
            if (isStableBabyLandingSurface(level, cursor, state)) {
                BlockPos above = cursor.above();
                BlockState aboveState = level.getBlockState(above);
                if (aboveState.getCollisionShape(level, above).isEmpty() && aboveState.getFluidState().isEmpty()) {
                    return above;
                }
            }
            cursor.move(Direction.DOWN);
        }
        return null;
    }

    private boolean isStableBabyLandingSurface(BlockGetter level, BlockPos pos,
                                               BlockState state) {
        if (state.isAir() || !state.getFluidState().isEmpty()) {
            return false;
        }
        return state.isSolidRender(level, pos) || state.isFaceSturdy(level, pos, Direction.UP);
    }
}

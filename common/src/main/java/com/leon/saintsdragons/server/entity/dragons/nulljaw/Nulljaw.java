package com.leon.saintsdragons.server.entity.dragons.nulljaw;

import com.mojang.serialization.Dynamic;
import com.leon.saintsdragons.util.animation.AnimationHelper;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.common.registry.ModAbilities;
import com.leon.saintsdragons.common.registry.ModTags;
import com.leon.saintsdragons.common.network.DragonRiderAction;
import com.leon.saintsdragons.common.network.MessageMountedTeleport;
import com.leon.saintsdragons.common.network.NetworkHandler;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrain;
import com.leon.saintsdragons.server.ai.dragonbrain.profiles.NulljawBrain;
import com.leon.saintsdragons.server.ai.navigation.async.AsyncFlightController;
import com.leon.saintsdragons.server.ai.navigation.async.AsyncFlyingPathNavigation;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.base.RideableFlyingDragon;
import com.leon.saintsdragons.server.entity.dragons.handlers.DragonBreedingInteractionHelper;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.handlers.NulljawAnimationHandler;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.handlers.NulljawSoundProfile;
import com.leon.saintsdragons.server.entity.interfaces.DragonMovementCapability;
import com.leon.saintsdragons.server.entity.interfaces.PackMember;
import com.leon.saintsdragons.server.entity.interfaces.DragonSoundProfile;
import com.leon.saintsdragons.server.flight.DragonFlightVisuals;
import com.leon.saintsdragons.server.flight.DragonRiderSeat;
import com.leon.saintsdragons.server.world.DragonSpawnRules;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Nulljaw extends RideableFlyingDragon implements PackMember<Nulljaw> {
    private static final NulljawBrain DRAGON_BRAIN = new NulljawBrain();

    @Override
    protected ResourceLocation getDragonAttributesId() {
        return DragonAttributeConfigLoader.NULLJAW_ID;
    }

    private static final double NATURAL_SPAWN_NULLJAW_RADIUS = 96.0D;
    private static final int MAX_NEARBY_WILD_NULLJAWS = 4;
    public static final double BREED_PARTNER_RANGE = 24.0D;
    public static final double BREED_DISTANCE_SQR = 9.0D;
    private static final double CARRIED_HITBOX_DOWNWARD_EXTENSION = 1.65D;
    private static final double CARRIED_COLLISION_ESCAPE_LIFT = 0.20D;
    private static final int MAX_CARRIED_RIDER_ESCAPE_LIFTS = 8;
    private static final int MIN_AMBIENT_DELAY = 220;
    private static final int MAX_AMBIENT_DELAY = 420;
    private static final int TAME_CHANCE_DENOMINATOR = 5;
    private static final int DEATH_SOUND_DURATION_TICKS = 44;
    private static final double RIDER_FLIGHT_SPEED = 0.32D;
    private static final double RIDER_ASCEND_SPEED = 0.16D;
    private static final double RIDER_DESCEND_SPEED = 0.18D;
    private static final double FORWARD_TELEPORT_DISTANCE = 16.0D;
    private static final double FORWARD_TELEPORT_SAMPLE_STEP = 0.5D;
    private static final double MIN_FORWARD_TELEPORT_DISTANCE = 1.0D;
    private static final double GROUND_CLEARANCE_LIFT = 0.08D;
    private static final double BABY_MAX_HEALTH = 70.0D;
    private static final double BABY_ARMOR = 4.0D;
    private static final float BABY_HITBOX_SCALE = 0.55F;
    private static final String CLOAK_ABILITY_ID = "nulljaw_cloak";
    private static final String CLOAK_TICKS_TAG = "NulljawCloakTicks";
    private static final String CLOAK_RIDER_TAG = "NulljawCloakRider";
    private static final int DEFAULT_CLOAK_DURATION_TICKS = 20 * 60 * 5;
    private static final int MAX_PACK_SIZE = 4;
    private static final double PACK_SEARCH_RADIUS = 28.0D;
    private static final EntityDataAccessor<Float> DATA_FLIGHT_PITCH =
            SynchedEntityData.defineId(Nulljaw.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_FEEDING_COOLDOWN =
            SynchedEntityData.defineId(Nulljaw.class, EntityDataSerializers.INT);

    private static final Map<String, VocalEntry> VOCAL_ENTRIES = new VocalEntryBuilder()
            .add("grumble1", AnimationHelper.VOCAL_CONTROLLER, "animation.nulljaw.grumble1", ModSounds.NULLJAW_GRUMBLE_1, 1.0f, 1.0f, 0.08f, true, true, false)
            .add("grumble2", AnimationHelper.VOCAL_CONTROLLER, "animation.nulljaw.grumble2", ModSounds.NULLJAW_GRUMBLE_2, 1.0f, 1.0f, 0.08f, true, true, false)
            .add("grumble3", AnimationHelper.VOCAL_CONTROLLER, "animation.nulljaw.grumble3", ModSounds.NULLJAW_GRUMBLE_3, 1.0f, 1.0f, 0.08f, true, true, false)
            .add("eat", "actions", "animation.nulljaw.eat", ModSounds.NULLJAW_EAT, 1.0f, 1.0f, 0.02f, true, true, false)
            .add("nulljaw_hurt", "instant", "animation.nulljaw.hurt", ModSounds.NULLJAW_HURT, 1.1f, 1.0f, 0.04f, true, true, true)
            .add("nulljaw_die", "instant", "animation.nulljaw.die", ModSounds.NULLJAW_DIE, 1.2f, 1.0f, 0.0f, true, true, true)
            .build();

    private final AnimatableInstanceCache dragonCache = GeckoLibUtil.createInstanceCache(this);
    private final NulljawAnimationHandler animationHandler = new NulljawAnimationHandler(this);
    private final AnimationController<Nulljaw> movementController;
    private final AnimationController<Nulljaw> actionController;
    private final AnimationController<Nulljaw> mountedController;
    private final AnimationController<Nulljaw> instantController;
    private final AnimationController<Nulljaw> vocalController;
    private final AnimationController<Nulljaw> interactionController;
    private final DragonFlightVisuals.State flightVisualState = new DragonFlightVisuals.State();

    @Nullable
    private UUID packLeaderUuid;
    @Nullable
    private UUID combatFormationLeaderUuid;
    private String combatFormationPhase = "NONE";
    private int combatFormationSlot = -1;
    private int combatFormationSize;
    private boolean combatAttackReserved;
    private boolean running;
    private boolean deathSoundQueued;
    private int cloakTicksRemaining;
    @Nullable
    private UUID cloakedRiderUuid;
    public Nulljaw(EntityType<? extends Nulljaw> type, Level level) {
        super(type, level);
        this.movementController = new AnimationController<>(this, "movement", 2, animationHandler::movementPredicate);
        this.actionController = new AnimationController<>(this, "actions", 2, animationHandler::actionPredicate);
        this.mountedController = new AnimationController<>(this, "mounted", 2, animationHandler::mountedPredicate);
        this.instantController = new AnimationController<>(this, "instant", 1, animationHandler::instantPredicate);
        this.vocalController = new AnimationController<>(this, AnimationHelper.VOCAL_CONTROLLER, 2, AnimationHelper::vocalIdle);
        this.interactionController = new AnimationController<>(this, AnimationHelper.INTERACTION_CONTROLLER, 1, AnimationHelper::interactionIdle);
        setupAnimationControllers();
        this.setFlying(true);
        this.setHovering(false);
        this.setTakeoff(false);
        this.setLanding(false);
        this.setNoGravity(true);
        this.switchToAirNavigation();
        seedAmbientSoundTimer(MIN_AMBIENT_DELAY, MAX_AMBIENT_DELAY, 80);
    }

    @Override
    public EnumSet<DragonMovementCapability> movementCapabilities() {
        return EnumSet.of(DragonMovementCapability.FLY);
    }

    @Override
    protected boolean breaksWaterliliesOnContact() {
        return false;
    }

    public static AttributeSupplier.Builder createAttributes() {
        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance().getConfig(DragonAttributeConfigLoader.NULLJAW_ID);
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, config.maxHealth())
                .add(Attributes.MOVEMENT_SPEED, 0.20D)
                .add(Attributes.FLYING_SPEED, config.flyingSpeed())
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.ARMOR, config.armor());
    }

    public void applyConfiguredAttributes() {
        DragonAttributeConfig config = getConfiguredDragonAttributes();
        applyConfiguredFlyingHealthAndArmor(
                config,
                BABY_MAX_HEALTH,
                BABY_ARMOR,
                config.flyingSpeed()
        );

        clampHealthToMax();
    }

    @Override
    public @NotNull SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor level,
                                                 @NotNull DifficultyInstance difficulty,
                                                 @NotNull MobSpawnType spawnReason,
                                                 @Nullable SpawnGroupData spawnData,
                                                 @Nullable CompoundTag dataTag) {
        spawnData = super.finalizeSpawn(level, difficulty, spawnReason, spawnData, dataTag);
        applyConfiguredAttributes();
        this.setHealth(this.getMaxHealth());
        return spawnData;
    }

    public static boolean canSpawnHere(EntityType<? extends Nulljaw> type,
                                       LevelAccessor level,
                                       MobSpawnType spawnType,
                                       BlockPos pos,
                                       RandomSource random) {
        if (DragonSpawnRules.isNaturalWildSpawn(spawnType)
                && !level.getBiome(pos).is(Biomes.END_BARRENS)) {
            return false;
        }
        boolean peaceful = level.getDifficulty() == Difficulty.PEACEFUL;
        if (peaceful) {
            return false;
        }
        boolean darkEnough = true;
        if (level instanceof ServerLevelAccessor serverLevel) {
            darkEnough = Monster.isDarkEnoughToSpawn(serverLevel, pos, random);
            if (!darkEnough) {
                return false;
            }
        }
        FluidState fluidAt = level.getFluidState(pos);
        FluidState fluidAbove = level.getFluidState(pos.above());
        if (!fluidAt.isEmpty() || !fluidAbove.isEmpty()) {
            return false;
        }
        BlockState stateAt = level.getBlockState(pos);
        BlockState stateAbove = level.getBlockState(pos.above());
        if (!stateAt.getCollisionShape(level, pos).isEmpty()
                || !stateAbove.getCollisionShape(level, pos.above()).isEmpty()) {
            return false;
        }

        return passesNulljawLocalSpawnCap(level, spawnType, pos);
    }

    private static boolean passesNulljawLocalSpawnCap(LevelAccessor level, MobSpawnType spawnType, BlockPos pos) {
        if (!(level instanceof ServerLevelAccessor serverLevelAccessor)) {
            return true;
        }
        if (!DragonSpawnRules.isNaturalWildSpawn(spawnType)) {
            return true;
        }

        AABB nearbyBounds = AABB.ofSize(
                Vec3.atCenterOf(pos),
                NATURAL_SPAWN_NULLJAW_RADIUS * 2.0D,
                NATURAL_SPAWN_NULLJAW_RADIUS * 2.0D,
                NATURAL_SPAWN_NULLJAW_RADIUS * 2.0D
        );
        int nearbyWildNulljaws = serverLevelAccessor.getLevel().getEntitiesOfClass(
                Nulljaw.class,
                nearbyBounds,
                nulljaw -> nulljaw.isAlive() && !nulljaw.isTame()
        ).size();
        return nearbyWildNulljaws <= MAX_NEARBY_WILD_NULLJAWS;
    }

    @Override
    public boolean checkSpawnRules(LevelAccessor level, MobSpawnType spawnType) {
        @SuppressWarnings("unchecked")
        EntityType<? extends Nulljaw> nulljawType = (EntityType<? extends Nulljaw>) this.getType();
        return canSpawnHere(nulljawType, level, spawnType, this.blockPosition(), this.getRandom());
    }

    @Override
    public boolean checkSpawnObstruction(LevelReader level) {
        return level.noCollision(this);
    }

    @Override
    public float getWalkTargetValue(@NotNull BlockPos pos, @NotNull LevelReader level) {
        return level.getBlockState(pos).isAir() && level.getFluidState(pos).isEmpty() ? 10.0F : 0.0F;
    }

    @Override
    protected FlyingPathNavigation createAirNavigation(Level level, AsyncFlightController controller) {
        return new AsyncFlyingPathNavigation(this, level, controller) {
            @Override
            public boolean isStableDestination(@NotNull BlockPos pos) {
                return this.level.hasChunkAt(pos)
                        && this.level.getBlockState(pos).isAir()
                        && this.level.getFluidState(pos).isEmpty();
            }
        };
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.cloakTicksRemaining = Math.max(0, tag.getInt(CLOAK_TICKS_TAG));
        this.cloakedRiderUuid = tag.hasUUID(CLOAK_RIDER_TAG) ? tag.getUUID(CLOAK_RIDER_TAG) : null;
        applyConfiguredAttributes();
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.cloakTicksRemaining > 0) {
            tag.putInt(CLOAK_TICKS_TAG, this.cloakTicksRemaining);
            if (this.cloakedRiderUuid != null) {
                tag.putUUID(CLOAK_RIDER_TAG, this.cloakedRiderUuid);
            }
        }
    }

    @Override
    protected void defineRideableDragonData() {
        this.entityData.define(DATA_FLIGHT_PITCH, 0f);
        this.entityData.define(DATA_FEEDING_COOLDOWN, 0);
    }

    @Override
    protected Brain.Provider<Nulljaw> brainProvider() {
        return DRAGON_BRAIN.brainProvider();
    }

    @Override
    protected Brain<?> makeBrain(Dynamic<?> dynamic) {
        return DragonBrain.makeBrain(DRAGON_BRAIN, dynamic);
    }

    @Override
    protected void customServerAiStep() {
        DragonBrain.tick(DRAGON_BRAIN, this);
        super.customServerAiStep();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(movementController, vocalController, actionController, mountedController, instantController, interactionController);
    }

    private void setupAnimationControllers() {
        animationHandler.setupActionController(actionController);
        animationHandler.setupInstantController(instantController);
        AnimationHelper.registerGrumbles(vocalController, this);
        animationHandler.setupInteractionController(interactionController);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return dragonCache;
    }

    @Override
    public DragonSoundProfile getSoundProfile() {
        return NulljawSoundProfile.INSTANCE;
    }

    @Override
    public Map<String, VocalEntry> getVocalEntries() {
        return VOCAL_ENTRIES;
    }

    public boolean isActuallyHovering() {
        return this.getDeltaMovement().lengthSqr() > 0.0015D;
    }

    public boolean isMovingForAnimation() {
        return this.getDeltaMovement().lengthSqr() > 0.003D;
    }

    public boolean canFloatWander() {
        if (!this.isAlive()
                || this.isOrderedToSit()
                || this.isSittingDownAnimation()
                || this.isVehicle()
                || this.isPassenger()
                || (this.getTarget() != null && this.getTarget().isAlive())) {
            return false;
        }
        if (this.isTame() && this.getCommand() == 1) {
            return false;
        }
        LivingEntity owner = this.getOwner();
        return owner == null || this.getCommand() != 0 || this.distanceToSqr(owner) <= 64.0D;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        tickStandardPitchingLogic();
        nudgeUpIfCarriedRiderInWall();
        if (!this.level().isClientSide) {
            this.setFlying(true);
            this.setTakeoff(false);
            this.setLanding(false);
            this.setNoGravity(true);
            if (!this.isUsingAirNavigation()) {
                this.switchToAirNavigation();
            }
            if (this.entityData.get(DATA_FEEDING_COOLDOWN) > 0) {
                this.entityData.set(DATA_FEEDING_COOLDOWN, this.entityData.get(DATA_FEEDING_COOLDOWN) - 1);
            }
            this.tickAsyncFlightNavigation();
            this.maintainGroundClearance();
            this.tickAmbientVocals();
            this.tickCloak();
        }
        if (!this.level().isClientSide) {
            this.tickAnimationStates();
        }
    }

    @Override
    protected DragonFlightVisuals.State getFlightVisualState() {
        return this.flightVisualState;
    }

    @Override
    protected EntityDataAccessor<Float> getFlightPitchAccessor() {
        return DATA_FLIGHT_PITCH;
    }

    @Override
    protected boolean shouldSpawnFlightDustEffects() {
        return false;
    }

    private void tickAmbientVocals() {
        if (this.isDeadOrDying() || this.isVehicle()) {
            return;
        }
        tickAmbientVocalSounds(MIN_AMBIENT_DELAY, MAX_AMBIENT_DELAY,
                () -> selectWeightedAmbientVocal("grumble1", 0.34f, "grumble2", 0.67f, "grumble3"));
    }

    private void maintainGroundClearance() {
        if (!this.isAlive() || !this.onGround() || this.isInWaterOrBubble() || this.isInLava()) {
            return;
        }

        Vec3 velocity = this.getDeltaMovement();
        if (velocity.y < GROUND_CLEARANCE_LIFT) {
            this.setDeltaMovement(velocity.x, GROUND_CLEARANCE_LIFT, velocity.z);
            this.hasImpulse = true;
        }
    }

    @Override
    public void travel(@NotNull Vec3 motion) {
        if (areRiderControlsLocked()) {
            super.travel(Vec3.ZERO);
            return;
        }

        if (this.isVehicle() && this.getControllingPassenger() instanceof Player rider
                && this.isTame() && this.isOwnedBy(rider)) {
            if (!this.getNavigation().isDone()) {
                this.getNavigation().stop();
            }
            super.travel(Vec3.ZERO);
            return;
        }

        super.travel(motion);
    }

    @Override
    protected void tickRidden(@NotNull Player player, @NotNull Vec3 travelVector) {
        super.tickRidden(player, travelVector);

        if (areRiderControlsLocked()) {
            player.fallDistance = 0.0F;
            this.fallDistance = 0.0F;
            this.setTarget(null);
            copyRiderYaw(player);
            this.setAccelerating(false);
            this.setGoingUp(false);
            this.setGoingDown(false);
            this.setDeltaMovement(Vec3.ZERO);
            return;
        }

        handleRiderFlight(player);
    }

    private void handleRiderFlight(Player rider) {
        float forward = rider.zza;
        float strafe = rider.xxa;
        if (forward < 0.0F) {
            forward *= 0.5F;
        }

        float currentYaw = this.getYRot();
        float targetYaw = rider.getYRot();
        float rawDiff = Mth.wrapDegrees(targetYaw - currentYaw);
        float yaw = currentYaw + (rawDiff * 0.35F);

        this.yRotO = currentYaw;
        this.yBodyRotO = this.yBodyRot;
        this.yHeadRotO = this.yHeadRot;
        this.xRotO = this.getXRot();
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.setYHeadRot(yaw);
        this.setXRot(0.0F);

        float yawRad = yaw * Mth.DEG_TO_RAD;
        float pitchRad = rider.getXRot() * Mth.DEG_TO_RAD;
        Vec3 forwardVec = new Vec3(-Mth.sin(yawRad) * Mth.cos(pitchRad), -Mth.sin(pitchRad), Mth.cos(yawRad) * Mth.cos(pitchRad));
        Vec3 strafeVec = new Vec3(Mth.cos(yawRad), 0.0D, Mth.sin(yawRad));
        Vec3 desired = forwardVec.scale(forward).add(strafeVec.scale(strafe * 0.55D));
        if (desired.lengthSqr() > 1.0E-6D) {
            desired = desired.normalize().scale(RIDER_FLIGHT_SPEED * (this.isAccelerating() ? 1.35D : 1.0D));
        }

        double vertical = desired.y;
        if (this.isGoingUp()) {
            vertical += RIDER_ASCEND_SPEED;
        } else if (this.isGoingDown()) {
            vertical -= RIDER_DESCEND_SPEED;
        }

        Vec3 blended = this.getDeltaMovement().add(desired.subtract(this.getDeltaMovement()).scale(0.25D));
        blended = new Vec3(blended.x * 0.96D, Mth.clamp(vertical, -0.45D, 0.45D), blended.z * 0.96D);
        blended = resolveMountedFlightCollision(blended);

        this.setSpeed((float) RIDER_FLIGHT_SPEED);
        this.move(net.minecraft.world.entity.MoverType.SELF, blended);
        this.setDeltaMovement(blended);
        this.hasImpulse = true;
    }

    @Override
    public @NotNull Vec3 getRiddenInput(@NotNull Player player, @NotNull Vec3 deltaIn) {
        Vec3 input = new Vec3(player.xxa * 0.55F, 0.0D, player.zza);
        return super.getRiddenInput(player, input);
    }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        return this.getFirstPassenger() instanceof LivingEntity living ? living : null;
    }

    @Override
    public double getPassengersRidingOffset() {
        return -CARRIED_HITBOX_DOWNWARD_EXTENSION;
    }

    @Override
    protected void positionRider(@NotNull Entity passenger, @NotNull Entity.MoveFunction moveFunction) {
        DragonRiderSeat.positionLocatorRider(
                this,
                passenger,
                moveFunction,
                getPassengersRidingOffset(),
                this.level().isClientSide ? this.getClientLocatorPosition("passengerLocator") : null,
                getPassengersRidingOffset()
        );
    }

    @Override
    public @NotNull Vec3 getDismountLocationForPassenger(@NotNull LivingEntity passenger) {
        return DragonRiderSeat.findRadialGroundDismount(
                passenger,
                this,
                new double[]{2.5D, 3.5D, 1.75D},
                new int[]{0, 30, -30, 60, -60, 90, -90, 180},
                6,
                2.0D
        );
    }

    @Override
    public @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        awardDragonEncounterAdvancement(player);
        ItemStack heldItem = player.getItemInHand(hand);
        if (ModItems.isDragonBrush(heldItem)) {
            if (!this.level().isClientSide) {
                this.tryBrush(player, heldItem);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if (ModItems.isScalePlucker(heldItem)) {
            if (!this.level().isClientSide) {
                this.tryPluckScale(player, heldItem);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if (player.getVehicle() == this) {
            return InteractionResult.PASS;
        }

        var baby = this.getBabyComponent();
        if (baby != null) {
            InteractionResult growthStuntResult = baby.tryStuntGrowth(
                    player,
                    heldItem,
                    "entity.saintsdragons.nulljaw",
                    this.canFeed(),
                    24,
                    () -> {
                        this.triggerAnim("interaction", "eat");
                        this.playEatMovingSound();
                    },
                    this::setFeedingCooldown
            );
            if (growthStuntResult != InteractionResult.PASS) {
                return growthStuntResult;
            }
        }

        if (heldItem.is(ModTags.Items.NULLJAW_FOODS)) {
            if (this.isTame()) {
                if (!this.canReceiveFoodFrom(player)) {
                    return InteractionResult.PASS;
                }

                if (player.isCrouching()) {
                    return handleBreeding(player, heldItem);
                }

                if (!canFeed()) {
                    if (!this.level().isClientSide && player instanceof ServerPlayer serverPlayer) {
                        serverPlayer.displayClientMessage(
                                Component.translatable("entity.saintsdragons.nulljaw.still_eating", this.getName()),
                                true
                        );
                    }
                    return InteractionResult.sidedSuccess(this.level().isClientSide);
                }

                if (!this.level().isClientSide) {
                    if (!player.getAbilities().instabuild) {
                        heldItem.shrink(1);
                    }
                    this.triggerAnim("interaction", "eat");
                    this.playEatMovingSound();
                    this.setFeedingCooldown(24);

                    if (this.isBaby() && baby != null) {
                        baby.applyBabyGrowth(player, false, "entity.saintsdragons.nulljaw", 2400, 4800);
                    } else {
                        boolean wasHungry = this.isHungry();
                        float newHealth = Math.min(this.getHealth() + 6.0F, this.getMaxHealth());
                        this.setHealth(newHealth);
                        this.applyFeedingHunger(false);
                        this.level().broadcastEntityEvent(this, (byte) 7);

                        if (player instanceof ServerPlayer serverPlayer) {
                            String messageKey = newHealth >= this.getMaxHealth()
                                    ? (wasHungry ? "entity.saintsdragons.dragon.feeding" : "entity.saintsdragons.nulljaw.fed")
                                    : "entity.saintsdragons.nulljaw.fed_partial";
                            serverPlayer.displayClientMessage(Component.translatable(messageKey, this.getName()), true);
                        }
                    }
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }

            if (!this.level().isClientSide) {
                if (!player.getAbilities().instabuild) {
                    heldItem.shrink(1);
                }
                this.triggerAnim("interaction", "eat");
                this.playEatMovingSound();

                if (!this.isTame()) {
                    if (this.getRandom().nextInt(TAME_CHANCE_DENOMINATOR) == 0) {
                        this.tame(player);
                        this.navigation.stop();
                        this.setTarget(null);
                        this.level().broadcastEntityEvent(this, (byte) 7);
                        if (player instanceof ServerPlayer serverPlayer) {
                            var advancement = serverPlayer.server.getAdvancements()
                                    .getAdvancement(SaintsDragonsCommon.rl("tame_nulljaw"));
                            if (advancement != null) {
                                serverPlayer.getAdvancements().award(advancement, "tame_nulljaw");
                            }
                        }
                    } else {
                        this.level().broadcastEntityEvent(this, (byte) 6);
                    }
                } else {
                    this.heal(6.0F);
                }
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        if (this.isTame() && this.isOwnedBy(player)) {
            if (heldItem.is(ModItems.NULLJAW_BINDER.get())) {
                return InteractionResult.PASS;
            }

            if (player.isCrouching() && hand == InteractionHand.MAIN_HAND && heldItem.isEmpty()) {
                if (!this.level().isClientSide) {
                    int next = this.getNextCommand();
                    this.setCommand(next);
                    if (player instanceof ServerPlayer serverPlayer) {
                        serverPlayer.displayClientMessage(
                                Component.translatable("entity.saintsdragons.all.command_" + next, this.getName()),
                                true
                        );
                    }
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }

            if (heldItem.is(ModItems.DRACONIC_CODEX.get())) {
                return InteractionResult.PASS;
            }

            if (!player.isCrouching() && hand == InteractionHand.MAIN_HAND) {
                if (this.isBaby() || !this.canOwnerMount(player)) {
                    return InteractionResult.sidedSuccess(this.level().isClientSide);
                }
                if (!this.level().isClientSide) {
                    if (player.startRiding(this)) {
                        nudgeUpIfCarriedRiderInWall();
                    }
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
        }

        return super.mobInteract(player, hand);
    }

    private InteractionResult handleBreeding(Player player, ItemStack food) {
        var baby = this.getBabyComponent();
        if (baby != null && !baby.ensureCanFeed(player, "entity.saintsdragons.nulljaw", this.canFeed())) {
            return InteractionResult.CONSUME;
        }

        return DragonBreedingInteractionHelper.handleBreeding(
                this,
                player,
                food,
                this::canFeed,
                "entity.saintsdragons.nulljaw.still_eating",
                24,
                () -> {
                    this.triggerAnim("interaction", "eat");
                    this.playEatMovingSound();
                },
                this::setFeedingCooldown
        );
    }

    @Override
    public boolean isFood(@NotNull ItemStack stack) {
        return stack.is(ModTags.Items.NULLJAW_FOODS);
    }

    @Override
    public boolean isBrushingAvailable() {
        return false;
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        rememberIncomingProjectile(source);
        if (source.getDirectEntity() instanceof ShulkerBullet) {
            return false;
        }
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide && source.getEntity() instanceof LivingEntity attacker) {
            alertNearbyNulljaws(attacker);
        }
        return hurt;
    }

    private void alertNearbyNulljaws(LivingEntity attacker) {
        if (!isValidRetaliationTarget(attacker)) {
            return;
        }

        if (this.isTame()) {
            assignRetaliationTarget(this, attacker, this.getUUID());
            return;
        }

        AABB alertBounds = this.getBoundingBox().inflate(PACK_SEARCH_RADIUS);
        List<Nulljaw> responders = new ArrayList<>();
        responders.add(this);
        responders.addAll(this.level().getEntitiesOfClass(
                Nulljaw.class,
                alertBounds,
                nulljaw -> nulljaw != this && !nulljaw.isBaby() && !nulljaw.isTame() && nulljaw.isAlive()
        ));
        responders.removeIf(nulljaw -> !nulljaw.isValidRetaliationTarget(attacker));
        responders.sort((first, second) -> compareUuid(first.getUUID(), second.getUUID()));

        int squadSize = Math.max(1, this.getMaxPackSize());
        for (int start = 0; start < responders.size(); start += squadSize) {
            List<Nulljaw> squad = responders.subList(start, Math.min(start + squadSize, responders.size()));
            UUID leaderUuid = chooseCombatFormationLeader(squad).getUUID();
            for (Nulljaw responder : squad) {
                assignRetaliationTarget(responder, attacker, leaderUuid);
            }
        }
    }

    private static Nulljaw chooseCombatFormationLeader(List<Nulljaw> squad) {
        return squad.stream()
                .min((first, second) -> {
                    int firstFollowers = referencedFollowerCount(squad, first.getUUID());
                    int secondFollowers = referencedFollowerCount(squad, second.getUUID());
                    if (firstFollowers != secondFollowers) {
                        return Integer.compare(secondFollowers, firstFollowers);
                    }
                    if (first.canLeadPack() != second.canLeadPack()) {
                        return first.canLeadPack() ? -1 : 1;
                    }
                    int priority = Integer.compare(
                            second.getPackLeadershipPriority(),
                            first.getPackLeadershipPriority()
                    );
                    return priority != 0 ? priority : compareUuid(first.getUUID(), second.getUUID());
                })
                .orElseThrow();
    }

    private static int referencedFollowerCount(List<Nulljaw> squad, UUID candidateUuid) {
        int followers = 0;
        for (Nulljaw member : squad) {
            if (candidateUuid.equals(member.getPackLeaderUuid())) {
                followers++;
            }
        }
        return followers;
    }

    private static int compareUuid(UUID first, UUID second) {
        int most = Long.compareUnsigned(first.getMostSignificantBits(), second.getMostSignificantBits());
        return most != 0
                ? most
                : Long.compareUnsigned(first.getLeastSignificantBits(), second.getLeastSignificantBits());
    }

    private boolean isValidRetaliationTarget(LivingEntity attacker) {
        if (this.isBaby()
                || attacker == this
                || attacker instanceof Nulljaw
                || !attacker.isAlive()
                || this.isAlly(attacker)) {
            return false;
        }
        if (attacker instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return false;
        }
        return !this.isTame() || attacker != this.getOwner();
    }

    private static void assignRetaliationTarget(Nulljaw nulljaw,
                                                LivingEntity attacker,
                                                UUID combatLeaderUuid) {
        nulljaw.setCombatFormationLeaderUuid(combatLeaderUuid);
        nulljaw.setLastHurtByMob(attacker);
        nulljaw.setTarget(attacker);
        nulljaw.beginAiFlight();
    }

    @Override
    public void die(@NotNull DamageSource cause) {
        if (!this.dead && !this.level().isClientSide && !deathSoundQueued) {
            deathSoundQueued = true;
            this.triggerAnim(AnimationHelper.INTERACTION_CONTROLLER, "nulljaw_die");
            VocalEntry deathEntry = VOCAL_ENTRIES.get("nulljaw_die");
            if (deathEntry != null && deathEntry.soundSupplier() != null) {
                float pitch = deathEntry.basePitch();
                if (deathEntry.pitchVariance() != 0.0F) {
                    pitch += (this.getRandom().nextFloat() - 0.5F) * deathEntry.pitchVariance() * 2.0F;
                }
                this.playSound(deathEntry.soundSupplier().get(), deathEntry.volume(), pitch);
            }
        }
        if (!this.level().isClientSide) {
            deactivateCloak();
        }
        super.die(cause);
    }

    @Override
    public void removePassenger(@NotNull Entity passenger) {
        if (!this.level().isClientSide
                && this.cloakTicksRemaining > 0
                && passenger.getUUID().equals(this.cloakedRiderUuid)) {
            deactivateCloak();
        }
        super.removePassenger(passenger);
    }

    @Override
    public int getDeathAnimationDurationTicks() {
        return DEATH_SOUND_DURATION_TICKS;
    }

    @Override
    protected void applyLoadedFlightState(boolean flying, boolean takeoff, boolean hovering, boolean landing) {
        this.setFlying(true);
        this.setTakeoff(false);
        this.setHovering(false);
        this.setLanding(false);
    }

    @Override
    protected float getBabyHitboxScale() {
        return BABY_HITBOX_SCALE;
    }

    @Override
    public void ageBoundaryReached() {
        super.ageBoundaryReached();
        applyConfiguredAttributes();
        refreshDimensions();
    }

    private void nudgeUpIfCarriedRiderInWall() {
        if (!this.isVehicle() || !isCarriedRiderInWall()) {
            return;
        }

        int lifts = 0;
        while (lifts < MAX_CARRIED_RIDER_ESCAPE_LIFTS && isCarriedRiderInWall()) {
            this.setPos(this.getX(), this.getY() + CARRIED_COLLISION_ESCAPE_LIFT, this.getZ());
            lifts++;
        }

        if (lifts > 0) {
            Vec3 velocity = this.getDeltaMovement();
            double raisedY = Math.max(velocity.y, CARRIED_COLLISION_ESCAPE_LIFT);
            this.setDeltaMovement(velocity.x, raisedY, velocity.z);
            this.hasImpulse = true;
        }
    }

    private boolean isCarriedRiderInWall() {
        Entity rider = getControllingPassenger();
        if (rider == null || rider.noPhysics) {
            return false;
        }

        float width = rider.getDimensions(Pose.STANDING).width * 0.8F;
        AABB riderProbe = AABB.ofSize(rider.position().add(0.0D, 0.5D, 0.0D), width, 1.0E-6D, width);
        return BlockPos.betweenClosedStream(riderProbe).anyMatch(pos -> {
            BlockState state = this.level().getBlockState(pos);
            return !state.isAir()
                    && state.isSuffocating(this.level(), pos)
                    && Shapes.joinIsNotEmpty(
                    state.getCollisionShape(this.level(), pos).move(pos.getX(), pos.getY(), pos.getZ()),
                    Shapes.create(riderProbe),
                    BooleanOp.AND
            );
        });
    }

    private Vec3 resolveMountedFlightCollision(Vec3 desiredMotion) {
        if (!this.isVehicle()) {
            return desiredMotion;
        }

        AABB currentShell = createMountedCollisionShell(this.position());
        if (this.level().noCollision(this, currentShell.move(desiredMotion))) {
            return desiredMotion;
        }

        Vec3 noDive = new Vec3(desiredMotion.x, Math.max(0.0D, desiredMotion.y), desiredMotion.z);
        if (this.level().noCollision(this, currentShell.move(noDive))) {
            return noDive;
        }

        Vec3 climb = new Vec3(desiredMotion.x * 0.35D, Math.max(CARRIED_COLLISION_ESCAPE_LIFT, desiredMotion.y), desiredMotion.z * 0.35D);
        if (this.level().noCollision(this, currentShell.move(climb))) {
            return climb;
        }

        Vec3 emergencyLift = new Vec3(0.0D, CARRIED_COLLISION_ESCAPE_LIFT, 0.0D);
        if (this.level().noCollision(this, currentShell.move(emergencyLift))) {
            return emergencyLift;
        }

        return Vec3.ZERO;
    }

    private AABB createMountedCollisionShell(Vec3 position) {
        EntityDimensions dimensions = this.getDimensions(this.getPose());
        double halfWidth = dimensions.width * 0.5D;
        return new AABB(
                position.x - halfWidth,
                position.y - CARRIED_HITBOX_DOWNWARD_EXTENSION,
                position.z - halfWidth,
                position.x + halfWidth,
                position.y + dimensions.height,
                position.z + halfWidth
        );
    }

    @Nullable
    public Vec3 findForwardTeleportDestination(ServerPlayer rider) {
        if (this.level().isClientSide || rider == null || rider.getVehicle() != this) {
            return null;
        }

        Vec3 direction = rider.getLookAngle();
        if (direction.lengthSqr() < 1.0E-6D) {
            return null;
        }
        direction = direction.normalize();

        Vec3 origin = this.position();
        Vec3 lastSafe = null;
        for (double distance = FORWARD_TELEPORT_SAMPLE_STEP;
             distance <= FORWARD_TELEPORT_DISTANCE + 1.0E-6D;
             distance += FORWARD_TELEPORT_SAMPLE_STEP) {
            Vec3 candidate = origin.add(direction.scale(distance));
            AABB collisionShell = createMountedCollisionShell(candidate).deflate(1.0E-4D);
            if (!this.level().hasChunkAt(BlockPos.containing(candidate))
                    || !this.level().getWorldBorder().isWithinBounds(collisionShell)
                    || !this.level().noCollision(this, collisionShell)) {
                break;
            }
            if (distance >= MIN_FORWARD_TELEPORT_DISTANCE) {
                lastSafe = candidate;
            }
        }
        return lastSafe;
    }

    public boolean teleportMountedTo(Vec3 destination) {
        if (this.level().isClientSide || destination == null || !this.isVehicle()) {
            return false;
        }

        AABB collisionShell = createMountedCollisionShell(destination).deflate(1.0E-4D);
        if (!this.level().hasChunkAt(BlockPos.containing(destination))
                || !this.level().getWorldBorder().isWithinBounds(collisionShell)
                || !this.level().noCollision(this, collisionShell)) {
            return false;
        }

        List<Entity> passengers = List.copyOf(this.getPassengers());
        Vec3 origin = this.position();
        this.getNavigation().stop();
        this.getAIMovement().stop();
        this.teleportTo(destination.x, destination.y, destination.z);
        this.hasImpulse = true;
        this.hurtMarked = true;

        MessageMountedTeleport teleportMessage = new MessageMountedTeleport(
                this.getId(),
                origin.x,
                origin.y,
                origin.z,
                destination.x,
                destination.y,
                destination.z,
                this.getYRot(),
                this.getXRot()
        );
        NetworkHandler.sendToTracking(this, teleportMessage);

        this.level().gameEvent(GameEvent.TELEPORT, origin, GameEvent.Context.of(this));
        if (!this.isSilent()) {
            this.level().playSound(
                    null,
                    origin.x,
                    origin.y,
                    origin.z,
                    SoundEvents.ENDERMAN_TELEPORT,
                    this.getSoundSource(),
                    1.0F,
                    1.0F
            );
            this.playSound(SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.0F);
        }

        for (Entity passenger : passengers) {
            this.positionRider(passenger, (entity, x, y, z) -> {
                if (entity instanceof ServerPlayer player) {
                    player.connection.teleport(x, y, z, player.getYRot(), player.getXRot());
                } else {
                    entity.teleportTo(x, y, z);
                }
                entity.hasImpulse = true;
                entity.hurtMarked = true;
            });
        }
        return true;
    }

    @Override
    public boolean canMate(@NotNull Animal otherAnimal) {
        return otherAnimal instanceof Nulljaw && super.canMate(otherAnimal);
    }

    public boolean canFeed() {
        return this.entityData.get(DATA_FEEDING_COOLDOWN) <= 0;
    }

    public void setFeedingCooldown(int ticks) {
        this.entityData.set(DATA_FEEDING_COOLDOWN, Math.max(0, ticks));
    }

    public void playEatMovingSound() {
        if (!this.level().isClientSide) {
            this.getSoundHandler().playMovingEntitySound(ModSounds.NULLJAW_EAT.get(), 1.0f, 1.0f, 22);
        }
    }

    public void consumeShulkerBullet(ShulkerBullet bullet) {
        if (this.level().isClientSide || bullet == null || !bullet.isAlive()) {
            return;
        }
        this.triggerAnim("interaction", "eat");
        this.playEatMovingSound();
        bullet.discard();
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(@NotNull ServerLevel level, @NotNull AgeableMob otherParent) {
        return createBreedOffspring(level, otherParent, ModEntities.NULLJAW.get(), Nulljaw::applyConfiguredAttributes);
    }

    @Override
    public @Nullable UUID getPackLeaderUuid() {
        return this.packLeaderUuid;
    }

    @Override
    public void setPackLeaderUuid(@Nullable UUID leaderUuid) {
        this.packLeaderUuid = leaderUuid;
    }

    @Override
    public int getMaxPackSize() {
        return MAX_PACK_SIZE;
    }

    @Override
    public double getPackSearchRadius() {
        return PACK_SEARCH_RADIUS;
    }

    @Override
    public boolean handleDirectAirPackFollow(Vec3 target, double speed) {
        if (this.level().isClientSide || !this.isAerial() || this.isLanding()) {
            return false;
        }
        this.beginAiFlight();
        this.getAIMovement().setAsyncAirWaypoint(target, speed);
        return true;
    }

    public @Nullable UUID getCombatFormationLeaderUuid() {
        return this.combatFormationLeaderUuid;
    }

    public void setCombatFormationLeaderUuid(@Nullable UUID leaderUuid) {
        this.combatFormationLeaderUuid = leaderUuid;
    }

    public void setCombatFormationDebug(String phase, int slot, int size, boolean attackReserved) {
        this.combatFormationPhase = phase;
        this.combatFormationSlot = slot;
        this.combatFormationSize = size;
        this.combatAttackReserved = attackReserved;
    }

    public void clearCombatFormationDebug() {
        this.combatFormationPhase = "NONE";
        this.combatFormationSlot = -1;
        this.combatFormationSize = 0;
        this.combatAttackReserved = false;
    }

    public String getCombatFormationDebugSummary() {
        String leader = this.combatFormationLeaderUuid == null
                ? "none"
                : this.combatFormationLeaderUuid.toString().substring(0, 8);
        String slot = this.combatFormationSlot < 0
                ? "none"
                : (this.combatFormationSlot + 1) + "/" + Math.max(1, this.combatFormationSize);
        return "leader=" + leader
                + ",phase=" + this.combatFormationPhase
                + ",slot=" + slot
                + ",reserved=" + this.combatAttackReserved;
    }

    @Override
    protected int getFlightMode() {
        return isActuallyHovering() ? 2 : 5;
    }

    @Override
    protected DragonAbilityType<?, ?> getHurtAbilityType() {
        return ModAbilities.NULLJAW_HURT;
    }

    @Override
    protected EntityDataAccessor<Boolean> getFlyingDataAccessor() {
        return DATA_FLYING;
    }

    @Override
    protected EntityDataAccessor<Boolean> getTakeoffDataAccessor() {
        return DATA_TAKEOFF;
    }

    @Override
    protected EntityDataAccessor<Boolean> getHoveringDataAccessor() {
        return DATA_HOVERING;
    }

    @Override
    protected EntityDataAccessor<Boolean> getLandingDataAccessor() {
        return DATA_LANDING;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public void setRunning(boolean running) {
        this.running = running;
    }

    @Override
    protected boolean shouldRedirectFlyingStartToTakeoff() {
        return false;
    }

    @Override
    protected boolean canApplyFlyingState(boolean flying) {
        return flying;
    }

    @Override
    protected boolean canApplyLandingState(boolean landing) {
        return !landing;
    }

    @Override
    protected void onLandingDataSet(boolean landing) {
    }

    @Override
    public float getFlightSpeed() {
        return (float) this.getAttributeValue(Attributes.FLYING_SPEED);
    }

    @Override
    public double getPreferredFlightAltitude() {
        return 8.0D;
    }

    @Override
    public boolean canTakeoff() {
        return true;
    }

    @Override
    public boolean canRiderShiftDismount() {
        return true;
    }

    @Override
    public boolean canAcceptSitCommand() {
        return true;
    }

    public boolean canBeBound() {
        return !isDying()
                && !isBaby()
                && !isVehicle()
                && !areRiderControlsLocked()
                && combatManager.getActiveAbility() == null;
    }

    @Override
    public void startTakeoffSequence(double minUpwardVelocity, int animationTicks) {
        this.setFlying(true);
        this.setTakeoff(true);
        this.setLanding(false);
        this.setHovering(false);
        this.setNoGravity(true);
        Vec3 velocity = this.getDeltaMovement();
        if (velocity.y < minUpwardVelocity) {
            this.setDeltaMovement(velocity.x, minUpwardVelocity, velocity.z);
            this.hasImpulse = true;
        }
    }

    @Override
    public void beginAiFlight() {
        this.setFlying(true);
        this.setTakeoff(false);
        this.setHovering(false);
        this.setLanding(false);
        this.setNoGravity(true);
        this.switchToAirNavigation();
    }

    @Override
    public void beginAiLanding() {
        beginAiFlight();
    }

    @Override
    public void markLandedNow() {
        this.beginAiFlight();
    }

    @Override
    public DragonAbilityType<?, ?> getPrimaryAttackAbility() {
        return this.isBaby() ? null : ModAbilities.NULLJAW_BITE;
    }

    @Override
    public RiderAbilityBinding getAttackRiderAbility() {
        return this.isBaby()
                ? null
                : new RiderAbilityBinding(ModAbilities.NULLJAW_BITE.getName(), RiderAbilityBinding.Activation.PRESS);
    }

    @Override
    public RiderAbilityBinding getPrimaryRiderAbility() {
        return this.isBaby()
                ? null
                : new RiderAbilityBinding(ModAbilities.NULLJAW_FORWARD_TELEPORT.getName(), RiderAbilityBinding.Activation.PRESS);
    }

    @Override
    public RiderAbilityBinding getSecondaryRiderAbility() {
        return !this.isTame() || this.isBaby()
                ? null
                : new RiderAbilityBinding(CLOAK_ABILITY_ID, RiderAbilityBinding.Activation.PRESS);
    }

    @Override
    protected void onRiderAbilityUse(Player player, String abilityName) {
        if (CLOAK_ABILITY_ID.equals(abilityName)) {
            toggleCloak(player);
            return;
        }
        super.onRiderAbilityUse(player, abilityName);
    }

    private void toggleCloak(Player rider) {
        if (this.level().isClientSide
                || !this.isTame()
                || this.isBaby()
                || !this.isOwnedBy(rider)
                || rider != this.getControllingPassenger()) {
            return;
        }

        if (this.cloakTicksRemaining > 0) {
            deactivateCloak();
            return;
        }

        int duration = Mth.clamp(
                (int)Math.round(getConfiguredDragonAttributes().extraDouble(
                        "invisibility_duration_ticks",
                        DEFAULT_CLOAK_DURATION_TICKS
                )),
                1,
                72000
        );
        this.cloakTicksRemaining = duration;
        this.cloakedRiderUuid = rider.getUUID();
        applyCloakEffect(this, duration);
        applyCloakEffect(rider, duration);
    }

    private void tickCloak() {
        if (this.cloakTicksRemaining <= 0) {
            return;
        }

        LivingEntity rider = this.getControllingPassenger();
        if (!(rider instanceof Player player)
                || !this.isTame()
                || !this.isOwnedBy(player)
                || !player.getUUID().equals(this.cloakedRiderUuid)) {
            deactivateCloak();
            return;
        }

        this.cloakTicksRemaining--;
        if (this.cloakTicksRemaining <= 0) {
            deactivateCloak();
            return;
        }

        if (!this.hasEffect(MobEffects.INVISIBILITY)) {
            applyCloakEffect(this, this.cloakTicksRemaining);
        }
        if (!player.hasEffect(MobEffects.INVISIBILITY)) {
            applyCloakEffect(player, this.cloakTicksRemaining);
        }
    }

    private void deactivateCloak() {
        this.removeEffect(MobEffects.INVISIBILITY);

        Player cloakedRider = null;
        if (this.getControllingPassenger() instanceof Player player
                && (this.cloakedRiderUuid == null || player.getUUID().equals(this.cloakedRiderUuid))) {
            cloakedRider = player;
        } else if (this.cloakedRiderUuid != null && this.level() instanceof ServerLevel serverLevel) {
            cloakedRider = serverLevel.getPlayerByUUID(this.cloakedRiderUuid);
        }
        if (cloakedRider != null) {
            cloakedRider.removeEffect(MobEffects.INVISIBILITY);
        }

        this.cloakTicksRemaining = 0;
        this.cloakedRiderUuid = null;
    }

    private static void applyCloakEffect(LivingEntity entity, int duration) {
        entity.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, duration, 0, false, false, true));
    }

    @Override
    protected boolean supportsRiderAction(DragonRiderAction action) {
        return switch (action) {
            case ABILITY_USE, ABILITY_STOP -> true;
            default -> super.supportsRiderAction(action);
        };
    }

    @Override
    protected boolean isRidingAbilityAllowed(DragonAbilityType<?, ?> abilityType) {
        return abilityType == ModAbilities.NULLJAW_BITE
                || abilityType == ModAbilities.NULLJAW_FORWARD_TELEPORT;
    }

    @Override
    protected boolean canUseBarrelRoll() {
        return false;
    }

}

package com.leon.saintsdragons.server.entity.dragons.nulljaw;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.common.registry.nulljaw.NulljawAbilities;
import com.leon.saintsdragons.common.registry.ModBlocks;
import com.leon.saintsdragons.server.ai.goals.base.*;
import com.leon.saintsdragons.server.ai.goals.nulljaw.*;
import com.leon.saintsdragons.server.ai.goals.base.DragonBreedGoal;
import com.leon.saintsdragons.server.ai.goals.base.DragonFollowParentGoal;
import com.leon.saintsdragons.server.ai.goals.base.DragonProtectBabiesGoal;
import com.leon.saintsdragons.server.ai.navigation.DragonPathNavigateGround;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.DragonGender;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.handlers.*;
import com.leon.saintsdragons.common.block.NulljawEggBlockEntity;
import com.leon.saintsdragons.server.entity.handler.DragonSoundHandler;
import com.leon.saintsdragons.server.entity.interfaces.*;
import com.leon.saintsdragons.common.network.DragonRiderAction;
import com.leon.saintsdragons.common.registry.ModSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import com.leon.saintsdragons.server.entity.controller.nulljaw.NulljawRiderController;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreathAirGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import javax.annotation.Nonnull;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.keyframe.event.SoundKeyframeEvent;
import software.bernie.geckolib.util.GeckoLibUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Nulljaw extends RideableDragonBase implements SemiAquaticDragon, ShakesScreen, SoundHandledDragon {

    // ===== VOCAL ENTRIES =====
    // IMPORTANT: Keys MUST match animation trigger names registered in NulljawAnimationHandler
    private static final Map<String, VocalEntry> VOCAL_ENTRIES = new VocalEntryBuilder()
            .add("grumble1", "action", "animation.nulljaw.grumble1", ModSounds.NULLJAW_GRUMBLE_1, 0.8f, 0.95f, 0.1f, false, false, true)
            .add("grumble2", "action", "animation.nulljaw.grumble2", ModSounds.NULLJAW_GRUMBLE_2, 0.8f, 0.95f, 0.1f, false, false, true)
            .add("grumble3", "action", "animation.nulljaw.grumble3", ModSounds.NULLJAW_GRUMBLE_3, 0.8f, 0.95f, 0.1f, false, false, true)
            .add("nulljaw_hurt", "hurt", "animation.nulljaw.hurt", ModSounds.NULLJAW_HURT, 1.1f, 0.95f, 0.1f, false, true, true)
            .add("nulljaw_die", "hurt", "animation.nulljaw.die", ModSounds.NULLJAW_DIE, 1.35f, 0.9f, 0.05f, false, true, true)
            .build();

    // ===== AMBIENT SOUND SYSTEM =====
    private int ambientSoundTimer;
    private int nextAmbientSoundDelay;
    private static final int MIN_AMBIENT_DELAY = 200;  // 10 seconds
    private static final int MAX_AMBIENT_DELAY = 600;  // 30 seconds

    // ===== BABY STATS =====
    private static final double BABY_MAX_HEALTH = 80.0D;
    private static final double BABY_ARMOR = 0.0D;
    private static final float BABY_HITBOX_SCALE = 0.5F;

    private static final EntityDataAccessor<Integer> DATA_GROUND_MOVE_STATE = SynchedEntityData.defineId(Nulljaw.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_RIDER_FORWARD = SynchedEntityData.defineId(Nulljaw.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_RIDER_STRAFE = SynchedEntityData.defineId(Nulljaw.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_ACCELERATING = SynchedEntityData.defineId(Nulljaw.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SWIMMING = SynchedEntityData.defineId(Nulljaw.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_SWIM_TURN = SynchedEntityData.defineId(Nulljaw.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_SWIM_PITCH = SynchedEntityData.defineId(Nulljaw.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_SWIM_PITCH_RAD = SynchedEntityData.defineId(Nulljaw.class, EntityDataSerializers.FLOAT); // Actual pitch angle for visuals
    private static final EntityDataAccessor<Boolean> DATA_PITCH_KEY_MODE = SynchedEntityData.defineId(Nulljaw.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_PHASE_TWO = SynchedEntityData.defineId(Nulljaw.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_SCREEN_SHAKE_AMOUNT = SynchedEntityData.defineId(Nulljaw.class, EntityDataSerializers.FLOAT);

    // Flight mode data accessor (not used for ground drake but required by interface)
    private static final EntityDataAccessor<Integer> DATA_FLIGHT_MODE = SynchedEntityData.defineId(Nulljaw.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_GOING_UP = SynchedEntityData.defineId(Nulljaw.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_GOING_DOWN = SynchedEntityData.defineId(Nulljaw.class, EntityDataSerializers.BOOLEAN);

    /** Entity data accessor for feeding cooldown ticks */
    private static final EntityDataAccessor<Integer> DATA_FEEDING_COOLDOWN =
            SynchedEntityData.defineId(Nulljaw.class, EntityDataSerializers.INT);

    public AnimatableInstanceCache dragonCache = GeckoLibUtil.createInstanceCache(this);
    private final DragonSoundHandler soundHandler = new DragonSoundHandler(this);
    private final NulljawAnimationHandler animationHandler = new NulljawAnimationHandler(this);
    private final NulljawInteractionHandler interactionHandler = new NulljawInteractionHandler(this);
    private final NulljawRiderController riderController;

    // ===== HARDCODED GROUND SPEEDS =====
    public static final double RIDER_WALK_SPEED = 0.22D;
    public static final double RIDER_RUN_SPEED = 0.42D;
    public static final float RIDER_KEY_PITCH_DEG = 25.0f;
    private final PathNavigation groundNavigation;
    private final MoveControl landMoveControl;
    private final RiftDrakeLookController landLookControl;
    private DragonGroundWanderGoal<Nulljaw> groundWanderGoal;
    private boolean swimming;
    private int swimTicks;
    private int ticksInWater;
    private int ticksOutOfWater;
    private float swimTurnSmoothedYaw;
    private int swimTurnState;
    private int swimPitchStateTicks;
    // Continuous swim roll angle for smooth banking (like Raevyx's flight banking)
    private float swimRollAngle = 0f;
    private float prevSwimRollAngle = 0f;
    // Swim pitch smoothing (procedural - visual tilt when swimming up/down)
    private float swimPitchRad = 0f;
    private float prevSwimPitchRad = 0f;
    private float smoothedPlayerSwimPitchRad = 0f;
    // Client-side smoothing for synced swim pitch (prevents jitter)
    private float clientSwimPitchRad = 0f;
    private float prevClientSwimPitchRad = 0f;
    private boolean useLeftClawNext = true; // Toggles between left/right claw attacks
    private boolean useLeftTailAttackNext = true; // Toggles between left/right tail attacks
    // ===== SCREEN SHAKE SYSTEM =====
    private static final float SHAKE_DECAY_PER_TICK = 0.02F;
    private float prevScreenShakeAmount = 0.0F;
    private float screenShakeAmount = 0.0F;
    // ===== SIT / SLEEP STATE =====
    private boolean isSittingDown = false;
    private boolean isStandingUp = false;
    private int sitTransitionTicks = 0;
    private boolean wasVehicleLastTick = false;
    private static final int PHASE_TWO_LINGER_TICKS = 20 * 30;
    private int phaseTwoLingerTicks = 0;

    // ===== LEAP FORWARD SYSTEM =====
    private boolean leaping = false;
    private int leapTicksLeft = 0;
    private int leapCooldownTicks = 0;
    private Vec3 leapVec = Vec3.ZERO;
    private int leapTicksElapsed = 0;
    private boolean leapDamageApplied = false;
    private boolean lastDashWasRight = false;
    private static final double LEAP_HORIZONTAL_DRAG = 0.92D;
    private static final double LEAP_VERTICAL_DRAG = 0.98D;

    // ===== UNTAMED RIDE / TAMING STATE =====
    private static final int MIN_WILD_TAME_TICKS = 60;
    private static final int MAX_TAMING_PROGRESS = 400;
    private static final int BUCK_INTERVAL_MIN = 60;
    private static final int BUCK_INTERVAL_MAX = 110;
    private static final int BUCK_WARNING_WINDOW = 15; // Ticks before buck where Nulljaw "winds up"
    private static final double BREED_PARTNER_RANGE = 30.0D;
    private static final double BREED_DISTANCE_SQR = 2500.0D;
    private boolean wildRideActive = false;
    private int wildRideTicks = 0;
    private int nextBuckAttemptTick = 0;
    private int cumulativeWildRideProgress = 0;

    // Client-side animation initialization grace period (fixes T-pose on world rejoin with shaders)
    private int clientAnimInitTicks = 0;
    private static final int ANIM_INIT_GRACE_PERIOD = 5; // Wait 5 ticks for entity data sync

    // Derived from mouth_origin locator in rift_drake.geo (Z negative means forward in model space)
    private static final double MODEL_SCALE = 1.0D;
    private static final double MOUTH_OFFSET_RIGHT = (0.0D / 16.0D) * MODEL_SCALE;
    private static final double MOUTH_OFFSET_UP = (10.25D / 16.0D) * MODEL_SCALE;
    private static final double MOUTH_OFFSET_FORWARD = (24.0D / 16.0D) * MODEL_SCALE;

    @Override
    protected boolean supportsRiderAction(DragonRiderAction action) {
        return switch (action) {
            case DOUBLE_TAP_W,
                 TOGGLE_PITCH_MODE, ABILITY_USE, ABILITY_STOP -> true;
            default -> super.supportsRiderAction(action);
        };
    }

    // Feeding cooldown synced via DATA_FEEDING_COOLDOWN entity data accessor

    public boolean canFeed() {
        int cooldownTicks = this.entityData.get(DATA_FEEDING_COOLDOWN);
        return cooldownTicks <= 0;
    }

    public void setFeedingCooldown(int ticks) {
        this.entityData.set(DATA_FEEDING_COOLDOWN, ticks);
    }

    public Nulljaw(EntityType<? extends Nulljaw> type, Level level) {
        super(type, level);
        this.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
        this.setPathfindingMalus(BlockPathTypes.WATER_BORDER, 0.0F);
        this.setMaxUpStep(1.4F);
        this.groundNavigation = new DragonPathNavigateGround(this, level);
        this.landMoveControl = new RiftDrakeMoveControl(this);
        this.landLookControl = new RiftDrakeLookController(this);
        this.navigation = this.groundNavigation;
        this.moveControl = this.landMoveControl;
        this.lookControl = this.landLookControl;
        this.riderController = new NulljawRiderController(this);
        this.setRideable();

        // Initialize ambient sound system with random offset
        RandomSource rng = this.getRandom();
        this.ambientSoundTimer = rng.nextInt(80);
        this.nextAmbientSoundDelay = MIN_AMBIENT_DELAY + rng.nextInt(MAX_AMBIENT_DELAY - MIN_AMBIENT_DELAY);
        if (!level.isClientSide) {
            applyConfiguredAttributes();
            this.setHealth(this.getMaxHealth());
        }
    }


    @Override
    protected float getRiderLockYawBlend() {
        return this.isPhaseTwoActive() ? 0.25F : 0.18F;
    }

    @Override
    protected float getRiderLockPitchBlend() {
        return this.isPhaseTwoActive() ? 0.25F : 0.18F;
    }

    @Override
    public boolean areRiderControlsLocked() {
        // Nulljaw-specific: also consider wild ride active state
        return super.areRiderControlsLocked() || isWildRideActive();
    }

    // Animation initialization system (fixes T-pose on world rejoin with shaders)
    public boolean isClientAnimationReady() {
        return clientAnimInitTicks >= ANIM_INIT_GRACE_PERIOD;
    }

    @Override
    public void lockRiderControls(int ticks) {
        super.lockRiderControls(ticks);  // Base handles tick counting and entity data
        // Nulljaw-specific: reset rider inputs and movement states during lock
        this.setAccelerating(false);
        this.setLastRiderForward(0.0F);
        this.setLastRiderStrafe(0.0F);
        this.setGroundMoveStateFromRider(0);
        this.setGoingUp(false);
        this.setGoingDown(false);
        this.setDeltaMovement(Vec3.ZERO);
        if (!this.level().isClientSide) {
            this.getNavigation().stop();
            this.setTarget(null);
        }
    }


    public void lockAbilities(int ticks) {
        combatManager.lockGlobalCooldown(ticks);
    }

    @Override
    protected boolean isRiderInputLocked(Player player) {
        return areRiderControlsLocked();
    }

    @Override
    public boolean canTakeoff() {
        return false; // Nulljaw is a ground/aquatic dragon, cannot fly
    }

    @Override
    public @NotNull net.minecraft.world.entity.EntityDimensions getDimensions(@NotNull net.minecraft.world.entity.Pose pose) {
        net.minecraft.world.entity.EntityDimensions baseDimensions = super.getDimensions(pose);
        if (isBaby()) {
            return baseDimensions.scale(BABY_HITBOX_SCALE);
        }
        return baseDimensions;
    }

    @Override
    protected void applyRiderVerticalInput(Player player, boolean goingUp, boolean goingDown, boolean locked) {
        if (locked) {
            setGoingUp(false);
            setGoingDown(false);
            return;
        }
        boolean inWater = this.isSwimming() || this.isInWaterOrBubble();

        // In water: allow vertical movement
        if (inWater) {
            setGoingUp(goingUp);
            setGoingDown(goingDown);
        } else {
            // On land: trigger jump when space is pressed
            setGoingUp(false);
            setGoingDown(false);
            if (goingUp && this.onGround()) {
                handleJumpRequest();
            }
        }
    }

    @Override
    protected void applyRiderMovementInput(Player player, float forward, float strafe, float yaw, boolean locked) {
        float fwd = locked ? 0f : applyInputDeadzone(forward);
        float str = locked ? 0f : applyInputDeadzone(strafe);
        setLastRiderForward(fwd);
        setLastRiderStrafe(str);
        int moveState = 0;
        float magnitude = Math.abs(fwd) + Math.abs(str);
        if (magnitude > 0.05f) {
            moveState = isAccelerating() ? 2 : 1;
        }
        setGroundMoveStateFromRider(moveState);
    }

    @Override
    protected void onRiderTakeoffRequest(Player player) {
        handleJumpRequest(); // Nulljaw is ground/aquatic, no flight takeoff
    }

    @Override
    protected void onRiderAbilityUse(Player player, String abilityName) {
        if (abilityName != null && !abilityName.isEmpty()) {
            useRidingAbility(abilityName);
        }
    }

    @Override
    protected void onRiderAbilityStop(Player player, String abilityName) {
        if (abilityName != null && !abilityName.isEmpty()) {
            forceEndActiveAbility();
        }
    }

    @Override
    protected boolean handleCustomRiderAction(ServerPlayer player, DragonRiderAction action,
                                              String abilityName, boolean locked) {
        if (locked) {
            return false;
        }

        return switch (action) {
            case TOGGLE_PITCH_MODE -> {
                setRiderPitchKeyMode(!isRiderPitchKeyMode());
                yield true;
            }
            case DOUBLE_TAP_W -> {
                onRiderDash(player);
                yield true;
            }
            default -> false;
        };
    }

    @Override
    protected void onRiderDash(Player player) {
        // Nulljaw's dash uses the leap implementation (animation is a leap, behavior is a dash).
        onRiderLeap(player);
    }

    /**
     * Phase 2 dash - triggered by double-tap W.
     */
    protected void onRiderLeap(Player player) {
        // Check cooldown
        if (leapCooldownTicks > 0) {
            return;
        }

        // Check if already leaping
        if (leaping) {
            return;
        }

        // Dash constants
        final boolean phaseTwo = isPhaseTwoActive();
        if (this.isSwimming() || this.isInWaterOrBubble()) {
            return;
        }
        final int LEAP_DURATION = phaseTwo
                ? (int) Math.round(1.6667 * 20) // Phase 2 dash animation length: 33 ticks
                : (int) Math.round(2.3333 * 20); // Phase 1 tail swipe length: 47 ticks
        final int LEAP_COOLDOWN = phaseTwo
                ? 38
                : 60; // Phase 1: 3 second cooldown
        final double LEAP_DISTANCE = 32; // blocks
        // Get forward vector (direction dragon is facing)
        float yawRad = (float) Math.toRadians(this.getYRot());
        double forwardX = -Math.sin(yawRad);
        double forwardZ = Math.cos(yawRad);

        // Account for drag so the integrated distance over the duration is ~LEAP_DISTANCE
        double dragScale = 1.0D - Math.pow(LEAP_HORIZONTAL_DRAG, LEAP_DURATION);
        double perTickSpeed = LEAP_DISTANCE * (1.0D - LEAP_HORIZONTAL_DRAG) / dragScale;

        // Create dash vector (forward direction only)
        Vec3 leapVector = new Vec3(forwardX * perTickSpeed, 0.0D, forwardZ * perTickSpeed);

        // Begin leap
        leaping = true;
        leapTicksLeft = LEAP_DURATION;
        leapCooldownTicks = LEAP_COOLDOWN;
        leapVec = leapVector;
        leapTicksElapsed = 0;
        leapDamageApplied = false;
        this.setDeltaMovement(leapVector);
        this.getNavigation().stop();
        this.hasImpulse = true;
        if (phaseTwo) {
            lastDashWasRight = !lastDashWasRight;
            triggerAnim("instant_action", lastDashWasRight ? "phase2_dash_right" : "phase2_dash_left");
        } else {
            triggerAnim("instant_action", "tail_swipe_left");
        }
    }

    /**
     * Handles the leap movement physics - applies velocity and decay.
     * Called every tick while leaping.
     */
    private void handleLeapMovement() {
        // Apply the leap velocity via delta movement so vanilla travel handles motion.
        this.setDeltaMovement(leapVec);
        this.hasImpulse = true;

        // Decay for next tick
        leapVec = leapVec.multiply(LEAP_HORIZONTAL_DRAG, LEAP_VERTICAL_DRAG, LEAP_HORIZONTAL_DRAG);

        if (--leapTicksLeft <= 0) {
            leaping = false;
            leapVec = Vec3.ZERO;
        }
    }

    /**
     * Tick the leap state - handle cooldowns and damage at 1.79 seconds.
     */
    private void tickLeapState() {
        // Tick down cooldown
        if (leapCooldownTicks > 0) {
            leapCooldownTicks--;
        }

        if (!leaping) {
            leapTicksElapsed = 0;
            leapDamageApplied = false;
            return;
        }

        leapTicksElapsed++;

        if (leapDamageApplied) {
            return;
        }

        boolean phaseTwo = isPhaseTwoActive();
        int damageTick = phaseTwo ? 18 : 32;
        if (leapTicksElapsed >= damageTick && this.isVehicle()) {
            leapDamageApplied = true;
            // Get tail position as damage origin (using mouth position as approximation)
            Vec3 tailPos = getMouthPosition();

            // Large area for tail swipe
            net.minecraft.world.phys.AABB damageBox = new net.minecraft.world.phys.AABB(tailPos, tailPos).inflate(8.0D);

            java.util.List<LivingEntity> entities = this.level().getEntitiesOfClass(
                LivingEntity.class,
                damageBox,
                entity -> entity != this && entity != this.getControllingPassenger() && !this.isAlly(entity)
            );

            for (LivingEntity target : entities) {
                // Deal damage
                float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 1.5F;
                target.hurt(this.damageSources().mobAttack(this), damage);

                if (!phaseTwo) {
                    // Apply strong knockback away from dragon
                    Vec3 knockbackDir = target.position().subtract(this.position()).normalize();
                    target.push(knockbackDir.x * 2.0, 0.8, knockbackDir.z * 2.0);
                    target.hurtMarked = true;
                }
            }
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance().getConfig(DragonAttributeConfigLoader.NULLJAW_ID);
        return TamableAnimal.createLivingAttributes()
                .add(Attributes.MAX_HEALTH, config.maxHealth())
                .add(Attributes.MOVEMENT_SPEED, 0.28D) // Hardcoded AI pathfinding speed
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ARMOR, 8.0D)
                .add(Attributes.ATTACK_DAMAGE, 10.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_SWIMMING, false);
        this.entityData.define(DATA_SWIM_TURN, 0);
        this.entityData.define(DATA_SWIM_PITCH, 0);
        this.entityData.define(DATA_SWIM_PITCH_RAD, 0.0F);
        this.entityData.define(DATA_PITCH_KEY_MODE, false);
        this.entityData.define(DATA_PHASE_TWO, false);
        this.entityData.define(DATA_SCREEN_SHAKE_AMOUNT, 0.0F);
        this.entityData.define(DATA_FEEDING_COOLDOWN, 0);
    }
    
    @Override
    protected void defineRideableDragonData() {
        this.entityData.define(DATA_GROUND_MOVE_STATE, 0);
        this.entityData.define(DATA_RIDER_FORWARD, 0.0F);
        this.entityData.define(DATA_RIDER_STRAFE, 0.0F);
        this.entityData.define(DATA_ACCELERATING, false);
        this.entityData.define(DATA_FLIGHT_MODE, -1);
        this.entityData.define(DATA_GOING_UP, false);
        this.entityData.define(DATA_GOING_DOWN, false);
    }
    
    // ===== REQUIRED ABSTRACT METHODS FROM RIDEABLEDRAGONBASE =====
    
    @Override
    protected EntityDataAccessor<Float> getRiderForwardAccessor() {
        return DATA_RIDER_FORWARD;
    }
    
    @Override
    protected EntityDataAccessor<Float> getRiderStrafeAccessor() {
        return DATA_RIDER_STRAFE;
    }
    
    @Override
    protected EntityDataAccessor<Integer> getGroundMoveStateAccessor() {
        return DATA_GROUND_MOVE_STATE;
    }
    
    @Override
    protected EntityDataAccessor<Integer> getFlightModeAccessor() {
        return DATA_FLIGHT_MODE;
    }
    
    @Override
    protected EntityDataAccessor<Boolean> getGoingUpAccessor() {
        return DATA_GOING_UP;
    }
    
    @Override
    protected EntityDataAccessor<Boolean> getGoingDownAccessor() {
        return DATA_GOING_DOWN;
    }
    
    @Override
    protected EntityDataAccessor<Boolean> getAcceleratingAccessor() {
        return DATA_ACCELERATING;
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new BreathAirGoal(this));
        if (!this.isBaby()) {
            this.goalSelector.addGoal(2, new NulljawCombatGoal(this));
        }
        this.goalSelector.addGoal(3, new DirectSwimToTargetGoal(this, 8.0F, 0.30D, true));
        this.goalSelector.addGoal(5, new NulljawLeaveWaterGoal(this));
        this.goalSelector.addGoal(6, new NulljawFindWaterGoal(this));
        this.goalSelector.addGoal(7, new NulljawFollowOwnerGoal(this));
        this.goalSelector.addGoal(8, new DirectSwimToTargetGoal(this, 8.0F, 0.25D, false));
        this.goalSelector.addGoal(10, new DirectSwimWanderGoal(this, 6.0F, 0.20D, 30));
        this.groundWanderGoal = new DragonGroundWanderGoal<>(this, 1.0D, 100);
        this.goalSelector.addGoal(11, groundWanderGoal);
        this.goalSelector.addGoal(11, new DragonFollowParentGoal<>(this, Nulljaw.class, 1.1D));
        this.goalSelector.addGoal(11, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(11, new LookAtPlayerGoal(this, Player.class, 8.0F));

        if (!this.isBaby()) {
            this.goalSelector.addGoal(12, new DragonBreedGoal<>(
                    this, 1.0D, Nulljaw.class, BREED_PARTNER_RANGE, BREED_DISTANCE_SQR
            ));
        }

        if (!this.isBaby()) {
            this.targetSelector.addGoal(1, new DragonOwnerHurtByTargetGoal(this));
            this.targetSelector.addGoal(2, new DragonOwnerHurtTargetGoal(this));
            this.targetSelector.addGoal(3, new DragonProtectBabiesGoal<>(this, Nulljaw.class));
            this.targetSelector.addGoal(4, new HurtByTargetGoal(this));
            this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                    target -> shouldAggroOnSight()));
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<Nulljaw> movementController =
                new AnimationController<>(this, "movement", 5, animationHandler::movementPredicate);
        AnimationController<Nulljaw> actions =
                new AnimationController<>(this, "action", 10, animationHandler::actionPredicate);
        AnimationController<Nulljaw> hurtController =
                new AnimationController<>(this, "hurt", 1, animationHandler::hurtPredicate);
        AnimationController<Nulljaw> instantActions =
                new AnimationController<>(this, "instant_action", 10, animationHandler::instantActionPredicate);
        animationHandler.setupHurtController(hurtController);

        // Sound keyframes
        movementController.setSoundKeyframeHandler(this::onAnimationSound);
        actions.setSoundKeyframeHandler(this::onAnimationSound);
        hurtController.setSoundKeyframeHandler(this::onAnimationSound);
        instantActions.setSoundKeyframeHandler(this::onAnimationSound);

        // Setup animation triggers
        animationHandler.setupActionController(actions);
        animationHandler.setupInstantActionController(instantActions);

        controllers.add(movementController);
        controllers.add(actions);
        controllers.add(hurtController);
        controllers.add(instantActions);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return dragonCache;
    }

    public DragonSoundHandler getSoundHandler() {
        return soundHandler;
    }

    @Override
    public DragonSoundProfile getSoundProfile() {
        return NulljawSoundProfile.INSTANCE;
    }

    @Override
    protected DragonAbilityType<?, ?> getHurtAbilityType() {
        return NulljawAbilities.HURT;
    }

    @Override
    protected DragonAbilityType<?, ?> getDeathAbilityType() {
        return NulljawAbilities.DIE;
    }

    @Override
    public int getDeathAnimationDurationTicks() {
        return 56; // 2.7917 seconds
    }

    @Override
    public Map<String, VocalEntry> getVocalEntries() {
        return VOCAL_ENTRIES;
    }

    // ===== AMBIENT SOUND METHODS =====

    /**
     * Plays appropriate ambient sound based on drake's current mood and state
     */
    private void playCustomAmbientSound() {
        RandomSource random = getRandom();

        // Don't make ambient sounds if dying, in combat, using abilities, or during phase transitions
        if (isDying() || getTarget() != null || getActiveAbility() != null || areRiderControlsLocked()) {
            return;
        }

        String vocalKey = null;

        // Simple grumbles for the amphibious drake
        float moodRoll = random.nextFloat();
        if (moodRoll < 0.4f) {
            vocalKey = "grumble1";
        } else if (moodRoll < 0.7f) {
            vocalKey = "grumble2";
        } else {
            vocalKey = "grumble3";
        }

        // Play/animate if we chose one
        if (vocalKey != null) {
            this.getSoundHandler().playVocal(vocalKey);
        }
    }

    /**
     * Handles all the ambient grumbling sounds
     */
    private void handleAmbientSounds() {
        // Suppress ambient sounds while transitioning or resting to prevent animation snapping
        // Also suppress during rider control lock (leap, abilities, etc.)
        if (isBaby() || isDying() || isOrderedToSit() || isSleeping() || isSleepTransitioning() || isInSitTransition() || getSleepAmbientCooldownTicks() > 0 || areRiderControlsLocked()) {
            return;
        }

        ambientSoundTimer++;

        // Time to make some noise?
        if (ambientSoundTimer >= nextAmbientSoundDelay) {
            playCustomAmbientSound();
            resetAmbientSoundTimer();
        }
    }

    /**
     * Resets the ambient sound timer with some randomness
     */
    private void resetAmbientSoundTimer() {
        RandomSource random = getRandom();
        ambientSoundTimer = 0;
        nextAmbientSoundDelay = MIN_AMBIENT_DELAY + random.nextInt(MAX_AMBIENT_DELAY - MIN_AMBIENT_DELAY);
    }

    @Override
    public void tick() {
        super.tick();
        soundHandler.tick();

        // Increment animation initialization counter on client (prevents T-pose on rejoin with shaders)
        if (level().isClientSide && clientAnimInitTicks < ANIM_INIT_GRACE_PERIOD) {
            clientAnimInitTicks++;
        }

        tickSittingState();
        tickMountedState();
        tickScreenShake();
        updateSittingProgress();
        tickFeedingCooldown();

        // Handle ambient sounds (server-side only)
        if (!level().isClientSide) {
            // Handle leap movement (must be called every tick for smooth movement)
            if (leaping) {
                handleLeapMovement();
            }
            // Tick leap cooldowns and damage timing
            tickLeapState();
            handleAmbientSounds();
            tickRiderControlLock();
            boolean inWater = this.isInWaterOrBubble();
            if (inWater) {
                this.setAirSupply(this.getMaxAirSupply());
                swimTicks = Math.min(swimTicks + 1, 200);
                ticksInWater = Math.min(ticksInWater + 1, 1200);
                ticksOutOfWater = 0;
            } else {
                swimTicks = Math.max(swimTicks - 1, 0);
                ticksOutOfWater = Math.min(ticksOutOfWater + 1, 1200);
                ticksInWater = 0;
            }

            if (inWater && !swimming) {
                enterSwimState();
            } else if (!inWater && swimming) {
                exitSwimState();
            }

            // Ensure proper look control when being ridden
            if (this.getControllingPassenger() != null && this.lookControl != landLookControl) {
                this.lookControl = landLookControl;
            }

            if ((isSleeping() || isSleepingEntering() || isSleepingExiting())
                    && (this.getTarget() != null || this.isAggressive() || this.isInWaterOrBubble())) {
                wakeUpImmediately();
                suppressSleep(200);
            }

            this.tickAnimationStates();
            if (swimming || isInWaterOrBubble()) {
                this.updateSwimOrientationState();
            }
            if (wildRideActive) {
                this.tickWildRideState();
            }
            if (this.isPhaseTwoActive()) {
                tickPhaseTwoLinger();
            }
        }

        tickClientSideUpdates();
    }

    @Override
    public void travel(@NotNull Vec3 motion) {
        // During a leap, preserve the stored leap velocity and let vanilla travel apply it without rider overrides
        if (leaping) {
            super.travel(Vec3.ZERO);
            return;
        }

        if (this.isVehicle() && this.getControllingPassenger() instanceof Player player && !isWildRideActive()) {
            if (areRiderControlsLocked()) {
                this.setDeltaMovement(Vec3.ZERO);
                return;
            }
            // Clear any AI navigation when being ridden
            if (this.getNavigation().getPath() != null) {
                this.getNavigation().stop();
            }

            if (this.isInWaterOrBubble()) {
                handleRiddenSwimming(motion);
            } else {
                setGoingUp(false);
                setGoingDown(false);
                this.setSpeed(riderController.getRiddenSpeed(player));
                // Use vanilla movement system for proper camera-relative movement
                // This will call getRiddenInput() and getRiddenSpeed() properly
                super.travel(motion);
            }
        } else {
            // Normal AI movement
            super.travel(motion);
        }
    }

    @Override
    public @NotNull PathNavigation getNavigation() {
        return groundNavigation;
    }

    @Override
    public double getSwimSpeed() {
        return 1;
    }


    @Override
    public @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        InteractionResult handlerResult = interactionHandler.handleInteraction(player, hand);
        if (handlerResult != InteractionResult.PASS) {
            return handlerResult;
        }

        // Fall back to base implementation for any unhandled interactions
        return super.mobInteract(player, hand);
    }
    
    /**
     * Allow interaction handler to call super.mobInteract
     */
    public InteractionResult superMobInteract(Player player, InteractionHand hand) {
        return super.mobInteract(player, hand);
    }

    public boolean beginUntamedRide(Player player) {
        if (this.isTame() || this.isBaby() || this.isVehicle() || player.isSecondaryUseActive()) {
            return false;
        }
        if (this.level().isClientSide) {
            return true;
        }
        player.startRiding(this);
        startWildRideSequence();
        return true;
    }

    public void awardTamingAdvancement(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            var advancement = serverPlayer.server.getAdvancements()
                    .getAdvancement(SaintsDragonsCommon.rl("tame_nulljaw"));
            if (advancement != null) {
                serverPlayer.getAdvancements().award(advancement, "tame_nulljaw");
            }
        }
    }

    public void applyConfiguredAttributes() {
        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance().getConfig(DragonAttributeConfigLoader.NULLJAW_ID);

        // Apply baby-specific stats or adult stats
        setAttributeBase(Attributes.MAX_HEALTH, isBaby() ? BABY_MAX_HEALTH : config.maxHealth());
        setAttributeBase(Attributes.ARMOR, isBaby() ? BABY_ARMOR : config.armor());
        // MOVEMENT_SPEED is hardcoded in createAttributes() - no config needed

        double maxHealth = isBaby() ? BABY_MAX_HEALTH : config.maxHealth();
        if (this.getHealth() > maxHealth) {
            this.setHealth((float) maxHealth);
        }
    }

    private void setAttributeBase(Attribute attribute, double value) {
        AttributeInstance instance = this.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    // Ground speeds are now hardcoded constants (RIDER_WALK_SPEED, RIDER_RUN_SPEED)

    @Override
    public boolean isFood(@Nonnull net.minecraft.world.item.ItemStack stack) {
        // Rift Drakes prefer fish like other dragons
        return stack.is(net.minecraft.world.item.Items.COD) ||
               stack.is(net.minecraft.world.item.Items.SALMON) ||
               stack.is(net.minecraft.world.item.Items.TROPICAL_FISH) ||
               stack.is(com.leon.saintsdragons.common.registry.ModItems.HEARTY_DRAGON_MEAL.get());
    }

    @Override
    public void setLastRiderForward(float forward) {
        this.entityData.set(DATA_RIDER_FORWARD, forward);
    }

    @Override
    public void setLastRiderStrafe(float strafe) {
        this.entityData.set(DATA_RIDER_STRAFE, strafe);
    }

    public float getLastRiderForward() {
        return this.entityData.get(DATA_RIDER_FORWARD);
    }

    public float getLastRiderStrafe() {
        return this.entityData.get(DATA_RIDER_STRAFE);
    }

    @Override
    public boolean isAccelerating() {
        return this.entityData.get(DATA_ACCELERATING);
    }

    @Override
    public void setAccelerating(boolean accelerating) {
        this.entityData.set(DATA_ACCELERATING, accelerating);
    }

    public void setGroundMoveStateFromRider(int state) {
        int s = Mth.clamp(state, 0, 2);
        if (this.entityData.get(DATA_GROUND_MOVE_STATE) != s) {
            this.entityData.set(DATA_GROUND_MOVE_STATE, s);
        }
        this.syncAnimState(s, getSyncedFlightMode());
    }

    /**
     * Allow AI goals to set ground move state explicitly
     */
    public void setGroundMoveStateFromAI(int state) {
        if (!this.level().isClientSide) {
            int s = Mth.clamp(state, 0, 2);
            if (this.entityData.get(DATA_GROUND_MOVE_STATE) != s) {
                this.entityData.set(DATA_GROUND_MOVE_STATE, s);
                this.syncAnimState(s, getSyncedFlightMode());
            }
        }
    }

    // ===== REQUIRED METHODS FROM RIDEABLEDRAGONBASE =====
    
    @Override
    public boolean isRunning() {
        return false; // Rift Drake doesn't have running state
    }
    
    @Override
    public void setRunning(boolean running) {
        // Rift Drake doesn't have running state
    }
    
    @Override
    public boolean isDragonFlying() {
        return false; // Rift Drake doesn't fly
    }
    
    @Override
    public boolean isHovering() {
        return false; // Rift Drake doesn't hover
    }
    
    @Override
    public boolean isTakeoff() {
        return false; // Rift Drake doesn't take off
    }
    
    @Override
    public boolean isLanding() {
        return false; // Rift Drake doesn't land
    }
    
    @Override
    public int getFlightMode() {
        return -1; // Ground mode
    }


    public void handleJumpRequest() {
        if (this.onGround()) {
            // Ground jump - standard jump height
            Vec3 movement = this.getDeltaMovement();
            this.setDeltaMovement(movement.x, 1.0, movement.z);
            this.hasImpulse = true;
        }
    }

    @Override
    public @NotNull Vec3 getRiddenInput(@NotNull Player player, @NotNull Vec3 deltaIn) {
        if (areRiderControlsLocked()) {
            return Vec3.ZERO;
        }
        Vec3 input = riderController.getRiddenInput(player, deltaIn);

        // Capture rider inputs for animation state
        if (!level().isClientSide) {
            float fwd = (float) Mth.clamp(input.z, -1.0D, 1.0D);
            float str = (float) Mth.clamp(input.x, -1.0D, 1.0D);
            // Apply simple threshold to filter noise
            setLastRiderForward(Math.abs(fwd) > 0.02f ? fwd : 0f);
            setLastRiderStrafe(Math.abs(str) > 0.02f ? str : 0f);
        }
        return input;
    }

    @Override
    protected void tickRidden(@NotNull Player player, @NotNull Vec3 travelVector) {
        if (!level().isClientSide) {
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 220, 0, true, false, false));
        }
        if (isWildRideActive()) {
            handleUntamedRideWhileMounted(player);
            return;
        }
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
        riderController.tickRidden(player, travelVector);
    }

    @Override
    public void removePassenger(@NotNull Entity passenger) {
        // Base implementation handles clearing control lock

        super.removePassenger(passenger);
        if (!this.level().isClientSide && !this.isTame() && wildRideActive) {
            endWildRide(false);
        }
        if (!this.level().isClientSide) {
            this.setAccelerating(false);
            this.setLastRiderForward(0.0F);
            this.setLastRiderStrafe(0.0F);
            this.setGroundMoveStateFromRider(0);
        }
    }

    // Let RideableDragonBase handle tickAnimationStates() for proper networking

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, @NotNull DamageSource source) {
        if (fallDistance <= 16.0F) {
            return false;
        }
        return super.causeFallDamage(fallDistance - 16.0F, damageMultiplier, source);
    }

    @Override
    public void jumpFromGround() {
        super.jumpFromGround();
        this.setGroundMoveStateFromRider(1);
    }

    public void initializeRiderState() {
        if (!this.level().isClientSide) {
            this.entityData.set(DATA_GROUND_MOVE_STATE, 0);
            this.entityData.set(DATA_RIDER_FORWARD, 0.0F);
            this.entityData.set(DATA_RIDER_STRAFE, 0.0F);
            this.setAccelerating(false);
        }
    }

    @Override
    public Vec3 getHeadPosition() {
        return this.getEyePosition();
    }

    @Override
    public Vec3 getMouthPosition() {
        Vec3 locator = getClientLocatorPosition("mouth_origin");
        if (locator != null) {
            return locator;
        }
        return computeMouthPositionFallback();
    }

    private Vec3 computeMouthPositionFallback() {
        double x = this.getX();
        double y = this.getY();
        double z = this.getZ();

        float yawDeg = this.yHeadRot;
        float pitchDeg = this.getXRot();

        double yaw = Math.toRadians(yawDeg);
        double pitch = Math.toRadians(pitchDeg);

        double cp = Math.cos(pitch);
        double sp = Math.sin(pitch);
        double pitchedUp = MOUTH_OFFSET_UP * cp - MOUTH_OFFSET_FORWARD * sp;
        double pitchedForward = MOUTH_OFFSET_UP * sp + MOUTH_OFFSET_FORWARD * cp;

        double cy = Math.cos(yaw);
        double sy = Math.sin(yaw);
        double offX = MOUTH_OFFSET_RIGHT * cy - pitchedForward * sy;
        double offZ = MOUTH_OFFSET_RIGHT * sy + pitchedForward * cy;

        return new Vec3(x + offX, y + pitchedUp, z + offZ);
    }

    public void useRidingAbility(String abilityName) {
        if (abilityName == null || abilityName.isEmpty()) {
            return;
        }
        if (areRiderControlsLocked()) {
            return;
        }
        Entity rider = this.getControllingPassenger();
        if (!(rider instanceof LivingEntity)) {
            return;
        }
        if (this.isTame() && rider instanceof Player player && !this.isOwnedBy(player)) {
            return;
        }

        DragonAbilityType<?, ?> type = com.leon.saintsdragons.common.registry.AbilityRegistry.get(abilityName);
        if (type != null) {
            combatManager.tryUseAbility(type);
        }
    }

    public void forceEndActiveAbility() {
        combatManager.forceEndActiveAbility();
    }

    // ===== RIDING METHODS =====
    
    @Override
    public double getPassengersRidingOffset() {
        return riderController.getPassengersRidingOffset();
    }

    @Override
    protected void positionRider(@Nonnull @NotNull Entity passenger, @Nonnull @NotNull Entity.MoveFunction moveFunction) {
        riderController.positionRider(passenger, moveFunction);
    }

    @Override
    public @Nullable net.minecraft.world.entity.LivingEntity getControllingPassenger() {
        return riderController.getControllingPassenger();
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (this.isBaby()) {
            super.setTarget(null);
            return;
        }
        super.setTarget(target);
    }

    private boolean shouldAggroOnSight() {
        if (isTame() || isBaby()) {
            return false;
        }
        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.NULLJAW_ID);
        return config.extraBoolean("aggressive_wild", false);
    }

    @Override
    public @NotNull Vec3 getDismountLocationForPassenger(@NotNull LivingEntity passenger) {
        return riderController.getDismountLocationForPassenger(passenger);
    }

    @Override
    public net.minecraft.world.entity.AgeableMob getBreedOffspring(@Nonnull net.minecraft.server.level.ServerLevel level, @Nonnull net.minecraft.world.entity.AgeableMob other) {
        Nulljaw baby = ModEntities.NULLJAW.get().create(level);
        if (baby != null) {
            baby.setGender(this.random.nextBoolean() ? DragonGender.FEMALE : DragonGender.MALE);
            java.util.UUID ownerId = this.getOwnerUUID();
            if (ownerId != null) {
                baby.setOwnerUUID(ownerId);
                baby.setTame(true);
            }

            baby.skipRespawnTicks = 5;
            baby.setAge(-24000);
            baby.setBaby(true);
            baby.applyConfiguredAttributes();
            baby.setHealth(baby.getMaxHealth());
            baby.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0F);
        }
        return baby;
    }

    @Override
    public boolean canMate(@Nonnull net.minecraft.world.entity.animal.Animal otherAnimal) {
        if (!this.canBreed()) {
            return false;
        }

        if (otherAnimal instanceof Nulljaw otherDragon) {
            if (this.isFemale() == otherDragon.isFemale()) {
                return false;
            }
            return otherDragon.canBreed();
        }

        return false;
    }

    @Override
    public void ageBoundaryReached() {
        super.ageBoundaryReached();
        applyConfiguredAttributes();
        refreshDimensions();
    }

    @Override
    public boolean canBreed() {
        return this.isTame() && !this.isBaby() && this.getHealth() >= this.getMaxHealth() && this.isInLove();
    }

    @Override
    public BlockState getEggBlockState() {
        return ModBlocks.NULLJAW_EGG.get().defaultBlockState();
    }

    @Override
    public void configureEggBlockEntity(BlockEntity blockEntity, @Nullable DragonEntity partner) {
        if (!(blockEntity instanceof NulljawEggBlockEntity eggEntity)) {
            return;
        }

        if (this.isTame() && this.getOwnerUUID() != null) {
            eggEntity.setOwnerUUID(this.getOwnerUUID());
        }

        DragonGender babyGender = this.getRandom().nextBoolean() ? DragonGender.FEMALE : DragonGender.MALE;
        eggEntity.setBabyGender(babyGender);
    }

    @Override
    public com.leon.saintsdragons.server.entity.ability.DragonAbilityType<?, ?> getPrimaryAttackAbility() {
        // Use melee mode to switch between bite and horn gore
        // Mode 0 = bite (primary), Mode 1 = horn gore (secondary)
        boolean useHornGore = getMeleeMode() == 1;

        // Phase 2 uses bite2 (faster bite) instead of normal bite
        if (isPhaseTwoActive()) {
            return useHornGore ? NulljawAbilities.NULLJAW_HORN_GORE : NulljawAbilities.NULLJAW_BITE2;
        }
        return useHornGore ? NulljawAbilities.NULLJAW_HORN_GORE : NulljawAbilities.NULLJAW_BITE;
    }

    public boolean isAbilityActive(DragonAbilityType<?, ?> abilityType) {
        return combatManager.isAbilityActive(abilityType);
    }

    public void forceEndAbility(DragonAbilityType<?, ?> abilityType) {
        combatManager.forceEndAbility(abilityType);
    }

    public static boolean canSpawnHere(EntityType<? extends Nulljaw> type,
                                       LevelAccessor level,
                                       MobSpawnType spawnType,
                                       BlockPos pos,
                                       RandomSource random) {
        if (!Animal.checkAnimalSpawnRules(type, level, spawnType, pos, random)) {
            return false;
        }

        // Allow amphibious spawning: valid on solid ground OR in water
        boolean fluidHere = !level.getFluidState(pos).isEmpty();

        if (!fluidHere) {
            // Land: require sturdy ground and free feet/head
            BlockPos below = pos.below();
            boolean solidGround = level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
            boolean feetFree = level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
            boolean headFree = level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty();
            return solidGround && feetFree && headFree;
        } else {
            // Water: require water at feet and head positions for clearance
            FluidState feet = level.getFluidState(pos);
            FluidState head = level.getFluidState(pos.above());
            boolean feetWater = !feet.isEmpty() && feet.isSource();
            boolean headWater = !head.isEmpty();
            return feetWater && headWater;
        }
    }

    private void enterSwimState() {
        swimming = true;
        this.entityData.set(DATA_SWIMMING, true);
    }

    private void exitSwimState() {
        swimming = false;
        Vec3 delta = this.getDeltaMovement();
        this.setDeltaMovement(new Vec3(delta.x, 0.0D, delta.z));
        if (groundWanderGoal != null) {
            groundWanderGoal.forceTrigger();
        }
        this.entityData.set(DATA_SWIMMING, false);
        this.entityData.set(DATA_SWIM_TURN, 0);
        this.entityData.set(DATA_SWIM_PITCH, 0);
        this.entityData.set(DATA_SWIM_PITCH_RAD, 0.0F);
        this.swimTurnSmoothedYaw = 0.0F;
        this.swimTurnState = 0;
        this.swimPitchStateTicks = 0;
        this.swimRollAngle = 0f;
        this.prevSwimRollAngle = 0f;
        this.swimPitchRad = 0f;
        this.prevSwimPitchRad = 0f;
        this.smoothedPlayerSwimPitchRad = 0f;
        this.clientSwimPitchRad = 0f;
        this.prevClientSwimPitchRad = 0f;
    }

    private void updateSwimOrientationState() {
        // Server-only: Update synced swim turn and pitch states
        if (!level().isClientSide) {
            int desiredTurn = this.entityData.get(DATA_SWIM_TURN);
            int desiredPitchState = this.entityData.get(DATA_SWIM_PITCH);

            boolean riderControlled = this.isTame()
                    && this.isVehicle()
                    && this.getControllingPassenger() instanceof Player player
                    && this.isOwnedBy(player)
                    && this.isInWaterOrBubble();

            if (riderControlled) {
                float yawDelta = Mth.wrapDegrees(this.getYRot() - this.yRotO);
                swimTurnSmoothedYaw = swimTurnSmoothedYaw * 0.6F + yawDelta * 0.4F;
                float enter = 0.35F;
                float exit = 0.12F;

                int targetState = swimTurnState;
                if (swimTurnSmoothedYaw > enter) {
                    targetState = -1;
                } else if (swimTurnSmoothedYaw < -enter) {
                    targetState = 1;
                } else if (Math.abs(swimTurnSmoothedYaw) < exit) {
                    targetState = 0;
                }

                swimTurnState = targetState;
                desiredTurn = swimTurnState;

                if (this.isGoingUp()) {
                    desiredPitchState = -1;
                    swimPitchStateTicks = 6;
                } else if (this.isGoingDown()) {
                    desiredPitchState = 1;
                    swimPitchStateTicks = 6;
                } else if (desiredPitchState != 0) {
                    if (swimPitchStateTicks > 0) {
                        swimPitchStateTicks--;
                    } else {
                        desiredPitchState = 0;
                    }
                }
            } else {
                swimTurnSmoothedYaw *= 0.5F;
                if (Math.abs(swimTurnSmoothedYaw) < 0.05F) {
                    swimTurnState = 0;
                }
                desiredTurn = swimTurnState;

                if (desiredPitchState != 0) {
                    if (++swimPitchStateTicks > 4) {
                        desiredPitchState = 0;
                        swimPitchStateTicks = 0;
                    }
                } else {
                    swimPitchStateTicks = 0;
                }
            }

            if (this.entityData.get(DATA_SWIM_TURN) != desiredTurn) {
                this.entityData.set(DATA_SWIM_TURN, desiredTurn);
            }

            if (this.entityData.get(DATA_SWIM_PITCH) != desiredPitchState) {
                this.entityData.set(DATA_SWIM_PITCH, desiredPitchState);
            }
        }

        // Client + Server: Calculate continuous swim roll angle (like Raevyx's banking)
        prevSwimRollAngle = swimRollAngle;

        // Reset when not swimming or controls locked
        if (!isSwimming() || areRiderControlsLocked()) {
            swimRollAngle = 0f;
            prevSwimRollAngle = 0f;
            swimTurnSmoothedYaw = 0f;
            swimPitchRad = 0f;
            prevSwimPitchRad = 0f;
            smoothedPlayerSwimPitchRad = 0f;
            clientSwimPitchRad = 0f;
            prevClientSwimPitchRad = 0f;
            // Sync reset to client
            if (!level().isClientSide) {
                this.entityData.set(DATA_SWIM_PITCH_RAD, 0.0F);
            }
            return;
        }

        // Calculate yaw velocity for banking (works on both client and server)
        float yawDelta = Mth.wrapDegrees(this.getYRot() - this.yRotO);
        swimTurnSmoothedYaw = swimTurnSmoothedYaw * 0.6F + yawDelta * 0.4F;

        // Convert smoothed yaw delta into a roll angle
        float targetAngle = Mth.clamp(swimTurnSmoothedYaw * 40.0f, -60f, 60f); // More roll than Raevyx
        // Ease toward the new target (more responsive than before)
        swimRollAngle = Mth.lerp(0.40f, swimRollAngle, targetAngle); // Increased from 0.25f to 0.40f
        if (Math.abs(swimRollAngle) < 0.01f) {
            swimRollAngle = 0f;
        }

        // === RIDER SWIM PITCH (server authoritative for sync) ===
        if (!level().isClientSide && this.isVehicle() && this.getControllingPassenger() instanceof Player player) {
            boolean useKeyPitch = isRiderPitchKeyMode();
            float riderForward = this.entityData.get(DATA_RIDER_FORWARD);
            float riderStrafe = this.entityData.get(DATA_RIDER_STRAFE);
            boolean hasMovementInput = Math.abs(riderForward) > 0.01f || Math.abs(riderStrafe) > 0.01f;

            prevSwimPitchRad = swimPitchRad;
            float targetPitchRad = 0f;
            if (useKeyPitch) {
                float rawKeyPitchRad = 0f;
                if (isGoingUp()) {
                    rawKeyPitchRad = (float) Math.toRadians(RIDER_KEY_PITCH_DEG);
                } else if (isGoingDown()) {
                    rawKeyPitchRad = (float) -Math.toRadians(RIDER_KEY_PITCH_DEG);
                }
                smoothedPlayerSwimPitchRad = smoothedPlayerSwimPitchRad * 0.65f + rawKeyPitchRad * 0.35f;
                targetPitchRad = Mth.clamp(smoothedPlayerSwimPitchRad, -Mth.HALF_PI, Mth.HALF_PI);
            } else if (hasMovementInput) {
                float rawPlayerPitchRad = -(float)Math.toRadians(player.getXRot());
                smoothedPlayerSwimPitchRad = smoothedPlayerSwimPitchRad * 0.65f + rawPlayerPitchRad * 0.35f;
                targetPitchRad = Mth.clamp(smoothedPlayerSwimPitchRad, -Mth.HALF_PI, Mth.HALF_PI);
            } else {
                smoothedPlayerSwimPitchRad = 0f;
            }

            swimPitchRad = Mth.lerp(0.35f, swimPitchRad, targetPitchRad);
            this.entityData.set(DATA_SWIM_PITCH_RAD, swimPitchRad);
        }

        // === AI SWIM PITCH (velocity-based for non-ridden) ===
        // Only update AI pitch when NOT being ridden (ridden uses camera-based pitch)
        if (!this.isVehicle()) {
            prevSwimPitchRad = swimPitchRad;

            Vec3 velocity = this.getDeltaMovement();
            double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);

            float targetPitchRad = 0f;
            if (horizontalSpeed > 0.05) {
                // Calculate pitch from velocity (like Spinosaurus and flying dragon AI)
                targetPitchRad = (float) Math.atan2(velocity.y, horizontalSpeed);
                targetPitchRad = Mth.clamp(targetPitchRad, -Mth.HALF_PI, Mth.HALF_PI);
            }

            // Smooth pitch transitions (0.35f lerp - matches ridden smoothness)
            swimPitchRad = Mth.lerp(0.35f, swimPitchRad, targetPitchRad);

            // Sync to client for visuals (server-side only)
            if (!level().isClientSide) {
                this.entityData.set(DATA_SWIM_PITCH_RAD, swimPitchRad);
            }
        }
        // When ridden, swimPitchRad is updated in handleRiddenSwimming()
    }

    public boolean isSwimming() {
        if (level().isClientSide) {
            return this.entityData.get(DATA_SWIMMING);
        }
        return swimming;
    }

    public boolean isSwimmingMoving() {
        if (!isSwimming()) {
            return false;
        }

        if (this.getNavigation().isInProgress() && this.getNavigation().getPath() != null) {
            return true;
        }

        if (this.isVehicle()) {
            float fwd = Math.abs(this.entityData.get(DATA_RIDER_FORWARD));
            float str = Math.abs(this.entityData.get(DATA_RIDER_STRAFE));
            if (fwd > 0.03F || str > 0.03F) {
                return true;
            }
        }

        return this.getDeltaMovement().horizontalDistanceSqr() > 0.0025D;
    }

    /**
     * Get the swim roll angle in degrees (like Raevyx's banking)
     */
    public float getSwimRollAngleDegrees() {
        return swimRollAngle;
    }

    /**
     * Get interpolated swim roll angle for smooth rendering
     */
    public float getSwimRollAngleDegrees(float partialTick) {
        return Mth.lerp(partialTick, prevSwimRollAngle, swimRollAngle);
    }

    /**
     * Get swim pitch in radians (for visual model tilting when swimming)
     */
    public float getSwimPitchRadians() {
        // Client uses smoothed value, server uses local value
        if (level().isClientSide) {
            return clientSwimPitchRad;
        }
        return swimPitchRad;
    }

    /**
     * Get interpolated swim pitch for smooth rendering
     */
    public float getSwimPitchRadians(float partialTick) {
        // Client uses client-side interpolation, server uses server-side interpolation
        if (level().isClientSide) {
            return Mth.lerp(partialTick, prevClientSwimPitchRad, clientSwimPitchRad);
        }
        return Mth.lerp(partialTick, prevSwimPitchRad, swimPitchRad);
    }

    public boolean isRiderPitchKeyMode() {
        return this.entityData.get(DATA_PITCH_KEY_MODE);
    }

    public void setRiderPitchKeyMode(boolean enabled) {
        this.entityData.set(DATA_PITCH_KEY_MODE, enabled);
    }

    public boolean isPhaseTwoActive() {
        return this.entityData.get(DATA_PHASE_TWO);
    }

    public void setPhaseTwoActive(boolean active, boolean syncAnim) {
        this.entityData.set(DATA_PHASE_TWO, active);
        if (syncAnim) {
            this.syncAnimState(this.entityData.get(DATA_GROUND_MOVE_STATE), this.getSyncedFlightMode());
        }
    }

    // ===== CLAW ALTERNATION SYSTEM =====

    public boolean shouldUseLeftClaw() {
        return useLeftClawNext;
    }

    public void toggleClawSide() {
        useLeftClawNext = !useLeftClawNext;
    }

    public boolean shouldUseLeftTailAttack() {
        return useLeftTailAttackNext;
    }

    public void toggleTailAttackSide() {
        useLeftTailAttackNext = !useLeftTailAttackNext;
    }


    @Override
    public RiderAbilityBinding getAttackRiderAbility() {
        // Use melee mode to switch between horn gore and bite/bite2
        // Mode 0 = bite (primary), Mode 1 = horn gore (secondary)
        if (getMeleeMode() == 1) {
            return new RiderAbilityBinding(NulljawAbilities.NULLJAW_HORN_GORE_ID, RiderAbilityBinding.Activation.PRESS);
        } else {
            // Phase 2 uses bite2, Phase 1 uses bite
            try {
                if (isPhaseTwoActive()) {
                    return new RiderAbilityBinding(NulljawAbilities.NULLJAW_BITE2_ID, RiderAbilityBinding.Activation.PRESS);
                }
            } catch (Exception e) {
                // Fallback to phase 1 bite if entity data isn't ready
            }
            return new RiderAbilityBinding(NulljawAbilities.NULLJAW_BITE_ID, RiderAbilityBinding.Activation.PRESS);
        }
    }

    @Override
    public RiderAbilityBinding getPrimaryRiderAbility() {
        // R key: Universal roar ability for all dragons (PRESS)
        return new RiderAbilityBinding(NulljawAbilities.NULLJAW_ROAR_ID, RiderAbilityBinding.Activation.PRESS);
    }

    @Override
    public RiderAbilityBinding getSecondaryRiderAbility() {
        return new RiderAbilityBinding(NulljawAbilities.NULLJAW_PHASE_SHIFT_ID, RiderAbilityBinding.Activation.PRESS);
    }

    @Override
    public RiderAbilityBinding getTertiaryRiderAbility() {
        // G key: Phase 2 gets claw attacks (PRESS, not HOLD like other dragons)
        if (isPhaseTwoActive()) {
            return new RiderAbilityBinding(NulljawAbilities.NULLJAW_CLAW_ID, RiderAbilityBinding.Activation.PRESS);
        }
        return new RiderAbilityBinding(NulljawAbilities.NULLJAW_TAIL_ATTACK_ID, RiderAbilityBinding.Activation.PRESS);
    }

    @Override
    public DragonAbilityType<?, ?> getChannelingAbility() {
        return NulljawAbilities.NULLJAW_PHASE_SHIFT;
    }

    public void onAnimationSound(SoundKeyframeEvent<Nulljaw> event) {
        // Delegate all keyframed sounds to the sound handler
        this.getSoundHandler().handleAnimationSound(this, event.getKeyframeData(), event.getController());
    }

    private void handleRiddenSwimming(Vec3 input) {
        Player player = (Player) getControllingPassenger();
        if (player == null) return;

        Vec3 velocity = this.getDeltaMovement();

        double swimSpeed = getSwimSpeed();
        if (isAccelerating()) {
            swimSpeed *= 1.6D;
        }

        // Get input direction
        double forwardInput = input.z;
        double strafeInput = input.x;
        boolean hasInput = Math.abs(forwardInput) > 0.01 || Math.abs(strafeInput) > 0.01;
        boolean hasRiderInput = Math.abs(player.zza) > 0.01f || Math.abs(player.xxa) > 0.01f;
        boolean useKeyPitch = isRiderPitchKeyMode();

        // === VISUAL PITCH CALCULATION ===
        // Update swim pitch for visual feedback (like flying dragons)
        float targetPitchRad = 0f;
        if (useKeyPitch) {
            float rawKeyPitchRad = 0f;
            if (isGoingUp()) {
                rawKeyPitchRad = (float) Math.toRadians(RIDER_KEY_PITCH_DEG);
            } else if (isGoingDown()) {
                rawKeyPitchRad = (float) -Math.toRadians(RIDER_KEY_PITCH_DEG);
            }
            smoothedPlayerSwimPitchRad = smoothedPlayerSwimPitchRad * 0.65f + rawKeyPitchRad * 0.35f;
            targetPitchRad = Mth.clamp(smoothedPlayerSwimPitchRad, -Mth.HALF_PI, Mth.HALF_PI);
        } else if (hasRiderInput) {
            // Player is swimming (WASD pressed)  use camera pitch for visuals
            // Negate because Minecraft xRot is positive=down
            float rawPlayerPitchRad = -(float)Math.toRadians(player.getXRot());

            // Exponential smoothing on player pitch input to avoid jitter
            smoothedPlayerSwimPitchRad = smoothedPlayerSwimPitchRad * 0.65f + rawPlayerPitchRad * 0.35f;

            targetPitchRad = Mth.clamp(smoothedPlayerSwimPitchRad, -Mth.HALF_PI, Mth.HALF_PI);
        } else {
            // Not swimming (no WASD)  level out
            smoothedPlayerSwimPitchRad = 0f;
            targetPitchRad = 0f;
        }

        // Smooth pitch transitions (0.35f lerp - matches flying dragons)
        prevSwimPitchRad = swimPitchRad;
        swimPitchRad = Mth.lerp(0.35f, swimPitchRad, targetPitchRad);

        // Sync to client for visuals
        this.entityData.set(DATA_SWIM_PITCH_RAD, swimPitchRad);

        // Calculate 3D direction using player camera pitch (like flying dragons)
        float yawRad = (float) Math.toRadians(this.getYRot());
        float pitchRad = useKeyPitch ? getKeyPitchRadians() : (float) Math.toRadians(player.getXRot());
        double forwardXZ = Math.cos(pitchRad);
        double forwardX = -Math.sin(yawRad) * forwardXZ;
        double forwardY = -Math.sin(pitchRad);
        double forwardZ = Math.cos(yawRad) * forwardXZ;
        double rightX = Math.cos(yawRad);
        double rightZ = Math.sin(yawRad);

        // Combine forward and strafe
        double targetDirX = forwardX * forwardInput + rightX * strafeInput * 0.5;
        double targetDirY = forwardY * forwardInput * 1.2; // Vertical component when moving
        double targetDirZ = forwardZ * forwardInput + rightZ * strafeInput * 0.5;
        double dirLength = Math.sqrt(targetDirX * targetDirX + targetDirY * targetDirY + targetDirZ * targetDirZ);

        Vec3 desired;
        if (hasInput && dirLength > 0.01) {
            // Normalize and scale by swim speed
            targetDirX /= dirLength;
            targetDirY /= dirLength;
            targetDirZ /= dirLength;

            desired = new Vec3(
                targetDirX * swimSpeed,
                targetDirY * swimSpeed,
                targetDirZ * swimSpeed
            );
        } else {
            // No input - drift with minimal vertical decay
            desired = new Vec3(0, velocity.y * 0.9, 0);
        }

        // Smooth acceleration
        Vec3 blended = velocity.add(desired.subtract(velocity).scale(0.28D));

        // Apply drag
        double dragFactor = this.isControlledByLocalInstance() ? 0.92D : 0.94D;
        blended = blended.multiply(dragFactor, 0.96D, dragFactor);

        // Spacebar/L-Alt override for quick vertical adjustments (legacy controls)
        double verticalVel = blended.y;
        if (isGoingUp()) {
            verticalVel = Math.min(swimSpeed * 0.8, verticalVel + 0.12D * swimSpeed);
        } else if (isGoingDown()) {
            verticalVel = Math.max(-swimSpeed * 0.8, verticalVel - 0.12D * swimSpeed);
        }
        // No sink when idle - maintains neutral buoyancy

        blended = new Vec3(blended.x, verticalVel, blended.z);

        this.setDeltaMovement(blended);
        this.move(MoverType.SELF, this.getDeltaMovement());
    }

    private float getKeyPitchRadians() {
        if (isGoingUp()) {
            return (float) -Math.toRadians(RIDER_KEY_PITCH_DEG);
        }
        if (isGoingDown()) {
            return (float) Math.toRadians(RIDER_KEY_PITCH_DEG);
        }
        return 0.0f;
    }

    // Required methods for RideableDragon interface
    @Override
    public boolean isGoingUp() {
        return this.entityData.get(DATA_GOING_UP);
    }
    
    @Override
    public void setGoingUp(boolean goingUp) {
        this.entityData.set(DATA_GOING_UP, goingUp);
    }
    
    @Override
    public boolean isGoingDown() {
        return this.entityData.get(DATA_GOING_DOWN);
    }
    
    @Override
    public void setGoingDown(boolean goingDown) {
        this.entityData.set(DATA_GOING_DOWN, goingDown);
    }

    private static class RiftDrakeMoveControl extends MoveControl {

        public RiftDrakeMoveControl(Nulljaw drake) {
            super(drake);
        }

        @Override
        public void tick() {
            super.tick();
        }
    }

    // ===== LOOK CONTROLLER =====
    public static class RiftDrakeLookController extends LookControl {
        private final Nulljaw dragon;

        public RiftDrakeLookController(Nulljaw dragon) {
            super(dragon);
            this.dragon = dragon;
        }

        @Override
        public void tick() {
            if (!this.dragon.isAlive()) {
                return;
            }

            // Stay still while sleeping or in sleep transitions
            if (dragon.isSleeping() || dragon.isSleepTransitioning()) {
                return;
            }

            LivingEntity rider = this.dragon.getControllingPassenger();
            if (this.dragon.isVehicle() && rider != null) {
                if (this.dragon.isControlledByLocalInstance()) {
                    float bodyYaw = rider.getYRot();
                    this.dragon.setYRot(bodyYaw);
                    this.dragon.setYHeadRot(bodyYaw);
                    this.dragon.yHeadRotO = bodyYaw;
                    this.dragon.yBodyRot = bodyYaw;
                    this.dragon.yBodyRotO = bodyYaw;

                    boolean allowRiderPitch = !(this.dragon.isPhaseTwoActive() && this.dragon.areRiderControlsLocked());
                    if (allowRiderPitch) {
                        float pitch = Mth.clamp(rider.getXRot(), -45.0F, 45.0F);
                        this.dragon.setXRot(pitch);
                        this.dragon.xRotO = pitch;
                    } else {
                        float eased = Mth.approachDegrees(this.dragon.getXRot(), 0.0F, 6.0F);
                        this.dragon.setXRot(eased);
                        this.dragon.xRotO = eased;
                    }
                } else {
                    float serverPitch = this.dragon.getXRot();
                    this.dragon.xRotO = serverPitch;
                }
                return;
            }

            super.tick();
        }
    }

    private void tickSittingState() {
        // Clear sitting state if the drake is being ridden
        if (!this.level().isClientSide && this.isVehicle() && this.isOrderedToSit()) {
            this.setOrderedToSit(false);
        }
    }

    private void tickMountedState() {
        boolean mounted = this.isVehicle();

        if (mounted && !wasVehicleLastTick) {
            clearSitProgress();
            isSittingDown = false;
            isStandingUp = false;
            sitTransitionTicks = 0;

            if (this.isOrderedToSit()) {
                this.setOrderedToSit(false);
                if (this.getCommand() == 1) {
                    this.setCommand(0);
                }
            }

            if (isSleeping() || isSleepingEntering() || isSleepingExiting()) {
                wakeUpImmediately();
                suppressSleep(300);
            }
        }

        if (!mounted && wasVehicleLastTick) {
            clearSitProgress();
            isSittingDown = false;
            isStandingUp = false;
            sitTransitionTicks = 0;
        }

        wasVehicleLastTick = mounted;
    }

    private void startWildRideSequence() {
        wildRideActive = true;
        wildRideTicks = 0;
        nextBuckAttemptTick = nextBuckDelay();
        this.getNavigation().stop();
        this.setTarget(null);
    }

    private void tickWildRideState() {
        if (!wildRideActive || this.isTame()) {
            wildRideActive = false;
            return;
        }

        Player rider = getWildRideRider();
        if (rider == null) {
            endWildRide(false);
            return;
        }

        wildRideTicks++;
        cumulativeWildRideProgress = Math.min(cumulativeWildRideProgress + 1, MAX_TAMING_PROGRESS);

        // Calculate how close we are to the buck
        int ticksUntilBuck = nextBuckAttemptTick - wildRideTicks;
        boolean inWarningWindow = ticksUntilBuck > 0 && ticksUntilBuck <= BUCK_WARNING_WINDOW;

        // In the warning window, Nulljaw gets more aggressive
        if (inWarningWindow) {
            applyWarningBehavior(ticksUntilBuck);
        } else {
            applyWildRideMotion();
        }

        // Time to buck!
        if (wildRideTicks >= nextBuckAttemptTick) {
            buckWildRider(rider);
            endWildRide(true);
            return;
        }

        // Check for taming success
        if (wildRideTicks >= MIN_WILD_TAME_TICKS) {
            float progressFactor = (float) cumulativeWildRideProgress / (float) MAX_TAMING_PROGRESS;

            // Load taming chance from config
            DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance()
                    .getConfig(DragonAttributeConfigLoader.NULLJAW_ID);
            double tamingChanceConfig = config.extraDoubles().getOrDefault("taming_chance", 6.0);

            // Convert config value to per-tick success chance
            // Higher values = much harder to tame (exponential scaling)
            // taming_chance of 6.0 means base chance of 1/6 per eligible tick
            // But we divide by 100 to make it a reasonable per-tick rate since this runs every tick
            float baseChance = 1.0F / (float) (tamingChanceConfig * 100.0);

            // Scale with progress: only becomes reasonable at high progress
            // This encourages players to stay on longer for better odds
            float successChance = baseChance * (1.0F + (progressFactor * progressFactor * 4.0F));

            if (this.getRandom().nextFloat() < successChance) {
                this.tame(rider);
                this.setOrderedToSit(false);
                this.setCommand(0);
                this.level().broadcastEntityEvent(this, (byte) 7);
                awardTamingAdvancement(rider);
                endWildRide(false);
            }
        }
    }

    /**
     * Warning behavior before buck - Nulljaw becomes more erratic and aggressive.
     * Like a real bronco "loading up" before the big buck.
     */
    private void applyWarningBehavior(int ticksUntilBuck) {
        // Stop navigation during wind-up
        this.getNavigation().stop();

        if (this.onGround()) {
            // Rapid small hops and spins - building tension
            if (this.tickCount % 2 == 0) {
                // Quick spin
                float spinAmount = (this.getRandom().nextFloat() - 0.5F) * 25.0F;
                this.setYRot(this.getYRot() + spinAmount);
                this.yBodyRot = this.getYRot();

                // Small agitated hop
                Vec3 hop = new Vec3(
                        (this.getRandom().nextDouble() - 0.5D) * 0.4D,
                        0.15D + this.getRandom().nextDouble() * 0.1D,
                        (this.getRandom().nextDouble() - 0.5D) * 0.4D
                );
                this.setDeltaMovement(this.getDeltaMovement().add(hop));
                this.hasImpulse = true;
            }
        }
    }

    private void handleUntamedRideWhileMounted(Player rider) {
        rider.fallDistance = 0.0F;
        this.fallDistance = 0.0F;
    }

    private void applyWildRideMotion() {
        // Frantic running - pick random sprint targets
        if (this.getNavigation().isDone()) {
            double targetX = this.getX() + (this.getRandom().nextDouble() - 0.5D) * 22.0D;
            double targetZ = this.getZ() + (this.getRandom().nextDouble() - 0.5D) * 22.0D;
            double targetY = this.getY();
            this.getNavigation().moveTo(targetX, targetY, targetZ, 1.65D); // Faster sprint
        }

        if (this.onGround()) {
            // Big bucking jumps - like a rodeo bronco
            if (this.tickCount % 6 == 0) {
                this.jumpFromGround();

                // Strong upward kick with random lateral twist
                Vec3 impulse = new Vec3(
                        (this.getRandom().nextDouble() - 0.5D) * 1.4D, // More lateral chaos
                        0.65D + this.getRandom().nextDouble() * 0.5D,  // Higher jumps
                        (this.getRandom().nextDouble() - 0.5D) * 1.4D
                );
                this.setDeltaMovement(this.getDeltaMovement().add(impulse));
                this.hasImpulse = true;
            }
            // Smaller "crow hops" between big bucks
            else if (this.tickCount % 3 == 0) {
                Vec3 lateral = new Vec3(
                        (this.getRandom().nextDouble() - 0.5D) * 0.8D,
                        0.2D + this.getRandom().nextDouble() * 0.2D, // Small hop
                        (this.getRandom().nextDouble() - 0.5D) * 0.8D
                );
                this.setDeltaMovement(this.getDeltaMovement().add(lateral));
                this.hasImpulse = true;
            }
        }
    }

    private Player getWildRideRider() {
        if (!this.isVehicle()) {
            return null;
        }
        Entity passenger = this.getFirstPassenger();
        if (passenger instanceof Player player && !this.isTame()) {
            return player;
        }
        return null;
    }

    private void buckWildRider(Player rider) {
        // Calculate launch direction - mostly upward with some randomness
        // Like a real bronco kicking its hind legs up
        Vec3 launch = new Vec3(
                (this.getRandom().nextDouble() - 0.5D) * 1.2D,  // Random X direction
                1.2D + this.getRandom().nextDouble() * 0.6D,    // Strong upward launch (1.2-1.8)
                (this.getRandom().nextDouble() - 0.5D) * 1.2D   // Random Z direction
        );

        // Dismount first, THEN apply velocity
        rider.stopRiding();

        // Set velocity directly instead of push() for reliable launch
        Vec3 currentVel = rider.getDeltaMovement();
        rider.setDeltaMovement(
                currentVel.x + launch.x,
                launch.y,  // Override Y completely for consistent height
                currentVel.z + launch.z
        );
        rider.hurtMarked = true;

        // Make Nulljaw also jump up during the buck for realism
        if (this.onGround()) {
            Vec3 drakeKick = new Vec3(0.0D, 0.7D, 0.0D);
            this.setDeltaMovement(this.getDeltaMovement().add(drakeKick));
            this.hasImpulse = true;
        }

        // Broadcast event for particles/sounds
        this.level().broadcastEntityEvent(this, (byte) 6);

        // Play aggressive roar sound and animation (phase 1 during taming)
        this.playSound(ModSounds.NULLJAW_ROAR.get(), 1.5F, 0.8F + this.getRandom().nextFloat() * 0.3F);
        this.triggerAnim("action", "roar");

        // Triumphant screen shake when successfully bucking player off
        this.triggerScreenShake(1.2F);
    }

    private void endWildRide(boolean bucked) {
        if (bucked) {
            cumulativeWildRideProgress = Math.max(0, cumulativeWildRideProgress - 40);
        } else {
            cumulativeWildRideProgress = Math.max(0, cumulativeWildRideProgress - 10);
        }
        wildRideActive = false;
        wildRideTicks = 0;
        nextBuckAttemptTick = 0;
    }

    private int nextBuckDelay() {
        return Mth.nextInt(this.getRandom(), BUCK_INTERVAL_MIN, BUCK_INTERVAL_MAX);
    }

    private boolean isWildRideActive() {
        return wildRideActive && !this.isTame();
    }

    private void updateSittingProgress() {
        if (level().isClientSide) {
            return;
        }

        if (this.isInWaterOrBubble()) {
            if (isSittingDown || isStandingUp || sitTransitionTicks > 0) {
                isSittingDown = false;
                isStandingUp = false;
                sitTransitionTicks = 0;
            }
            if (getSitProgress() != 0f || getPrevSitProgress() != 0f) {
                clearSitProgress();
            }
            return;
        }

        if (sitTransitionTicks > 0) {
            sitTransitionTicks--;
            if (sitTransitionTicks == 0) {
                isSittingDown = false;
                isStandingUp = false;
            }
        }

        float sitProgress = getSitProgress();
        if (this.isOrderedToSit()) {
            if ((sitProgress == 0f || isStandingUp) && !isSittingDown) {
                animationHandler.triggerSitDownAnimation();
                isSittingDown = true;
                isStandingUp = false;
                sitTransitionTicks = getSitDownAnimationTicks();
            }

            if (sitProgress < maxSitTicks()) {
                sitProgress++;
                setSitProgress(sitProgress);
            }
        } else {
            if (isVehicle()) {
                if (sitProgress != 0f) {
                    clearSitProgress();
                    isSittingDown = false;
                    isStandingUp = false;
                    sitTransitionTicks = 0;
                }
            } else if (sitProgress > 0f) {
                if ((sitProgress >= maxSitTicks() || isSittingDown) && !isStandingUp) {
                    animationHandler.triggerSitUpAnimation();
                    isStandingUp = true;
                    isSittingDown = false;
                    sitTransitionTicks = getSitUpAnimationTicks();
                }

                float decrementRate = maxSitTicks() / (float) getSitUpAnimationTicks();
                sitProgress -= decrementRate;
                if (sitProgress < 0f) {
                    sitProgress = 0f;
                }
                setSitProgress(sitProgress);
            }
        }
    }

    private void tickClientSideUpdates() {
        if (level().isClientSide) {
            // Smooth client-side swim pitch to prevent jitter from network updates
            prevClientSwimPitchRad = clientSwimPitchRad;
            float targetPitch = this.entityData.get(DATA_SWIM_PITCH_RAD);
            clientSwimPitchRad = Mth.lerp(0.5f, clientSwimPitchRad, targetPitch);
        }
    }


    private void tickFeedingCooldown() {
        int cooldownTicks = this.entityData.get(DATA_FEEDING_COOLDOWN);
        if (cooldownTicks > 0) {
            cooldownTicks--;
            this.entityData.set(DATA_FEEDING_COOLDOWN, cooldownTicks);
        }
    }

    private void tickPhaseTwoLinger() {
        if (this.isVehicle()) {
            phaseTwoLingerTicks = 0;
            return;
        }

        if (!this.isPhaseTwoActive()) {
            phaseTwoLingerTicks = 0;
            return;
        }

        if (this.isAbilityActive(NulljawAbilities.NULLJAW_PHASE_SHIFT)) {
            return;
        }

        LivingEntity target = this.getTarget();
        boolean targetTooFar = target != null && isTargetTooFar(target);

        if (target == null || targetTooFar) {
            if (phaseTwoLingerTicks <= 0) {
                phaseTwoLingerTicks = PHASE_TWO_LINGER_TICKS;
            } else {
                phaseTwoLingerTicks--;
            }

            if (phaseTwoLingerTicks <= 0) {
                this.combatManager.tryUseAbility(NulljawAbilities.NULLJAW_PHASE_SHIFT);
            }
        } else {
            phaseTwoLingerTicks = 0;
        }
    }

    private boolean isTargetTooFar(LivingEntity target) {
        double followRange = this.getAttributeValue(Attributes.FOLLOW_RANGE);
        if (followRange <= 0.0D) {
            followRange = 16.0D;
        }
        double maxDistanceSq = followRange * followRange;
        return this.distanceToSqr(target) > maxDistanceSq;
    }

    // ===== SIT TRANSITION HELPERS =====

    public boolean isInSitTransition() {
        return isSittingDown || isStandingUp;
    }
    public boolean isSittingDownAnimation() {
        return isSittingDown;
    }
    public boolean isStandingUpAnimation() {
        return isStandingUp;
    }

    @Override
    public float maxSitTicks() {
        return 38.0F; // down animation is 1.88s = 38 ticks
    }

    public int getSitDownAnimationTicks() {
        return 38; // down animation is 1.88s = 38 ticks
    }

    public int getSitUpAnimationTicks() {
        return 38; // up animation is 1.88s = 38 ticks
    }

    public int getFallAsleepAnimationTicks() {
        return 38; // fall_asleep animation is 1.88s = 38 ticks
    }

    public int getWakeUpAnimationTicks() {
        return 38; // up animation is 1.88s = 38 ticks
    }


    // ===== SLEEP SYSTEM =====

    @Override
    public boolean supportsSleep() {
        return true;
    }

    @Override
    protected boolean useSleepSitDownTimer() {
        return true;
    }

    @Override
    protected boolean requireSeatedBeforeFallAsleep() {
        return true;
    }

    @Override
    protected boolean sleepForceSitDownOnEnter() {
        return true;
    }

    @Override
    protected int getSleepSitDownDuration() {
        return getSitDownAnimationTicks();
    }

    @Override
    protected int getSleepFallAsleepDuration() {
        return getFallAsleepAnimationTicks();
    }

    @Override
    protected int getSleepWakeUpDuration() {
        return getWakeUpAnimationTicks();
    }

    @Override
    protected int getSleepSitUpDuration() {
        return getSitUpAnimationTicks();
    }

    @Override
    protected int getSleepLoopLeadTicks() {
        return 3;
    }

    @Override
    protected void onSleepLockCommand(int snapshot) {
        if (level().isClientSide) {
            return;
        }
        this.setOrderedToSit(true);
        this.getNavigation().stop();
        this.setTarget(null);
        this.setDeltaMovement(Vec3.ZERO);
        this.setRunning(false);
        setGroundMoveStateFromAI(0);
        if (this.getCommand() != 1) {
            this.setCommand(1);
        }
    }

    @Override
    protected void onSleepUnlockCommand(int desired) {
        if (level().isClientSide) {
            return;
        }
        if (desired >= 0 && desired != this.getCommand()) {
            this.setCommand(desired);
            this.setOrderedToSit(desired == 1);
        }
    }

    @Override
    protected void onSleepFreezeTick() {
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);
        this.setRunning(false);
        setGroundMoveStateFromAI(0);
    }

    @Override
    protected void onSleepSitDownAnimation() {
        animationHandler.triggerSitDownAnimation();
    }

    @Override
    protected void onSleepFallAsleepAnimation() {
        animationHandler.triggerFallAsleepAnimation();
    }

    @Override
    protected void onSleepLoopAnimation() {
        animationHandler.triggerSleepAnimation();
    }

    @Override
    protected void onSleepWakeUpAnimation() {
        animationHandler.triggerWakeUpAnimation();
    }

    @Override
    protected void onSleepSitUpAnimation() {
        animationHandler.triggerSitUpAnimation();
        setOrderedToSit(false);
    }

    @Override
    protected void onSleepExitSeated() {
        setSitProgress(Math.max(getSitProgress(), maxSitTicks()));
    }

    @Override
    protected void onSleepExitStarted() {
    }

    @Override
    protected void onSleepWakeUpImmediate() {
    }

    @Override
    public DragonSleepBehavior.DragonSleepPreferences getSleepPreferences() {
        // Nulljaw are nocturnal sleepers (sleep at night, active during day)
        return DragonSleepBehavior.DragonSleepPreferences.NOCTURNAL();
    }

    @Override
    public boolean canSleepNow() {
        return !isVehicle() && !this.isInWaterOrBubble() && getActiveAbility() == null && !isPhaseTwoActive();
    }

    private void tickScreenShake() {
        if (level().isClientSide) {
            prevScreenShakeAmount = screenShakeAmount;
            screenShakeAmount = this.entityData.get(DATA_SCREEN_SHAKE_AMOUNT);
            return;
        }

        prevScreenShakeAmount = screenShakeAmount;
        if (screenShakeAmount > 0.0F) {
            float newAmount = Math.max(0.0F, screenShakeAmount - SHAKE_DECAY_PER_TICK);
            screenShakeAmount = newAmount;
            this.entityData.set(DATA_SCREEN_SHAKE_AMOUNT, newAmount);
        } else if (this.entityData.get(DATA_SCREEN_SHAKE_AMOUNT) != 0.0F) {
            this.entityData.set(DATA_SCREEN_SHAKE_AMOUNT, 0.0F);
        }
    }

    @Override
    public boolean hurt(@javax.annotation.Nonnull net.minecraft.world.damagesource.DamageSource damageSource, float amount) {
        // During dying sequence, ignore all damage (entity is already dead, playing death animation)
        if (super.isDying()) {
            return false;
        }
        // Wake if sleeping and suppress re-entry on damage
        if (isSleeping() || isSleepingEntering() || isSleepingExiting()) {
            wakeUpImmediately();
            suppressSleep(200);
        }
        return super.hurt(damageSource, amount);
    }

    /**
     * Returns a larger bounding box for frustum culling to prevent the model from
     * disappearing when the entity's collision box is off-screen but the visual model
     * (wings, tail, etc.) should still be visible.
     */
    @Override
    public net.minecraft.world.phys.AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling().inflate(8.0, 4.0, 8.0);
    }

    @Override
    public void addAdditionalSaveData(@NotNull net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        saveRideableData(tag);
        tag.putBoolean("PhaseTwo", isPhaseTwoActive());
        tag.putBoolean("RiderPitchKeyMode", isRiderPitchKeyMode());

        // Persist feeding cooldown (synced via entity data but saved for redundancy)
        tag.putInt("FeedingCooldownTicks", Math.max(0, this.entityData.get(DATA_FEEDING_COOLDOWN)));
    }

    @Override
    public void readAdditionalSaveData(@NotNull net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        loadRideableData(tag);

        // Sleep state is ephemeral - not loaded (sleep goal re-evaluates naturally)

        // Restore feeding cooldown (synced via entity data but loaded for redundancy)
        if (tag.contains("FeedingCooldownTicks")) {
            this.entityData.set(DATA_FEEDING_COOLDOWN, Math.max(0, tag.getInt("FeedingCooldownTicks")));
        }

        // Don't force wake on chunk reload - let sleep behavior re-evaluate naturally (like Naturalist mod)
        // Sleep transition states are ephemeral and will be re-evaluated by DragonSleepBehavior
        if (tag.contains("PhaseTwo")) {
            setPhaseTwoActive(tag.getBoolean("PhaseTwo"), false);
        }
        if (tag.contains("RiderPitchKeyMode")) {
            setRiderPitchKeyMode(tag.getBoolean("RiderPitchKeyMode"));
        }

        // Apply config attributes when loading from NBT (Forge fix)
        applyConfiguredAttributes();
    }

    @Override
    public void handleEntityEvent(byte eventId) {
        if (eventId == 6) {
            // Failed taming - show smoke particles ONLY, no sitting behavior at all
            if (level().isClientSide) {
                // Show smoke particles for failed taming
                for (int i = 0; i < 7; ++i) {
                    double d0 = this.random.nextGaussian() * 0.02D;
                    double d1 = this.random.nextGaussian() * 0.02D;
                    double d2 = this.random.nextGaussian() * 0.02D;
                    this.level().addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE,
                            this.getRandomX(1.0D), this.getRandomY() + 0.5D, this.getRandomZ(1.0D), d0, d1, d2);
                }
            }
            // IMPORTANT: Don't call super for event 6 - it might trigger sitting behavior
        } else if (eventId == 7) {
            // Successful taming - show hearts only, sitting is handled separately
            if (level().isClientSide) {
                // Show heart particles for successful taming
                for (int i = 0; i < 7; ++i) {
                    double d0 = this.random.nextGaussian() * 0.02D;
                    double d1 = this.random.nextGaussian() * 0.02D;
                    double d2 = this.random.nextGaussian() * 0.02D;
                    this.level().addParticle(net.minecraft.core.particles.ParticleTypes.HEART,
                            this.getRandomX(1.0D), this.getRandomY() + 0.5D, this.getRandomZ(1.0D), d0, d1, d2);
                }
            }
            // IMPORTANT: Don't call super for event 7 either - sitting is explicitly handled in mobInteract
        } else {
            // Call super for all other entity events (NOT 6 or 7)
            super.handleEntityEvent(eventId);
        }
    }

    //LOCATOR
    private final Map<String, Vec3> clientLocatorCache = new ConcurrentHashMap<>();

    public void setClientLocatorPosition(String name, Vec3 pos) {
        if (name == null || pos == null) return;
        this.clientLocatorCache.put(name, pos);
    }

    public Vec3 getClientLocatorPosition(String name) {
        if (name == null) return null;
        return this.clientLocatorCache.get(name);
    }

    @Override
    public float getScreenShakeAmount(float partialTicks) {
        float currentAmount = this.entityData.get(DATA_SCREEN_SHAKE_AMOUNT);
        return prevScreenShakeAmount + (currentAmount - prevScreenShakeAmount) * partialTicks;
    }

    @Override
    public double getShakeDistance() {
        return 18.0;
    }

    @Override
    public boolean canFeelShake(Entity player) {
        return true;
    }

    public void triggerScreenShake(float intensity) {
        float clamped = Math.max(0.0F, intensity);
        if (clamped <= 0.0F) {
            return;
        }
        if (level().isClientSide) {
            return;
        }
        screenShakeAmount = Math.max(screenShakeAmount, clamped);
        this.entityData.set(DATA_SCREEN_SHAKE_AMOUNT, screenShakeAmount);
    }

    public boolean canBeBound() {
        return !isDying()
                && !isAccelerating()
                && !areRiderControlsLocked()
                && !isAbilityActive(NulljawAbilities.NULLJAW_PHASE_SHIFT);
    }

    // ===== AMPHIBIOUS BEHAVIOR =====

    @Override
    public boolean shouldEnterWater() {
        // Don't enter water if sitting or being ridden
        if (this.isOrderedToSit() || this.isVehicle()) {
            return false;
        }

        // Emergency: Seek water when on fire or low health with a target
        if (this.isOnFire()) {
            return true;
        }
        if (this.getHealth() < this.getMaxHealth() * 0.5F && this.getTarget() != null) {
            return true;
        }

        // Natural patrol behavior - semi-aquatic predator rotates between environments
        // After ~50 seconds on land (1000 ticks), has 8% chance per check to enter water
        return ticksOutOfWater > 1000 && this.getRandom().nextFloat() < 0.08F;
    }

    @Override
    public boolean shouldLeaveWater() {
        // Don't leave water if sitting
        if (this.isOrderedToSit()) {
            return false;
        }

        // Follow tamed owner onto land if they're far away
        if (this.isTame() && this.getOwner() != null) {
            LivingEntity owner = this.getOwner();
            if (!owner.isInWater() && this.distanceToSqr(owner) > 100.0D) { // 10 blocks
                return true;
            }
        }

        // Natural patrol behavior - after ~60 seconds in water (1200 ticks),
        // has 8% chance per check to leave for land patrol
        return ticksInWater > 1200 && this.getRandom().nextFloat() < 0.08F;
    }
}

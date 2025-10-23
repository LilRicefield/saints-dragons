/**
 * My name's Zap Van Dink. I'm a lightning wyvern.
 */
package com.leon.saintsdragons.server.entity.dragons.raevyx;

//Custom stuff
import com.leon.saintsdragons.common.particle.raevyx.RaevyxLightningArcData;
import com.leon.saintsdragons.common.particle.raevyx.RaevyxLightningStormData;
import com.leon.saintsdragons.common.registry.raevyx.RaevyxAbilities;
import com.leon.saintsdragons.server.ai.goals.raevyx.RaevyxDodgeGoal;
import com.leon.saintsdragons.server.ai.goals.raevyx.RaevyxFlightGoal;
import com.leon.saintsdragons.server.ai.goals.raevyx.RaevyxFollowOwnerGoal;
import com.leon.saintsdragons.server.ai.goals.raevyx.RaevyxGroundWanderGoal;
import com.leon.saintsdragons.server.ai.goals.raevyx.RaevyxPanicGoal;
import com.leon.saintsdragons.server.ai.goals.raevyx.RaevyxTemptGoal;
import com.leon.saintsdragons.server.ai.goals.raevyx.*;
import com.leon.saintsdragons.server.ai.navigation.DragonFlightMoveHelper;
import com.leon.saintsdragons.server.entity.controller.raevyx.RaevyxPhysicsController;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.DragonGender;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.interfaces.*;
import com.leon.saintsdragons.server.entity.controller.raevyx.RaevyxFlightController;
import com.leon.saintsdragons.server.entity.handler.DragonKeybindHandler;
import com.leon.saintsdragons.server.entity.interfaces.DragonSoundProfile;
import com.leon.saintsdragons.server.entity.dragons.raevyx.handlers.RaevyxInteractionHandler;
import com.leon.saintsdragons.server.entity.dragons.raevyx.handlers.RaevyxAnimationHandler;
import com.leon.saintsdragons.server.entity.dragons.raevyx.handlers.RaevyxSoundProfile;
import static com.leon.saintsdragons.server.entity.dragons.raevyx.handlers.RaevyxConstantsHandler.*;
import com.leon.saintsdragons.server.entity.interfaces.ElectricalConductivityCapable;
import com.leon.saintsdragons.server.entity.conductivity.ElectricalConductivityProfile;
import com.leon.saintsdragons.server.entity.conductivity.ElectricalConductivityState;
import com.leon.saintsdragons.server.entity.controller.raevyx.RaevyxRiderController;
import com.leon.saintsdragons.server.entity.handler.DragonSoundHandler;
import com.leon.saintsdragons.util.DragonMathUtil;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.common.registry.AbilityRegistry;
import com.leon.saintsdragons.common.network.DragonRiderAction;
import java.util.Map;

//Minecraft
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;


//GeckoLib
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.keyframe.event.SoundKeyframeEvent;

//WHO ARE THESE SUCKAS
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;
import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

//Just everything
public class Raevyx extends RideableDragonBase implements FlyingAnimal, RangedAttackMob,
        DragonCombatCapable, DragonFlightCapable, DragonSleepCapable, ShakesScreen, SoundHandledDragon, DragonControlStateHolder, ElectricalConductivityCapable {
    public static final float MAX_BEAM_YAW_DEG = 40.0f;
    public static final float MAX_BEAM_PITCH_DEG = 50.0f;
    public static final float[] IDLE_NECK_WEIGHTS = {0.10f, 0.15f, 0.20f, 0.25f};
    public static final float[] BEAM_NECK_WEIGHTS = {0.18f, 0.22f, 0.26f, 0.30f};

    // Simple per-field caches - more maintainable than generic system
    private double cachedOwnerDistance = Double.MAX_VALUE;
    private int ownerDistanceCacheTime = -1;
    private List<Projectile> cachedNearbyProjectiles = new java.util.concurrent.CopyOnWriteArrayList<>();
    private int nearbyProjectilesCacheTime = -1;
    private int projectileCacheIntervalTicks = 3; // dynamic backoff (min 3)
    private int emptyProjectileScans = 0;
    private double cachedHorizontalSpeed = 0.0;
    private int horizontalSpeedCacheTime = -1;
    private static final double RIDER_GLIDE_ALTITUDE_THRESHOLD = 40.0D;
    private static final double RIDER_GLIDE_ALTITUDE_EXIT = 30.0D; // Hysteresis: exit at lower altitude
    private static final double RIDER_LANDING_BLEND_ALTITUDE = 8.5D;
    private static final int RIDER_LANDING_BLEND_DURATION = 5; // ticks to keep landing blend active after triggering
    private boolean inHighAltitudeGlide = false; // Track glide state for smooth transitions
    private static final Map<String, VocalEntry> VOCAL_ENTRIES = new VocalEntryBuilder()
            .add("grumble1", "action", "animation.raevyx.grumble1", ModSounds.RAEVYX_GRUMBLE_1, 0.8f, 0.95f, 0.1f, false, false, false)
            .add("grumble2", "action", "animation.raevyx.grumble2", ModSounds.RAEVYX_GRUMBLE_2, 0.8f, 0.95f, 0.1f, false, false, false)
            .add("grumble3", "action", "animation.raevyx.grumble3", ModSounds.RAEVYX_GRUMBLE_3, 0.8f, 0.95f, 0.1f, false, false, false)
            .add("purr", "action", "animation.raevyx.purr", ModSounds.RAEVYX_PURR, 0.8f, 1.05f, 0.05f, true, false, true)
            .add("snort", "action", "animation.raevyx.snort", ModSounds.RAEVYX_SNORT, 0.9f, 0.9f, 0.2f, false, false, false)
            .add("chuff", "action", "animation.raevyx.chuff", ModSounds.RAEVYX_CHUFF, 0.9f, 0.9f, 0.2f, false, false, false)
            .add("content", "action", "animation.raevyx.content", ModSounds.RAEVYX_CONTENT, 0.8f, 1.0f, 0.1f, true, false, true)
            .add("excited", "action", "", ModSounds.RAEVYX_EXCITED, 1.0f, 1.0f, 0.3f, false, false, false)  // Sound-only, no animation
            .add("annoyed", "action", "animation.raevyx.annoyed", ModSounds.RAEVYX_ANNOYED, 1.0f, 0.9f, 0.2f, false, false, false)
            .add("growl_warning", "action", "", ModSounds.RAEVYX_GROWL_WARNING, 1.2f, 0.8f, 0.4f, false, false, false)  // Sound-only, no animation
            .add("roar", "action", "animation.raevyx.roar", ModSounds.RAEVYX_ROAR, 1.4f, 0.9f, 0.15f, false, false, false)
            .add("roar_ground", "action", "animation.raevyx.roar_ground", ModSounds.RAEVYX_ROAR, 1.4f, 0.9f, 0.15f, false, false, false)
            .add("roar_air", "action", "animation.raevyx.roar_air", ModSounds.RAEVYX_ROAR, 1.4f, 0.9f, 0.15f, false, false, false)
            .add("raevyx_hurt", "action", "animation.raevyx.hurt", ModSounds.RAEVYX_HURT, 1.2f, 0.95f, 0.1f, true, true, true)
            .add("raevyx_die", "action", "animation.raevyx.die", ModSounds.RAEVYX_DIE, 1.5f, 0.95f, 0.1f, false, true, true)
            .build();

    private boolean manualSitCommand = false;
    private boolean commandChangeManual = false;
    private int riderLandingBlendTicks = 0;

    @Override
    public void setCommand(int command) {
        int previous = super.getCommand();
        super.setCommand(command);
        if (command == 1) {
            this.manualSitCommand = this.commandChangeManual || this.manualSitCommand;
        } else {
            this.manualSitCommand = false;
        }
        this.commandChangeManual = false;
    }

    public void setCommandManual(int command) {
        this.commandChangeManual = true;
        this.setCommand(command);
    }

    public void setCommandAuto(int command) {
        this.commandChangeManual = false;
        this.setCommand(command);
    }

    // ===== AMBIENT SOUND SYSTEM =====
    private int ambientSoundTimer;
    private int nextAmbientSoundDelay;
    // ===== SCREEN SHAKE SYSTEM =====
    private float prevScreenShakeAmount = 0.0F;
    private float screenShakeAmount = 0.0F;
    // ===== ENTITY DATA HELPER METHODS =====
    /**
     * Helper method for boolean entity data access
     */
    private boolean getBooleanData(EntityDataAccessor<Boolean> accessor) {
        return this.entityData.get(accessor);
    }

    /**
     * Helper method for boolean entity data setting
     */
    private void setBooleanData(EntityDataAccessor<Boolean> accessor, boolean value) {
        this.entityData.set(accessor, value);
    }
    
    /**
     * Helper method for integer entity data access
     */
    private int getIntegerData(EntityDataAccessor<Integer> accessor) {
        return this.entityData.get(accessor);
    }
    
    /**
     * Helper method for integer entity data setting
     */
    private void setIntegerData(EntityDataAccessor<Integer> accessor, int value) {
        this.entityData.set(accessor, value);
    }
    
    /**
     * Helper method for float entity data access
     */
    private float getFloatData(EntityDataAccessor<Float> accessor) {
        return this.entityData.get(accessor);
    }

    /**
     * Helper method for float entity data setting
     */
    private void setFloatData(EntityDataAccessor<Float> accessor, float value) {
        this.entityData.set(accessor, value);
    }

    // ===== STATE VARIABLES (Package-private for controller access) =====
    public int timeFlying = 0;
    public boolean landingFlag = false;

    public int landingTimer = 0;
    int runningTicks = 0;
    // Banking smoothing state
    private float bankSmoothedYaw = 0f;
    private int bankHoldTicks = 0;
    private int bankDir = 0; // -1 left, 0 none, 1 right
    private float bankAngle = 0f;
    private float prevBankAngle = 0f;

    // Pitching smoothing state
    private float pitchSmoothedPitch = 0f;
    private int pitchHoldTicks = 0;
    private int pitchDir = 0; // -1 down, 0 none, 1 up

    // Dodge system
    boolean dodging = false;
    int dodgeTicksLeft = 0;
    Vec3 dodgeVec = Vec3.ZERO;
    // ===== NEW ATTACK STATE SYSTEM (Cataclysm-style) =====
    /** Attack state timing counter */
    public int attackTicks = 0;
    
    /** Attack cooldown timer */
    public int attackCooldown = 0;
    private boolean allowGroundBeamDuringStorm = false;
    // Sit transition state
    // Track sit down/up animations separately from sitProgress (which is for sit pose interpolation)
    private int sitTransitionTicks = 0; // Counts down during down/up animations
    private boolean isSittingDown = false; // True during "down" animation (93 ticks)
    private boolean isStandingUp = false;  // True during "up" animation (23 ticks)

    // Sleep transition state
    // Sleep transition states now synced via entity data (DATA_SLEEPING_ENTERING, DATA_SLEEPING_EXITING)
    // Removed public boolean fields in favor of synced entity data
    private int sleepTransitionTicks = 0;
    // Tiny ambient resume buffer after exit completes
    private int sleepAmbientCooldownTicks = 0;
    // Re-entry suppression after aggression/damage to prevent instant resleep
    private int sleepReentryCooldownTicks = 0;
    // Hard-stop flag to kill sleep clips immediately across ticks
    private int sleepCancelTicks = 0;
    private boolean sleepLocked = false;
    private int sleepCommandSnapshot = -1;

    // Post-load stabilization to preserve midair riding state across save/load
    private int postLoadAirStabilizeTicks = 0; // counts down after world load if we saved while flying
    private int followFailsafeCooldown = 0;
    private int postStandUnlockTicks = 0;

    // Rider takeoff request timer: while > 0, flight controller treats state as takeoff
    private int riderTakeoffTicks = 0;


    // Last landing completion time (server game time). Used for takeoff cooldowns.
    private long lastLandingGameTime = Long.MIN_VALUE;

    public long getLastLandingGameTime() {
        return lastLandingGameTime;
    }


    public void markLandedNow() {
        if (!level().isClientSide) {
            this.lastLandingGameTime = level().getGameTime();
        }
    }


    // Navigation (Package-private for controller access)
    public final GroundPathNavigation groundNav;
    public final FlyingPathNavigation airNav;
    public boolean usingAirNav = false;

    // ===== CONTROLLER INSTANCES =====
    public final RaevyxFlightController flightController;
    private final RaevyxInteractionHandler lightningInteractionHandler;
    private final RaevyxAnimationHandler animationHandler;

    // ===== SPECIALIZED HANDLER SYSTEMS =====
    private final DragonKeybindHandler keybindHandler;
    private final RaevyxRiderController riderController;
    private final DragonSoundHandler soundHandler;

    // ===== CLIENT LOCATOR CACHE (client-side only) =====
    private final Map<String, Vec3> clientLocatorCache = new ConcurrentHashMap<>();

    // ===== CUSTOM SITTING SYSTEM =====
    // Completely replace TamableAnimal's broken sitting behavior


    @Override
    public boolean isInSittingPose() {
        return super.isInSittingPose() && !(this.isVehicle() || this.isPassenger() || this.isFlying());
    }
    // GeckoLib cache is now handled by base DragonEntity class

    //FLIGHT
    public float getGlidingFraction() {
        return animationController.glidingFraction;
    }
    public float getFlappingFraction() {
        return animationController.flappingFraction;
    }
    public float getHoveringFraction() {
        return animationController.hoveringFraction;
    }
    private final RaevyxPhysicsController animationController = new RaevyxPhysicsController(this);

    // Animation controller is internal-only; external integration goes via GeckoLib controllers.


    public Raevyx(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        this.setMaxUpStep(1.25F);

        // Initialize both navigators with custom pathfinding
        this.groundNav = new com.leon.saintsdragons.server.ai.navigation.DragonPathNavigateGround(this, level);
        this.airNav = new FlyingPathNavigation(this, level) {
            @Override
            public boolean isStableDestination(@Nonnull BlockPos pos) {
                return !this.level.getBlockState(pos.below()).isAir();
            }
        };
        this.airNav.setCanOpenDoors(false);
        this.airNav.setCanFloat(false);
        this.airNav.setCanPassDoors(false);

        // Start with ground navigation
        this.navigation = this.groundNav;
        this.moveControl = new MoveControl(this);
        this.lookControl = new DragonLookController(this);

        // Pathfinding setup
        this.setPathfindingMalus(BlockPathTypes.DANGER_FIRE, -1.0F);
        this.setPathfindingMalus(BlockPathTypes.WATER, -1.0F);
        this.setPathfindingMalus(BlockPathTypes.WATER_BORDER, -1.0F);
        this.setPathfindingMalus(BlockPathTypes.FENCE, -1.0F);

        // Initialize controllers
        this.flightController = new RaevyxFlightController(this);
        this.lightningInteractionHandler = new RaevyxInteractionHandler(this);
        this.animationHandler = new RaevyxAnimationHandler(this);

        // Initialize specialized handler systems
        this.keybindHandler = new DragonKeybindHandler(this);
        this.riderController = new RaevyxRiderController(this);
        this.soundHandler = new DragonSoundHandler(this);

        // Desynchronize ambient system across instances to avoid synchronized vocals/animations
        RandomSource rng = this.getRandom();
        this.ambientSoundTimer = rng.nextInt(80); // small random offset
        this.nextAmbientSoundDelay = MIN_AMBIENT_DELAY + rng.nextInt(MAX_AMBIENT_DELAY - MIN_AMBIENT_DELAY);

    }

    // ===== HANDLER ACCESS METHODS (expose only what is used externally) =====

    public DragonSoundHandler getSoundHandler() {
        return soundHandler;
    }

    // Client-only: stash per-frame sampled locator world positions
    public void setClientLocatorPosition(String name, Vec3 pos) {
        if (name == null || pos == null) return;
        this.clientLocatorCache.put(name, pos);
    }

    public Vec3 getClientLocatorPosition(String name) {
        if (name == null) return null;
        return this.clientLocatorCache.get(name);
    }

    public boolean isStayOrSitMuted() {
        return this.isOrderedToSit() || this.isInSittingPose();
    }

    @Override
    protected void playStepSound(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        // Intentionally empty — step sounds are driven by GeckoLib keyframes (step1/step2)
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 180.0D)
                .add(Attributes.MOVEMENT_SPEED, WALK_SPEED)
                .add(Attributes.FOLLOW_RANGE, 80.0D)
                .add(Attributes.FLYING_SPEED, 1.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ARMOR, 8.0D);
    }

    // Cooldown to prevent hurt sound spam when ridden or under rapid hits
    private int hurtSoundCooldown = 0;
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        defineRideableDragonData();
        // Define Lightning Dragon specific data
        this.entityData.define(DATA_ATTACK_STATE, ATTACK_STATE_IDLE);
        this.entityData.define(DATA_SCREEN_SHAKE_AMOUNT, 0.0F);
        this.entityData.define(DATA_BEAMING, false);
        this.entityData.define(DATA_RIDER_LANDING_BLEND, false);
        this.entityData.define(DATA_RIDER_LOCKED, false);
        this.entityData.define(DATA_SLEEPING_ENTERING, false);
        this.entityData.define(DATA_SLEEPING_EXITING, false);
        this.entityData.define(DATA_BEAM_END_SET, false);
        this.entityData.define(DATA_BEAM_END_X, 0f);
        this.entityData.define(DATA_BEAM_END_Y, 0f);
        this.entityData.define(DATA_BEAM_END_Z, 0f);
        this.entityData.define(DATA_BEAM_START_SET, false);
        this.entityData.define(DATA_BEAM_START_X, 0f);
        this.entityData.define(DATA_BEAM_START_Y, 0f);
        this.entityData.define(DATA_BEAM_START_Z, 0f);
        this.entityData.define(DATA_SLEEPING, false);
    }

    @Override
    protected void defineRideableDragonData() {
        // Define all rideable wyvern data keys for LightningDragonEntity
        this.entityData.define(DATA_FLYING, false);
        this.entityData.define(DATA_TAKEOFF, false);
        this.entityData.define(DATA_HOVERING, false);
        this.entityData.define(DATA_LANDING, false);
        this.entityData.define(DATA_RUNNING, false);
        this.entityData.define(DATA_GROUND_MOVE_STATE, 0);
        this.entityData.define(DATA_FLIGHT_MODE, -1);
        this.entityData.define(DATA_RIDER_FORWARD, 0f);
        this.entityData.define(DATA_RIDER_STRAFE, 0f);
        this.entityData.define(DATA_GOING_UP, false);
        this.entityData.define(DATA_GOING_DOWN, false);
        this.entityData.define(DATA_ACCELERATING, false);
    }

    // Implementation of abstract accessor methods
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
    @SuppressWarnings("unchecked")
    public <T extends DragonEntity> DragonAbility<T> getActiveAbility() {
        return (DragonAbility<T>) combatManager.getActiveAbility();
    }
    public boolean canUseAbility() {
        return !isBaby() && combatManager.canUseAbility();
    }
    public void useRidingAbility(String abilityName) {
        if (isBaby()) {
            return;
        }
        if (abilityName == null || abilityName.isEmpty()) return;
        // Only allow when actually being ridden by a living controller (owner ideally)
        var cp = getControllingPassenger();
        if (!(cp instanceof net.minecraft.world.entity.LivingEntity)) {
            return;
        }
        // Block during rider control lock (e.g., Summon Storm windup)
        if (areRiderControlsLocked()) {
            return;
        }
        if (this.isTame() && cp instanceof net.minecraft.world.entity.player.Player p && !this.isOwnedBy(p)) {
            return; // owner-gate abilities on tamed dragons
        }
        var type = AbilityRegistry.get(abilityName);
        if (type != null) {
            // Delegate to combat manager which handles proper generic casting
            combatManager.tryUseAbility(type);
        }
    }

    @Override
    public <T extends DragonEntity> void tryActivateAbility(DragonAbilityType<T, ?> abilityType) {
        if (isBaby()) {
            return;
        }
        super.tryActivateAbility(abilityType);
    }


    @Override
    protected boolean isRiderInputLocked(Player player) {
        return areRiderControlsLocked();
    }

    @Override
    protected void applyRiderVerticalInput(Player player, boolean goingUp, boolean goingDown, boolean locked) {
        if (this.isFlying()) {
            setGoingUp(goingUp);
            setGoingDown(goingDown);
        } else {
            setGoingUp(false);
            setGoingDown(false);
        }
    }

    @Override
    protected void applyRiderMovementInput(Player player, float forward, float strafe, float yaw, boolean locked) {
        float fwd = applyInputDeadzone(forward);
        float str = applyInputDeadzone(strafe);
        setLastRiderForward(fwd);
        setLastRiderStrafe(str);
        if (!isFlying()) {
            int moveState = 0;
            float magnitude = Math.abs(fwd) + Math.abs(str);
            if (magnitude > 0.05f) {
                moveState = this.isAccelerating() ? 2 : 1;
            }
            if (this.getEntityData().get(DATA_GROUND_MOVE_STATE) != moveState) {
                this.getEntityData().set(DATA_GROUND_MOVE_STATE, moveState);
                this.syncAnimState(moveState, this.getSyncedFlightMode());
            }
        }
    }

    @Override
    protected void handleRiderAction(ServerPlayer player, DragonRiderAction action, String abilityName, boolean locked) {
        if (action == null) {
            return;
        }
        switch (action) {
            case TAKEOFF_REQUEST -> {
                if (!locked) {
                    requestRiderTakeoff();
                }
            }
            case ACCELERATE -> {
                if (!locked) {
                    setAccelerating(true);
                }
            }
            case STOP_ACCELERATE -> setAccelerating(false);
            case ABILITY_USE -> {
                if (abilityName != null && !abilityName.isEmpty()) {
                    useRidingAbility(abilityName);
                }
            }
            case ABILITY_STOP -> {
                if (abilityName != null && !abilityName.isEmpty()) {
                    var active = getActiveAbility();
                    if (active != null) {
                        forceEndActiveAbility();
                    }
                }
            }
            default -> { }
        }
    }


    /**
     * Forces the wyvern to take off when being ridden. Called when player presses Space while on ground.
     */
    public void requestRiderTakeoff() {
        riderController.requestRiderTakeoff();
    }

    // (External callers should use triggerable action keys on the GeckoLib controller.)

    @Override
    public Vec3 getHeadPosition() {
        return getEyePosition();
    }
    @Override
    public Vec3 getMouthPosition() {
        // Try to use bone-based mouth position from renderer cache (most accurate!)
        Vec3 mouthLoc = getClientLocatorPosition("mouth_origin");
        if (mouthLoc != null) {
            return mouthLoc;
        }
        // Fallback to computed position if bone data not available (server-side, etc.)
        return computeHeadMouthOrigin(1.0f);
    }

    /**
     * Compute a mouth origin in world space from head yaw/pitch and a fixed local offset.
     * FALLBACK ONLY - bone-based positioning is preferred and more accurate!
     */
    public Vec3 computeHeadMouthOrigin(float partialTicks) {
        double x = Mth.lerp(partialTicks, this.xo, this.getX());
        double y = Mth.lerp(partialTicks, this.yo, this.getY());
        double z = Mth.lerp(partialTicks, this.zo, this.getZ());

        float yawDeg = Mth.lerp(partialTicks, this.yHeadRotO, this.yHeadRot);
        float pitchDeg = Mth.lerp(partialTicks, this.xRotO, this.getXRot());

        double yaw = Math.toRadians(yawDeg);
        double pitch = Math.toRadians(pitchDeg);

        // Local offsets (Right=X, Up=Y, Forward=Z)
        // Small rightward nudge (negative flips to the other side in world yaw math)
        double R = (-0.5 / 16.0) * MODEL_SCALE;
        double U = (6.6 / 16.0) * MODEL_SCALE;
        double F = (14.65 / 16.0) * MODEL_SCALE;

        // Pitch around X axis - transform Up and Forward components
        double cp = Math.cos(pitch), sp = Math.sin(pitch);
        // Local offsets after applying pitch (around X axis)
        double up = U * cp - F * sp;       // Y component after pitch
        double fwd = U * sp + F * cp;      // Z component after pitch

        // Yaw around Y axis - transform all components into world space
        double cy = Math.cos(yaw), sy = Math.sin(yaw);
        double offX = R * cy - fwd * sy; // World X after yaw
        double offZ = R * sy + fwd * cy; // World Z after yaw

        return new Vec3(x + offX, y + up, z + offZ);
    }

    public void forceEndActiveAbility() {
        combatManager.forceEndActiveAbility();
    }

    public boolean isBeaming() { return getBooleanData(DATA_BEAMING); }
    public void setBeaming(boolean beaming) {
        boolean wasBeaming = getBooleanData(DATA_BEAMING);
        setBooleanData(DATA_BEAMING, beaming);
        if (!beaming || !wasBeaming) {
            resetBeamAim();
        }
    }

    // (No client/server rider anchor fields; seat uses math-based head-space anchor)

    // ===== BEAM END SYNC + CLIENT LERP =====
    private Vec3 prevClientBeamEnd = null;
    private Vec3 clientBeamEnd = null;
    private Vec3 beamLookLerp = null;
    private Vec3 beamAimDir = null;
    private float beamYawOffsetRad = 0.0f;
    private float beamPitchOffsetRad = 0.0f;

    public void setBeamEndPosition(@org.jetbrains.annotations.Nullable Vec3 pos) {
        if (pos == null) {
            this.entityData.set(DATA_BEAM_END_SET, false);
        } else {
            this.entityData.set(DATA_BEAM_END_SET, true);
            this.entityData.set(DATA_BEAM_END_X, (float) pos.x);
            this.entityData.set(DATA_BEAM_END_Y, (float) pos.y);
            this.entityData.set(DATA_BEAM_END_Z, (float) pos.z);
        }
    }

    public Vec3 getBeamEndPosition() {
        if (!getBooleanData(DATA_BEAM_END_SET)) return null;
        return new Vec3(
                getFloatData(DATA_BEAM_END_X),
                getFloatData(DATA_BEAM_END_Y),
                getFloatData(DATA_BEAM_END_Z)
        );
    }

    public Vec3 getClientBeamEndPosition(float partialTicks) {
        if (clientBeamEnd != null && prevClientBeamEnd != null) {
            Vec3 d = clientBeamEnd.subtract(prevClientBeamEnd);
            return prevClientBeamEnd.add(d.scale(partialTicks));
        }
        Vec3 serverPos = getBeamEndPosition();
        return clientBeamEnd != null ? clientBeamEnd : (serverPos != null ? serverPos : Vec3.ZERO);
    }

    public void setBeamStartPosition(@org.jetbrains.annotations.Nullable Vec3 pos) {
        if (pos == null) {
            this.entityData.set(DATA_BEAM_START_SET, false);
        } else {
            this.entityData.set(DATA_BEAM_START_SET, true);
            this.entityData.set(DATA_BEAM_START_X, (float) pos.x);
            this.entityData.set(DATA_BEAM_START_Y, (float) pos.y);
            this.entityData.set(DATA_BEAM_START_Z, (float) pos.z);
        }
    }

    public Vec3 getBeamStartPosition() {
        if (!getBooleanData(DATA_BEAM_START_SET)) return null;
        return new Vec3(
                getFloatData(DATA_BEAM_START_X),
                getFloatData(DATA_BEAM_START_Y),
                getFloatData(DATA_BEAM_START_Z)
        );
    }

    // ===== NAVIGATION SWITCHING =====
    public void switchToAirNavigation() {
        if (!this.usingAirNav) {
            this.navigation = this.airNav;
            this.moveControl = new DragonFlightMoveHelper(this);
            this.usingAirNav = true;
        }
    }

    public void switchToGroundNavigation() {
        if (this.usingAirNav) {
            this.navigation = this.groundNav;
            this.moveControl = new MoveControl(this);
            this.usingAirNav = false;
        }
    }

    @Override
    protected @NotNull PathNavigation createNavigation(@Nonnull Level level) {
        return new com.leon.saintsdragons.server.ai.navigation.DragonPathNavigateGround(this, level);
    }

    // ===== STATE MANAGEMENT =====

    public void setFlying(boolean flying) {
        if (flying && this.isBaby()) flying = false;

        // Prevent forced grounding when being ridden by a player (unless explicitly ordered to sit or actually on ground)
        if (!flying && isVehicle() && !isOrderedToSit() && !onGround()) {
            // Dragon is being ridden, not ordered to sit, and not actually touching ground - ignore forced grounding
            return;
        }

        boolean wasFlying = isFlying();
        this.entityData.set(DATA_FLYING, flying);

        // Reset acceleration state when transitioning between ground and flight modes
        // This prevents ground sprinting from affecting flight speed and vice versa
        if (wasFlying != flying) {
            this.setAccelerating(false);
        }

        if (wasFlying != flying) {
            if (flying) {
                switchToAirNavigation();
                setRunning(false);
            } else {
                switchToGroundNavigation();
            }
        }
    }

    public void setTakeoff(boolean takeoff) {
        if (takeoff && this.isBaby()) takeoff = false;
        this.entityData.set(DATA_TAKEOFF, takeoff);
    }

    public void setHovering(boolean hovering) {
        if (hovering && this.isBaby()) hovering = false;
        this.entityData.set(DATA_HOVERING, hovering);
    }




    public boolean isWalking() {
        if (isFlying()) return false;
        int s = level().isClientSide ? getEffectiveGroundState() : this.entityData.get(DATA_GROUND_MOVE_STATE);
        if (s == 1) return true;
        if (s == 2) return false;
        if (level().isClientSide && super.getEffectiveGroundState() < 0) {
            double speed = getDeltaMovement().horizontalDistanceSqr();
            return speed > 0.004 && speed <= 0.10;
        }
        return false;
    }

    public boolean isActuallyRunning() {
        if (isFlying()) return false;
        int s = level().isClientSide ? getEffectiveGroundState() : this.entityData.get(DATA_GROUND_MOVE_STATE);
        if (s == 2) return true;
        if (s == 1) return false;
        // Fallback: rely on synced running flag if state not yet set
        return getBooleanData(DATA_RUNNING);
    }


    public void setLanding(boolean landing) {
        // Prevent forced landing when being ridden by a player
        if (landing && isVehicle()) {
            // Dragon is being ridden, ignore landing requests to maintain player control
            return;
        }

        this.entityData.set(DATA_LANDING, landing);
        if (landing) {
            landingTimer = 0;
            this.getNavigation().stop();
            this.setTakeoff(true);
            this.landingFlag = true;
            if (!level().isClientSide) {
                this.flightController.planSmartLanding();
            }
        } else {
            this.landingFlag = false;
        }
    }

    // DATA STUFF

    // Old attack system methods removed - using new state system instead
    
    // ===== NEW ATTACK STATE SYSTEM METHODS =====
    
    /**
     * Get the current attack state (Cataclysm-style simple state system)
     */
    public int getAttackState() {
        return getIntegerData(DATA_ATTACK_STATE);
    }
    
    /**
     * Set the attack state and reset attack ticks
     */
    public void setAttackState(int state) {
        this.attackTicks = 0;
        setIntegerData(DATA_ATTACK_STATE, state);
        // Broadcast entity event for animation triggers
        this.level().broadcastEntityEvent(this, (byte) -state);
    }
    
    /**
     * Check if the wyvern is currently in an attack state (Cataclysm-style)
     */
    public boolean isInAttackState() {
        return getAttackState() != ATTACK_STATE_IDLE;
    }
    
    /**
     * Check if the wyvern can start a new attack (not on cooldown)
     */
    public boolean canAttack() {
        return !isBaby() && attackCooldown <= 0 && !isInAttackState();
    }

    // ===== Lightning Dragon Specific Methods =====
    
    // Flight mode accessor for controllers (avoids accessing protected entityData outside entity)
    public int getSyncedFlightMode() { return getIntegerData(DATA_FLIGHT_MODE); }

    // Debug/inspection helper: expose raw ground move state
    public int getGroundMoveState() { return getIntegerData(DATA_GROUND_MOVE_STATE); }
    
    // ===== RideableDragonBase Abstract Method Implementations =====
    
    @Override
    protected int getFlightMode() {
        if (!isFlying()) {
            inHighAltitudeGlide = false; // Reset when not flying
            return -1; // Ground state
        }
        if (isTakeoff()) return 3;  // Takeoff
        if (isHovering()) return 2; // Hover
        if (isLanding()) return 2;  // Landing (treat as hover)

        // Altitude-based animation for ridden dragons
        if (this.isTame() && this.isVehicle()) {
            Entity rider = this.getControllingPassenger();
            if (rider instanceof Player player && this.isOwnedBy(player)) {
                int groundY = this.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        Mth.floor(this.getX()), Mth.floor(this.getZ()));
                double altitudeAboveTerrain = this.getY() - groundY;

                // Hysteresis: Enter glide at 40 blocks, exit at 30 blocks
                if (inHighAltitudeGlide) {
                    // Already gliding - stay in glide until we drop below exit threshold
                    if (altitudeAboveTerrain > RIDER_GLIDE_ALTITUDE_EXIT) {
                        return 0; // High-altitude glide
                    } else {
                        inHighAltitudeGlide = false;
                        return 1; // Low altitude - flap
                    }
                } else {
                    // Not gliding yet - enter glide if above entry threshold
                    if (altitudeAboveTerrain > RIDER_GLIDE_ALTITUDE_THRESHOLD) {
                        inHighAltitudeGlide = true;
                        return 0; // High-altitude glide
                    } else {
                        return 1; // Low altitude - flap
                    }
                }
            }
        } else {
            // Not being ridden by owner - reset flag
            inHighAltitudeGlide = false;
        }

        // Fallback for wild/untamed dragons: Check if gliding (moving horizontally without significant vertical movement)
        double horizontalSpeedSqr = getDeltaMovement().horizontalDistanceSqr();
        double yDelta = this.getY() - this.yo;
        if (Math.abs(yDelta) < 0.06 && horizontalSpeedSqr > 0.01) {
            return 0; // Glide
        }

        return 1; // Forward flight
    }
    
    @Override
    protected boolean isDragonFlying() {
        return getBooleanData(DATA_FLYING);
    }
    
    @Override
    public boolean isTakeoff() {
        return getBooleanData(DATA_TAKEOFF);
    }
    
    @Override
    public boolean isLanding() {
        return getBooleanData(DATA_LANDING);
    }
    
    @Override
    public boolean isHovering() {
        return getBooleanData(DATA_HOVERING);
    }
    
    @Override
    public boolean isRunning() {
        return getBooleanData(DATA_RUNNING);
    }
    
    @Override
    public void setRunning(boolean running) {
        setBooleanData(DATA_RUNNING, running);
        if (running) {
            runningTicks = 0;
            Objects.requireNonNull(this.getAttribute(Attributes.MOVEMENT_SPEED)).setBaseValue(RUN_SPEED);
        } else {
            Objects.requireNonNull(this.getAttribute(Attributes.MOVEMENT_SPEED)).setBaseValue(WALK_SPEED);
        }
    }
    
    // ===== RideableDragonBase Override for Custom Logic =====
    
    @Override
    public void tickAnimationStates() {
        // Lightning Dragon already has this logic implemented in aiStep()
        // This method is here to satisfy the interface contract
        // The actual implementation is in the aiStep() method above
    }

    // ===== Client animation overrides (for robust observer sync) =====
    @Override
    public int getEffectiveGroundState() {
        return super.getEffectiveGroundState();
    }

    // Expose last-tick vertical delta for robust flight-mode decisions
    public double getYDelta() { return this.getY() - this.yo; }

    // Allow AI goals to set ground move state explicitly
    public void setGroundMoveStateFromAI(int state) {
        if (!this.level().isClientSide) {
            int s = Math.max(0, Math.min(2, state));
            if (this.entityData.get(DATA_GROUND_MOVE_STATE) != s) {
                this.entityData.set(DATA_GROUND_MOVE_STATE, s);
                this.syncAnimState(s, getSyncedFlightMode());
            }
        }
    }


    // Control state system
    private byte controlState = 0;

    @Override
    public byte getControlState() {
        return controlState;
    }

    @Override
    public void setControlState(byte controlState) {
        this.controlState = controlState;  // Update local cached control state
        // Only process keybind logic on the server
        if (!level().isClientSide) {
            keybindHandler.setControlState(controlState);
        }
    }

    @Override
    public boolean canPlayerModifyControlState(Player player) {
        return player != null && this.isOwnedBy(player);
    }

    // Riding utilities
    @Nullable
    public Player getRidingPlayer() {
        if (this.getControllingPassenger() instanceof Player player) {
            return player;
        }
        return null;
    }

    // Command get/set now inherited from base DragonEntity

    public boolean canOwnerCommand(Player ownerPlayer) {
        return ownerPlayer.isCrouching(); // Shift key pressed
    }

    @Override
    public RiderAbilityBinding getTertiaryRiderAbility() {
        return new RiderAbilityBinding(RaevyxAbilities.RAEVYX_LIGHTNING_BEAM.getName(), RiderAbilityBinding.Activation.HOLD);
    }

    @Override
    public RiderAbilityBinding getPrimaryRiderAbility() {
        return new RiderAbilityBinding(RaevyxAbilities.RAEVYX_ROAR.getName(), RiderAbilityBinding.Activation.PRESS);
    }

    @Override
    public RiderAbilityBinding getSecondaryRiderAbility() {
        return new RiderAbilityBinding(RaevyxAbilities.RAEVYX_SUMMON_STORM.getName(), RiderAbilityBinding.Activation.PRESS);
    }

    @Override
    public byte buildClientControlState(boolean ascendDown, boolean descendDown, boolean attackDown, boolean primaryDown, boolean secondaryDown, boolean sneakDown) {
        byte state = 0;
        if (ascendDown) state |= 1;
        if (descendDown) state |= 2;
        if (attackDown) state |= 4;
        if (primaryDown) state |= 8;
        if (secondaryDown) state |= 16;
        if (sneakDown) state |= 32;
        return state;
    }


    // ===== RIDING SUPPORT =====
    @Override
    public double getPassengersRidingOffset() {
        return riderController.getPassengersRidingOffset();
    }

    @Override
    protected void positionRider(@Nonnull Entity passenger, @Nonnull Entity.MoveFunction moveFunction) {
        riderController.positionRider(passenger, moveFunction);
    }

    @Override
    public @NotNull Vec3 getDismountLocationForPassenger(@Nonnull LivingEntity passenger) {
        return riderController.getDismountLocationForPassenger(passenger);
    }

    // Dodge system
    public boolean isDodging() { return dodging; }

    public void beginDodge(Vec3 vec, int ticks) {
        this.dodging = true;
        this.dodgeVec = vec;
        this.dodgeTicksLeft = Math.max(1, ticks);
        this.getNavigation().stop();
        this.hasImpulse = true;
    }

    // ===== MAIN TICK METHOD =====
    @Override
    public void tick() {
        animationController.tick();
        super.tick();
        tickScreenShake();
        tickSittingState();
        tickSound();
        tickPostLoadStabilization();
        tickRiderTakeoff();
        tickControllers();
        tickHurtSoundCooldown();
        tickAttackState();
        if (!level().isClientSide) {
            // When ridden and flying, never stay in 'hovering' unless explicitly landing or beaming or taking off
            if (isFlying() && getControllingPassenger() != null) {
                if (!isLanding() && !isBeaming() && !isTakeoff() && isHovering()) {
                    setHovering(false);
                }
            }
            tickRiderControlLock();
            tickRiderControlLockMovement();
            handleAmbientSounds();
            // no-op
        }
        if (!level().isClientSide) {
            tickSuperchargeTimer();
            tickTempInvulnTimer();
            tickSuperchargeVfx();
            tickSleepTransition();
            tickSleepCooldowns();
            tickMountingState();
            tickFollowFailsafe();
            if (postStandUnlockTicks > 0) {
                postStandUnlockTicks--;
            }
        }
        
        // Update banking and pitching logic every tick
        tickBankingLogic();
        tickPitchingLogic();

        // Wake up if mounted or target appears/aggression
        if (!level().isClientSide && (isSleeping() || isSleepingEntering() || isSleepingExiting())) {
            if (this.isVehicle()) {
                wakeUpImmediately();
                // Clear all states when mounted to ensure full control
                clearAllStatesWhenMounted();
            } else if (this.getTarget() != null || this.isAggressive()) {
                // On aggression/target, clear immediately and suppress re-entry for a short window
                wakeUpImmediately();
                suppressSleep(200); // ~10s; adjust as desired
            } else if (this.isInWaterOrBubble() || this.isInLava()) {
                // Never sleep in fluids; wake and suppress to avoid drowning
                wakeUpImmediately();
                suppressSleep(200);
            }

            // Use RideableDragonBase animation state management
            super.tickAnimationStates();
        }

        // Use RideableDragonBase animation state management for normal ticks
        if (!level().isClientSide && !(isSleeping() || isSleepingEntering() || isSleepingExiting())) {
            super.tickAnimationStates();
        }

        // Handle dodge movement first
        if (!level().isClientSide && this.isDodging()) {
            handleDodgeMovement();
            return;
        }

        tickRunningTime();
        tickBeamLook();

        if (!level().isClientSide && isBaby()) {
            if (getTarget() != null) {
                super.setTarget(null);
            }
            if (getActiveAbility() != null) {
                combatManager.forceEndActiveAbility();
            }
            setAggressive(false);
        }

        tickClientSideUpdates();
    }


    // ===== TICK SUBMETHODS =====
    
    private void tickScreenShake() {
        // Update screen shake interpolation
        prevScreenShakeAmount = screenShakeAmount;
        if (screenShakeAmount > 0) {
            screenShakeAmount = Math.max(0, screenShakeAmount - 0.34F);
            this.entityData.set(DATA_SCREEN_SHAKE_AMOUNT, this.screenShakeAmount);
        }
    }
    
    private void tickHurtSoundCooldown() {
        // Cool down hurt sound throttle
        if (hurtSoundCooldown > 0) hurtSoundCooldown--;
    }
    
    private void tickAttackState() {
        // Handle attack state timing (Cataclysm-style)
        if (this.getAttackState() > ATTACK_STATE_IDLE) {
            ++this.attackTicks;
        }
        
        // Handle attack cooldown
        if (attackCooldown > 0) {
            --attackCooldown;
        }
    }
    
    private void tickSound() {
        // Drive pending sound scheduling (both sides)
        this.getSoundHandler().tick();
    }
    
    private void tickSittingState() {
        // Clear sitting state if the wyvern is being ridden
        if (!this.level().isClientSide && this.isVehicle() && this.isOrderedToSit()) {
            this.setOrderedToSit(false);
        }
    }
    
    private boolean wasVehicleLastTick = false;
    
    private void tickMountingState() {
        // Check if wyvern just became a vehicle and clear all states
        if (!this.level().isClientSide && this.isVehicle() && !wasVehicleLastTick) {
            clearAllStatesWhenMounted();
        }
        wasVehicleLastTick = this.isVehicle();
    }
    
    /**
     * Clears all wyvern states (sleep, sit) when mounted to ensure full player control
     */
    private void clearAllStatesWhenMounted() {
        if (!this.level().isClientSide && this.isVehicle()) {
            // Clear sleep states aggressively - including any ongoing transitions
            wakeUpImmediately();
            
            // Clear sitting state and sync command value
            if (this.isOrderedToSit()) {
                this.setOrderedToSit(false);
                // If wyvern was sitting, set command to Follow (0) when mounted
                if (this.getCommand() == 1) {
                    this.setCommandAuto(0);
                }
            }
            
            // Stop any AI navigation
            if (this.getNavigation().getPath() != null) {
                this.getNavigation().stop();
            }
            
            // Suppress sleep for a longer period to prevent immediate re-entry
            suppressSleep(300); // ~15 seconds
        }
    }
    
    private void tickRunningTime() {
        // Track running time for animations
        if (this.isRunning()) {
            runningTicks++;
        } else {
            runningTicks = Math.max(0, runningTicks - 2);
        }
    }
    
    // Steer the head/neck chain toward the active beam so VFX and rig stay aligned.
    private void tickBeamLook() {
        if (!isBeaming()) {
            resetBeamAim();
            return;
        }

        Vec3 start = getBeamStartPosition();
        if (start == null) {
            start = computeHeadMouthOrigin(1.0f);
        }
        if (start == null) {
            resetBeamAim();
            return;
        }

        Vec3 aimDir = refreshBeamAimDirection(start, true);
        if (aimDir == null) {
            beamAimDir = Vec3.directionFromRotation(this.getXRot(), this.yHeadRot).normalize();
            aimDir = beamAimDir;
            updateBeamOffsets(aimDir);
        }

        Vec3 desiredLook = start.add(aimDir.scale(6.0));
        // Unified smoothing for both client and server to prevent jitter
        double alpha = 0.35D;
        beamLookLerp = beamLookLerp == null
                ? desiredLook
                : beamLookLerp.add(desiredLook.subtract(beamLookLerp).scale(alpha));

        float yawSpeed = Math.max(90.0F, (float)this.getHeadRotSpeed());
        float pitchRange = (float)this.getMaxHeadXRot();
        this.getLookControl().setLookAt(beamLookLerp.x, beamLookLerp.y, beamLookLerp.z, yawSpeed, pitchRange);
    }

    public Vec3 getBeamAimDirection() {
        return beamAimDir;
    }

    public float getBeamYawOffsetRad() {
        return beamYawOffsetRad;
    }

    public float getBeamPitchOffsetRad() {
        return beamPitchOffsetRad;
    }

    public Vec3 refreshBeamAimDirection(Vec3 start, boolean smooth) {
        Vec3 desiredDir = computeRawBeamAimDirection(start);
        if (desiredDir == null) {
            updateBeamOffsets(null);
            return null;
        }
        Vec3 clamped = clampBeamDirection(desiredDir);
        if (clamped == null) {
            updateBeamOffsets(null);
            return null;
        }

        if (beamAimDir == null) {
            beamAimDir = clamped;
        } else if (smooth) {
            // Unified smoothing for both client and server to prevent jitter
            double blend = 0.35D;
            beamAimDir = beamAimDir.add(clamped.subtract(beamAimDir).scale(blend));
            double len = beamAimDir.length();
            if (len > 1.0E-6) {
                beamAimDir = beamAimDir.scale(1.0 / len);
            } else {
                beamAimDir = clamped;
            }
        } else {
            beamAimDir = clamped;
        }

        updateBeamOffsets(beamAimDir);
        return beamAimDir;
    }

    private Vec3 computeRawBeamAimDirection(Vec3 start) {
        Entity cp = getControllingPassenger();
        if (cp instanceof LivingEntity rider) {
            Vec3 riderLook = rider.getLookAngle();
            if (riderLook.lengthSqr() > 1.0E-6) {
                return riderLook.normalize();
            }
        }

        LivingEntity target = getTarget();
        if (target != null && target.isAlive()) {
            Vec3 aimPoint = target.getEyePosition().add(0.0, -0.25, 0.0);
            Vec3 towardTarget = aimPoint.subtract(start);
            if (towardTarget.lengthSqr() > 1.0E-6) {
                return towardTarget.normalize();
            }
        }

        Vec3 fallbackDir = Vec3.directionFromRotation(this.getXRot(), this.yHeadRot);
        return fallbackDir.lengthSqr() > 1.0E-6 ? fallbackDir.normalize() : Vec3.ZERO;
    }

    private Vec3 clampBeamDirection(Vec3 desiredDir) {
        if (desiredDir == null || desiredDir.lengthSqr() < 1.0E-6) {
            updateBeamOffsets(null);
            return null;
        }

        Vec3 dir = desiredDir.normalize();

        float desiredYawDeg = (float)(Math.atan2(-dir.x, dir.z) * (180.0 / Math.PI));
        float desiredPitchDeg = (float)(-Math.atan2(dir.y, Math.sqrt(dir.x * dir.x + dir.z * dir.z)) * (180.0 / Math.PI));

        float headYaw = this.yHeadRot;
        float headPitch = this.getXRot();

        float yawErrDeg = net.minecraft.util.Mth.degreesDifference(headYaw, desiredYawDeg);
        float pitchErrDeg = desiredPitchDeg - headPitch;

        float clampedYawErr = net.minecraft.util.Mth.clamp(yawErrDeg, -MAX_BEAM_YAW_DEG, MAX_BEAM_YAW_DEG);
        float clampedPitchErr = net.minecraft.util.Mth.clamp(pitchErrDeg, -MAX_BEAM_PITCH_DEG, MAX_BEAM_PITCH_DEG);

        float finalYaw = headYaw + clampedYawErr;
        float finalPitch = headPitch + clampedPitchErr;

        Vec3 finalDir = Vec3.directionFromRotation(finalPitch, finalYaw);
        return finalDir.lengthSqr() > 1.0E-6 ? finalDir.normalize() : null;
    }

    private void resetBeamAim() {
        beamLookLerp = null;
        beamAimDir = null;
        beamYawOffsetRad = 0.0f;
        beamPitchOffsetRad = 0.0f;
    }

    private void updateBeamOffsets(@org.jetbrains.annotations.Nullable Vec3 direction) {
        if (direction == null || direction.lengthSqr() < 1.0E-6) {
            beamYawOffsetRad = 0.0f;
            beamPitchOffsetRad = 0.0f;
            return;
        }

        Vec3 dir = direction.normalize();
        float finalYawDeg = (float)(Math.atan2(-dir.x, dir.z) * (180.0 / Math.PI));
        float finalPitchDeg = (float)(-Math.atan2(dir.y, Math.sqrt(dir.x * dir.x + dir.z * dir.z)) * (180.0 / Math.PI));

        float headYaw = this.yHeadRot;
        float headPitch = this.getXRot();

        float yawOffsetDeg = net.minecraft.util.Mth.degreesDifference(headYaw, finalYawDeg);
        float pitchOffsetDeg = finalPitchDeg - headPitch;

        beamYawOffsetRad = yawOffsetDeg * net.minecraft.util.Mth.DEG_TO_RAD;
        beamPitchOffsetRad = pitchOffsetDeg * net.minecraft.util.Mth.DEG_TO_RAD;
    }

    private void tickClientSideUpdates() {
        // Update client-side sit progress and lerp beam end from synchronized data
        if (level().isClientSide) {
            prevSitProgress = sitProgress;
            sitProgress = this.entityData.get(DATA_SIT_PROGRESS);

            // Beam end/start lerp
            this.prevClientBeamEnd = this.clientBeamEnd;
            this.clientBeamEnd = getBeamEndPosition();
        }
    }
    
    private void tickRiderTakeoff() {
        // Decrement rider takeoff window (no-op while dying)
        if (!level().isClientSide && riderTakeoffTicks > 0 && !isDying()) {
            riderTakeoffTicks--;
        }
    }
    
    private void tickPostLoadStabilization() {
        // If we loaded while flying (e.g., player saved while riding in air), hold flight for a short grace period
        if (!level().isClientSide && postLoadAirStabilizeTicks > 0) {
            // Ensure air nav + flight flags are consistent
            if (!isFlying()) setFlying(true);
            if (!isTakeoff()) setTakeoff(true);
            setLanding(false);
            setHovering(true);
            switchToAirNavigation();

            // Reset flight timer so auto-landing logic doesn't immediately cancel flight
            timeFlying = Math.min(timeFlying, 5);
            landingFlag = false;
            landingTimer = 0;

            // Give stronger buoyancy to counter immediate drop before rider inputs arrive
            var v = getDeltaMovement();
            if (v.y < 0.05) {
                // Apply upward force to prevent drifting down
                setDeltaMovement(v.x * 0.95, Math.max(0.05, v.y + 0.02), v.z * 0.95);
            }

            postLoadAirStabilizeTicks--;
        }
    }
    
    private void tickControllers() {
        // Delegate to controllers (disabled while dying)
        if (!isDying()) {
            flightController.handleFlightLogic();
        }
        updateSittingProgress();
    }

    private void updateSittingProgress() {
        if (level().isClientSide) {
            return;
        }

        // Tick down sit transition animations
        if (sitTransitionTicks > 0) {
            sitTransitionTicks--;
            if (sitTransitionTicks == 0) {
                // Transition animation finished
                isSittingDown = false;
                isStandingUp = false;
            }
        }

        if (this.isOrderedToSit()) {
            // Trigger sit_down animation when STARTING to sit (transition from 0 → 1)
            if (sitProgress == 0f && !isSittingDown) {
                animationHandler.triggerSitDownAnimation();
                isSittingDown = true;
                sitTransitionTicks = 93; // down animation is 4.6667s = 93 ticks
            }

            if (sitProgress < maxSitTicks()) {
                sitProgress++;
                this.entityData.set(DATA_SIT_PROGRESS, sitProgress);
            }
        } else {
            if (!this.level().isClientSide && super.isInSittingPose()) {
                this.setInSittingPose(false);
            }

            // Trigger sit_up animation when STARTING to stand (transition from sitting → standing)
            if (sitProgress == maxSitTicks() && !isStandingUp) {
                animationHandler.triggerSitUpAnimation();
                isStandingUp = true;
                sitTransitionTicks = 23; // up animation is 1.125s = 23 ticks
            }

            if (this.isVehicle()) {
                if (sitProgress != 0f) {
                    sitProgress = 0f;
                    prevSitProgress = 0f;
                    this.entityData.set(DATA_SIT_PROGRESS, 0f);
                    // Cancel any ongoing transitions
                    isSittingDown = false;
                    isStandingUp = false;
                    sitTransitionTicks = 0;
                }
            } else if (sitProgress > 0f) {
                sitProgress--;
                if (sitProgress < 0f) {
                    sitProgress = 0f;
                }
                this.entityData.set(DATA_SIT_PROGRESS, sitProgress);
            }
        }
    }
    
    private void tickRiderControlLockMovement() {
        // While rider controls are locked (e.g., Summon Storm windup), freeze movement and AI
        if (!areRiderControlsLocked()) {
            return;
        }

        // If there's no rider anymore, release the lock so AI can resume
        if (getControllingPassenger() == null) {
            riderControlLockTicks = 0;
            return;
        }

        this.getNavigation().stop();
        this.setTarget(null);
        this.setDeltaMovement(0, 0, 0);
    }
    
    private void tickFollowFailsafe() {
        if (followFailsafeCooldown > 0) {
            followFailsafeCooldown--;
            return;
        }
        followFailsafeCooldown = 20; // roughly once per second

        if (isSleepLocked() || isOrderedToSit() || isPassenger() || isVehicle() || isDying()) {
            return;
        }

        if (getCommand() == 1 && !isOrderedToSit()) {
            setCommandAuto(0);
        }

        LivingEntity owner = getOwner();
        if (owner == null || !owner.isAlive()) {
            return;
        }
        if (owner.level() != level()) {
            return;
        }

        if (getTarget() != null && getTarget().isAlive()) {
            return; // combat movement handles this case
        }

        if (isFlying()) {
            return; // airborne logic handled elsewhere
        }

        double distSq = this.distanceToSqr(owner);
        if (distSq < (18.0 * 18.0)) {
            /*
             * If we're in follow mode but not actively pathing, we'll keep nudging the navigation
             * so standing dragons don't sit idly in wander command after reloads.
             */
            if (!this.getNavigation().isInProgress() && this.getCommand() == 0 && !this.isOrderedToSit()) {
                this.getNavigation().moveTo(owner, 0.8);
            }
            return;
        }

        boolean moveGoalActive = this.goalSelector.getRunningGoals().anyMatch(wrapped -> {
            Goal goal = wrapped.getGoal();
            return goal instanceof RaevyxFollowOwnerGoal || goal instanceof RaevyxMoveGoal;
        });
        if (moveGoalActive) {
            return;
        }

        switchToGroundNavigation();
        boolean shouldRun = distSq > (25.0 * 25.0);
        setRunning(shouldRun);
        setGroundMoveStateFromAI(shouldRun ? 2 : 1);
        double speed = shouldRun ? 1.35 : 0.9;
        if (!this.getNavigation().moveTo(owner, speed)) {
            this.getNavigation().stop();
            attemptOwnerTeleport(owner);
        }
    }

    private void attemptOwnerTeleport(LivingEntity owner) {
        BlockPos ownerPos = owner.blockPosition();
        for (int i = 0; i < 8; i++) {
            int dx = this.random.nextInt(7) - 3;
            int dz = this.random.nextInt(7) - 3;
            BlockPos candidate = ownerPos.offset(dx, 0, dz);
            if (isTeleportFriendlyBlock(candidate)) {
                this.teleportTo(candidate.getX() + 0.5, candidate.getY(), candidate.getZ() + 0.5);
                this.getNavigation().stop();
                return;
            }
        }
    }

    private boolean isTeleportFriendlyBlock(BlockPos pos) {
        BlockPos below = pos.below();
        BlockState floor = level().getBlockState(below);
        BlockState body = level().getBlockState(pos);
        BlockState above = level().getBlockState(pos.above());
        return floor.isSolidRender(level(), below) && body.isAir() && above.isAir();
    }
    
    private void tickSuperchargeTimer() {
        // Supercharge timer (summon storm)
        if (superchargeTicks > 0) {
            superchargeTicks--;
            // When supercharge ends, restore normal max health
            if (superchargeTicks == 0) {
                Objects.requireNonNull(this.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(180.0D);
                // Don't let health go above the new max
                if (this.getHealth() > this.getMaxHealth()) {
                    this.setHealth(this.getMaxHealth());
                }
                allowGroundBeamDuringStorm = false;
            }
        }
    }

    private void tickTempInvulnTimer() {
        // Temporary invulnerability timer
        if (tempInvulnTicks > 0) {
            tempInvulnTicks--;
            if (tempInvulnTicks == 0 && !isDying()) this.setInvulnerable(false);
        }
    }
    
    private void tickSuperchargeVfx() {
        // Supercharge VFX: periodic arcs/sparks around the body
        if ((isSupercharged() || this.level().isThundering()) && superchargeVfxCooldown-- <= 0) {
            spawnSuperchargeVfx();
            superchargeVfxCooldown = 6 + this.random.nextInt(6); // pulse every ~0.3-0.6s
        }
    }
    
    private void tickSleepTransition() {
        // Handle sleep enter transition: sit_down → fall_asleep → sleep loop
        if (isSleepingEntering() && !level().isClientSide) {
            // Check if sit_down animation is complete (sitProgress reached max)
            if (getSitProgress() >= maxSitTicks()) {
                // Sit down complete, now trigger fall_asleep if we haven't started the transition timer yet
                if (sleepTransitionTicks == 50) {
                    // Just reached sitting position, trigger fall_asleep
                    animationHandler.triggerFallAsleepAnimation();
                }
            }
        }

        if (sleepTransitionTicks > 0) {
            sleepTransitionTicks--;
            if (sleepTransitionTicks == 0) {
                if (isSleepingEntering()) {
                    // fall_asleep finished: mark sleeping and trigger loop animation
                    setSleeping(true);
                    setSleepingEntering(false);
                    animationHandler.triggerSleepAnimation();
                } else if (isSleepingExiting()) {
                    // wake_up finished: dragon is now sitting, will stand up via normal sit system
                    setSleepingExiting(false);
                    // Start small ambient cooldown buffer (~0.5s)
                    sleepAmbientCooldownTicks = 10;
                }
            }
        }
    }
    
    private void tickSleepCooldowns() {
        if (sleepAmbientCooldownTicks > 0) sleepAmbientCooldownTicks--;
        if (sleepReentryCooldownTicks > 0) sleepReentryCooldownTicks--;
        if (sleepCancelTicks > 0) sleepCancelTicks--;
    }
    
    private void tickBankingLogic() {
        prevBankAngle = bankAngle;

        // Reset banking when not flying or when controls are locked - instant snap back
        if (areRiderControlsLocked() || !isFlying() || isOrderedToSit()) {
            if (bankDir != 0 || bankAngle != 0f || bankSmoothedYaw != 0f) {
                bankDir = 0;
                bankSmoothedYaw = 0f;
                bankHoldTicks = 0;
                bankAngle = 0f;
                prevBankAngle = 0f;
            }
            return;
        }

        // Exponential smoothing on yaw delta to avoid jitter, wrap to account for crossing 360 -> 0
        float yawChange = Mth.wrapDegrees(getYRot() - yRotO);
        bankSmoothedYaw = bankSmoothedYaw * 0.75f + yawChange * 0.25f;

        // Convert smoothed yaw delta into a banking roll. Multiplying gives us headroom for aggressive turns.
        float targetAngle = Mth.clamp(bankSmoothedYaw * 5.0f, -90f, 90f);
        // Ease toward the new target so long sweeping turns feel weighty but responsive.
        bankAngle = Mth.lerp(0.28f, bankAngle, targetAngle);
        if (Math.abs(bankAngle) < 0.01f) {
            bankAngle = 0f;
        }

        // Update coarse direction for animation fallbacks / sound hooks
        float enter = 10.0f;
        float exit = 4.0f;

        int desiredDir = bankDir;
        if (bankAngle > enter) desiredDir = 1;
        else if (bankAngle < -enter) desiredDir = -1;
        else if (Math.abs(bankAngle) < exit) desiredDir = 0;  // banking_off when flying straight

        if (desiredDir != bankDir) {
            // If transitioning to "off" (0), use very short hold time for instant reset
            int holdTime = (desiredDir == 0) ? 1 : 2;
            if (bankHoldTicks >= holdTime) {
                bankDir = desiredDir;
                bankHoldTicks = 0;
            } else {
                bankHoldTicks++;
            }
        } else {
            bankHoldTicks = Math.min(bankHoldTicks + 1, 10);  // Reduced max from 20 to 10
        }
    }
    
    private void tickRiderLandingBlendTimer() {
        if (!isVehicle() || !isFlying() || onGround()) {
            riderLandingBlendTicks = 0;
            if (!level().isClientSide) {
                this.entityData.set(DATA_RIDER_LANDING_BLEND, false);
            }
            return;
        }
        if (riderLandingBlendTicks > 0) {
            riderLandingBlendTicks--;
            if (riderLandingBlendTicks == 0 && !level().isClientSide) {
                this.entityData.set(DATA_RIDER_LANDING_BLEND, false);
            }
        }
    }

    private void triggerRiderLandingBlend() {
        riderLandingBlendTicks = RIDER_LANDING_BLEND_DURATION;
        if (!level().isClientSide) {
            this.entityData.set(DATA_RIDER_LANDING_BLEND, true);
        }
    }

    public boolean isRiderLandingBlendActive() {
        // Use synced entity data so client can see it
        return this.entityData.get(DATA_RIDER_LANDING_BLEND);
    }

    private double getAltitudeAboveTerrain() {
        net.minecraft.core.BlockPos pos = this.blockPosition();
        if (!level().hasChunkAt(pos)) {
            return Double.POSITIVE_INFINITY;
        }
        int groundY = this.level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                pos.getX(), pos.getZ());
        return this.getY() - groundY;
    }
    
    private void tickPitchingLogic() {
        tickRiderLandingBlendTimer();
        // Reset pitching when not flying or when controls are locked - INSTANT reset
        if (areRiderControlsLocked() || !isFlying() || isOrderedToSit()) {
            if (pitchDir != 0) {
                pitchDir = 0;
                pitchSmoothedPitch = 0f;
                pitchHoldTicks = 0;
            }
            return;
        }
        
        int desiredDir = pitchDir;

        if (this.isVehicle() && this.getControllingPassenger() instanceof Player) {
            if (isGoingUp()) {
                desiredDir = -1;  // Pitching up
            } else if (isGoingDown()) {
                desiredDir = 1;   // Pitching down
            } else {
                desiredDir = 0;   // No pitching
            }
            if (isGoingDown()) {
                double altitude = getAltitudeAboveTerrain();
                // Trigger landing blend when descending below threshold altitude
                if (altitude != Double.POSITIVE_INFINITY && altitude >= -0.25D && altitude <= RIDER_LANDING_BLEND_ALTITUDE) {
                    // Trigger landing blend immediately when below altitude threshold
                    desiredDir = 0; // Stop pitching down
                    triggerRiderLandingBlend();
                }
            }
        } else {
            float pitchChange = getXRot() - xRotO;
            pitchSmoothedPitch = pitchSmoothedPitch * 0.85f + pitchChange * 0.15f;

            // Hysteresis thresholds - tighter for more responsive straight flight
            float enter = 3.0f;
            float exit = 3.0f;

            if (pitchSmoothedPitch > enter) desiredDir = 1;
            else if (pitchSmoothedPitch < -enter) desiredDir = -1;
            else if (Math.abs(pitchSmoothedPitch) < exit) desiredDir = 0;  // pitching_off when flying straight
        }

        // Faster reset to off state (reduced hold time)
        if (desiredDir != pitchDir) {
            // If transitioning to "off" (0), use shorter hold time for faster reset
            int holdTime = (desiredDir == 0) ? 1 : 2;
            if (pitchHoldTicks >= holdTime) {
                pitchDir = desiredDir;
                pitchHoldTicks = 0;
            } else {
                pitchHoldTicks++;
            }
        } else {
            pitchHoldTicks = Math.min(pitchHoldTicks + 1, 20);
        }
    }

    @Override
    protected void playHurtSound(@Nonnull DamageSource source) {
        // Hurt ability pipeline plays custom audio.
    }

    @Override
    protected DragonAbilityType<?, ?> getHurtAbilityType() {
        return RaevyxAbilities.HURT;
    }

    @Override
    protected void onSuccessfulDamage(DamageSource source, float amount) {
        if (isDying()) {
            return;
        }
        if (hurtSoundCooldown > 0) {
            return;
        }
        super.onSuccessfulDamage(source, amount);
        this.hurtSoundCooldown = this.isVehicle() ? 15 : 8;
    }
    /**
     * Plays appropriate ambient sound based on wyvern's current mood and state
     */
    private void playCustomAmbientSound() {
        if (isBaby()) {
            return;
        }
        RandomSource random = getRandom();

        // Don't make ambient sounds if we're in combat or using abilities
        if (isDying() || isAggressive() || isBeaming() || getActiveAbility() != null) {
            return;
        }
        String vocalKey = null;

        // Choose sound based on current state and mood
        if (isOrderedToSit()) {
            // Content sitting sounds
            vocalKey = (random.nextFloat() < 0.6f) ? "content" : "purr";
        } else if (isFlying()) {
            // Occasional aerial sounds
            if (random.nextFloat() < 0.3f) vocalKey = "chuff";
        } else if (!isFlying() && !isTakeoff() && !isLanding() && !isHovering() && (isWalking() || isRunning())) {
            // Ground movement sounds - different based on speed
            if (isRunning()) {
                vocalKey = "snort"; // Heavy breathing when running
            } else {
                vocalKey = "chuff"; // Gentle snorts when walking
            }
        } else {
            // Regular idle grumbling
            float grumbleChance = random.nextFloat();
            if (grumbleChance < 0.4f) {
                vocalKey = "grumble1";
            } else if (grumbleChance < 0.7f) {
                vocalKey = "grumble2";
            } else if (grumbleChance < 0.9f) {
                vocalKey = "grumble3";
            } else {
                vocalKey = "purr";
            }
        }
        // Play/animate if we chose one
        if (vocalKey != null) this.getSoundHandler().playVocal(vocalKey);
    }
    /**
     * Handles all the ambient grumbling and personality sounds
     * Because a silent wyvern is a boring wyvern
     */
    private void handleAmbientSounds() {
        // Suppress ambient sounds during transitions to prevent animation snapping
        if (isBaby() || isDying() || isSleeping() || isSleepTransitioning() || isInSitTransition() || sleepAmbientCooldownTicks > 0) return;
        ambientSoundTimer++;

        // Time to make some noise?
        if (ambientSoundTimer >= nextAmbientSoundDelay) {
            playCustomAmbientSound(); // Renamed to avoid conflict with Mob.playAmbientSound()
            resetAmbientSoundTimer();
        }
    }

    private void suppressAmbientSounds(int ticks) {
        this.sleepAmbientCooldownTicks = Math.max(this.sleepAmbientCooldownTicks, ticks);
        this.ambientSoundTimer = 0;
        this.nextAmbientSoundDelay = Math.max(this.nextAmbientSoundDelay, ticks);
    }
    /**
     * Resets the ambient sound timer with some randomness
     */
    private void resetAmbientSoundTimer() {
        RandomSource random = getRandom();
        ambientSoundTimer = 0;
        nextAmbientSoundDelay = MIN_AMBIENT_DELAY + random.nextInt(MAX_AMBIENT_DELAY - MIN_AMBIENT_DELAY);
    }
    /**
     * Call this method when wyvern gets excited/happy (like when player approaches)
     * Uses GeckoLib animation keyframe system - sound is handled by animation
     */
    public void playExcitedSound() {
        getSoundHandler().playVocal("excited");
    }

    /**
     * Call this when wyvern gets annoyed (like when attacked by something weak)
     * Uses GeckoLib animation keyframe system - sound is handled by animation
     */
    public void playAnnoyedSound() {
        getSoundHandler().playVocal("annoyed");
    }

    private void handleDodgeMovement() {
        Vec3 current = this.getDeltaMovement();
        Vec3 boosted = current.add(dodgeVec.scale(0.25));
        this.setDeltaMovement(boosted.multiply(0.92, 0.95, 0.92));
        this.hasImpulse = true;

        if (--dodgeTicksLeft <= 0) {
            dodging = false;
            dodgeVec = Vec3.ZERO;
        }
    }

    // ===== TRAVEL METHOD =====
    @Override
    public void travel(@NotNull Vec3 motion) {
        // Handle sitting/dodging/dying states first
        boolean sittingLocked = (this.isOrderedToSit() || this.isInSittingPose()) && postStandUnlockTicks <= 0;
        if (sittingLocked || this.isDodging() || this.isDying() || this.isSleeping() || this.isSleepTransitioning()) {
            if (this.getNavigation().getPath() != null) {
                this.getNavigation().stop();
            }
            motion = Vec3.ZERO;
            super.travel(motion);
            return;
        }

        if (dodging) {
            super.travel(motion);
            return;
        }

        // Riding logic
        if (this.isVehicle() && this.getControllingPassenger() instanceof Player player) {
            // Clear any AI navigation when being ridden
            if (this.getNavigation().getPath() != null) {
                this.getNavigation().stop();
            }

            // Let tickRidden handle rotation smoothly
            // No instant rotation here - all handled in tickRidden for responsiveness

            if (isFlying()) {
                // Delegate flying movement to rider controller for consistency
                this.riderController.handleRiderMovement(player, motion);
            } else {
                // Ground movement - use vanilla system which calls getRiddenInput()
                super.travel(motion);
            }
        } else {
            // Normal AI movement
            if (isFlying()) {
                // AI flight movement
                flightController.handleFlightTravel(motion);
            } else {
                // AI ground movement
                super.travel(motion);
            }
        }

    }

    // ===== RANGED ATTACK IMPLEMENTATION =====
    @Override
    public void performRangedAttack(@Nonnull LivingEntity target, float distanceFactor) {
        // No ranged logic for now; ground melee goal handles combat
        // Intentionally left blank to avoid unintended ranged behavior
    }

    // Attack resolution is handled by DragonMeleeAttackGoal via abilities


    // ===== UTILITY METHODS =====

    public boolean isFlightControllerStuck() {
        if (this.moveControl instanceof com.leon.saintsdragons.server.ai.navigation.DragonFlightMoveHelper flightHelper) {
            return flightHelper.hasGivenUp();
        }
        return false;
    }


    @SuppressWarnings("unused") // Forge interface requires these parameters
    public static boolean canSpawnHere(EntityType<Raevyx> type,
                                       net.minecraft.world.level.LevelAccessor level,
                                       MobSpawnType reason,
                                       BlockPos pos,
                                       net.minecraft.util.RandomSource random) {
        BlockPos below = pos.below();
        if (!level.getFluidState(pos).isEmpty()) {
            return false;
        }
        if (!level.getFluidState(below).isEmpty()) {
            return false;
        }
        boolean solidGround = level.getBlockState(below).isFaceSturdy(level, below, net.minecraft.core.Direction.UP);
        boolean feetFree = level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
        boolean headFree = level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty();
        return solidGround && feetFree && headFree;
    }

    @Override
    public int getMaxHeadXRot() {
        return isBeaming() ? (int)MAX_BEAM_PITCH_DEG : 180;
    }

    // Help the wyvern keep its gaze while running: allow wide, fast head turns
    @Override
    public int getMaxHeadYRot() {
        return isBeaming() ? (int)MAX_BEAM_YAW_DEG : 180;
    }

    @Override
    public int getHeadRotSpeed() {
        // Slightly slower head-only snapping while beaming to avoid neck doing all the work
        return isBeaming() ? 90 : 180;
    }

    // ===== LOOK CONTROLLER =====
    public static class DragonLookController extends LookControl {
        private final Raevyx dragon;

        public DragonLookController(Raevyx dragon) {
            super(dragon);
            this.dragon = dragon;
        }

        @Override
        public void tick() {
            if (!this.dragon.isAlive()) {
                return;
            }

            LivingEntity rider = this.dragon.getControllingPassenger();
            if (this.dragon.isVehicle() && rider != null) {
                boolean controlsLocked = this.dragon.areRiderControlsLocked();
                float baseYaw = this.dragon.getYRot();
                float targetYaw = rider.getYRot();

                float maxOffsetDeg = (this.dragon.isFlying() ? 0.9F : 0.6F) * Mth.RAD_TO_DEG;
                float desiredOffset = Mth.degreesDifference(baseYaw, targetYaw);
                float clampedOffset = controlsLocked
                        ? Mth.approachDegrees(0.0F, desiredOffset, 6.0F)
                        : Mth.clamp(desiredOffset, -maxOffsetDeg, maxOffsetDeg);
                float headYaw = baseYaw + clampedOffset;

                // Don't set yHeadRotO - let Minecraft interpolate smoothly between old and new values
                this.dragon.setYHeadRot(headYaw);

                if (!controlsLocked) {
                    float minPitch = this.dragon.getRiderLockPitchMin();
                    float maxPitch = this.dragon.getRiderLockPitchMax();
                    float targetPitch = Mth.clamp(rider.getXRot(), minPitch, maxPitch);
                    float newPitch = Mth.approachDegrees(this.dragon.getXRot(), targetPitch, 7.0F);
                    // Don't set xRotO - let Minecraft interpolate smoothly
                    this.dragon.setXRot(newPitch);
                } else {
                    float easedPitch = Mth.approachDegrees(this.dragon.getXRot(), 0.0F, 6.0F);
                    // Don't set xRotO - let Minecraft interpolate smoothly
                    this.dragon.setXRot(easedPitch);
                }
                return;
            }

            super.tick();
        }
    }
    // ===== AI GOALS =====
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new RaevyxPanicGoal(this));
        this.goalSelector.addGoal(2, new RaevyxDodgeGoal(this));
        this.goalSelector.addGoal(5, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(6, new FloatGoal(this));
        this.goalSelector.addGoal(7, new RaevyxFollowParentGoal(this, 1.15D));
        this.goalSelector.addGoal(7, new RaevyxBreedGoal(this, 1.0D));

        // Combat goals (prioritized to avoid conflicts)
        // Using new Cataclysm-style separated goal system focused on ground combat
        // New separated combat system (ensure combat decision outranks chase)
        this.goalSelector.addGoal(3, new RaevyxCombatGoal(this));          // Attack coordination
        this.goalSelector.addGoal(4, new RaevyxMoveGoal(this, true, 1.4)); // Pure movement - yields when attacking
        
        // Attack execution goals (high priority, interrupt movement)
        this.goalSelector.addGoal(10, new RaevyxAttackGoal(this, ATTACK_STATE_HORN_WINDUP, ATTACK_STATE_HORN_WINDUP, 15, 10, 4.0f));
        this.goalSelector.addGoal(10, new RaevyxAttackGoal(this, ATTACK_STATE_BITE_WINDUP, ATTACK_STATE_BITE_WINDUP, 11, 8, 3.0f));
        this.goalSelector.addGoal(10, new RaevyxAttackGoal(this, ATTACK_STATE_HORN_ACTIVE, ATTACK_STATE_HORN_ACTIVE, 5, 5, 4.0f));
        this.goalSelector.addGoal(10, new RaevyxAttackGoal(this, ATTACK_STATE_BITE_ACTIVE, ATTACK_STATE_BITE_ACTIVE, 3, 3, 3.0f));
        this.goalSelector.addGoal(10, new RaevyxAttackGoal(this, ATTACK_STATE_RECOVERY, ATTACK_STATE_RECOVERY, 5, 5, 4.0f));
        
        // State transition goals
        this.goalSelector.addGoal(11, new RaevyxStateGoal(this, ATTACK_STATE_HORN_WINDUP, ATTACK_STATE_HORN_WINDUP, ATTACK_STATE_HORN_ACTIVE, 10, 10));
        this.goalSelector.addGoal(11, new RaevyxStateGoal(this, ATTACK_STATE_BITE_WINDUP, ATTACK_STATE_BITE_WINDUP, ATTACK_STATE_BITE_ACTIVE, 8, 8));
        this.goalSelector.addGoal(11, new RaevyxStateGoal(this, ATTACK_STATE_HORN_ACTIVE, ATTACK_STATE_HORN_ACTIVE, ATTACK_STATE_RECOVERY, 5, 5));
        this.goalSelector.addGoal(11, new RaevyxStateGoal(this, ATTACK_STATE_BITE_ACTIVE, ATTACK_STATE_BITE_ACTIVE, ATTACK_STATE_RECOVERY, 3, 3));
        this.goalSelector.addGoal(11, new RaevyxStateGoal(this, ATTACK_STATE_RECOVERY, ATTACK_STATE_RECOVERY, ATTACK_STATE_IDLE, 5, 5));

        // Movement/idle
        // Unified sleep goal: high priority to preempt follow/wander, but calm() prevents overriding combat/aggro
        this.goalSelector.addGoal(0, new RaevyxSleepGoal(this));         // Higher priority than follow
        this.goalSelector.addGoal(1, new RaevyxRestGoal(this));          // Casual sitting for wild dragons
        this.goalSelector.addGoal(8, new RaevyxFollowOwnerGoal(this));   // Lower priority than combat
        this.goalSelector.addGoal(9, new RaevyxGroundWanderGoal(this, 1.0, 60)); // Lower priority than combat
        
        // Item pickup behavior (like foxes eating berries) + ground fish taming
        this.goalSelector.addGoal(10, new RaevyxTemptGoal(this, 1.2,
                net.minecraft.world.item.crafting.Ingredient.of(net.minecraft.world.item.Items.SALMON, 
                                                               net.minecraft.world.item.Items.COD, 
                                                               net.minecraft.world.item.Items.TROPICAL_FISH, 
                                                               net.minecraft.world.item.Items.PUFFERFISH), false));
        
        this.goalSelector.addGoal(11, new RaevyxFlightGoal(this));
        this.goalSelector.addGoal(12, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(12, new LookAtPlayerGoal(this, Player.class, 8.0F));

        // Target selection - use custom goals that respect ally system
        this.targetSelector.addGoal(1, new com.leon.saintsdragons.server.ai.goals.base.DragonOwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new com.leon.saintsdragons.server.ai.goals.base.DragonOwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
        // Neutral behavior: do not proactively target players. Only retaliate when hurt or defend owner.
    }

    @Override
    public boolean hurt(@Nonnull DamageSource damageSource, float amount) {
        // During dying sequence, ignore all damage except the final generic kill used by DieAbility
        if (isDying()) {
            if (damageSource.is(DamageTypes.GENERIC_KILL)) {
                return super.hurt(damageSource, amount);
            }
            return false;
        }
        // Immune to lightning damage
        if (damageSource.is(DamageTypes.LIGHTNING_BOLT)) {
            return false;
        }
        // Wake if sleeping and suppress re-entry on damage
        if (isSleeping() || isSleepingEntering() || isSleepingExiting()) {
            wakeUpImmediately();
            suppressSleep(200);
        }
        if (damageSource.is(DamageTypes.FALL)) {
            return false;
        }

        // Intercept lethal damage to play custom death ability first
        if (handleLethalDamage(damageSource, amount, RaevyxAbilities.DIE)) {
            return true;
        }

        // Store previous flying state to restore if being ridden
        boolean wasFlying = isFlying();
        boolean wasRidden = isVehicle();

        boolean result = super.hurt(damageSource, amount);

        // If the wyvern was being ridden and flying before taking damage,
        // ensure it stays flying regardless of any AI goals triggered by damage
        if (result && wasRidden && wasFlying && isVehicle()) {
            // Restore flying state to prevent forced landing when ridden
            setFlying(true);
            // Cancel any landing sequence that might have been triggered
            setLanding(false);
            // Ensure we're in air navigation mode
            switchToAirNavigation();
        }

        return result;
    }

    @Override
    public boolean isInvulnerableTo(@Nonnull DamageSource source) {
        if (source.is(DamageTypes.LIGHTNING_BOLT)) return true;
        return super.isInvulnerableTo(source);
    }

    // ===== BREATHING / AIR SUPPLY =====
    // Allow the wyvern to hold its breath underwater for ~3 minutes (3600 ticks)
    @Override
    public int getMaxAirSupply() {
        return 20 * 60 * 3; // 3600 ticks ~= 180s
    }

    // Speed up air refill when out of water so it doesn't take excessively long
    @Override
    public int increaseAirSupply(int currentAir) {
        int refillPerTick = 50; // ~3600/72 ticks ≈ 3.6s to refill from 0
        return Math.min(getMaxAirSupply(), currentAir + refillPerTick);
    }

    // Prevent lightning strikes from igniting or applying any side effects
    @Override
    public void thunderHit(@Nonnull ServerLevel level, @Nonnull LightningBolt lightning) {
        // Do not call super; ignore ignition and effects
        if (this.isOnFire()) this.clearFire();
    }
    // ===== ANIMATION HELPER METHODS =====
    
    /**
     * Gets the current bank direction for animation purposes
     * @return -1 for left, 0 for none, 1 for right
     */
    public int getBankDirection() {
        return bankDir;
    }

    /**
     * Gets the current bank angle in degrees. Positive values bank right, negative bank left.
     */
    public float getBankAngleDegrees() {
        return bankAngle;
    }

    /**
     * Interpolated bank angle for smooth client-side rendering.
     */
    public float getBankAngleDegrees(float partialTick) {
        return Mth.lerp(partialTick, prevBankAngle, bankAngle);
    }
    
    /**
     * Gets the current pitch direction for animation purposes
     * @return -1 for up, 0 for none, 1 for down
     */
    public int getPitchDirection() {
        return pitchDir;
    }

    /**
     * Checks if the wyvern is currently summoning (controls locked for ability)
     * @return true if summoning
     */
    public boolean isSummoning() {
        return false;
    }

    // ===== SUPERCHARGE (Summon Storm) =====
    private int superchargeTicks = 0;
    public void startSupercharge(int ticks) {
        boolean wasNotSupercharged = !isSupercharged();
        this.superchargeTicks = Math.max(this.superchargeTicks, Math.max(0, ticks));
        
        // When becoming supercharged, double the max health attribute and heal to full
        if (wasNotSupercharged && isSupercharged()) {
            // Double the max health attribute
            Objects.requireNonNull(this.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(360.0D);
            // Heal to full health
            this.setHealth(this.getMaxHealth());
            this.allowGroundBeamDuringStorm = true;
        }
    }
    
    
    public boolean isSupercharged() { return superchargeTicks > 0; }
    public float getDamageMultiplier() { return isSupercharged() ? 2.0f : 1.0f; }
    // Temporary invulnerability timer (e.g., during Summon Storm windup)
    private int tempInvulnTicks = 0;
    public void startTemporaryInvuln(int ticks) {
        this.tempInvulnTicks = Math.max(this.tempInvulnTicks, Math.max(0, ticks));
        this.setInvulnerable(true);
    }

    // VFX pulse throttle
    private int superchargeVfxCooldown = 0;
    private void spawnSuperchargeVfx() {
        if (!(this.level() instanceof net.minecraft.server.level.ServerLevel server)) return;
        // Center around chest
        net.minecraft.world.phys.Vec3 center = this.position().add(0, this.getBbHeight() * 0.6, 0);
        double radius = Math.max(this.getBoundingBox().getXsize(), this.getBoundingBox().getZsize()) * 0.55;
        // 2-4 micro-bursts per pulse, each very short and randomized
        int bursts = 2 + this.random.nextInt(3);
        for (int i = 0; i < bursts; i++) {
            // Short local segment
            net.minecraft.world.phys.Vec3 dir = randomUnit(this.random);
            double length = 0.4 + this.random.nextDouble() * 0.7; // ~0.4-1.1 blocks
            net.minecraft.world.phys.Vec3 offset = randomUnit(this.random).scale(radius * 0.35);
            net.minecraft.world.phys.Vec3 from = center.add(offset);
            net.minecraft.world.phys.Vec3 to = from.add(dir.scale(length));
            float size = 0.5f + this.random.nextFloat() * 0.25f; // smaller sprites
            emitMicroArc(server, from, to, size);
        }
        // A couple of sparks near center (smaller spread)
        server.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                center.x, center.y, center.z,
                3, radius * 0.15, radius * 0.15, radius * 0.15, 0.0);
    }
    private void emitMicroArc(net.minecraft.server.level.ServerLevel server, net.minecraft.world.phys.Vec3 from, net.minecraft.world.phys.Vec3 to, float size) {
        net.minecraft.world.phys.Vec3 delta = to.subtract(from);
        int steps = 2 + this.random.nextInt(3); // 2-4 points only
        net.minecraft.world.phys.Vec3 step = delta.scale(1.0 / steps);
        net.minecraft.world.phys.Vec3 pos = from;
        net.minecraft.world.phys.Vec3 dir = step.lengthSqr() > 1.0e-6 ? step.normalize() : randomUnit(this.random);
        boolean female = this.isFemale();
        for (int i = 0; i <= steps; i++) {
            // Randomly pick arc type per point and randomly drop some points for a crackly feel
            if (this.random.nextFloat() < 0.7f) {
                if (this.random.nextBoolean()) {
                    server.sendParticles(new RaevyxLightningArcData(size, female),
                            pos.x, pos.y, pos.z, 1, dir.x, dir.y, dir.z, 0.0);
                } else {
                    server.sendParticles(new RaevyxLightningStormData(size, female),
                            pos.x, pos.y, pos.z, 1, dir.x, dir.y, dir.z, 0.0);
                }
            }
            pos = pos.add(step);
        }
    }
    private static net.minecraft.world.phys.Vec3 randomUnit(net.minecraft.util.RandomSource rnd) {
        double u = rnd.nextDouble();
        double v = rnd.nextDouble();
        double theta = 2 * Math.PI * u;
        double z = 2 * v - 1;
        double r = Math.sqrt(1 - z * z);
        return new net.minecraft.world.phys.Vec3(r * Math.cos(theta), z, r * Math.sin(theta));
    }

    // ===== SIT TRANSITIONS =====
    public boolean isInSitTransition() {
        return isSittingDown || isStandingUp;
    }

    public boolean isSittingDownAnimation() {
        return isSittingDown;
    }

    public boolean isStandingUpAnimation() {
        return isStandingUp;
    }

    // ===== SLEEPING =====
    public boolean isSleeping() {
        return getBooleanData(DATA_SLEEPING);
    }
    public void setSleeping(boolean sleeping) {
        setBooleanData(DATA_SLEEPING, sleeping);
    }
    public boolean isSleepTransitioning() {
        return isSleepingEntering() || isSleepingExiting();
    }

    public boolean isSleepingEntering() {
        return getBooleanData(DATA_SLEEPING_ENTERING);
    }

    public void setSleepingEntering(boolean entering) {
        setBooleanData(DATA_SLEEPING_ENTERING, entering);
    }

    public boolean isSleepingExiting() {
        return getBooleanData(DATA_SLEEPING_EXITING);
    }

    public void setSleepingExiting(boolean exiting) {
        setBooleanData(DATA_SLEEPING_EXITING, exiting);
    }
    public boolean isSleepLocked() {
        return sleepLocked || isSleeping() || isSleepingEntering() || isSleepingExiting();
    }

    private void enterSleepLock() {
        int snapshot = this.getCommand();
        if (!sleepLocked) {
            sleepLocked = true;
            sleepCommandSnapshot = snapshot;
        }
        if (snapshot == 1) {
            this.setCommandManual(1);
        } else {
            this.setCommandAuto(1);
        }
        this.setOrderedToSit(true);
        this.getNavigation().stop();
        this.setTarget(null);
        this.setRunning(false);
        this.setGroundMoveStateFromAI(0);
        this.setDeltaMovement(Vec3.ZERO);
        this.setFlying(false);
        this.setLanding(false);
        this.setTakeoff(false);
        this.setHovering(false);
    }

    private void releaseSleepLock() {
        if (sleepLocked) {
            int desired = sleepCommandSnapshot;
            sleepCommandSnapshot = -1;
            sleepLocked = false;
            if (desired == 1) {
                this.setCommandManual(1);
                this.setOrderedToSit(true);
            } else {
                this.setCommandAuto(desired);
                this.setOrderedToSit(false);
            }
        }
        this.getNavigation().stop();
        this.setRunning(false);
        this.setGroundMoveStateFromAI(0);
    }

    public void startSleepEnter() {
        if (isSleeping() || isSleepingEntering() || isSleepingExiting()) return;
        setSleepingEntering(true);
        // New system: sit_down (uses sitProgress) → fall_asleep (2.5s = 50 ticks) → sleep loop
        // Total transition: ~15 ticks (sit down) + 50 ticks (fall asleep) = ~65 ticks
        sleepTransitionTicks = 50; // Duration of fall_asleep animation (2.5 seconds)
        // Trigger sit_down animation first (handled by sit progress system)
        animationHandler.triggerSitDownAnimation();
        if (!level().isClientSide) {
            enterSleepLock();
        }
    }

    public void startSleepExit() {
        if ((!isSleeping() && !isSleepingEntering()) || isSleepingExiting()) return;
        this.entityData.set(DATA_SLEEPING, false);
        setSleepingEntering(false);
        setSleepingExiting(true);
        // New system: wake_up (7.6667s = ~153 ticks) → sit (brief) → sit_up
        sleepTransitionTicks = 153; // Duration of wake_up animation (7.6667 seconds)
        // Trigger wake_up animation
        animationHandler.triggerWakeUpAnimation();
        if (!level().isClientSide) {
            suppressSleep(20);
            releaseSleepLock();
        }
    }

    public void wakeUpImmediately() {
        this.entityData.set(DATA_SLEEPING, false);
        setSleepingEntering(false);
        setSleepingExiting(false);
        sleepTransitionTicks = 0;
        sleepCancelTicks = 2;
        if (!level().isClientSide) {
            suppressSleep(20);
            releaseSleepLock();
        }
    }

    public void suppressSleep(int ticks) {
        sleepReentryCooldownTicks = Math.max(sleepReentryCooldownTicks, ticks);
    }
    public boolean isSleepSuppressed() {
        return sleepReentryCooldownTicks > 0;
    }

    // ===== INTERACTION =====
    @Override
    public @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        // Delegate to the specialized interaction handler
        InteractionResult result = lightningInteractionHandler.handleInteraction(player, hand);
        
        // If the handler didn't handle it, fall back to super implementation
        if (result == InteractionResult.PASS) {
            return super.mobInteract(player, hand);
        }
        
        return result;
    }

    @Override
    public void setOrderedToSit(boolean sitting) {
        super.setOrderedToSit(sitting);

        if (sitting) {
            // Force landing if flying when ordered to sit
            if (isFlying()) {
                this.setLanding(true);
            }
            this.setRunning(false);
            this.getNavigation().stop();
        } else {
            // Reset sit progress when standing up
            if (!level().isClientSide) {
                sitProgress = 0;
                this.entityData.set(DATA_SIT_PROGRESS, 0.0f);
                // Ensure we are in ground navigation mode and not stuck in legacy flight flags
                switchToGroundNavigation();
                if (isFlying()) {
                    setFlying(false);
                }
                setTakeoff(false);
                setLanding(false);
                setHovering(false);
                this.usingAirNav = false;
                postStandUnlockTicks = Math.max(postStandUnlockTicks, 20);
            }
            if (this.getCommand() == 1) {
                this.setCommandAuto(0);
            }
            if (!level().isClientSide) {
                this.followFailsafeCooldown = 0;
                this.getNavigation().stop();
                this.tickFollowFailsafe();
            }
        }
    }

    @Override
    public void handleEntityEvent(byte eventId) {
        // Handle attack state events (Cataclysm-style)
        if (eventId <= 0) {
            // Attack state change event (negative values)
            this.attackTicks = 0;
            return;
        }
        
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

    @Override
    public boolean isFood(@Nonnull ItemStack stack) {
        return stack.is(Items.SALMON) || stack.is(Items.COD);
    }


    // ===== SAVE/LOAD =====
    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Flying", isFlying());
        tag.putBoolean("Takeoff", isTakeoff());
        tag.putBoolean("Hovering", isHovering());
        tag.putBoolean("Landing", isLanding());
        tag.putBoolean("Running", isRunning());
        tag.putInt("TimeFlying", timeFlying);
        tag.putBoolean("UsingAirNav", usingAirNav);
        tag.putFloat("SitProgress", sitProgress);
        tag.putInt("RiderTakeoffTicks", riderTakeoffTicks);

        // Save critical flight state variables that were missing
        tag.putLong("LastLandingGameTime", lastLandingGameTime);
        tag.putBoolean("LandingFlag", landingFlag);
        tag.putInt("LandingTimer", landingTimer);

        // Save lock states (transient rider/takeoff locks intentionally omitted)

        // Persist combat cooldowns
        this.combatManager.saveToNBT(tag);

        // Persist supercharge timer so logout/login doesn't clear buff early
        tag.putInt("SuperchargeTicks", Math.max(0, this.superchargeTicks));

        // Persist temporary invulnerability timer (e.g., during Summon Storm windup)
        tag.putInt("TempInvulnTicks", Math.max(0, this.tempInvulnTicks));
        tag.putBoolean("AllowGroundBeamStorm", this.allowGroundBeamDuringStorm);

        // Persist sleep state and transition timers
        tag.putBoolean("Sleeping", this.isSleeping());
        // Sleep transition states now synced via entity data, no need to save separately
        tag.putInt("SleepTransitionTicks", Math.max(0, this.sleepTransitionTicks));
        tag.putInt("SleepAmbientCooldownTicks", Math.max(0, this.sleepAmbientCooldownTicks));
        tag.putInt("SleepReentryCooldownTicks", Math.max(0, this.sleepReentryCooldownTicks));
        tag.putInt("SleepCancelTicks", Math.max(0, this.sleepCancelTicks));
        tag.putBoolean("SleepLock", this.sleepLocked);
        tag.putInt("SleepCommandSnapshot", this.sleepCommandSnapshot);
        tag.putBoolean("ManualSitCommand", this.manualSitCommand);

        animationController.writeToNBT(tag);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setFlying(tag.getBoolean("Flying"));
        this.setTakeoff(tag.getBoolean("Takeoff"));
        this.setHovering(tag.getBoolean("Hovering"));
        this.setLanding(tag.getBoolean("Landing"));
        this.setRunning(tag.getBoolean("Running"));
        this.timeFlying = tag.getInt("TimeFlying");
        this.usingAirNav = tag.getBoolean("UsingAirNav");
        this.sitProgress = tag.getFloat("SitProgress");
        this.prevSitProgress = this.sitProgress;
        // Sync the sit progress with client
        this.entityData.set(DATA_SIT_PROGRESS, this.sitProgress);
        this.riderTakeoffTicks = tag.contains("RiderTakeoffTicks") ? tag.getInt("RiderTakeoffTicks") : 0;

        // Restore critical flight state variables that were missing
        this.lastLandingGameTime = tag.contains("LastLandingGameTime") ? tag.getLong("LastLandingGameTime") : Long.MIN_VALUE;
        this.landingFlag = tag.contains("LandingFlag") && tag.getBoolean("LandingFlag");
        this.landingTimer = tag.contains("LandingTimer") ? tag.getInt("LandingTimer") : 0;

        // Restore lock states (transient rider/takeoff locks reset on load)
        this.riderControlLockTicks = 0;
        this.takeoffLockTicks = 0;

        // Restore combat cooldowns
        this.combatManager.loadFromNBT(tag);

        // Restore supercharge timer if present
        if (tag.contains("SuperchargeTicks")) {
            this.superchargeTicks = Math.max(0, tag.getInt("SuperchargeTicks"));
        }
        // Restore temporary invulnerability
        if (tag.contains("TempInvulnTicks")) {
            this.tempInvulnTicks = Math.max(0, tag.getInt("TempInvulnTicks"));
            if (this.tempInvulnTicks > 0) {
                this.setInvulnerable(true);
            }
        }

        if (tag.contains("AllowGroundBeamStorm")) {
            this.allowGroundBeamDuringStorm = tag.getBoolean("AllowGroundBeamStorm");
        }

        // Restore sleep state and transition timers
        if (tag.contains("Sleeping")) this.setSleeping(tag.getBoolean("Sleeping"));
        // Sleep transition states now synced via entity data, loaded automatically
        this.sleepTransitionTicks = Math.max(0, tag.getInt("SleepTransitionTicks"));
        this.sleepAmbientCooldownTicks = Math.max(0, tag.getInt("SleepAmbientCooldownTicks"));
        this.sleepReentryCooldownTicks = Math.max(0, tag.getInt("SleepReentryCooldownTicks"));
        this.sleepCancelTicks = Math.max(0, tag.getInt("SleepCancelTicks"));
        this.sleepLocked = tag.getBoolean("SleepLock");
        this.sleepCommandSnapshot = tag.getInt("SleepCommandSnapshot");

        animationController.readFromNBT(tag);

        this.manualSitCommand = tag.contains("ManualSitCommand") && tag.getBoolean("ManualSitCommand");

        if (this.usingAirNav) {
            switchToAirNavigation();
        } else {
            switchToGroundNavigation();
        }

        // Safety: if we reloaded while flagged as sitting but owner command isn't "sit",
        // clear the stuck sitting state so pathfinding goals can resume.
        if (this.getCommand() != 1 && this.isOrderedToSit()) {
            this.setOrderedToSit(false);
        }

        // If we reload while sleep-locked (or mid-transition), immediately wake and clear the lock so AI goals resume.
        if (this.sleepLocked || this.isSleepingEntering() || this.isSleepingExiting() || this.entityData.get(DATA_SLEEPING)) {
            this.releaseSleepLock();
            this.wakeUpImmediately();
            this.suppressSleep(200);
        }
        this.setSleepingEntering(false);
        this.setSleepingExiting(false);
        this.sleepTransitionTicks = 0;
        this.entityData.set(DATA_SLEEPING, false);
        this.sleepCommandSnapshot = -1;
        this.followFailsafeCooldown = 0;

        // If we saved while flying, keep the wyvern in the air briefly after load
        if (tag.getBoolean("Flying")) {
            this.postLoadAirStabilizeTicks = 60; // ~3 seconds of grace to receive rider inputs
            // Also treat as takeoff for a short while to apply upward force
            this.riderTakeoffTicks = Math.max(this.riderTakeoffTicks, 40);

            // Ensure flight controller is properly reactivated for ALL dragons
            if (!level().isClientSide) {
                // For all dragons, restart flight controller to prevent drifting
                this.flightController.shouldTakeoff();

                // Force proper flight state restoration
                this.setFlying(true);
                this.setTakeoff(true);
                this.setHovering(true);
                this.setLanding(false);
                this.switchToAirNavigation();
            }
        }

        // Clear navigation and target if wyvern is sitting to prevent AI goal issues on world reload
        if (this.isOrderedToSit()) {
            this.getNavigation().stop();
            this.setTarget(null);
            this.setAggressive(false);
        }

        if (this.getCommand() == 1 && !this.manualSitCommand) {
            this.setCommandAuto(0);
            this.setOrderedToSit(false);
        }
    }

    // Rider takeoff window accessors for controllers
    public int getRiderTakeoffTicks() { return riderTakeoffTicks; }
    public void setRiderTakeoffTicks(int ticks) { this.riderTakeoffTicks = Math.max(0, ticks); }
    
    /**
     * Clears all states when mounting to ensure clean transition to rider control
     */
    public void clearAllStatesForMounting() {
        // Clear combat states
        this.setTarget(null);
        this.setAttacking(false);
        this.setBeaming(false);
        
        // Clear movement states
        this.setRunning(false);
        this.getNavigation().stop();
        
        // Clear flight states (will be controlled by rider)
        this.setTakeoff(false);
        this.setLanding(false);
        this.setHovering(false);
        
        // Clear sitting state and sync command value
        if (this.isOrderedToSit()) {
            this.setOrderedToSit(false);
            // If wyvern was sitting, set command to Follow (0) when mounted
            if (this.getCommand() == 1) {
                this.setCommandAuto(0);
            }
        }
        
        // Clear any pending timers
        this.riderTakeoffTicks = 0;
        this.postLoadAirStabilizeTicks = 0;
        
        // Reset combat manager
        this.combatManager.clearAllStates();
        
        // Clear any sleep suppression (fresh start)
        this.sleepReentryCooldownTicks = 0;
        this.sleepAmbientCooldownTicks = 0;
    }



    // ===== ANIMATION TRIGGERS =====
    /**
     * Triggers the dodge animation - called when wyvern dodges projectiles
     */
    public void triggerDodgeAnimation() {
        // Trigger native GeckoLib action key
        animationHandler.triggerDodgeAnimation();
    }

    // Note: We rely on GeckoLib triggerAnim(...) to play action clips.

    // ===== GECKOLIB =====
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Use entity-specific controller names to prevent animation bleeding between dragons
        // Update frequency: run every tick to maintain accurate keyframe timing
        AnimationController<Raevyx> movementController =
                new AnimationController<>(this, "movement", 3, animationController::handleMovementAnimation);

        // Action controller uses ONLY triggers (no predicate logic)
        // All animations (combat, abilities, sleep, death) are triggered via triggerAnim()
        AnimationController<Raevyx> actionController =
                new AnimationController<>(this, "action", 3, state -> PlayState.STOP);

        // Sound keyframes - only register on relevant controllers to prevent duplication
        // Movement controller: handles footsteps, wing flaps during locomotion
        movementController.setSoundKeyframeHandler(this::onAnimationSound);
        // Action controller: handles combat sounds, ability sounds, vocalizations
        actionController.setSoundKeyframeHandler(this::onAnimationSound);

        // Setup animation triggers via animation handler
        animationHandler.setupActionController(actionController);

        // Babies don't fly, so skip banking/pitching controllers
        if (!this.isBaby()) {
            AnimationController<Raevyx> bankingController =
                    new AnimationController<>(this, "banking", 8, animationHandler::bankingPredicate);
            AnimationController<Raevyx> pitchingController =
                    new AnimationController<>(this, "pitching", 6, animationHandler::pitchingPredicate);
            // Banking/Pitching controllers: NO sound keyframes (purely visual animations)

            // Add controllers in order
            controllers.add(bankingController);
            controllers.add(pitchingController);
        }

        controllers.add(movementController);
        controllers.add(actionController);
    }

    @Override
    public Map<String, VocalEntry> getVocalEntries() {
        return VOCAL_ENTRIES;
    }

    @Override
    public DragonSoundProfile getSoundProfile() {
        return RaevyxSoundProfile.INSTANCE;
    }

    @Override
    public com.leon.saintsdragons.server.entity.ability.DragonAbilityType<?, ?> getPrimaryAttackAbility() {
        // Lightning Dragon alternates between bite and horn gore attacks
        // Use entity tick count to alternate between attacks (every 2 seconds)
        boolean useHornGore = (tickCount / 40) % 2 == 1; // Switch every 2 seconds
        return useHornGore ? 
            RaevyxAbilities.RAEVYX_HORN_GORE :
            RaevyxAbilities.RAEVYX_BITE;
    }

    @Override
    public com.leon.saintsdragons.server.entity.ability.DragonAbilityType<?, ?> getRoaringAbility() {
        return RaevyxAbilities.RAEVYX_ROAR;
    }

    @Override
    public com.leon.saintsdragons.server.entity.ability.DragonAbilityType<?, ?> getChannelingAbility() {
        return RaevyxAbilities.RAEVYX_SUMMON_STORM;
    }

    public void onAnimationSound(SoundKeyframeEvent<Raevyx> event) {
        // Delegate all keyframed sounds to the sound handler
        // Pass the raw event data to the sound handler
        this.getSoundHandler().handleAnimationSound(this, event.getKeyframeData(), event.getController());
    }
    // Cache frequently used calculations
    public double getCachedDistanceToOwner() {
        // Lower cache window for snappier follow responsiveness
        int currentTick = tickCount;
        if (currentTick - ownerDistanceCacheTime >= 3) {
            LivingEntity owner = getOwner();
            cachedOwnerDistance = owner != null ? distanceToSqr(owner) : Double.MAX_VALUE;
            ownerDistanceCacheTime = currentTick;
        }
        return cachedOwnerDistance;
    }
    public List<Projectile> getCachedNearbyProjectiles() {
        // Server-side only; clients don't need this heavy scan
        if (!(this.level() instanceof net.minecraft.server.level.ServerLevel)) {
            return java.util.Collections.emptyList();
        }
        if (tickCount - nearbyProjectilesCacheTime >= projectileCacheIntervalTicks) {
            List<Projectile> found = DragonMathUtil.getEntitiesNearby(this, Projectile.class, 30.0);
            cachedNearbyProjectiles = found;
            nearbyProjectilesCacheTime = tickCount;

            if (found.isEmpty()) {
                emptyProjectileScans = Math.min(emptyProjectileScans + 1, 4);
                // Back off up to ~11 ticks when calm (3,5,7,9,11)
                projectileCacheIntervalTicks = 3 + (emptyProjectileScans * 2);
            } else {
                emptyProjectileScans = 0;
                projectileCacheIntervalTicks = 3;
            }
        }
        return cachedNearbyProjectiles;
    }
    // DYNAMIC EYE HEIGHT SYSTEM
    // Will be calculated dynamically from renderer

    // While > 0, rider input is ignored to keep action animation coherent (e.g., roar, summon storm)
    private int riderControlLockTicks = 0;

    public boolean areRiderControlsLocked() {
        return level().isClientSide ? this.entityData.get(DATA_RIDER_LOCKED) : riderControlLockTicks > 0;
    }

    private void tickRiderControlLock() {
        if (riderControlLockTicks > 0) {
            riderControlLockTicks--;
            if (riderControlLockTicks <= 0) {
                this.entityData.set(DATA_RIDER_LOCKED, false);
            }
        }
    }

    public void lockRiderControls(int ticks) {
        riderControlLockTicks = Math.max(riderControlLockTicks, Math.max(0, ticks));
        this.entityData.set(DATA_RIDER_LOCKED, true);
        this.setAccelerating(false);
        // Reset rider inputs
        this.setGoingUp(false);
        this.setGoingDown(false);
        this.setDeltaMovement(Vec3.ZERO);
        if (!this.level().isClientSide) {
            this.getNavigation().stop();
            this.setTarget(null);
        }
    }

    // While > 0, only takeoff is locked (allows running/movement during roar)
    private int takeoffLockTicks = 0;
    public boolean isTakeoffLocked() { return takeoffLockTicks > 0; }
    public void lockTakeoff(int ticks) { this.takeoffLockTicks = Math.max(this.takeoffLockTicks, Math.max(0, ticks)); }
    private void tickTakeoffLock() { if (takeoffLockTicks > 0) takeoffLockTicks--; }

    // ===== RECENT AGGRO TRACKING (for roar lightning targeting) =====
    private final java.util.Map<Integer, Long> recentAggroIds = new java.util.concurrent.ConcurrentHashMap<>();

    public void noteAggroFrom(net.minecraft.world.entity.LivingEntity target) {
        if (target == null || target.level().isClientSide) return;
        recentAggroIds.put(target.getId(), this.level().getGameTime() + AGGRO_TTL_TICKS);
    }

    public java.util.List<net.minecraft.world.entity.LivingEntity> getRecentAggro() {
        java.util.List<net.minecraft.world.entity.LivingEntity> out = new java.util.ArrayList<>();
        long now = this.level().getGameTime();
        java.util.Iterator<java.util.Map.Entry<Integer, Long>> it = recentAggroIds.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            if (e.getValue() < now) { it.remove(); continue; }
            net.minecraft.world.entity.Entity ent = this.level().getEntity(e.getKey());
            if (ent instanceof net.minecraft.world.entity.LivingEntity le && le.isAlive()) {
                out.add(le);
            } else {
                // Clean up dead/invalid entities
                it.remove();
            }
        }
        return out;
    }

    @Override
    public float getEyeHeight(@Nonnull Pose pose) {
        // Always use dynamically calculated eye height when available
        EntityDimensions dimensions = getDimensions(pose);
        return dimensions.height * 0.6f;
    }

    @Override
    protected float getStandingEyeHeight(@Nonnull Pose pose, @Nonnull EntityDimensions dimensions) {
        // Always use cached value when available (both client and server need this)
        return dimensions.height * 0.6f;
    }
    // Cache horizontal flight speed - used in physics calculations
    public double getCachedHorizontalSpeed() {
        if (tickCount != horizontalSpeedCacheTime) {
            Vec3 velocity = getDeltaMovement();
            cachedHorizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
            horizontalSpeedCacheTime = tickCount;
        }
        return cachedHorizontalSpeed;
    }
    @Override
    public boolean canMate(@Nonnull Animal otherAnimal) {
        // Basic breeding checks
        if (!this.canBreed()) {
            return false;
        }

        // Prevent same-sex breeding
        if (otherAnimal instanceof Raevyx otherDragon) {
            if (this.isFemale() == otherDragon.isFemale()) {
                return false; // Same sex can't breed
            }
            return otherDragon.canBreed();
        }

        return false;
    }

    @Override
    public boolean canBreed() {
        // Allow breeding even when tamed (bypass TamableAnimal's sitting requirement)
        // Requirements:
        // 1. Not a baby
        // 2. Has love hearts (in love mode from feeding)
        // 3. Health is sufficient
        return !this.isBaby() && this.getHealth() >= this.getMaxHealth() && this.isInLove();
    }

    @Override
    @Nullable
    public AgeableMob getBreedOffspring(@Nonnull net.minecraft.server.level.ServerLevel level, @Nonnull AgeableMob otherParent) {
        Raevyx baby = ModEntities.RAEVYX.get().create(level);
        if (baby != null) {
            baby.setGender(this.random.nextBoolean() ? DragonGender.FEMALE : DragonGender.MALE);
            java.util.UUID ownerId = this.getOwnerUUID();
            if (ownerId != null) {
                baby.setOwnerUUID(ownerId);
                baby.setTame(true);
            }
            baby.setAge(-24000);
        }
        return baby;
    }

    // ===== RIDING INPUT IMPLEMENTATION =====
    @Override
    public @NotNull Vec3 getRiddenInput(@Nonnull Player player, @Nonnull Vec3 deltaIn) {
        if (areRiderControlsLocked()) {
            // Ignore rider strafe/forward while locked
            return net.minecraft.world.phys.Vec3.ZERO;
        }
        Vec3 input = riderController.getRiddenInput(player, deltaIn);
        // Call parent implementation to handle standard rideable wyvern input processing
        return super.getRiddenInput(player, input);
    }
    @Override
    protected void tickRidden(@Nonnull Player player, @Nonnull Vec3 travelVector) {
        super.tickRidden(player, travelVector);
        // Decrement locks on server authority
        if (!this.level().isClientSide) {
            tickTakeoffLock();
        }
        if (!areRiderControlsLocked()) {
            riderController.tickRidden(player, travelVector);
        } else {
            // While locked, keep rider safe and aligned but do not apply rider-driven yaw/pitch changes
            player.fallDistance = 0.0F;
            this.fallDistance = 0.0F;
            this.setTarget(null);
            copyRiderLook(player);
            // Stop acceleration & vertical intents during lock
            this.setAccelerating(false);
            if (!this.isFlying()) {
                // On ground: block takeoff/vertical motion entirely
                this.setGoingUp(false);
                this.setGoingDown(false);
            }
        }
    }

    @Override
    protected float getRiddenSpeed(@Nonnull Player rider) {
        if (areRiderControlsLocked()) {
            return 0.0F;
        }
        return riderController.getRiddenSpeed(rider);
    }

    @Override
    public void removePassenger(@Nonnull Entity passenger) {
        // Prevent dismounting while rider controls are locked (e.g., Summon Storm windup)
        if (areRiderControlsLocked() && passenger == getControllingPassenger()) {
            return;
        }
        // Call parent implementation to handle standard rideable wyvern cleanup
        super.removePassenger(passenger);
    }
    // Cooldown for aggro growl to prevent spam while ridden or under repeated retargeting
    private int aggroGrowlCooldown = 0;

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (isBaby()) {
            super.setTarget(null);
            return;
        }
        LivingEntity previousTarget = this.getTarget();
        super.setTarget(target);

        if (!this.level().isClientSide) {
            // Decrement here too in case tick() hasn't yet
            if (aggroGrowlCooldown > 0) aggroGrowlCooldown--;

            // Play growl when entering combat from idle, but throttle and avoid while being ridden
            if (target != null && previousTarget == null && aggroGrowlCooldown <= 0) {
                // Suppress frequent growls when mounted; lengthen cooldown if mounted
                if (!this.isVehicle() && !isStayOrSitMuted()) {
                    // Uses GeckoLib animation keyframe system - sound is handled by animation
                    getSoundHandler().playVocal("growl_warning");
                }
                // Set cooldown (mounted has longer to avoid flicker from rider clearing target)
                this.aggroGrowlCooldown = this.isVehicle() ? 120 : 80;
            }
        }
    }
    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        return riderController.getControllingPassenger();
    }

    // Prevent wyvern and its riders from taking fall damage when mounted/landing
    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, @Nonnull DamageSource source) {
        // Absorb fall damage; clear accumulated fall distance for passengers
        if (!this.level().isClientSide) {
            this.fallDistance = 0.0F;
            for (Entity e : this.getPassengers()) {
                if (e instanceof LivingEntity le) {
                    le.fallDistance = 0.0F;
                }
            }
        }
        return false;
    }
    
    // ===== DRAGON COMBAT CAPABLE INTERFACE =====
    
    @Override
    public double getMeleeRange() {
        return 3.5; // Lightning dragons have longer reach
    }
    
    @Override
    public float getMeleeDamage() {
        return 8.0f; // Lightning wyvern melee damage
    }
    
    @Override
    public boolean canMeleeAttack() {
        return !isBeaming() && !isCharging() && getTarget() != null;
    }
    
    @Override
    public void performMeleeAttack(LivingEntity target) {
        if (target != null && canMeleeAttack()) {
            // Perform melee attack
            target.hurt(this.damageSources().mobAttack(this), getMeleeDamage());
            
            // Lightning effect chance
            if (getRandom().nextFloat() < 0.3f) {
                playLightningEffect(target.position());
            }
        }
    }
    
    @Override
    public int getAttackCooldown() {
        return 20; // 1 second cooldown
    }
    
    @Override
    public boolean isAttacking() {
        return isBeaming() || isCharging();
    }
    
    @Override
    public void setAttacking(boolean attacking) {
        setBeaming(attacking);
    }
    
    // ===== DRAGON FLIGHT CAPABLE INTERFACE =====
    @Override
    public float getFlightSpeed() {
        return 1.0f; // Base flight speed
    }
    
    @Override
    public double getPreferredFlightAltitude() {
        return 15.0; // Preferred altitude above ground
    }
    
    @Override
    public boolean canTakeoff() {
        return !isInWaterOrBubble() && !isInLava() && onGround();
    }
    
    // ===== DRAGON SLEEP CAPABLE INTERFACE =====
    // Note: Most sleep methods already exist in LightningDragonEntity
    
    @Override
    public DragonSleepCapable.SleepPreferences getSleepPreferences() {
        return new DragonSleepCapable.SleepPreferences(
            false, // canSleepAtNight - wild Lightning Dragons are nocturnal, they sleep during day
            true,  // canSleepDuringDay - wild Lightning Dragons sleep during day
            true,  // requiresShelter
            true,  // avoidsThunderstorms (Lightning Dragons should not sleep in storms like other dragons)
            true   // sleepsNearOwner
        );
    }
    
    @Override
    public boolean canSleepNow() {
        return !isCharging() && !isBeaming() && !isVehicle();
    }
    
    // ===== LIGHTNING DRAGON SPECIFIC METHODS =====
    
    public void playLightningEffect(Vec3 position) {
        // Lightning effect implementation
        if (level().isClientSide) {
            // Client-side lightning effect
            level().addParticle(new RaevyxLightningStormData(1.0f, this.isFemale()),
                position.x, position.y, position.z, 0.0, 0.0, 0.0);
        }
    }

    public boolean hasEnhancedLineOfSight(Vec3 target) {
        // Lightning Dragons can see through some obstacles
        // For now, use simple distance check - can be enhanced later
        return position().distanceTo(target) < 50.0;
    }
    
    public float getEnergyLevel() {
        // Return energy level (0.0 to 1.0)
        return 0.8f; // Default high energy for Lightning Dragons
    }
    
    public boolean isCharging() {
        // Check if Lightning Dragon is charging
        return false; // Implement based on your charging logic
    }

    // ===== ELECTRICAL CONDUCTIVITY =====

    private static final ElectricalConductivityProfile CONDUCTIVITY_PROFILE =
            new ElectricalConductivityProfile(1.0f, 0.5f, 0.0f, 1.0, 0.3, 0.0);

    @Override
    public ElectricalConductivityProfile getConductivityProfile() {
        return CONDUCTIVITY_PROFILE;
    }

    @Override
    public ElectricalConductivityState getConductivityState() {
        return ElectricalConductivityCapable.super.getConductivityState();
    }

    @Override
    public Raevyx asConductiveEntity() {
        return this;
    }

    /**
     * Check if this wyvern can be bound (not playing dead, not sleeping, etc.)
     */
    public boolean canBeBound() {
        return !isSleeping() && !isDying() && !isCharging() && !isBeaming();
    }

    // ===== SCREEN SHAKE INTERFACE IMPLEMENTATION =====
    
    @Override
    public float getScreenShakeAmount(float partialTicks) {
        float currentAmount = getFloatData(DATA_SCREEN_SHAKE_AMOUNT);
        return prevScreenShakeAmount + (currentAmount - prevScreenShakeAmount) * partialTicks;
    }

    @Override
    public double getShakeDistance() {
        return 25.0; // Lightning Dragons have a larger shake radius than Tremorsaurus
    }

    @Override
    public boolean canFeelShake(Entity player) {
        // Allow screen shake regardless of whether player is on ground
        // This is important for wyvern riding scenarios
        return true;
    }

    /**
     * Triggers screen shake for the specified intensity.
     * 
     * @param intensity The shake intensity (0.0 to 1.0+)
     */
    public void triggerScreenShake(float intensity) {
        this.screenShakeAmount = Math.max(this.screenShakeAmount, intensity);
        this.entityData.set(DATA_SCREEN_SHAKE_AMOUNT, this.screenShakeAmount);
    }
}


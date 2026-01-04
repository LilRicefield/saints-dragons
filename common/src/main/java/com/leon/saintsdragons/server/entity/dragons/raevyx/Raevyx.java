/**
 * My name's Zap Van Dink. I'm a lightning wyvern.
 */
package com.leon.saintsdragons.server.entity.dragons.raevyx;

//Custom stuff
import com.leon.saintsdragons.common.particle.raevyx.*;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.raevyx.RaevyxAbilities;
import com.leon.saintsdragons.server.ai.goals.raevyx.RaevyxFlightGoal;
import com.leon.saintsdragons.server.ai.goals.raevyx.RaevyxFollowOwnerGoal;
import com.leon.saintsdragons.server.ai.goals.raevyx.RaevyxGroundWanderGoal;
import com.leon.saintsdragons.server.ai.goals.raevyx.RaevyxTemptGoal;
import com.leon.saintsdragons.server.ai.goals.raevyx.*;
import com.leon.saintsdragons.server.ai.goals.raevyx.baby.RaevyxFollowParentGoal;
import com.leon.saintsdragons.server.ai.navigation.DragonFlightMoveHelper;
import com.leon.saintsdragons.server.entity.behavior.DragonSleepBehavior;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.DragonGender;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.interfaces.*;
import com.leon.saintsdragons.server.entity.interfaces.DragonSoundProfile;
import com.leon.saintsdragons.server.entity.dragons.raevyx.handlers.RaevyxInteractionHandler;
import com.leon.saintsdragons.server.entity.dragons.raevyx.handlers.RaevyxAnimationHandler;
import com.leon.saintsdragons.server.entity.dragons.raevyx.handlers.RaevyxSoundProfile;
import com.leon.saintsdragons.server.entity.dragons.raevyx.handlers.RaevyxTamingHandler;
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
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.particles.ParticleTypes;


//GeckoLib
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.keyframe.event.SoundKeyframeEvent;

//WHO ARE THESE SUCKAS
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

//Just everything
public class Raevyx extends RideableDragonBase implements FlyingAnimal,
        DragonFlightCapable, ShakesScreen, SoundHandledDragon, ElectricalConductivityCapable {
    private static final float TAMING_HEALTH_RATIO = 1.0F / 3.0F;

    // ===== CONSTANTS =====

    /** Minimum delay between ambient sounds (in ticks) */
    public static final int MIN_AMBIENT_DELAY = 200;  // 10 seconds

    /** Maximum delay between ambient sounds (in ticks) */
    public static final int MAX_AMBIENT_DELAY = 600;  // 30 seconds

    /** Scale factor for the wyvern model */
    public static final float MODEL_SCALE = 1.0f;

    /** Time to live for aggression tracking (in ticks) */
    public static final int AGGRO_TTL_TICKS = 200; // ~10s

    // ===== ENTITY DATA ACCESSORS =====

    /** Entity data accessor for flying state */
    public static final EntityDataAccessor<Boolean> DATA_FLYING =
            net.minecraft.network.syncher.SynchedEntityData.defineId(Raevyx.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);

    /** Entity data accessor for takeoff state */
    public static final EntityDataAccessor<Boolean> DATA_TAKEOFF =
            net.minecraft.network.syncher.SynchedEntityData.defineId(Raevyx.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);

    /** Entity data accessor for hovering state */
    public static final EntityDataAccessor<Boolean> DATA_HOVERING =
            net.minecraft.network.syncher.SynchedEntityData.defineId(Raevyx.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);

    /** Entity data accessor for landing state */
    public static final EntityDataAccessor<Boolean> DATA_LANDING =
            net.minecraft.network.syncher.SynchedEntityData.defineId(Raevyx.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);

    /** Entity data accessor for landed state (post-landing settle animation) */
    public static final EntityDataAccessor<Boolean> DATA_LANDED =
            net.minecraft.network.syncher.SynchedEntityData.defineId(Raevyx.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);

    /** Entity data accessor for running state */
    public static final EntityDataAccessor<Boolean> DATA_RUNNING =
            net.minecraft.network.syncher.SynchedEntityData.defineId(Raevyx.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);

    /** Entity data accessor for ground move state (0=idle, 1=walk, 2=run) */
    public static final EntityDataAccessor<Integer> DATA_GROUND_MOVE_STATE =
            net.minecraft.network.syncher.SynchedEntityData.defineId(Raevyx.class, net.minecraft.network.syncher.EntityDataSerializers.INT);

    /** Entity data accessor for flight mode (0=glide,1=forward,2=hover,3=takeoff,-1=ground) */
    public static final EntityDataAccessor<Integer> DATA_FLIGHT_MODE =
            net.minecraft.network.syncher.SynchedEntityData.defineId(Raevyx.class, net.minecraft.network.syncher.EntityDataSerializers.INT);

    /** Entity data accessor for rider forward input */
    public static final EntityDataAccessor<Float> DATA_RIDER_FORWARD =
            net.minecraft.network.syncher.SynchedEntityData.defineId(Raevyx.class, net.minecraft.network.syncher.EntityDataSerializers.FLOAT);

    /** Entity data accessor for rider strafe input */
    public static final EntityDataAccessor<Float> DATA_RIDER_STRAFE =
            net.minecraft.network.syncher.SynchedEntityData.defineId(Raevyx.class, net.minecraft.network.syncher.EntityDataSerializers.FLOAT);

    /** Entity data accessor for screen shake amount */
    public static final EntityDataAccessor<Float> DATA_SCREEN_SHAKE_AMOUNT =
            net.minecraft.network.syncher.SynchedEntityData.defineId(Raevyx.class, net.minecraft.network.syncher.EntityDataSerializers.FLOAT);

    /** Entity data accessor for beaming state */
    public static final EntityDataAccessor<Boolean> DATA_BEAMING =
            net.minecraft.network.syncher.SynchedEntityData.defineId(Raevyx.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);

    /** Tracks whether the emissive glow layer should be active (beam startup/active) */
    public static final EntityDataAccessor<Boolean> DATA_BEAM_GLOW =
            net.minecraft.network.syncher.SynchedEntityData.defineId(Raevyx.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);

    /** Entity data accessor for rider landing blend active state */
    public static final EntityDataAccessor<Boolean> DATA_RIDER_LANDING_BLEND =
            net.minecraft.network.syncher.SynchedEntityData.defineId(Raevyx.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);


    /** Entity data accessor for beam end position set flag */
    public static final EntityDataAccessor<Boolean> DATA_BEAM_END_SET =
            net.minecraft.network.syncher.SynchedEntityData.defineId(Raevyx.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);

    /** Entity data accessor for beam end X coordinate */
    public static final EntityDataAccessor<Float> DATA_BEAM_END_X =
            net.minecraft.network.syncher.SynchedEntityData.defineId(Raevyx.class, net.minecraft.network.syncher.EntityDataSerializers.FLOAT);

    /** Entity data accessor for beam end Y coordinate */
    public static final EntityDataAccessor<Float> DATA_BEAM_END_Y =
            net.minecraft.network.syncher.SynchedEntityData.defineId(Raevyx.class, net.minecraft.network.syncher.EntityDataSerializers.FLOAT);

    /** Entity data accessor for beam end Z coordinate */
    public static final EntityDataAccessor<Float> DATA_BEAM_END_Z =
            net.minecraft.network.syncher.SynchedEntityData.defineId(Raevyx.class, net.minecraft.network.syncher.EntityDataSerializers.FLOAT);

    /** Entity data accessor for beam start position set flag */
    public static final EntityDataAccessor<Boolean> DATA_BEAM_START_SET =
            net.minecraft.network.syncher.SynchedEntityData.defineId(Raevyx.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);

    /** Entity data accessor for beam start X coordinate */
    public static final EntityDataAccessor<Float> DATA_BEAM_START_X =
            net.minecraft.network.syncher.SynchedEntityData.defineId(Raevyx.class, net.minecraft.network.syncher.EntityDataSerializers.FLOAT);

    /** Entity data accessor for beam start Y coordinate */
    public static final EntityDataAccessor<Float> DATA_BEAM_START_Y =
            net.minecraft.network.syncher.SynchedEntityData.defineId(Raevyx.class, net.minecraft.network.syncher.EntityDataSerializers.FLOAT);

    /** Entity data accessor for beam start Z coordinate */
    public static final EntityDataAccessor<Float> DATA_BEAM_START_Z =
            net.minecraft.network.syncher.SynchedEntityData.defineId(Raevyx.class, net.minecraft.network.syncher.EntityDataSerializers.FLOAT);

    /** Entity data accessor for going up state */
    public static final EntityDataAccessor<Boolean> DATA_GOING_UP =
            net.minecraft.network.syncher.SynchedEntityData.defineId(Raevyx.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);

    /** Entity data accessor for going down state */
    public static final EntityDataAccessor<Boolean> DATA_GOING_DOWN =
            net.minecraft.network.syncher.SynchedEntityData.defineId(Raevyx.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);

    /** Entity data accessor for accelerating state */
    public static final EntityDataAccessor<Boolean> DATA_ACCELERATING =
            net.minecraft.network.syncher.SynchedEntityData.defineId(Raevyx.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);

    /** Entity data accessor for dashing state */
    public static final EntityDataAccessor<Boolean> DATA_DASHING =
            net.minecraft.network.syncher.SynchedEntityData.defineId(Raevyx.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);

    /** Entity data accessor for dash alternating state (true = last was right, false = last was left) */
    public static final EntityDataAccessor<Boolean> DATA_LAST_DASH_RIGHT =
            net.minecraft.network.syncher.SynchedEntityData.defineId(Raevyx.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);

    /** Entity data accessor for sleeping state */
    public static final EntityDataAccessor<Boolean> DATA_SLEEPING =
            net.minecraft.network.syncher.SynchedEntityData.defineId(Raevyx.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);

    /** Entity data accessor for sleep enter transition state */
    public static final EntityDataAccessor<Boolean> DATA_SLEEPING_ENTERING =
            net.minecraft.network.syncher.SynchedEntityData.defineId(Raevyx.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);

    /** Entity data accessor for sleep exit transition state */
    public static final EntityDataAccessor<Boolean> DATA_SLEEPING_EXITING =
            net.minecraft.network.syncher.SynchedEntityData.defineId(Raevyx.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);

    /** Entity data accessor for feeding cooldown ticks */
    public static final EntityDataAccessor<Integer> DATA_FEEDING_COOLDOWN =
            net.minecraft.network.syncher.SynchedEntityData.defineId(Raevyx.class, net.minecraft.network.syncher.EntityDataSerializers.INT);
    /** Tracks whether the wyvern is stunned during a taming attempt */
    public static final EntityDataAccessor<Boolean> DATA_TAMING_STUNNED =
            net.minecraft.network.syncher.SynchedEntityData.defineId(Raevyx.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);

    // ===== OTHER CONSTANTS =====
    public AnimatableInstanceCache dragonCache = GeckoLibUtil.createInstanceCache(this);

    public static final float MAX_BEAM_YAW_DEG = 40.0f;
    public static final float MAX_BEAM_PITCH_DEG = 50.0f;


    // Simple per-field caches - more maintainable than generic system
    private double cachedOwnerDistance = Double.MAX_VALUE;
    private int ownerDistanceCacheTime = -1;
    private double cachedHorizontalSpeed = 0.0;
    private int horizontalSpeedCacheTime = -1;
    private static final double RIDER_GLIDE_ALTITUDE_THRESHOLD = 40.0D;
    private static final double RIDER_GLIDE_ALTITUDE_EXIT = 30.0D; // Hysteresis: exit at lower altitude
    private static final double RIDER_LOW_ALTITUDE_GLIDE_THRESHOLD = 6.0D;
    private static final double RIDER_WATER_SURFACE_LEVEL = 62.0D;
    private static final double RIDER_WATER_SURFACE_TOLERANCE = 2.0D;
    private static final int RIDER_WATER_SCAN_RADIUS = 2;
    public static final double LANDING_BLEND_ALTITUDE = 8.0D;
    private static final int RIDER_LANDING_BLEND_DURATION = 5; // ticks to keep landing blend active after triggering
    private boolean inHighAltitudeGlide = false; // Track glide state for smooth transitions

    // Position tracking for FLY_IDLE detection (xo/yo/zo are synced too early in tick cycle)
    private double lastCheckedX = 0;
    private double lastCheckedY = 0;
    private double lastCheckedZ = 0;
    private int ticksSinceLastMovement = 0;
    private static final Map<String, VocalEntry> VOCAL_ENTRIES = new VocalEntryBuilder()
            .add("grumble1", "action", "animation.raevyx.grumble1", ModSounds.RAEVYX_GRUMBLE_1, 0.8f, 0.95f, 0.1f, false, false, false)
            .add("grumble2", "action", "animation.raevyx.grumble2", ModSounds.RAEVYX_GRUMBLE_2, 0.8f, 0.95f, 0.1f, false, false, false)
            .add("grumble3", "action", "animation.raevyx.grumble3", ModSounds.RAEVYX_GRUMBLE_3, 0.8f, 0.95f, 0.1f, false, false, false)
            .add("purr", "action", "animation.raevyx.purr", ModSounds.RAEVYX_PURR, 0.8f, 1.05f, 0.05f, true, false, true)
            .add("snort", "action", "animation.raevyx.snort", ModSounds.RAEVYX_SNORT, 0.9f, 0.9f, 0.2f, false, false, false)
            .add("chuff", "action", "animation.raevyx.chuff", ModSounds.RAEVYX_CHUFF, 0.9f, 0.9f, 0.2f, false, false, false)
            .add("content", "action", "animation.raevyx.content", ModSounds.RAEVYX_CONTENT, 0.8f, 1.0f, 0.1f, true, false, true)
            .add("excited", "action", "", ModSounds.RAEVYX_EXCITED, 1.0f, 1.0f, 0.3f, false, false, false)  // Sound-only, no animation
            .add("growl_warning", "action", "", ModSounds.RAEVYX_GROWL_WARNING, 1.2f, 0.8f, 0.4f, false, false, false)  // Sound-only, no animation
            .add("roar", "action", "animation.raevyx.roar", ModSounds.RAEVYX_ROAR, 1.4f, 0.9f, 0.15f, false, false, false)
            .add("roar_ground", "action", "animation.raevyx.roar_ground", ModSounds.RAEVYX_ROAR, 1.4f, 0.9f, 0.15f, false, false, false)
            .add("roar_air", "action", "animation.raevyx.roar_air", ModSounds.RAEVYX_ROAR, 1.4f, 0.9f, 0.15f, false, false, false)
            .add("raevyx_hurt", "hurt", "animation.raevyx.hurt", ModSounds.RAEVYX_HURT, 1.2f, 0.95f, 0.1f, true, true, true)
            .add("raevyx_die", "action", "animation.raevyx.die", ModSounds.RAEVYX_DIE, 1.5f, 0.95f, 0.1f, false, true, true)
            .add("baby_raevyx_hurt", "hurt", "animation.raevyx.hurt", ModSounds.BABY_RAEVYX_HURT, 1.4f, 1.05f, 0.15f, true, true, true)
            .add("baby_raevyx_die", "action", "animation.raevyx.die", ModSounds.BABY_RAEVYX_DIE, 1.3f, 1.0f, 0.1f, false, true, true)
            .build();

    private boolean manualSitCommand = false;
    private boolean commandChangeManual = false;
    private int riderLandingBlendTicks = 0;
    private static final int DISMOUNT_RECALL_WINDOW = 60;
    private int dismountRecallTicks = 0;

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return dragonCache;
    }

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
    
    public boolean shouldForceOwnerFollow() {
        return dismountRecallTicks > 0;
    }

    public void clearForcedOwnerFollow() {
        this.dismountRecallTicks = 0;
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
     * Helper method for float entity data access
     */
    private float getFloatData(EntityDataAccessor<Float> accessor) {
        return this.entityData.get(accessor);
    }


    // ===== STATE VARIABLES (Package-private for controller access) =====
    public int timeFlying = 0;
    public boolean landingFlag = false;
    public boolean landedFlag = false;

    public int landingTimer = 0;
    public int landedTimer = 0;
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
    private static final double DODGE_HORIZONTAL_DRAG = 0.92D;
    private static final double DODGE_VERTICAL_DRAG = 0.95D;
    private static final int DODGE_DURATION_TICKS = 12;
    private static final int DODGE_IFRAMES_TICKS = 8;
    private static final int RIDER_DODGE_COOLDOWN_TICKS = 30;
    private static final int AI_DODGE_COOLDOWN_TICKS = 60;
    private static final double DODGE_DISTANCE_BLOCKS = 20;
    boolean dodging = false;
    int dodgeTicksLeft = 0;
    Vec3 dodgeVec = Vec3.ZERO;
    int dodgeCooldownTicks = 0; // Rider dodge cooldown
    int aiDodgeCooldownTicks = 0; // AI dodge cooldown
    int dodgeIFramesTicks = 0; // Invulnerability frames during dodge

    // Dash forward system
    private static final double DASH_HORIZONTAL_DRAG = 0.92D;
    private static final double DASH_VERTICAL_DRAG = 0.95D;
    private boolean dashing = false;
    private int dashTicksLeft = 0; // Duration: 25 ticks = 1.25 seconds
    private int dashCooldownTicks = 0; // Cooldown between dashes
    private Vec3 dashVec = Vec3.ZERO;
    private boolean lastDashWasRight = false; // Track which dash animation to play next (alternating)
    private final java.util.Map<Integer, Integer> dashHitCooldowns = new java.util.HashMap<>(); // entityId -> cooldown ticks

    // Client-side animation initialization grace period (fixes T-pose on world rejoin with shaders)
    private int clientAnimInitTicks = 0;
    private static final int ANIM_INIT_GRACE_PERIOD = 5; // Wait 5 ticks for entity data sync

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
    private boolean sleepFallAsleepTriggered = false;
    private boolean sleepSitUpTriggered = false;
    private boolean sleepLocked = false;
    private int sleepCommandSnapshot = -1;

    private int followFailsafeCooldown = 0;
    private int postStandUnlockTicks = 0;

    // Feeding cooldown synced via DATA_FEEDING_COOLDOWN entity data accessor
    private final RaevyxTamingHandler tamingController = new RaevyxTamingHandler(this);
    private int tamingAbortCalmTicks = 0;

    public boolean canFeed() {
        int cooldownTicks = this.entityData.get(DATA_FEEDING_COOLDOWN);
        return cooldownTicks > 0;
    }

    public void setFeedingCooldown(int ticks) {
        this.entityData.set(DATA_FEEDING_COOLDOWN, ticks);
    }

    public boolean isTamingStunned() {
        return this.entityData.get(DATA_TAMING_STUNNED);
    }

    public void enterTamingStun() {
        tamingController.enterStun();
    }

    public void setTamingRecoveryTarget(float targetHealth) {
        tamingController.setRecoveryTarget(targetHealth);
    }

    public void clearTamingRecovery() {
        tamingController.clearRecovery();
    }

    public void incrementTamingFailures() {
        tamingController.incrementFailures();
    }

    public void resetTamingFailures() {
        tamingController.resetFailures();
    }

    public boolean isAwaitingTamingFeed() {
        return tamingController.isAwaitingFeed();
    }

    public void abortTamingAttempt() {
        abortTamingAttempt(true);
    }

    public void abortTamingAttempt(boolean restoreHealth) {
        clearTamingRecovery();
        resetTamingFailures();
        setTarget(null);
        setAggressive(false);
        setLastHurtByMob(null);
        this.setLastHurtByPlayer(null);
        this.combatManager.clearAllStates();
        this.hurtMarked = false;
        if (restoreHealth && isAlive()) {
            setHealth(getMaxHealth());
        }
        tamingAbortCalmTicks = Math.max(tamingAbortCalmTicks, 100);
    }

    public boolean isBelowTamingThreshold() {
        return this.getHealth() <= getTamingThreshold();
    }

    public float getTamingThreshold() {
        return this.getMaxHealth() * TAMING_HEALTH_RATIO;
    }

    // Rider takeoff request timer: while > 0, flight controller treats state as takeoff
    private int riderTakeoffTicks = 0;


    // Last landing completion time (server game time). Used for takeoff cooldowns.
    private long lastLandingGameTime = Long.MIN_VALUE;

    public long getLastLandingGameTime() {
        return lastLandingGameTime;
    }


    @Override
    public void markLandedNow() {
        setFlying(false);
        setLanding(false);
        setTakeoff(false);
        this.riderTakeoffTicks = 0;
        this.timeFlying = 0;

        if (!level().isClientSide) {
            this.lastLandingGameTime = level().getGameTime();
        }

        // Note: Rider landing is handled separately in tickRiderLandingBlendTimer()
        // This path only handles AI/non-rider landings
    }

    public void handleAiLandingComplete() {
        if (isInWaterOrBubble()) {
            suppressSleep(60);
            markLandedNow();
            return;
        }
        if (!level().isClientSide) {
            triggerAnim("action", "landed");
            suppressSleep(60);
        }
        markLandedNow();
    }

    // Navigation (Package-private for controller access)
    public final GroundPathNavigation groundNav;
    public final FlyingPathNavigation airNav;
    public boolean usingAirNav = false;

    // ===== HARDCODED GROUND SPEEDS =====
    public static final double RIDER_WALK_SPEED = 0.18D;
    public static final double RIDER_RUN_SPEED = 0.40D;

    private final RaevyxInteractionHandler lightningInteractionHandler;
    private final RaevyxAnimationHandler animationHandler;

    // ===== SPECIALIZED HANDLER SYSTEMS =====
    private final RaevyxRiderController riderController;
    private final DragonSoundHandler soundHandler;

    // ===== CLIENT LOCATOR CACHE (client-side only) =====
    private final Map<String, Vec3> clientLocatorCache = new ConcurrentHashMap<>();

    @Override
    public boolean isInSittingPose() {
        return super.isInSittingPose() && !(this.isVehicle() || this.isPassenger() || this.isFlying());
    }
    // GeckoLib cache is now handled by base DragonEntity class

    //FLIGHT
    // (Flight mode logic is already inline in getFlightMode() - no physics controller needed)

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

        // Pathfinding setup
        this.setPathfindingMalus(BlockPathTypes.DANGER_FIRE, -1.0F);
        this.setPathfindingMalus(BlockPathTypes.WATER, -1.0F);
        this.setPathfindingMalus(BlockPathTypes.WATER_BORDER, -1.0F);
        this.setPathfindingMalus(BlockPathTypes.FENCE, -1.0F);

        // Initialize controllers (FlightController removed)
        this.lightningInteractionHandler = new RaevyxInteractionHandler(this);
        this.animationHandler = new RaevyxAnimationHandler(this);

        // Initialize specialized handler systems
        this.riderController = new RaevyxRiderController(this);
        this.soundHandler = new DragonSoundHandler(this);

        // Desynchronize ambient system across instances to avoid synchronized vocals/animations
        RandomSource rng = this.getRandom();
        this.ambientSoundTimer = rng.nextInt(80); // small random offset
        this.nextAmbientSoundDelay = MIN_AMBIENT_DELAY + rng.nextInt(MAX_AMBIENT_DELAY - MIN_AMBIENT_DELAY);

        if (!level.isClientSide) {
            applyConfiguredAttributes();
        }
    }

    @Override
    protected float getBodyTurnSpeed() {
        return 0.6f; // Match default turn speed like other dragons
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
        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance().getConfig(DragonAttributeConfigLoader.RAEVYX_ID);
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, config.maxHealth())
                .add(Attributes.MOVEMENT_SPEED, 0.25D) // Hardcoded AI pathfinding speed
                .add(Attributes.FOLLOW_RANGE, 80.0D)
                .add(Attributes.FLYING_SPEED, config.flyingSpeed())
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ARMOR, config.armor());
    }

    // Cooldown to prevent hurt sound spam when ridden or under rapid hits
    private int hurtSoundCooldown = 0;
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        // Define Lightning Dragon specific data
        this.entityData.define(DATA_SCREEN_SHAKE_AMOUNT, 0.0F);
        this.entityData.define(DATA_BEAMING, false);
        this.entityData.define(DATA_BEAM_GLOW, false);
        this.entityData.define(DATA_RIDER_LANDING_BLEND, false);
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
        this.entityData.define(DATA_FEEDING_COOLDOWN, 0);
        this.entityData.define(DATA_TAMING_STUNNED, false);
    }

    @Override
    protected void defineRideableDragonData() {
        // Define all rideable wyvern data keys for LightningDragonEntity
        this.entityData.define(DATA_FLYING, false);
        this.entityData.define(DATA_TAKEOFF, false);
        this.entityData.define(DATA_HOVERING, false);
        this.entityData.define(DATA_LANDING, false);
        this.entityData.define(DATA_LANDED, false);
        this.entityData.define(DATA_RUNNING, false);
        this.entityData.define(DATA_GROUND_MOVE_STATE, 0);
        this.entityData.define(DATA_FLIGHT_MODE, -1);
        this.entityData.define(DATA_RIDER_FORWARD, 0f);
        this.entityData.define(DATA_RIDER_STRAFE, 0f);
        this.entityData.define(DATA_GOING_UP, false);
        this.entityData.define(DATA_GOING_DOWN, false);
        this.entityData.define(DATA_ACCELERATING, false);
        this.entityData.define(DATA_DASHING, false);
        this.entityData.define(DATA_LAST_DASH_RIGHT, false);
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
    protected void applyLoadedFlightState(boolean flying, boolean takeoff, boolean hovering, boolean landing) {
        setFlying(flying);
        setTakeoff(takeoff);
        setHovering(hovering);
        setLanding(landing);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends DragonEntity> DragonAbility<T> getActiveAbility() {
        return (DragonAbility<T>) combatManager.getActiveAbility();
    }

    public boolean isAbilityActive(DragonAbilityType<?, ?> abilityType) {
        return combatManager.isAbilityActive(abilityType);
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
        boolean inWater = this.isInWater() || this.isInWaterOrBubble();

        // WATER OVERRIDES LOCK - must always allow vertical input in water!
        if (inWater) {
            setGoingUp(goingUp);
            setGoingDown(goingDown);
            return;
        }

        if (locked) {
            setGoingUp(false);
            setGoingDown(false);
            return;
        }

        // Allow vertical input when flying
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
            case TOGGLE_MELEE -> {
                if (!locked) {
                    toggleMeleeMode();
                }
            }
            case DOUBLE_TAP_A -> {
                if (!locked) {
                    onRiderDodge(player, true);
                }
            }
            case DOUBLE_TAP_D -> {
                if (!locked) {
                    onRiderDodge(player, false);
                }
            }
            case DOUBLE_TAP_W -> {
                if (!locked) {
                    onRiderDash(player);
                }
            }
            case DOUBLE_TAP_S -> {
                if (!locked) {
                    onRiderBackwardDodge(player);
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

    public Vec3 getBeamStartAnchor(float partialTicks) {
        Vec3 clientBone = getClientLocatorPosition("beamBoneOrigin");
        if (clientBone != null) {
            return clientBone;
        }
        return computeHeadMouthOrigin(partialTicks);
    }

    public void forceEndActiveAbility() {
        combatManager.forceEndActiveAbility();
    }

    public boolean isBeaming() { return getBooleanData(DATA_BEAMING); }
    public void setBeaming(boolean beaming) {
        boolean wasBeaming = getBooleanData(DATA_BEAMING);
        setBooleanData(DATA_BEAMING, beaming);
        if (!beaming) {
            clearBeamPath();
            beamTime = 0;
            beamServerTarget = null;
        }
        if (!beaming || !wasBeaming) {
            resetBeamAim();
        }
        if (beaming && !wasBeaming) {
            // Just started beaming - initialize targeting
            beamTime = 0;
            beamServerTarget = createInitialBeamTarget();
        }
    }

    public boolean isBeamGlowActive() {
        return this.entityData.get(DATA_BEAM_GLOW);
    }

    public void setBeamGlowActive(boolean active) {
        this.entityData.set(DATA_BEAM_GLOW, active);
    }

    // (No client/server rider anchor fields; seat uses math-based head-space anchor)

    // ===== BEAM END SYNC + CLIENT LERP =====
    private Vec3 prevClientBeamEnd = null;
    private Vec3 clientBeamEnd = null;
    private Vec3 beamLookLerp = null;
    private Vec3 beamAimDir = null;
    private float beamYawOffsetRad = 0.0f;
    private float beamPitchOffsetRad = 0.0f;

    // ===== BEAM TARGETING (AI-driven aiming) =====
    private int beamTime = 0; // Tracks how long beam has been active for accuracy ramping
    private Vec3 beamServerTarget = null; // Server-side smooth target position with wobble

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

    public void syncBeamPath(@Nullable Vec3 start, @Nullable Vec3 end) {
        setBeamStartPosition(start);
        setBeamEndPosition(end);
    }

    public void clearBeamPath() {
        setBeamStartPosition(null);
        setBeamEndPosition(null);
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
        boolean wasTakeoff = isTakeoff();
        this.entityData.set(DATA_TAKEOFF, takeoff);
        if (takeoff && !wasTakeoff && !level().isClientSide) {
            float pitch = 0.94f + this.getRandom().nextFloat() * 0.12f;
            this.playSound(ModSounds.RAEVYX_TAKEOFF.get(), 1.2f, pitch);
        }
    }

    /**
     * Break vegetation blocks that the dragon collides with during takeoff
     * Prevents wobbling when taking off near trees
     */
    private void breakBlocksDuringTakeoff() {
        if (level().isClientSide) return;

        // Get bounding box
        var bb = this.getBoundingBox();

        // Expand slightly to catch blocks we're about to hit
        bb = bb.inflate(0.2);

        // Check all block positions within the bounding box
        BlockPos minPos = BlockPos.containing(bb.minX, bb.minY, bb.minZ);
        BlockPos maxPos = BlockPos.containing(bb.maxX, bb.maxY, bb.maxZ);

        for (BlockPos pos : BlockPos.betweenClosed(minPos, maxPos)) {
            var state = level().getBlockState(pos);

            // Skip air
            if (state.isAir()) {
                continue;
            }

            // Check if it's breakable vegetation
            if (isBreakableVegetation(state)) {
                // Break the block without drops (just destroy it)
                level().destroyBlock(pos, false);
            }
        }
    }

    /**
     * Check if a block is breakable vegetation
     */
    private boolean isBreakableVegetation(net.minecraft.world.level.block.state.BlockState state) {
        var block = state.getBlock();
        return block instanceof net.minecraft.world.level.block.LeavesBlock ||
               block instanceof net.minecraft.world.level.block.VineBlock ||
               block instanceof net.minecraft.world.level.block.TallGrassBlock ||
               block instanceof net.minecraft.world.level.block.FlowerBlock ||
               block instanceof net.minecraft.world.level.block.DoublePlantBlock ||
               block instanceof net.minecraft.world.level.block.SaplingBlock ||
               block instanceof net.minecraft.world.level.block.BushBlock;
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
            this.setTakeoff(false);
            this.landingFlag = true;
        } else {
            this.landingFlag = false;
        }
    }

    public void setLanded(boolean landed) {
        this.entityData.set(DATA_LANDED, landed);
        if (landed) {
            landedTimer = 0;
            landedFlag = true;
        } else {
            landedFlag = false;
        }
    }

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
        // Treat the first few ticks of flight as takeoff even if the explicit flag cleared early
        if (isTakeoff() || this.timeFlying < 35) return 3;  // Takeoff
        if (isHovering()) return 2; // Hover
        if (isLanding()) return 2;  // Landing (treat as hover)

        // Altitude-based animation for ridden dragons
        if (this.isTame() && this.isVehicle()) {
            Entity rider = this.getControllingPassenger();
            if (rider instanceof Player player && this.isOwnedBy(player)) {
                // Track position changes manually (xo/yo/zo are synced before getFlightMode() is called)
                double deltaX = this.getX() - lastCheckedX;
                double deltaY = this.getY() - lastCheckedY;
                double deltaZ = this.getZ() - lastCheckedZ;
                double positionChangeSqr = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;

                boolean goingUp = isGoingUp();
                boolean goingDown = isGoingDown();
                boolean accelerating = isAccelerating();

                // Update position tracking and movement timer
                if (positionChangeSqr > 0.0001 || goingUp || goingDown || accelerating) {
                    // Dragon is moving or player is giving directional input
                    ticksSinceLastMovement = 0;
                    lastCheckedX = this.getX();
                    lastCheckedY = this.getY();
                    lastCheckedZ = this.getZ();
                } else {
                    // No movement detected
                    ticksSinceLastMovement++;
                }

                // Only switch to FLY_IDLE after being stationary for a bit (prevents flicker)
                if (ticksSinceLastMovement > 3) { // 3 ticks = 0.15 seconds of no movement
                    return 5; // FLY_IDLE - stationary rider hover
                }

                // Check for sprinting
                if (accelerating) {
                    return 4; // SPRINT_FLAP
                }

                int groundY = this.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        Mth.floor(this.getX()), Mth.floor(this.getZ()));
                double altitudeAboveTerrain = this.getY() - groundY;

                // Force gliding when skimming water or ground effect altitude
                if (shouldGlideNearWaterSurface() || altitudeAboveTerrain <= RIDER_LOW_ALTITUDE_GLIDE_THRESHOLD) {
                    inHighAltitudeGlide = false;
                    return 0;
                }

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

    public boolean isLanded() {
        return getBooleanData(DATA_LANDED);
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
    public RiderAbilityBinding getAttackRiderAbility() {
        // Use melee mode to switch between bite and horn gore
        // Mode 0 = bite (primary), Mode 1 = horn gore (secondary)
        if (getMeleeMode() == 0) {
            return new RiderAbilityBinding(RaevyxAbilities.RAEVYX_BITE.getName(), RiderAbilityBinding.Activation.PRESS);
        } else {
            return new RiderAbilityBinding(RaevyxAbilities.RAEVYX_HORN_GORE.getName(), RiderAbilityBinding.Activation.PRESS);
        }
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

    public boolean isDashing() {
        // Use synced entity data so client can see dashing state for animations
        return this.entityData.get(DATA_DASHING);
    }

    public boolean wasLastDashRight() {
        // Use synced entity data so client can determine which animation to play
        return this.entityData.get(DATA_LAST_DASH_RIGHT);
    }

    public void beginDodge(Vec3 vec, int ticks) {
        this.dodging = true;
        this.dodgeVec = vec;
        this.dodgeTicksLeft = Math.max(1, ticks);
        this.setDeltaMovement(vec);
        this.getNavigation().stop();
        this.hasImpulse = true;
    }

    @Override
    protected void onRiderDodge(net.minecraft.world.entity.player.Player player, boolean isLeft) {
        // Only allow dodge on ground - dragon is fast enough in air with strafe
        if (isFlying()) {
            return;
        }

        // Cancel dash if currently dashing (dodge interrupts dash)
        if (dashing) {
            dashing = false;
            dashTicksLeft = 0;
            dashVec = Vec3.ZERO;
            this.entityData.set(DATA_DASHING, false);
            dashHitCooldowns.clear();
        }

        // Check cooldown
        if (dodgeCooldownTicks > 0) {
            return;
        }

        // Dodge constants
        final int DODGE_CONTROL_LOCK = 12; // 1 second rider lock so attacks/abilities don't override dodge

        // Get right vector (perpendicular to facing direction)
        float yawRad = (float) Math.toRadians(this.getYRot());
        double forwardX = -Math.sin(yawRad);
        double forwardZ = Math.cos(yawRad);
        double rightX = Math.cos(yawRad);
        double rightZ = Math.sin(yawRad);

        // Account for drag so the integrated distance over the duration is ~DODGE_DISTANCE
        double dragScale = 1.0D - Math.pow(DODGE_HORIZONTAL_DRAG, DODGE_DURATION_TICKS);
        double perTickSpeed = DODGE_DISTANCE_BLOCKS * (1.0D - DODGE_HORIZONTAL_DRAG) / dragScale;

        // Calculate dodge direction (left or right)
        // FLIPPED: was backwards, A went right and D went left!
        double dodgeDirX = rightX * (isLeft ? 1 : -1);
        double dodgeDirZ = rightZ * (isLeft ? 1 : -1);

        // Create dodge vector (horizontal only, no vertical component)
        Vec3 dodgeVector = new Vec3(dodgeDirX * perTickSpeed, 0, dodgeDirZ * perTickSpeed);

        // Begin dodge
        beginDodge(dodgeVector, DODGE_DURATION_TICKS);
        lockRiderControls(DODGE_CONTROL_LOCK);

        // Set cooldown and i-frames
        dodgeCooldownTicks = RIDER_DODGE_COOLDOWN_TICKS;
        dodgeIFramesTicks = DODGE_IFRAMES_TICKS;

        // Trigger dodge animation
        if (isLeft) {
            animationHandler.triggerDodgeLeftAnimation();
        } else {
            animationHandler.triggerDodgeRightAnimation();
        }
    }

    @Override
    protected void onRiderBackwardDodge(net.minecraft.world.entity.player.Player player) {
        // Only allow dodge on ground - dragon is fast enough in air with strafe
        if (isFlying()) {
            return;
        }

        // Cancel dash if currently dashing (dodge interrupts dash)
        if (dashing) {
            dashing = false;
            dashTicksLeft = 0;
            dashVec = Vec3.ZERO;
            this.entityData.set(DATA_DASHING, false);
            dashHitCooldowns.clear();
        }

        // Check cooldown
        if (dodgeCooldownTicks > 0) {
            return;
        }

        // Dodge constants
        final int DODGE_CONTROL_LOCK = 12; // 1 second rider lock so attacks/abilities don't override dodge

        // Get backward vector (opposite of facing direction)
        float yawRad = (float) Math.toRadians(this.getYRot());
        double forwardX = -Math.sin(yawRad);
        double forwardZ = Math.cos(yawRad);

        // Account for drag so the integrated distance over the duration is ~DODGE_DISTANCE
        double dragScale = 1.0D - Math.pow(DODGE_HORIZONTAL_DRAG, DODGE_DURATION_TICKS);
        double perTickSpeed = DODGE_DISTANCE_BLOCKS * (1.0D - DODGE_HORIZONTAL_DRAG) / dragScale;

        // Calculate backward dodge direction (opposite of forward)
        double dodgeDirX = -forwardX;
        double dodgeDirZ = -forwardZ;

        // Create dodge vector (horizontal only, no vertical component)
        Vec3 dodgeVector = new Vec3(dodgeDirX * perTickSpeed, 0, dodgeDirZ * perTickSpeed);

        // Begin dodge
        beginDodge(dodgeVector, DODGE_DURATION_TICKS);
        lockRiderControls(DODGE_CONTROL_LOCK);

        // Set cooldown and i-frames
        dodgeCooldownTicks = RIDER_DODGE_COOLDOWN_TICKS;
        dodgeIFramesTicks = DODGE_IFRAMES_TICKS;

        // Trigger backward dodge animation
        animationHandler.triggerDodgeBackwardAnimation();
    }

    public boolean tryAIGroundDodge(@Nullable LivingEntity threat) {
        // Only allow dodge on ground - dragon is fast enough in air with strafe
        if (isFlying() || isDodging()) {
            return false;
        }

        // Cancel dash if currently dashing (dodge interrupts dash)
        if (dashing) {
            dashing = false;
            dashTicksLeft = 0;
            dashVec = Vec3.ZERO;
            this.entityData.set(DATA_DASHING, false);
            dashHitCooldowns.clear();
        }

        // Check cooldown
        if (aiDodgeCooldownTicks > 0) {
            return false;
        }

        float yawRad = (float) Math.toRadians(this.getYRot());
        double forwardX = -Math.sin(yawRad);
        double forwardZ = Math.cos(yawRad);
        double rightX = Math.cos(yawRad);
        double rightZ = Math.sin(yawRad);

        double dragScale = 1.0D - Math.pow(DODGE_HORIZONTAL_DRAG, DODGE_DURATION_TICKS);
        double perTickSpeed = DODGE_DISTANCE_BLOCKS * (1.0D - DODGE_HORIZONTAL_DRAG) / dragScale;

        boolean doBackward = threat != null
                && this.distanceToSqr(threat) < 36.0D
                && this.getRandom().nextFloat() < 0.35f;
        boolean isLeft = this.getRandom().nextBoolean();

        Vec3 dodgeVector;
        if (doBackward) {
            dodgeVector = new Vec3(-forwardX * perTickSpeed, 0, -forwardZ * perTickSpeed);
        } else {
            double dodgeDirX = rightX * (isLeft ? 1 : -1);
            double dodgeDirZ = rightZ * (isLeft ? 1 : -1);
            dodgeVector = new Vec3(dodgeDirX * perTickSpeed, 0, dodgeDirZ * perTickSpeed);
        }

        beginDodge(dodgeVector, DODGE_DURATION_TICKS);
        aiDodgeCooldownTicks = AI_DODGE_COOLDOWN_TICKS;
        dodgeIFramesTicks = DODGE_IFRAMES_TICKS;

        if (doBackward) {
            animationHandler.triggerDodgeBackwardAnimation();
        } else if (isLeft) {
            animationHandler.triggerDodgeLeftAnimation();
        } else {
            animationHandler.triggerDodgeRightAnimation();
        }

        return true;
    }

    public boolean tryAIGroundDash(@Nullable LivingEntity target) {
        // Only allow dash on ground
        if (isFlying() || isTakeoff() || isLanding() || isHovering()) {
            return false;
        }
        if (dashing || isDodging()) {
            return false;
        }
        if (dashCooldownTicks > 0) {
            return false;
        }

        if (target != null) {
            double dx = target.getX() - this.getX();
            double dz = target.getZ() - this.getZ();
            if (dx * dx + dz * dz > 0.0001) {
                float targetYaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
                this.setYRot(targetYaw);
                this.yBodyRot = targetYaw;
            }
        }

        final int DASH_DURATION = 20;
        final int DASH_COOLDOWN = 40; // 2 seconds
        final double DASH_DISTANCE = 25; // blocks

        float yawRad = (float) Math.toRadians(this.getYRot());
        double forwardX = -Math.sin(yawRad);
        double forwardZ = Math.cos(yawRad);

        double dragScale = 1.0D - Math.pow(DASH_HORIZONTAL_DRAG, DASH_DURATION);
        double perTickSpeed = DASH_DISTANCE * (1.0D - DASH_HORIZONTAL_DRAG) / dragScale;

        Vec3 dashVector = new Vec3(forwardX * perTickSpeed, 0, forwardZ * perTickSpeed);

        dashing = true;
        this.entityData.set(DATA_DASHING, true);
        dashTicksLeft = DASH_DURATION;
        dashCooldownTicks = DASH_COOLDOWN;
        dashVec = dashVector;
        this.setDeltaMovement(dashVector);
        this.getNavigation().stop();
        this.hasImpulse = true;

        lastDashWasRight = !lastDashWasRight;
        this.entityData.set(DATA_LAST_DASH_RIGHT, lastDashWasRight);

        return true;
    }

    // Dash forward system
    private void tickDashState() {
        // Tick down cooldown
        if (dashCooldownTicks > 0) {
            dashCooldownTicks--;
        }

        // Tick down hit cooldowns for all entities
        dashHitCooldowns.entrySet().removeIf(entry -> {
            entry.setValue(entry.getValue() - 1);
            return entry.getValue() <= 0;
        });

        // Handle collision damage while dashing
        if (dashing && this.isVehicle()) {
            // Get mouth position as damage origin
            Vec3 mouthPos = getMouthPosition();

            // Combine hitbox + mouth area for coverage
            AABB dragonBox = this.getBoundingBox().inflate(1.5D);
            AABB mouthBox = new AABB(mouthPos, mouthPos).inflate(2.0D);
            AABB combinedBox = dragonBox.minmax(mouthBox);

            java.util.List<LivingEntity> entities = this.level().getEntitiesOfClass(
                LivingEntity.class,
                combinedBox,
                entity -> entity != this && entity != this.getControllingPassenger() && !this.isAlliedTo(entity)
            );

            // Damage and knockback each entity
            for (LivingEntity target : entities) {
                int entityId = target.getId();

                // Check if entity is on cooldown (hit recently)
                if (dashHitCooldowns.containsKey(entityId)) {
                    continue; // Skip this entity, still on cooldown
                }

                // Apply damage (10 HP)
                target.hurt(this.damageSources().mobAttack(this), 10.0F);

                // Apply knockback from mouth position
                double knockbackStrength = 1.5D;
                double dx = target.getX() - mouthPos.x;
                double dz = target.getZ() - mouthPos.z;
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > 0) {
                    target.knockback(
                        knockbackStrength,
                        -dx / dist,  // Shove away from mouth
                        -dz / dist
                    );
                }

                // Add cooldown: 5 ticks = 0.25 seconds = 4 hits per second per entity
                dashHitCooldowns.put(entityId, 5);
            }
        }
    }

    @Override
    protected void onRiderDash(Player player) {
        // Only allow dash on ground
        if (isFlying() || isTakeoff() || isLanding() || isHovering()) {
            return;
        }

        // Check cooldown
        if (dashCooldownTicks > 0) {
            return;
        }

        // Check if already dashing
        if (dashing) {
            return;
        }

        // Dash constants
        final int DASH_DURATION = 20;
        final int DASH_COOLDOWN = 40; // 2 seconds
        final double DASH_DISTANCE = 30; // blocks

        // Get forward vector (direction dragon is facing)
        float yawRad = (float) Math.toRadians(this.getYRot());
        double forwardX = -Math.sin(yawRad);
        double forwardZ = Math.cos(yawRad);

        // Account for drag so the integrated distance over the duration is ~DASH_DISTANCE
        double dragScale = 1.0D - Math.pow(DASH_HORIZONTAL_DRAG, DASH_DURATION);
        double perTickSpeed = DASH_DISTANCE * (1.0D - DASH_HORIZONTAL_DRAG) / dragScale;

        // Create dash vector (forward direction, no vertical component)
        Vec3 dashVector = new Vec3(forwardX * perTickSpeed, 0, forwardZ * perTickSpeed);

        // Begin dash
        dashing = true;
        this.entityData.set(DATA_DASHING, true);
        dashTicksLeft = DASH_DURATION;
        dashCooldownTicks = DASH_COOLDOWN;
        dashVec = dashVector;
        this.setDeltaMovement(dashVector);
        this.getNavigation().stop();
        this.hasImpulse = true;

        // Toggle which dash animation will play next (movement controller handles the actual animation)
        lastDashWasRight = !lastDashWasRight;
        this.entityData.set(DATA_LAST_DASH_RIGHT, lastDashWasRight);

        // No control lock - allow melee attacks during dash!
    }

    // Animation initialization system (fixes T-pose on world rejoin with shaders)
    public boolean isClientAnimationReady() {
        return clientAnimInitTicks >= ANIM_INIT_GRACE_PERIOD;
    }

    // ===== MAIN TICK METHOD =====
    @Override
    public void tick() {
        // === CORE TICK (every tick) ===
        super.tick();
        tickControllers(); // Physics/flight - needs every tick for smooth movement

        // === ANIMATION LOGIC (every tick for smooth visuals) ===
        tickBankingLogic();
        tickPitchingLogic();
        tickRunningTime();
        tickScreenShake(); // Both sides: client reads, server decays

        // === CLIENT-SIDE ONLY ===
        if (level().isClientSide) {
            // Increment animation initialization counter (prevents T-pose on rejoin with shaders)
            if (clientAnimInitTicks < ANIM_INIT_GRACE_PERIOD) {
                clientAnimInitTicks++;
            }
            tickSound();
            return; // Early exit for client - nothing else needed
        }

        // === SERVER-SIDE: EVERY TICK (lightweight or critical) ===
        tickSittingState();
        tickRiderTakeoff();
        tickHurtSoundCooldown();
        tickDismountRecall();
        spawnBabiesIfNeeded(); // Has internal check, only spawns once

        // Update timeFlying counter (like Cindervane)
        if (isFlying()) {
            timeFlying++;
        } else {
            timeFlying = 0;
        }

        // When ridden and flying, never stay in 'hovering' unless explicitly landing/beaming/takeoff
        if (isFlying() && getControllingPassenger() != null) {
            if (!isLanding() && !isBeaming() && !isTakeoff() && isHovering()) {
                setHovering(false);
            }
        }

        if (postStandUnlockTicks > 0) {
            postStandUnlockTicks--;
        }

        // Rider control locks must tick every server tick to match animation timings
        tickRiderControlLock();

        // === SERVER-SIDE: EVERY 2 TICKS (input/movement - slight delay acceptable) ===
        if (tickCount % 2 == 0) {
            tickRiderControlLockMovement();
            if (isFlying()) {
                tickWaterDisturbance();
            }
        }

        // === SERVER-SIDE: EVERY TICK (precise timing needed) ===
        tickFeedingCooldown();
        if (tamingAbortCalmTicks > 0) {
            tamingAbortCalmTicks--;
        }
        // Dodge system cooldowns
        if (dodgeCooldownTicks > 0) {
            dodgeCooldownTicks--;
        }
        if (aiDodgeCooldownTicks > 0) {
            aiDodgeCooldownTicks--;
        }
        if (dodgeIFramesTicks > 0) {
            dodgeIFramesTicks--;
        }

        // Dash system
        if (dashing || dashCooldownTicks > 0 || !dashHitCooldowns.isEmpty()) {
            tickDashState();
        }

        tamingController.tickServer();
        tickSleepTransition();
        tickSleepCooldowns();
        handleAmbientSounds();
        if (isFlying() || isTakeoff()) {
            tickFlightPhysics(); // Apply takeoff/landing forces
        }

        // === SERVER-SIDE: EVERY 5 TICKS (timers/cooldowns/state machines - no precision needed) ===
        if (tickCount % 5 == 0) {
            tickSuperchargeTimer();
            tickTempInvulnTimer();
            tickSuperchargeVfx();
            tickMountingState();
            tickFollowFailsafe();
        }

        if (tickCount % 100 == 0) {
            tickRecentAggroCleanup();
        }

        // === SERVER-SIDE: SLEEP WAKE-UP LOGIC ===
        // Wake up if mounted or target appears/aggression
        if (isSleeping() || isSleepingEntering() || isSleepingExiting()) {
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
        if (!(isSleeping() || isSleepingEntering() || isSleepingExiting())) {
            super.tickAnimationStates();
        }

        // === SERVER-SIDE: DODGE, DASH & BEAM TRACKING (every tick for smooth control) ===
        // Handle dodge movement first
        if (this.isDodging()) {
            handleDodgeMovement();
            return;
        }

        // Handle dash movement
        if (dashing) {
            handleDashMovement();
            // Don't return - allow beam tracking and other systems during dash
        }

        // Beam head tracking - only needed when beaming or cleaning up beam offsets
        if (isBeaming() || beamAimDir != null) {
            tickBeamLook();
        }

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

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.level().isClientSide) {
            // Clear sit progress if not sitting
            if (!this.isOrderedToSit() && this.entityData.get(DATA_SIT_PROGRESS) != 0f) {
                this.entityData.set(DATA_SIT_PROGRESS, 0f);
            }

            // Only consider SOLID ground, not water (mirrors Cindervane)
            boolean onGroundNow = this.onGround() && !this.isInWater();

            if (isFlying()) {
                this.fallDistance = 0.0F;

                // Break vegetation blocks during takeoff
                if (isTakeoff()) {
                    breakBlocksDuringTakeoff();
                }

                // Clear takeoff flag after animation completes (35 ticks)
                if (isTakeoff() && timeFlying > 35) {
                    setTakeoff(false);
                }

                // Auto-land when touching ground (like Cindervane)
                if (onGroundNow && !isTakeoff()) {
                    if (isLanding()) {
                        handleAiLandingComplete();
                    } else {
                        setLanding(false);
                    }
                    setFlying(false);
                }
            }

            // Handle landing settle (like Cindervane - 20 ticks = 1 second)
            if (isLanding()) {
                if (onGroundNow) {
                    landingTimer++;
                    if (landingTimer >= 5) {
                        markLandedNow();
                    }
                } else {
                    landingTimer = 0;
                }
            } else {
                landingTimer = 0;
            }

            // Update animation states
            tickAnimationStates();
        }

        // CRITICAL: Update NoGravity every tick (like Cindervane)
        // This allows proper landing - NoGravity is disabled when grounded
        this.setNoGravity(isFlying() || isHovering());

        // Switch to ground navigation when landed
        if (!isFlying() && usingAirNav) {
            switchToGroundNavigation();
        }
    }

    // ===== TICK SUBMETHODS =====

    private void tickScreenShake() {
        // Client side: just read the synced value from entity data
        if (level().isClientSide) {
            prevScreenShakeAmount = screenShakeAmount;
            screenShakeAmount = this.entityData.get(DATA_SCREEN_SHAKE_AMOUNT);
            return;
        }

        // Server side: decay and update entity data
        prevScreenShakeAmount = screenShakeAmount;
        if (screenShakeAmount > 0) {
            screenShakeAmount = Math.max(0, screenShakeAmount - 0.34F);
            this.entityData.set(DATA_SCREEN_SHAKE_AMOUNT, this.screenShakeAmount);
        } else if (this.entityData.get(DATA_SCREEN_SHAKE_AMOUNT) != 0.0F) {
            this.entityData.set(DATA_SCREEN_SHAKE_AMOUNT, 0.0F);
        }
    }

    /**
     * Apply flight physics forces (takeoff lift, falling resistance, etc.)
     * Mirrors Cindervane's minimal approach - just upward force during takeoff
     */
    private void tickFlightPhysics() {
        if (level().isClientSide) return;

        // Apply takeoff upward force during initial flight (first 40 ticks = 2s animation)
        // This gives the dragon height during takeoff animation
        if (isTakeoff() && isFlying() && timeFlying < 35) {
            Vec3 motion = getDeltaMovement();
            double upwardForce = 0.11D;
            setDeltaMovement(motion.add(0, upwardForce, 0));
        }

        // Reduce falling speed while flying
        if (isFlying() && getDeltaMovement().y < 0 && isAlive()) {
            setDeltaMovement(getDeltaMovement().multiply(1, 0.6, 1));
        }
    }

    // ===== WATER DISTURBANCE EFFECT (wing downforce) =====

    // Tuneable constants
    private static final double WATER_EFFECT_MAX_HEIGHT = 8.0;   // Max height above water to trigger effect
    private static final double WATER_EFFECT_INTENSITY = 0.6;    // Multiplier for particle count (smaller than Cindervane)

    /**
     * Creates water disturbance effects when flying over water.
     * Uses vanilla-style splash logic based on bounding box size.
     * Bigger dragons automatically create bigger splashes!
     */
    private void tickWaterDisturbance() {
        // Only run on server side
        if (level().isClientSide) return;

        // Only when flying
        if (!isFlying()) return;

        // Get dragon position and bounding box
        Vec3 pos = position();
        AABB box = getBoundingBox();

        // Check for water below (scan down from dragon position)
        for (int checkDown = 0; checkDown < WATER_EFFECT_MAX_HEIGHT; checkDown++) {
            BlockPos checkPos = new BlockPos(
                (int) Math.floor(pos.x),
                (int) Math.floor(pos.y) - checkDown,
                (int) Math.floor(pos.z)
            );

            BlockState state = level().getBlockState(checkPos);

            // Found water surface?
            if (!state.getFluidState().isEmpty()) {
                double waterY = checkPos.getY() + 1.0; // Top of water block

                // === VANILLA-STYLE SPLASH BASED ON BOUNDING BOX SIZE ===
                // Calculate bounding box dimensions
                double boxWidth = box.getXsize();   // Width (X axis)
                double boxLength = box.getZsize();  // Length (Z axis)

                // Calculate particle count based on entity size (vanilla formula)
                // Bigger bounding box = more particles
                int particleCount = (int) Math.ceil((boxWidth + boxLength) / 2.0 * WATER_EFFECT_INTENSITY * 8.0);
                particleCount = Math.min(particleCount, 50); // Cap to prevent lag

                // Spawn particles around the perimeter of the bounding box
                for (int i = 0; i < particleCount; i++) {
                    // Random position within bounding box horizontal area
                    double offsetX = (random.nextDouble() - 0.5) * boxWidth;
                    double offsetZ = (random.nextDouble() - 0.5) * boxLength;

                    double particleX = pos.x + offsetX;
                    double particleZ = pos.z + offsetZ;

                    // Spawn splash particles
                    ((ServerLevel) level()).sendParticles(
                        ParticleTypes.SPLASH,
                        particleX, waterY, particleZ,
                        1,
                        offsetX * 0.2, 0.1, offsetZ * 0.2,  // Velocity based on offset (spreads outward)
                        0.1
                    );

                    // Bubbles (fewer than splashes)
                    if (random.nextFloat() < 0.25f) {
                        ((ServerLevel) level()).sendParticles(
                            ParticleTypes.BUBBLE_POP,
                            particleX, waterY, particleZ,
                            1,
                            0.0, 0.0, 0.0,
                            0.0
                        );
                    }
                }

                break; // Found water, stop scanning down
            }
        }
    }

    private boolean shouldGlideNearWaterSurface() {
        if (!isFlying()) {
            return false;
        }
        // Quick altitude gate so we only scan when close to ocean level
        if (this.getY() > RIDER_WATER_SURFACE_LEVEL + RIDER_WATER_SURFACE_TOLERANCE) {
            return false;
        }

        int baseX = Mth.floor(getX());
        int baseY = Mth.floor(getY());
        int baseZ = Mth.floor(getZ());
        BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();

        for (int dx = -RIDER_WATER_SCAN_RADIUS; dx <= RIDER_WATER_SCAN_RADIUS; dx++) {
            for (int dz = -RIDER_WATER_SCAN_RADIUS; dz <= RIDER_WATER_SCAN_RADIUS; dz++) {
                for (int dy = 0; dy < WATER_EFFECT_MAX_HEIGHT; dy++) {
                    checkPos.set(baseX + dx, baseY - dy, baseZ + dz);
                    if (!level().hasChunkAt(checkPos)) {
                        continue;
                    }
                    BlockState state = level().getBlockState(checkPos);
                    if (!state.getFluidState().isEmpty()) {
                        double surfaceY = checkPos.getY() + 1.0;
                        if (Math.abs(this.getY() - surfaceY) <= RIDER_WATER_SURFACE_TOLERANCE) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private void tickHurtSoundCooldown() {
        // Cool down hurt sound throttle
        if (hurtSoundCooldown > 0) hurtSoundCooldown--;
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

    private void tickDismountRecall() {
        if (this.isVehicle()) {
            clearForcedOwnerFollow();
            return;
        }
        if (dismountRecallTicks > 0) {
            dismountRecallTicks--;
        }
    }

    private void triggerForcedOwnerFollow() {
        if (this.level().isClientSide) {
            return;
        }
        this.setOrderedToSit(false);
        this.setCommandAuto(0);
        this.setHovering(false);
        this.setLanding(false);
        this.getNavigation().stop();
        this.dismountRecallTicks = DISMOUNT_RECALL_WINDOW;
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
            clearForcedOwnerFollow();
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
    // When NOT beaming, allow vanilla head tracking to work normally.
    private void tickBeamLook() {
        if (!isBeaming()) {
            resetBeamAim();
            // Don't return early - let vanilla head tracking work!
            // Minecraft will automatically sync yHeadRot based on rider's look direction
            return;
        }

        Vec3 start = getBeamStartAnchor(1.0f);
        if (start == null) {
            resetBeamAim();
            return;
        }

        boolean riderControlled = getControllingPassenger() != null;
        Vec3 aimDir = refreshBeamAimDirection(start, riderControlled);
        if (aimDir == null) {
            beamAimDir = Vec3.directionFromRotation(this.getXRot(), this.yHeadRot).normalize();
            aimDir = beamAimDir;
            updateBeamOffsets(aimDir);
        }

        // CRITICAL: Only apply beam look when NOT rider-controlled
        // When riding, the rider controller sets dragon rotation - applyBeamLook would fight it
        if (!riderControlled) {
            applyBeamLook(aimDir);
        }
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
        // Rider always has perfect control
        Entity cp = getControllingPassenger();
        if (cp instanceof LivingEntity rider) {
            Vec3 riderLook = rider.getLookAngle();
            if (riderLook.lengthSqr() > 1.0E-6) {
                return riderLook.normalize();
            }
        }

        // AI targeting with smart tracking
        if (!level().isClientSide) {
            tickBeamTargeting(start);
        }

        if (beamServerTarget != null) {
            Vec3 towardTarget = beamServerTarget.subtract(start);
            if (towardTarget.lengthSqr() > 1.0E-6) {
                return towardTarget.normalize();
            }
        }

        // Fallback to looking direction
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

        // Calculate errors - keep it simple and symmetric for both yaw and pitch
        float yawErrDeg = net.minecraft.util.Mth.degreesDifference(headYaw, desiredYawDeg);
        float pitchErrDeg = desiredPitchDeg - headPitch;  // Simple subtraction, same as yaw uses degreesDifference

        // Clamp the ERRORS, not the angles
        float clampedYawErr = net.minecraft.util.Mth.clamp(yawErrDeg, -MAX_BEAM_YAW_DEG, MAX_BEAM_YAW_DEG);
        float clampedPitchErr = net.minecraft.util.Mth.clamp(pitchErrDeg, -MAX_BEAM_PITCH_DEG, MAX_BEAM_PITCH_DEG);

        // Apply clamped errors to current angles
        float finalYaw = headYaw + clampedYawErr;
        float finalPitch = headPitch + clampedPitchErr;

        Vec3 finalDir = Vec3.directionFromRotation(finalPitch, finalYaw);
        return finalDir.lengthSqr() > 1.0E-6 ? finalDir.normalize() : null;
    }

    /**
     * Creates initial beam target position with wild random offset.
     * Makes the beam start very inaccurate.
     */
    private Vec3 createInitialBeamTarget() {
        LivingEntity target = getTarget();
        Vec3 shootFrom = getBeamStartAnchor(1.0f);
        if (shootFrom == null) {
            shootFrom = position().add(0, getBbHeight() * 0.5, 0);
        }

        if (target != null && target.isAlive()) {
            // Start with a moderate random offset around target
            Vec3 randomOffset = new Vec3(
                -12 + random.nextFloat() * 24F,
                -6 + random.nextFloat() * 12F,
                -12 + random.nextFloat() * 24F
            );
            return target.position().add(randomOffset);
        } else {
            // No target - aim forward with random spread
            Vec3 forward = new Vec3(0, random.nextBoolean() ? 50 : 10, 30)
                .yRot((float) Math.toRadians(-this.yBodyRot));
            return shootFrom.add(forward);
        }
    }

    /**
     * Updates beam targeting with accuracy ramping and dynamic wobble.
     * Called each tick while beaming to smoothly track targets.
     */
    private void tickBeamTargeting(Vec3 shootFrom) {
        beamTime++;

        LivingEntity target = getTarget();
        Vec3 currentTarget = beamServerTarget != null ? beamServerTarget : shootFrom;

        if (target != null && target.isAlive()) {
            // Calculate accuracy: starts inaccurate, converges quickly
            float maxBeamTime = 30.0F;
            float time = Math.min(beamTime, (int) maxBeamTime) / maxBeamTime;
            float accuracy = 1.0F - time;

            // Create dynamic wobble pattern that scales with inaccuracy
            Vec3 wobbleOffset = new Vec3(
                Math.sin(tickCount * 0.2F) * 2.5,
                Math.sin(tickCount * 0.15F) * 1.5,
                Math.cos(tickCount * 0.2F) * -2.5
            ).yRot((float) Math.toRadians(-this.yBodyRot)).scale(accuracy);

            // Aim point with wobble
            Vec3 targetPoint = target.getEyePosition().add(0, -0.25, 0).add(wobbleOffset);

            // Smooth approach: move 20% toward desired position each tick
            beamServerTarget = targetPoint.subtract(currentTarget).scale(0.2F).add(currentTarget);
        } else {
            // No target - slowly sweep the beam forward
            Vec3 sweepOffset = new Vec3(
                Math.sin(tickCount * 0.1F) * 10,
                0,
                6
            ).yRot((float) Math.toRadians(-this.yBodyRot));
            Vec3 sweepTarget = shootFrom.add(sweepOffset);
            beamServerTarget = sweepTarget.subtract(currentTarget).scale(0.1F).add(currentTarget);
        }
    }

    private void applyBeamLook(@Nullable Vec3 aimDir) {
        if (aimDir == null) {
            return;
        }
        float desiredYaw = (float)(Math.atan2(-aimDir.x, aimDir.z) * (180.0 / Math.PI));
        float desiredPitch = (float)(-Math.atan2(aimDir.y, Math.sqrt(aimDir.x * aimDir.x + aimDir.z * aimDir.z)) * (180.0 / Math.PI));

        float headYawSpeed = 15.0F;
        float headPitchSpeed = 12.0F;

        this.yHeadRot = Mth.approachDegrees(this.yHeadRot, desiredYaw, headYawSpeed);

        float currentPitch = this.getXRot();
        float pitchDelta = desiredPitch - currentPitch;
        float pitchChange = Mth.clamp(pitchDelta, -headPitchSpeed, headPitchSpeed);
        this.setXRot(currentPitch + pitchChange);

        float yawDiff = Mth.degreesDifferenceAbs(desiredYaw, Mth.wrapDegrees(this.yBodyRot));
        if (yawDiff > MAX_BEAM_YAW_DEG * 0.6F) {
            float bodyRotSpeed = 8.0F;
            this.setYRot(Mth.approachDegrees(this.getYRot(), desiredYaw, bodyRotSpeed));
            this.yBodyRot = Mth.approachDegrees(this.yBodyRot, desiredYaw, bodyRotSpeed);
        }
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

            // Only apply boost if NOT being ridden (rider controller handles takeoff boost instead)
            if (getControllingPassenger() == null) {
                // Apply upward boost during takeoff window
                Vec3 velocity = this.getDeltaMovement();
                double boost = this.isFlying() ? 0.08D : 0.12D;
                if (velocity.y < boost) {
                    this.setDeltaMovement(velocity.x, boost, velocity.z);
                }
                this.hasImpulse = true;
            }
        }
    }
    
    private void tickControllers() {
        // FlightController disabled - now using vanilla travel like Cindervane/Ignivorus
        // This fixes the reload drift bug (no longer need postLoadAirStabilizeTicks workaround)
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
            // Trigger sit down animation when:
            // 1. Starting from standing (sitProgress == 0), OR
            // 2. Interrupting a stand-up animation (isStandingUp = true)
            if ((sitProgress == 0f || isStandingUp) && !isSittingDown) {
                animationHandler.triggerSitDownAnimation();
                isSittingDown = true;
                isStandingUp = false; // Cancel the stand-up
                sitTransitionTicks = getSitDownAnimationTicks();
            }

            if (sitProgress < maxSitTicks()) {
                sitProgress++;
                this.entityData.set(DATA_SIT_PROGRESS, sitProgress);
            }
        } else {
            if (!this.level().isClientSide && super.isInSittingPose()) {
                this.setInSittingPose(false);
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
                // Trigger sit up animation when:
                // 1. At max sitting and ready to stand, OR
                // 2. Interrupting a sit-down animation (isSittingDown = true)
                if ((sitProgress == maxSitTicks() || isSittingDown) && !isStandingUp) {
                    animationHandler.triggerSitUpAnimation();
                    isStandingUp = true;
                    isSittingDown = false; // Cancel the sit-down
                    sitTransitionTicks = getSitUpAnimationTicks();
                }

                // Decrement sitProgress when standing up
                // Speed matches stand-up animation duration (20 ticks) not sit-down (30 ticks)
                float decrementRate = maxSitTicks() / (float) getSitUpAnimationTicks(); // 30/20 = 1.5
                sitProgress -= decrementRate;
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
            clearRiderControlLock();
            return;
        }

        this.getNavigation().stop();
        this.setTarget(null);
        this.setDeltaMovement(0, 0, 0);
    }
    
    private void tickFollowFailsafe() {
        // This is called every 5 ticks, so decrement by 5
        if (followFailsafeCooldown > 0) {
            followFailsafeCooldown -= 5;
            return;
        }
        followFailsafeCooldown = 20; // roughly once per second (20 ticks)

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
            return goal instanceof RaevyxFollowOwnerGoal || goal instanceof RaevyxGroundCombatGoal || goal instanceof RaevyxAirCombatGoal;
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
        // This is called every 5 ticks, so decrement by 5 to match real-time duration
        if (superchargeTicks > 0) {
            superchargeTicks -= 5;
            if (superchargeTicks <= 0) {
                superchargeTicks = 0;
                // When supercharge ends, restore normal max health
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
        // This is called every 5 ticks, so decrement by 5 to match real-time duration
        if (tempInvulnTicks > 0) {
            tempInvulnTicks -= 5;
            if (tempInvulnTicks <= 0) {
                tempInvulnTicks = 0;
                if (!isDying()) this.setInvulnerable(false);
            }
        }
    }
    
    private void tickSuperchargeVfx() {
        // Supercharge VFX: periodic arcs/sparks around the body
        if (isSupercharged() && this.level().isThundering() && superchargeVfxCooldown-- <= 0) {
            spawnSuperchargeVfx();
            superchargeVfxCooldown = 6 + this.random.nextInt(6); // pulse every ~0.3-0.6s
        }
    }
    
    private void tickSleepTransition() {
        // Handle sleep enter transition: down -> sit -> fall_asleep -> sleep loop
        if (isSleepingEntering() && !level().isClientSide) {
            if (!sleepFallAsleepTriggered) {
                // Wait until sit_down completes before starting fall_asleep
                if (getSitProgress() >= maxSitTicks()) {
                    sleepFallAsleepTriggered = true;
                    sleepTransitionTicks = getFallAsleepAnimationTicks();
                    animationHandler.triggerFallAsleepAnimation();
                } else {
                    // Hold timer steady while sitting down
                    sleepTransitionTicks = getFallAsleepAnimationTicks();
                    return;
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
                    // wake_up finished: now play sit_up, then release
                    if (!sleepSitUpTriggered) {
                        // If owner commanded sit, stop at sit after wake_up (no stand-up)
                        if (shouldStaySeatedCommand()) {
                            setSleeping(false);
                            sleepSitUpTriggered = false;
                            setSleepingExiting(false);
                            sleepTransitionTicks = 0;
                            sleepAmbientCooldownTicks = 10;
                            setOrderedToSit(true);
                            this.entityData.set(DATA_SIT_PROGRESS, maxSitTicks());
                            if (!level().isClientSide) {
                                releaseSleepLock();
                            }
                            return;
                        }

                        sleepSitUpTriggered = true;
                        sleepTransitionTicks = getSleepSitUpDuration();
                        animationHandler.triggerSitUpAnimation();
                        // Allow stand-up by clearing sit lock
                        setOrderedToSit(false);
                    } else {
                        // sit_up finished
                        setSleeping(false);
                        sleepSitUpTriggered = false;
                        setSleepingExiting(false);
                        sleepAmbientCooldownTicks = 10;
                        if (!level().isClientSide) {
                            releaseSleepLock();
                        }
                    }
                }
            }
        }
    }
    
    private void tickSleepCooldowns() {
        if (sleepAmbientCooldownTicks > 0) sleepAmbientCooldownTicks--;
        if (sleepReentryCooldownTicks > 0) sleepReentryCooldownTicks--;
        if (sleepCancelTicks > 0) sleepCancelTicks--;
    }

    private void tickFeedingCooldown() {
        if (level().isClientSide) {
            return;
        }
        int cooldownTicks = this.entityData.get(DATA_FEEDING_COOLDOWN);
        if (cooldownTicks > 0) {
            cooldownTicks--;
            this.entityData.set(DATA_FEEDING_COOLDOWN, cooldownTicks);
        }
    }

    private void tickBankingLogic() {
        prevBankAngle = bankAngle;

        // Reset banking when in water, not flying, or when controls are locked - instant snap back
        boolean inWater = this.isInWater() || this.isInWaterOrBubble();
        if (inWater || areRiderControlsLocked() || !isFlying() || isOrderedToSit()) {
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
        bankSmoothedYaw = bankSmoothedYaw * 0.65f + yawChange * 0.35f; // More reactive (was 0.75/0.25)

        // Convert smoothed yaw delta into a banking roll. Multiplying gives us headroom for aggressive turns.
        float targetAngle = Mth.clamp(bankSmoothedYaw * 6.5f, -90f, 90f); // More dramatic (was 5.0)
        // Ease toward the new target so long sweeping turns feel weighty but responsive.
        bankAngle = Mth.lerp(0.40f, bankAngle, targetAngle); // Snappier (was 0.28)
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
            // If we were actively landing and now touched ground, trigger landed animation
            boolean wasLanding = riderLandingBlendTicks > 0 && isRiderLandingBlendActive();
            riderLandingBlendTicks = 0;
            if (!level().isClientSide) {
                this.entityData.set(DATA_RIDER_LANDING_BLEND, false);

                // Trigger landed animation when rider landing completes
                if (wasLanding && onGround() && isVehicle()) {
                    // Properly clear flight state to prevent T-pose gliding bug
                    setFlying(false);
                    setTakeoff(false);
                    timeFlying = 0;
                    triggerAnim("action", "landed");  // Trigger as one-shot animation
                    lockRiderControls(30);  // Lock controls for 1.50 seconds while animation plays
                }
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

        // Use MOTION_BLOCKING (includes water surface) to find the actual surface
        int surfaceY = this.level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                pos.getX(), pos.getZ());

        // Use MOTION_BLOCKING_NO_LEAVES to find solid ground below water
        int groundY = this.level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                pos.getX(), pos.getZ());

        // Scan a column from slightly above dragon down to BELOW the heightmap (heightmap returns +1 above actual blocks)
        int dragonY = (int) Math.floor(this.getY());
        int scanTopY = dragonY + 3; // Check a bit above dragon (in case partially submerged)
        int scanBottomY = Math.min(groundY - 1, dragonY - 15);  // Scan below heightmap to catch water

        // Scan downward from above dragon to ground, checking for water at any height
        for (int y = scanTopY; y >= scanBottomY; y--) {
            net.minecraft.core.BlockPos checkPos = new net.minecraft.core.BlockPos(pos.getX(), y, pos.getZ());
            net.minecraft.world.level.block.state.BlockState checkState = level().getBlockState(checkPos);

            if (!checkState.getFluidState().isEmpty()) {
                // Water/lava detected in column - don't trigger landing
                return Double.POSITIVE_INFINITY;
            }
        }

        return this.getY() - groundY;
    }
    
    private void tickPitchingLogic() {
        tickRiderLandingBlendTimer();
        // Reset pitching when in water, not flying, or when controls are locked - INSTANT reset
        boolean inWater = this.isInWater() || this.isInWaterOrBubble();
        if (inWater || areRiderControlsLocked() || !isFlying() || isOrderedToSit()) {
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
                if (altitude != Double.POSITIVE_INFINITY && altitude >= -0.25D && altitude <= LANDING_BLEND_ALTITUDE) {
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
        return isBaby() ? RaevyxAbilities.BABY_HURT : RaevyxAbilities.HURT;
    }

    @Override
    protected DragonAbilityType<?, ?> getDeathAbilityType() {
        return isBaby() ? RaevyxAbilities.BABY_DIE : RaevyxAbilities.DIE;
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
        if (isBaby() || isDying() || isSleeping() || isSleepTransitioning() || isInSitTransition() || sleepAmbientCooldownTicks > 0 || areRiderControlsLocked()) return;
        ambientSoundTimer++;

        // Time to make some noise?
        if (ambientSoundTimer >= nextAmbientSoundDelay) {
            playCustomAmbientSound(); // Renamed to avoid conflict with Mob.playAmbientSound()
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
        // Apply the dodge velocity directly
        this.setDeltaMovement(dodgeVec);
        this.hasImpulse = true;

        // Decay for next tick
        dodgeVec = dodgeVec.multiply(DODGE_HORIZONTAL_DRAG, DODGE_VERTICAL_DRAG, DODGE_HORIZONTAL_DRAG);

        if (--dodgeTicksLeft <= 0) {
            dodging = false;
            dodgeVec = Vec3.ZERO;
        }
    }

    private void handleDashMovement() {
        // Apply the dash velocity directly
        this.setDeltaMovement(dashVec);
        this.hasImpulse = true;

        // Decay for next tick
        dashVec = dashVec.multiply(DASH_HORIZONTAL_DRAG, DASH_VERTICAL_DRAG, DASH_HORIZONTAL_DRAG);

        if (--dashTicksLeft <= 0) {
            dashing = false;
            dashVec = Vec3.ZERO;
            this.entityData.set(DATA_DASHING, false);
            dashHitCooldowns.clear();
        }
    }

    // ===== TRAVEL METHOD =====
    @Override
    public void travel(@NotNull Vec3 motion) {
        // During a dodge, preserve the stored dodge velocity and let vanilla travel apply it without rider overrides.
        if (this.isDodging()) {
            super.travel(Vec3.ZERO);
            return;
        }

        // During a dash, preserve the stored dash velocity and let vanilla travel apply it without rider overrides.
        if (dashing) {
            super.travel(Vec3.ZERO);
            return;
        }

        // Handle sitting/dying states
        boolean sittingLocked = (this.isOrderedToSit() || this.isInSittingPose()) && postStandUnlockTicks <= 0;
        if (sittingLocked || this.isDying() || this.isSleeping() || this.isSleepTransitioning()) {
            if (this.getNavigation().getPath() != null) {
                this.getNavigation().stop();
            }
            motion = Vec3.ZERO;
            super.travel(motion);
            return;
        }

        // Check if in water (prioritize water handling over flight)
        boolean inWater = this.isInWater() || this.isInWaterOrBubble();

        // WATER OVERRIDE: If in water, clear ALL states that might block movement
        if (inWater) {
            if (!level().isClientSide) {
                // Exit flight mode if flying
                if (isFlying()) {
                    this.setFlying(false);
                    this.setTakeoff(false);
                    this.setHovering(false);
                    this.setLanding(false);
                    this.switchToGroundNavigation();
                }
            }
        }

        // Riding logic
        if (this.isVehicle() && this.getControllingPassenger() instanceof Player player) {
            // Clear any AI navigation when being ridden
            if (this.getNavigation().getPath() != null) {
                this.getNavigation().stop();
            }

            // Let tickRidden handle rotation smoothly
            // No instant rotation here - all handled in tickRidden for responsiveness

            if (inWater) {
                // WATER ALWAYS TAKES PRIORITY - swimming handler
                handleWaterSwimming(motion);
            } else if (isFlying()) {
                // Delegate flying movement to rider controller for consistency
                this.riderController.handleRiderMovement(player, motion);
            } else {
                // Ground movement - use vanilla system which calls getRiddenInput()
                this.setSpeed(this.riderController.getRiddenSpeed(player));
                super.travel(motion);
            }
        } else {
            // Normal AI movement - use vanilla travel for everything like Cindervane/Ignivorus
            super.travel(motion);
        }

    }

    /**
     * Handles swimming movement for Raevyx when in water.
     * Raevyx is not aquatic, so it swims slowly and gradually sinks.
     * Player must hold spacebar to swim upward.
     * Can breach the surface to take off if moving upward with enough speed.
     */
    private void handleWaterSwimming(Vec3 input) {
        Vec3 velocity = this.getDeltaMovement();

        // Raevyx swims much slower than Nulljaw (not aquatic)
        double swimSpeed = 0.4D; // Slow base speed
        if (isAccelerating()) {
            swimSpeed *= 1.3D; // Slight boost when sprinting
        }

        // Calculate desired movement based on input
        Vec3 desired = getSwimVec3(input, swimSpeed, velocity);

        // Blend toward desired velocity (less responsive than flight)
        Vec3 blended = velocity.add(desired.subtract(velocity).scale(0.15D));

        // Apply drag
        double dragFactor = 0.88D; // More drag than in air
        blended = blended.multiply(dragFactor, 0.92D, dragFactor);

        // Vertical movement: gradual sinking unless player is actively swimming up
        double dy = blended.y;
        if (isGoingUp()) {
            // Swimming up - slow ascent
            dy = Math.min(swimSpeed * 0.6D, dy + 0.08D);
        } else if (isGoingDown()) {
            // Swimming down - faster descent
            dy = Math.max(-swimSpeed * 0.8D, dy - 0.12D);
        } else {
            // Not holding up/down: gradual sinking (Raevyx is not buoyant)
            dy -= 0.03D; // Sink slowly
        }

        blended = new Vec3(blended.x, dy, blended.z);

        this.setDeltaMovement(blended);
        this.move(MoverType.SELF, this.getDeltaMovement());

        // AFTER movement, check for breach attempt
        // If at surface with good upward velocity, allow takeoff
        boolean atSurface = !this.isUnderWater(); // Head above water
        boolean tryingToAscend = isGoingUp();
        double currentVelY = this.getDeltaMovement().y;

        if (atSurface && tryingToAscend && currentVelY > 0.0D && !this.isFlying()) {
            // Breach the surface and start flying!
            if (!level().isClientSide) {
                this.setFlying(true);
                this.setTakeoff(true);
                this.setHovering(false);
                this.setLanding(false);
                this.switchToAirNavigation();
                // Give upward boost for breach
                this.setDeltaMovement(this.getDeltaMovement().add(0, 0.4D, 0));
                // Trigger takeoff (FlightController removed)
                this.riderTakeoffTicks = 35;
            }
        }
    }

    /**
     * Convert rider input into world-space swimming movement vector
     */
    private Vec3 getSwimVec3(Vec3 wishDir, double swimSpeed, Vec3 velocity) {
        double strafe = wishDir.x;
        double forward = wishDir.z;
        float yawRad = this.getYRot() * ((float) Math.PI / 180F);
        double sin = Math.sin(yawRad);
        double cos = Math.cos(yawRad);

        double worldX = strafe * cos - forward * sin;
        double worldZ = forward * cos + strafe * sin;

        double dx = worldX * 0.6D * swimSpeed;
        double dz = worldZ * 0.6D * swimSpeed;

        // Y is handled in handleWaterSwimming
        return new Vec3(dx, 0, dz);
    }


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

    /**
     * Family group spawning: When a wild Raevyx spawns naturally, it has a 60% chance
     * to spawn with 2-3 baby hatchlings, creating a family group.
     */
    private boolean shouldSpawnBabies = false;
    private int babiesToSpawn = 0;

    public @NotNull SpawnGroupData finalizeSpawn(
            @Nonnull net.minecraft.world.level.ServerLevelAccessor level,
            @Nonnull net.minecraft.world.DifficultyInstance difficulty,
            @Nonnull MobSpawnType spawnReason,
            @Nullable SpawnGroupData spawnData,
            @Nullable CompoundTag dataTag
    ) {
        spawnData = super.finalizeSpawn(level, difficulty, spawnReason, spawnData, dataTag);

        // Only spawn families during chunk generation
        // Use a custom SpawnGroupData to track if we've already spawned babies
        if (spawnReason == MobSpawnType.CHUNK_GENERATION) {
            // Check if this is the parent (not a baby we're spawning)
            if (!(spawnData instanceof RaevyxFamilyData)) {
                // 60% chance to spawn with babies
                if (this.random.nextFloat() < 0.6F) {
                    // Mark this as a family spawn (false = don't spawn baby via vanilla logic)
                    spawnData = new RaevyxFamilyData(false);
                    
                    // Schedule baby spawning for first tick (when parent is positioned)
                    this.shouldSpawnBabies = true;
                    this.babiesToSpawn = 2 + this.random.nextInt(2); // 2 or 3 babies
                }
            }
        }

        applyConfiguredAttributes();
        this.setHealth(this.getMaxHealth());
        return spawnData;
    }

    private void applyConfiguredAttributes() {
        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance().getConfig(DragonAttributeConfigLoader.RAEVYX_ID);
        setAttributeBase(Attributes.MAX_HEALTH, config.maxHealth());
        setAttributeBase(Attributes.FLYING_SPEED, config.flyingSpeed());
        setAttributeBase(Attributes.ARMOR, config.armor());
        // MOVEMENT_SPEED is hardcoded in createAttributes() - no config needed

        double maxHealth = config.maxHealth();
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
    
    private void spawnBabiesIfNeeded() {
        if (!shouldSpawnBabies || babiesToSpawn <= 0) {
            return;
        }
        
        shouldSpawnBabies = false;
        
        for (int i = 0; i < babiesToSpawn; i++) {
            Raevyx baby = ModEntities.RAEVYX.get().create(level());
            if (baby != null) {
                // Set baby properties BEFORE adding to world to ensure they persist
                baby.setGender(this.random.nextBoolean() ? DragonGender.FEMALE : DragonGender.MALE);

                // Set skip flag BEFORE setAge to prevent respawn logic
                baby.skipRespawnTicks = 5;
                baby.setBaby(true);
                baby.setAge(-24000); // Standard baby age

                // Position baby VERY close to parent (parent is now positioned!)
                double angle = (Math.PI * 2.0 * i) / babiesToSpawn;
                double distance = 1.0 + this.random.nextDouble() * 0.5; // 1-1.5 blocks away (very close)
                double offsetX = Math.cos(angle) * distance;
                double offsetZ = Math.sin(angle) * distance;

                baby.moveTo(
                        this.getX() + offsetX,
                        this.getY(),
                        this.getZ() + offsetZ,
                        this.random.nextFloat() * 360.0F,
                        0.0F
                );

                // Add to world
                level().addFreshEntity(baby);
            }
        }
        
        babiesToSpawn = 0;
    }

    /**
     * Custom SpawnGroupData to track family spawning and prevent recursive baby spawning.
     * Extends AgeableMobGroupData to satisfy parent class requirements.
     */
    private static class RaevyxFamilyData extends AgeableMob.AgeableMobGroupData {
        public RaevyxFamilyData(boolean shouldSpawnBaby) {
            super(shouldSpawnBaby);
        }
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

    // ===== AI GOALS =====
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new com.leon.saintsdragons.server.ai.goals.base.DragonFloatGoal(this));
        this.goalSelector.addGoal(1, new com.leon.saintsdragons.server.ai.goals.base.DragonWaterEscapeGoal((com.leon.saintsdragons.server.entity.interfaces.DragonFlightCapable)this));
        // Sleep is now handled by DragonSleepBehavior in base class tick
        this.goalSelector.addGoal(3, new RaevyxAirCombatGoal(this));
        this.goalSelector.addGoal(3, new RaevyxGroundCombatGoal(this));
        this.goalSelector.addGoal(5, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(7, new RaevyxFollowParentGoal(this, 1.15D));
        this.goalSelector.addGoal(7, new RaevyxBreedGoal(this, 1.0D));
        this.goalSelector.addGoal(8, new RaevyxFollowOwnerGoal(this));   // Lower priority than combat
        this.goalSelector.addGoal(9, new RaevyxGroundWanderGoal(this, 1.0, 60)); // Lower priority than combat
        this.goalSelector.addGoal(10, new RaevyxTemptGoal(this, 1.2,
                net.minecraft.world.item.crafting.Ingredient.of(net.minecraft.world.item.Items.SALMON, 
                                                               net.minecraft.world.item.Items.COD,
                                                               net.minecraft.world.item.Items.PUFFERFISH), false));
        
        this.goalSelector.addGoal(11, new RaevyxFlightGoal(this));
        // Look goals that skip when being ridden (so rider has full control)
        this.goalSelector.addGoal(12, new RandomLookAroundGoal(this) {
            @Override
            public boolean canUse() {
                return !Raevyx.this.isVehicle() && super.canUse();
            }
        });
        this.goalSelector.addGoal(12, new LookAtPlayerGoal(this, Player.class, 8.0F) {
            @Override
            public boolean canUse() {
                return !Raevyx.this.isVehicle() && super.canUse();
            }
        });

        // Target selection - use custom goals that respect ally system
        this.targetSelector.addGoal(1, new com.leon.saintsdragons.server.ai.goals.base.DragonOwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new com.leon.saintsdragons.server.ai.goals.base.DragonOwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new RaevyxProtectBabiesGoal(this));  // Protect nearby babies
        this.targetSelector.addGoal(4, new HurtByTargetGoal(this));
        // Neutral behavior: do not proactively target players. Only retaliate when hurt or defend owner.
    }

    /**
     * Returns a larger bounding box for frustum culling to prevent the model from
     * disappearing when the entity's collision box is off-screen but the visual model
     * (wings, tail, etc.) should still be visible.
     */
    @Override
    public AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling().inflate(5.0, 3.0, 5.0);
    }

    @Override
    public boolean hurt(@Nonnull DamageSource damageSource, float amount) {
        // During dying sequence, ignore all damage (entity is already dead, playing death animation)
        if (isDying()) {
            return false;
        }
        // Invulnerability during dodge (i-frames)
        if (dodgeIFramesTicks > 0) {
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

        // Handle damage during taming stun - dragon is completely vulnerable
        // Player can choose to kill it or leave it to recover after timeout
        if (isTamingStunned() && !isTame()) {
            // Allow damage normally - stunned dragon can be killed
            return super.hurt(damageSource, amount);
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

    // Lightning immunity is now handled by DragonEntity base class via DragonType.LIGHTNING elemental profile

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
        if (this.isOnFire()) this.clearFire();
    }
    // ===== ANIMATION HELPER METHODS =====
    public float getBankAngleDegrees(float partialTick) {
        return Mth.lerp(partialTick, prevBankAngle, bankAngle);
    }
    public int getPitchDirection() {
        return pitchDir;
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
            if (this.random.nextFloat() < 0.7f) {
                server.sendParticles(new RaevyxLightningStormData(size, female),
                        pos.x, pos.y, pos.z, 1, dir.x, dir.y, dir.z, 0.0);
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

    // ===== ANIMATION TIMING HELPERS =====
    private int getSitDownAnimationTicks() {
        return 30; // 1.5s for both baby and adult (unified)
    }
    private int getSitUpAnimationTicks() {
        return 20; // 1.0s - matches actual animation length
    }
    private int getFallAsleepAnimationTicks() {
        return 50; // 2.5s for both baby and adult (unified)
    }
    private int getWakeUpAnimationTicks() {
        return 53; // 2.625s for both baby and adult (unified, ~2.65s)
    }

    public int getSleepSitUpDuration() {
        return getSitUpAnimationTicks();
    }

    @Override
    public float maxSitTicks() {
        return 30.0F; // Matches sit_down animation (1.5s = 30 ticks)
    }

    // ===== SLEEPING =====
    @Override
    public DragonSleepBehavior.DragonSleepPreferences getSleepPreferences() {
        // Raevyx are daylight sleepers (avoid thunderstorms)
        return DragonSleepBehavior.DragonSleepPreferences.DIURNAL();
    }

    @Override
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
        sleepFallAsleepTriggered = false;
        sleepSitUpTriggered = false;
        // New system: sit_down (uses sitProgress) → fall_asleep → sleep loop
        boolean alreadySitting = isOrderedToSit() || getSitProgress() >= maxSitTicks() || shouldStaySeatedCommand();
        if (alreadySitting) {
            sleepTransitionTicks = getFallAsleepAnimationTicks();
            sleepFallAsleepTriggered = true;
            animationHandler.triggerFallAsleepAnimation();
            this.setOrderedToSit(true);
            if (!level().isClientSide) {
                enterSleepLock();
            }
        } else {
            sleepTransitionTicks = getFallAsleepAnimationTicks();
            // Trigger sit_down animation first (handled by sit progress system)
            animationHandler.triggerSitDownAnimation();
            if (!level().isClientSide) {
                enterSleepLock();
            }
        }
    }

    public void startSleepExit() {
        if ((!isSleeping() && !isSleepingEntering()) || isSleepingExiting()) return;
        // Keep sleeping flag true through wake_up so the controller stays in sleep pose until the animation plays
        setSleepingEntering(false);
        setSleepingExiting(true);
        sleepFallAsleepTriggered = false;
        sleepSitUpTriggered = false;
        // New system: wake_up → sit (brief) → sit_up
        sleepTransitionTicks = getWakeUpAnimationTicks();
        // Trigger wake_up animation
        animationHandler.triggerWakeUpAnimation();
        setOrderedToSit(true); // hold sit while wake_up plays
        if (!level().isClientSide) {
            suppressSleep(20);
            // releaseSleepLock is deferred until sit_up completes to avoid early stand-up
        }
    }

    public void wakeUpImmediately() {
        this.entityData.set(DATA_SLEEPING, false);
        setSleepingEntering(false);
        setSleepingExiting(false);
        sleepTransitionTicks = 0;
        sleepFallAsleepTriggered = false;
        sleepSitUpTriggered = false;
        sleepCancelTicks = 2;
        // Clear sitting state so dragon can move/fight immediately
        setOrderedToSit(false);
        if (!level().isClientSide) {
            suppressSleep(20);
            releaseSleepLock();
        }
        this.entityData.set(DATA_SLEEPING, false);
    }

    public void suppressSleep(int ticks) {
        sleepReentryCooldownTicks = Math.max(sleepReentryCooldownTicks, ticks);
    }
    public boolean isSleepSuppressed() {
        return sleepReentryCooldownTicks > 0 || isTamingStunned();
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
        boolean wasSitting = isOrderedToSit();
        super.setOrderedToSit(sitting);

        if (sitting) {
            // Force landing if flying when ordered to sit
            if (isFlying()) {
                this.setLanding(true);
            }
            this.setRunning(false);
            this.getNavigation().stop();
        } else if (wasSitting) {
            if (!level().isClientSide) {
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
        return stack.is(Items.SALMON) ||
                stack.is(Items.COD) ||
                stack.is(com.leon.saintsdragons.common.registry.ModItems.HEARTY_DRAGON_MEAL.get());
    }


    // ===== SAVE/LOAD =====
    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        saveRideableData(tag);
        tag.putInt("TimeFlying", timeFlying);
        tag.putBoolean("UsingAirNav", usingAirNav);
        tag.putInt("RiderTakeoffTicks", riderTakeoffTicks);
        tag.putLong("LastLandingGameTime", lastLandingGameTime);
        tag.putBoolean("LandingFlag", landingFlag);
        tag.putInt("LandingTimer", landingTimer);
        tag.putBoolean("LandedFlag", landedFlag);
        tag.putInt("LandedTimer", landedTimer);
        this.combatManager.saveToNBT(tag);
        tag.putInt("SuperchargeTicks", Math.max(0, this.superchargeTicks));
        tag.putInt("TempInvulnTicks", Math.max(0, this.tempInvulnTicks));
        tag.putBoolean("AllowGroundBeamStorm", this.allowGroundBeamDuringStorm);
        tag.putBoolean("ManualSitCommand", this.manualSitCommand);
        tag.putInt("FeedingCooldownTicks", Math.max(0, this.entityData.get(DATA_FEEDING_COOLDOWN)));
        tamingController.save(tag);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        loadRideableData(tag);
        boolean savedFlying = tag.getBoolean("Flying");
        this.timeFlying = tag.getInt("TimeFlying");
        this.usingAirNav = tag.getBoolean("UsingAirNav");
        this.riderTakeoffTicks = tag.contains("RiderTakeoffTicks") ? tag.getInt("RiderTakeoffTicks") : 0;
        this.lastLandingGameTime = tag.contains("LastLandingGameTime") ? tag.getLong("LastLandingGameTime") : Long.MIN_VALUE;
        this.landingFlag = tag.contains("LandingFlag") && tag.getBoolean("LandingFlag");
        this.landingTimer = tag.contains("LandingTimer") ? tag.getInt("LandingTimer") : 0;
        this.landedFlag = tag.contains("LandedFlag") && tag.getBoolean("LandedFlag");
        this.landedTimer = tag.contains("LandedTimer") ? tag.getInt("LandedTimer") : 0;
        if (!savedFlying) {
            landingTimer = 0;
        }
        this.clearRiderControlLock();
        this.takeoffLockTicks = 0;
        // Restore combat cooldowns
        this.combatManager.loadFromNBT(tag);
        if (tag.contains("SuperchargeTicks")) {
            this.superchargeTicks = Math.max(0, tag.getInt("SuperchargeTicks"));
        }
        if (tag.contains("TempInvulnTicks")) {
            this.tempInvulnTicks = Math.max(0, tag.getInt("TempInvulnTicks"));
            if (this.tempInvulnTicks > 0) {
                this.setInvulnerable(true);
            }
        }

        if (tag.contains("AllowGroundBeamStorm")) {
            this.allowGroundBeamDuringStorm = tag.getBoolean("AllowGroundBeamStorm");
        }
        if (tag.contains("FeedingCooldownTicks")) {
            this.entityData.set(DATA_FEEDING_COOLDOWN, Math.max(0, tag.getInt("FeedingCooldownTicks")));
        }
        tamingController.load(tag);

        this.manualSitCommand = tag.contains("ManualSitCommand") && tag.getBoolean("ManualSitCommand");

        if (this.usingAirNav) {
            switchToAirNavigation();
        } else {
            switchToGroundNavigation();
        }
        if (this.getCommand() != 1 && this.isOrderedToSit()) {
            this.setOrderedToSit(false);
        }

        // Don't force wake on chunk reload - let sleep behavior re-evaluate naturally (like Naturalist mod)
        // Sleep transition states are ephemeral and will be re-evaluated by DragonSleepBehavior
        boolean shouldHaveNoGravity = isFlying() || isHovering();
        this.setNoGravity(shouldHaveNoGravity);

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

        // Wild wyverns should never persist sit/sleep suppression after reload; reset fully to allow sleep re-evaluation
        if (!this.isTame()) {
            this.setCommandAuto(0);
            this.setOrderedToSit(false);
            this.setInSittingPose(false);
            this.sitProgress = 0f;
            this.prevSitProgress = 0f;
            this.entityData.set(DATA_SIT_PROGRESS, 0f);
            this.isSittingDown = false;
            this.isStandingUp = false;
            this.sitTransitionTicks = 0;
            this.sleepReentryCooldownTicks = 0;
            this.sleepAmbientCooldownTicks = 0;
        }

        // Apply config attributes when loading from NBT (Forge fix)
        applyConfiguredAttributes();
    }

    @Override
    protected boolean repositionEntityAfterLoad() {
        // Flying dragons should NOT be repositioned - keep exact loaded position
        // This prevents passenger ejection when reloading while flying
        return !isFlying() && !isHovering();
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

        // Reset combat manager
        this.combatManager.clearAllStates();
        
        // Clear any sleep suppression (fresh start)
        this.sleepReentryCooldownTicks = 0;
        this.sleepAmbientCooldownTicks = 0;
    }

    // ===== GECKOLIB =====
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<Raevyx> movementController =
                new AnimationController<>(this, "movement", 8, animationHandler::handleMovementAnimation);
        AnimationController<Raevyx> actionController =
                new AnimationController<>(this, "action", 2, state -> {
                    if (isTamingStunned()) {
                        return PlayState.STOP;
                    }
                    return PlayState.STOP;
                });

        movementController.setSoundKeyframeHandler(this::onAnimationSound);
        actionController.setSoundKeyframeHandler(this::onAnimationSound);
        animationHandler.setupActionController(actionController);

        // Dedicated controller for instant hurt reactions (no transition easing)
        AnimationController<Raevyx> HurtController =
                new AnimationController<>(this, "hurt", 3, state -> PlayState.STOP);
        HurtController.triggerableAnim("raevyx_hurt",
                RawAnimation.begin().thenPlay("animation.raevyx.hurt"));
        HurtController.triggerableAnim("baby_raevyx_hurt",
                RawAnimation.begin().thenPlay("animation.raevyx.hurt"));

        HurtController.setSoundKeyframeHandler(this::onAnimationSound);
        controllers.add(HurtController);

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
    public DragonAbilityType<?, ?> getPrimaryAttackAbility() {
        // Use melee mode to switch between bite and horn gore
        // Mode 0 = bite (primary), Mode 1 = horn gore (secondary)
        return getMeleeMode() == 0 ?
            RaevyxAbilities.RAEVYX_BITE :
            RaevyxAbilities.RAEVYX_HORN_GORE;
    }

    @Override
    public DragonAbilityType<?, ?> getRoaringAbility() {
        return RaevyxAbilities.RAEVYX_ROAR;
    }

    @Override
    public DragonAbilityType<?, ?> getChannelingAbility() {
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
    // DYNAMIC EYE HEIGHT SYSTEM
    // Will be calculated dynamically from renderer

    @Override
    public void lockRiderControls(int ticks) {
        super.lockRiderControls(ticks);  // Base handles tick counting and entity data
        // Raevyx-specific: reset movement states during lock
        this.setAccelerating(false);
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
    public void clearTakeoffLock() { this.takeoffLockTicks = 0; }
    private void tickTakeoffLock() { if (takeoffLockTicks > 0) takeoffLockTicks--; }

    public void clearTemporaryInvuln() {
        this.tempInvulnTicks = 0;
        if (!isDying()) this.setInvulnerable(false);
    }

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

    private void tickRecentAggroCleanup() {
        if (recentAggroIds.isEmpty()) {
            return;
        }
        long now = this.level().getGameTime();
        recentAggroIds.entrySet().removeIf(entry -> entry.getValue() < now);
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

    @Override
    public @NotNull EntityDimensions getDimensions(@Nonnull Pose pose) {
        // Scale down hitbox for babies to prevent pushing parent dragon
        EntityDimensions baseDimensions = super.getDimensions(pose);
        if (this.isBaby()) {
            // Scale babies to 40% of adult size (0.4x)
            // Adult: 3.5F x 3.0F -> Baby: 1.4F x 1.2F
            return baseDimensions.scale(0.4F);
        }
        return baseDimensions;
    }

    @Override
    public void ageBoundaryReached() {
        super.ageBoundaryReached();
        // Refresh hitbox dimensions when baby grows into adult
        this.refreshDimensions();
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

            // IMPORTANT: Set skip flag BEFORE calling setAge to prevent respawn logic
            baby.skipRespawnTicks = 5; // Skip respawn for 5 ticks (enough for vanilla to finish spawning)
            baby.setAge(-24000);

            // Position the baby near the parent (this is called when spawn egg is used on adult)
            // Find safe ground position to prevent spawning mid-air
            net.minecraft.core.BlockPos safePos = findSafeBabySpawnPos(level, this.blockPosition());
            double spawnY = safePos != null ? safePos.getY() : this.getY();
            baby.moveTo(this.getX(), spawnY, this.getZ(), this.getYRot(), 0.0F);
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
            // While locked, keep rider safe and aligned but do not apply rider-driven pitch changes
            player.fallDistance = 0.0F;
            this.fallDistance = 0.0F;
            this.setTarget(null);
            copyRiderYaw(player);
            // Stop acceleration & vertical intents during lock
            this.setAccelerating(false);
            if (!this.isFlying()) {
                // On ground: block takeoff/vertical motion entirely
                this.setGoingUp(false);
                this.setGoingDown(false);
            }
        }

        if (isBeaming()) {
            Vec3 start = getBeamStartAnchor(1.0f);
            if (start == null) {
                resetBeamAim();
                copyRiderLook(player);
            } else {
                // Calculate beam aim direction for server-side beam path, but DON'T apply it to dragon rotation
                // The rider controller (line 4412) already handles rotation - applyBeamLook would fight it
                Vec3 aim = refreshBeamAimDirection(start, true);
                if (aim == null) {
                    copyRiderLook(player);
                }
                // Skip applyBeamLook when riding - rider controller handles rotation
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
        boolean shouldRecallOwner = !this.level().isClientSide
                && passenger == getControllingPassenger()
                && passenger == getOwner()
                && !this.onGround();
        // Base implementation handles clearing control lock
        super.removePassenger(passenger);
        if (shouldRecallOwner) {
            triggerForcedOwnerFollow();
        }
    }
    // Cooldown for aggro growl to prevent spam while ridden or under repeated retargeting
    private int aggroGrowlCooldown = 0;

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if ((isTamingStunned() || tamingAbortCalmTicks > 0) && target != null) {
            return;
        }
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

    // ===== DRAGON FLIGHT CAPABLE INTERFACE =====
    @Override
    public float getFlightSpeed() {
        return 1.0f; // Base flight speed
    }

    @Override
    public double getPreferredFlightAltitude() {
        return 15.0;
    }
    
    @Override
    public boolean canTakeoff() {
        return !isInWaterOrBubble() && !isInLava() && onGround();
    }

    private boolean shouldStaySeatedCommand() {
        return this.isTame() && this.getCommand() == 1;
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

    public boolean canBeBound() {
        return !isSleeping() && !isDying() && !isBeaming();
    }

    // ===== SCREEN SHAKE INTERFACE IMPLEMENTATION =====
    @Override
    public float getScreenShakeAmount(float partialTicks) {
        float currentAmount = getFloatData(DATA_SCREEN_SHAKE_AMOUNT);
        return prevScreenShakeAmount + (currentAmount - prevScreenShakeAmount) * partialTicks;
    }

    @Override
    public double getShakeDistance() {
        return 25.0; // larger shake radius
    }

    @Override
    public boolean canFeelShake(Entity player) {
        return true;
    }
    public void triggerScreenShake(float intensity) {
        this.screenShakeAmount = Math.max(this.screenShakeAmount, intensity);
        this.entityData.set(DATA_SCREEN_SHAKE_AMOUNT, this.screenShakeAmount);
    }
}

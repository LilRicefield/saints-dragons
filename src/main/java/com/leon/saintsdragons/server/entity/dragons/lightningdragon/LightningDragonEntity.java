/**
 * My name's Zap Van Dink. I'm a lightning dragon.
 */
package com.leon.saintsdragons.server.entity.dragons.lightningdragon;

//Custom stuff
import com.leon.saintsdragons.common.particle.lightningdragon.LightningArcData;
import com.leon.saintsdragons.common.particle.lightningdragon.LightningStormData;
import com.leon.saintsdragons.server.ai.goals.lightningdragon.LightningDragonDodgeGoal;
import com.leon.saintsdragons.server.ai.goals.lightningdragon.LightningDragonFlightGoal;
import com.leon.saintsdragons.server.ai.goals.lightningdragon.LightningDragonFollowOwnerGoal;
import com.leon.saintsdragons.server.ai.goals.lightningdragon.LightningDragonGroundWanderGoal;
import com.leon.saintsdragons.server.ai.goals.lightningdragon.LightningDragonPanicGoal;
import com.leon.saintsdragons.server.ai.goals.lightningdragon.*;
import com.leon.saintsdragons.server.ai.navigation.DragonFlightMoveHelper;
import com.leon.saintsdragons.server.entity.controller.lightningdragon.LightningDragonPhysicsController;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.interfaces.DragonCombatCapable;
import com.leon.saintsdragons.server.entity.interfaces.DragonFlightCapable;
import com.leon.saintsdragons.server.entity.interfaces.DragonSleepCapable;
import com.leon.saintsdragons.server.entity.controller.lightningdragon.LightningDragonFlightController;
import com.leon.saintsdragons.server.entity.handler.DragonInteractionHandler;
import com.leon.saintsdragons.server.entity.handler.DragonKeybindHandler;
import com.leon.saintsdragons.server.entity.controller.lightningdragon.LightningDragonRiderController;
import com.leon.saintsdragons.server.entity.handler.DragonSoundHandler;
import com.leon.saintsdragons.util.DragonMathUtil;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.common.registry.AbilityRegistry;

//Minecraft
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;

//GeckoLib
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.keyframe.event.SoundKeyframeEvent;

//WHO ARE THESE SUCKAS
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;
import org.jetbrains.annotations.NotNull;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

//Just everything
public class LightningDragonEntity extends DragonEntity implements FlyingAnimal, RangedAttackMob,
        DragonCombatCapable, DragonFlightCapable, DragonSleepCapable {
    // Simple per-field caches - more maintainable than generic system
    private double cachedOwnerDistance = Double.MAX_VALUE;
    private int ownerDistanceCacheTime = -1;
    private List<Projectile> cachedNearbyProjectiles = new java.util.concurrent.CopyOnWriteArrayList<>();
    private int nearbyProjectilesCacheTime = -1;
    private int projectileCacheIntervalTicks = 3; // dynamic backoff (min 3)
    private int emptyProjectileScans = 0;
    private double cachedHorizontalSpeed = 0.0;
    private int horizontalSpeedCacheTime = -1;
    // ===== AMBIENT SOUND SYSTEM =====
    private int ambientSoundTimer;
    private int nextAmbientSoundDelay;

    // Sound frequency constants (in ticks)
    private static final int MIN_AMBIENT_DELAY = 200;  // 10 seconds
    private static final int MAX_AMBIENT_DELAY = 600;  // 30 seconds

    // ===== CORE ANIMATIONS =====
    public static final RawAnimation GROUND_IDLE = RawAnimation.begin().thenLoop("animation.lightning_dragon.ground_idle");
    public static final RawAnimation GROUND_WALK = RawAnimation.begin().thenLoop("animation.lightning_dragon.walk");
    public static final RawAnimation GROUND_RUN = RawAnimation.begin().thenLoop("animation.lightning_dragon.run");
    public static final RawAnimation SIT = RawAnimation.begin().thenLoop("animation.lightning_dragon.sit");

    public static final RawAnimation TAKEOFF = RawAnimation.begin().thenPlay("animation.lightning_dragon.takeoff");
    public static final RawAnimation FLY_GLIDE = RawAnimation.begin().thenLoop("animation.lightning_dragon.fly_gliding");
    public static final RawAnimation FLY_FORWARD = RawAnimation.begin().thenLoop("animation.lightning_dragon.fly_forward");
    // Air hover/stationary flight (gentle wing holds)
    public static final RawAnimation FLAP = RawAnimation.begin().thenLoop("animation.lightning_dragon.flap");
    public static final RawAnimation LANDING = RawAnimation.begin().thenPlay("animation.lightning_dragon.landing");

    public static final RawAnimation DODGE = RawAnimation.begin().thenPlay("animation.lightning_dragon.dodge");

    // ===== CONSTANTS =====
    public static final float MODEL_SCALE = 4.5f;

    // Speed constants
    private static final double WALK_SPEED = 0.25D;
    private static final double RUN_SPEED = 0.45D;


    // ===== DATA ACCESSORS (Package-private for controller access) =====
    static final EntityDataAccessor<Boolean> DATA_FLYING =
            SynchedEntityData.defineId(LightningDragonEntity.class, EntityDataSerializers.BOOLEAN);
    static final EntityDataAccessor<Boolean> DATA_TAKEOFF =
            SynchedEntityData.defineId(LightningDragonEntity.class, EntityDataSerializers.BOOLEAN);
    static final EntityDataAccessor<Boolean> DATA_HOVERING =
            SynchedEntityData.defineId(LightningDragonEntity.class, EntityDataSerializers.BOOLEAN);
    static final EntityDataAccessor<Boolean> DATA_LANDING =
            SynchedEntityData.defineId(LightningDragonEntity.class, EntityDataSerializers.BOOLEAN);
    static final EntityDataAccessor<Boolean> DATA_RUNNING =
            SynchedEntityData.defineId(LightningDragonEntity.class, EntityDataSerializers.BOOLEAN);
    // 0 = idle, 1 = walk, 2 = run (server-authoritative ground move state)
    public static final EntityDataAccessor<Integer> DATA_GROUND_MOVE_STATE =
            SynchedEntityData.defineId(LightningDragonEntity.class, EntityDataSerializers.INT);
    // 0=glide,1=forward,2=hover,3=takeoff,-1=ground
    public static final EntityDataAccessor<Integer> DATA_FLIGHT_MODE =
            SynchedEntityData.defineId(LightningDragonEntity.class, EntityDataSerializers.INT);
    // Latest rider input snapshot for reliable ground animation sync
    static final EntityDataAccessor<Float> DATA_RIDER_FORWARD =
            SynchedEntityData.defineId(LightningDragonEntity.class, EntityDataSerializers.FLOAT);
    static final EntityDataAccessor<Float> DATA_RIDER_STRAFE =
            SynchedEntityData.defineId(LightningDragonEntity.class, EntityDataSerializers.FLOAT);
    static final EntityDataAccessor<Integer> DATA_ATTACK_KIND =
            SynchedEntityData.defineId(LightningDragonEntity.class, EntityDataSerializers.INT);
    static final EntityDataAccessor<Integer> DATA_ATTACK_PHASE =
            SynchedEntityData.defineId(LightningDragonEntity.class, EntityDataSerializers.INT);
    static final EntityDataAccessor<Boolean> DATA_BEAMING =
            SynchedEntityData.defineId(LightningDragonEntity.class, EntityDataSerializers.BOOLEAN);
    static final EntityDataAccessor<Boolean> DATA_BEAM_END_SET =
            SynchedEntityData.defineId(LightningDragonEntity.class, EntityDataSerializers.BOOLEAN);
    static final EntityDataAccessor<Float> DATA_BEAM_END_X =
            SynchedEntityData.defineId(LightningDragonEntity.class, EntityDataSerializers.FLOAT);
    static final EntityDataAccessor<Float> DATA_BEAM_END_Y =
            SynchedEntityData.defineId(LightningDragonEntity.class, EntityDataSerializers.FLOAT);
    static final EntityDataAccessor<Float> DATA_BEAM_END_Z =
            SynchedEntityData.defineId(LightningDragonEntity.class, EntityDataSerializers.FLOAT);
    static final EntityDataAccessor<Boolean> DATA_BEAM_START_SET =
            SynchedEntityData.defineId(LightningDragonEntity.class, EntityDataSerializers.BOOLEAN);
    static final EntityDataAccessor<Float> DATA_BEAM_START_X =
            SynchedEntityData.defineId(LightningDragonEntity.class, EntityDataSerializers.FLOAT);
    static final EntityDataAccessor<Float> DATA_BEAM_START_Y =
            SynchedEntityData.defineId(LightningDragonEntity.class, EntityDataSerializers.FLOAT);
    static final EntityDataAccessor<Float> DATA_BEAM_START_Z =
            SynchedEntityData.defineId(LightningDragonEntity.class, EntityDataSerializers.FLOAT);

    // Riding control state accessors
    static final EntityDataAccessor<Boolean> DATA_GOING_UP =
            SynchedEntityData.defineId(LightningDragonEntity.class, EntityDataSerializers.BOOLEAN);
    static final EntityDataAccessor<Boolean> DATA_GOING_DOWN =
            SynchedEntityData.defineId(LightningDragonEntity.class, EntityDataSerializers.BOOLEAN);
    static final EntityDataAccessor<Boolean> DATA_ACCELERATING =
            SynchedEntityData.defineId(LightningDragonEntity.class, EntityDataSerializers.BOOLEAN);
    static final EntityDataAccessor<Boolean> DATA_SLEEPING =
            SynchedEntityData.defineId(LightningDragonEntity.class, EntityDataSerializers.BOOLEAN);


    // ===== STATE VARIABLES (Package-private for controller access) =====
    public int timeFlying = 0;
    public boolean landingFlag = false;

    public int landingTimer = 0;
    int runningTicks = 0;
    // Banking smoothing state
    private float bankSmoothedYaw = 0f;
    private int bankHoldTicks = 0;
    private int bankDir = 0; // -1 left, 0 none, 1 right

    // Pitching smoothing state
    private float pitchSmoothedPitch = 0f;
    private int pitchHoldTicks = 0;
    private int pitchDir = 0; // -1 down, 0 none, 1 up

    // Dodge system
    boolean dodging = false;
    int dodgeTicksLeft = 0;
    Vec3 dodgeVec = Vec3.ZERO;

    // Dying gate to coordinate custom death ability/timing
    private boolean dying = false;
    // Sleep transition state
    private boolean sleepingEntering = false;
    private boolean sleepingExiting = false;
    private int sleepTransitionTicks = 0;
    // Tiny ambient resume buffer after exit completes
    private int sleepAmbientCooldownTicks = 0;
    // Re-entry suppression after aggression/damage to prevent instant resleep
    private int sleepReentryCooldownTicks = 0;
    // Hard-stop flag to kill sleep clips immediately across ticks
    private int sleepCancelTicks = 0;

    // Post-load stabilization to preserve midair riding state across save/load
    private int postLoadAirStabilizeTicks = 0; // counts down after world load if we saved while flying

    // Rider takeoff request timer: while > 0, flight controller treats state as takeoff
    private int riderTakeoffTicks = 0;


    // Last landing completion time (server game time). Used for takeoff cooldowns.
    private long lastLandingGameTime = Long.MIN_VALUE;

    public long getLastLandingGameTime() {
        return lastLandingGameTime;
    }

    // Last broadcasted animation state for observer pulse
    private int lastBroadcastGroundState = Integer.MIN_VALUE;
    private int lastBroadcastFlightMode = Integer.MIN_VALUE;

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
    public final LightningDragonFlightController flightController;
    public final DragonInteractionHandler interactionHandler;

    // ===== SPECIALIZED HANDLER SYSTEMS =====
    private final DragonKeybindHandler keybindHandler;
    private final LightningDragonRiderController riderController;
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
    private final LightningDragonPhysicsController animationController = new LightningDragonPhysicsController(this);

    // Animation controller is internal-only; external integration goes via GeckoLib controllers.


    public LightningDragonEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        this.setMaxUpStep(1.25F);

        // Initialize both navigators
        this.groundNav = new GroundPathNavigation(this, level);
        this.airNav = new FlyingPathNavigation(this, level) {
            @Override
            public boolean isStableDestination(@NotNull BlockPos pos) {
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
        this.flightController = new LightningDragonFlightController(this);
        this.interactionHandler = new DragonInteractionHandler(this);

        // Initialize specialized handler systems
        this.keybindHandler = new DragonKeybindHandler(this);
        this.riderController = new LightningDragonRiderController(this);
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
    protected void playStepSound(@NotNull BlockPos pos, @NotNull BlockState state) {
        // Intentionally empty — step sounds are driven by GeckoLib keyframes (step1/step2)
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 180.0D)
                .add(Attributes.MOVEMENT_SPEED, WALK_SPEED)
                .add(Attributes.FOLLOW_RANGE, 80.0D)
                .add(Attributes.FLYING_SPEED, 0.9D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D);
    }

    // Cooldown to prevent hurt sound spam when ridden or under rapid hits
    private int hurtSoundCooldown = 0;
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_FLYING, false);
        this.entityData.define(DATA_TAKEOFF, false);
        this.entityData.define(DATA_HOVERING, false);
        this.entityData.define(DATA_LANDING, false);
        this.entityData.define(DATA_RUNNING, false);
        this.entityData.define(DATA_GROUND_MOVE_STATE, 0);
        this.entityData.define(DATA_FLIGHT_MODE, -1);
        this.entityData.define(DATA_RIDER_FORWARD, 0f);
        this.entityData.define(DATA_RIDER_STRAFE, 0f);
        this.entityData.define(DATA_ATTACK_KIND, 0);
        this.entityData.define(DATA_ATTACK_PHASE, 0);
        this.entityData.define(DATA_BEAMING, false);
        this.entityData.define(DATA_BEAM_END_SET, false);
        this.entityData.define(DATA_BEAM_END_X, 0f);
        this.entityData.define(DATA_BEAM_END_Y, 0f);
        this.entityData.define(DATA_BEAM_END_Z, 0f);
        this.entityData.define(DATA_BEAM_START_SET, false);
        this.entityData.define(DATA_BEAM_START_X, 0f);
        this.entityData.define(DATA_BEAM_START_Y, 0f);
        this.entityData.define(DATA_BEAM_START_Z, 0f);
        this.entityData.define(DATA_GOING_UP, false);
        this.entityData.define(DATA_GOING_DOWN, false);
        this.entityData.define(DATA_ACCELERATING, false);
        this.entityData.define(DATA_SLEEPING, false);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends DragonEntity> DragonAbility<T> getActiveAbility() {
        return (DragonAbility<T>) combatManager.getActiveAbility();
    }
    public boolean canUseAbility() {
        return combatManager.canUseAbility();
    }
    public void useRidingAbility(String abilityName) {
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

    /**
     * Forces the dragon to take off when being ridden. Called when player presses Space while on ground.
     */
    public void requestRiderTakeoff() {
        riderController.requestRiderTakeoff();
    }

    // (External callers should use triggerable action keys on the GeckoLib controller.)

    @Override
    public Vec3 getHeadPosition() {
        // Use GeckoLib bone position if available
        Vec3 bonePos = getBonePosition("head");
        if (bonePos != null) {
            return bonePos;
        }
        // Fallback to eye position
        return getEyePosition();
    }

    // Get mouth position using beam_origin bone from GeckoLib model
    @Override
    public Vec3 getMouthPosition() {
        // Try to get the beam_origin bone position (works on both client and server)
        Vec3 bonePos = getBonePosition("beam_origin");
        if (bonePos != null) {
            return bonePos;
        }
        Vec3 basePos = getHeadPosition();
        double localX = 15.0 / 16.0 * MODEL_SCALE;    // Forward from head
        double localY = 6.6 / 16.0 * MODEL_SCALE;     // Up from head base (matches geo ~6.6)
        double localZ = 0.0;                          // No lateral offset; beam_origin pivot x=0 in geo

        // Apply rotation based on entity's body rotation
        double radians = Math.toRadians(-yBodyRot); // Negative to rotate local X into world forward correctly
        double rotatedX = localX * Math.cos(radians) - localZ * Math.sin(radians);
        double rotatedZ = localX * Math.sin(radians) + localZ * Math.cos(radians);

        Vec3 lookDirection = getLookAngle();
        double pitchAdjustment = 0;
        if (isFlying()) {
            // When flying and looking down, adjust the beam origin to be more forward
            float pitch = getXRot(); // Positive pitch means looking down
            if (pitch > 0) {
                // The more the dragon looks down, the more forward the beam origin should be
                pitchAdjustment = (pitch / 90.0) * MODEL_SCALE * 0.5; // Scale with pitch
            }
        }

        // Final position calculation
        double finalX = basePos.x + rotatedX + (lookDirection.x * pitchAdjustment);
        double finalY = basePos.y + localY - (lookDirection.y * pitchAdjustment); // Subtract to go forward when looking down
        double finalZ = basePos.z + rotatedZ + (lookDirection.z * pitchAdjustment);

        return new Vec3(finalX, finalY, finalZ);
    }

    /**
     * Compute a mouth origin in world space from head yaw/pitch and a fixed local offset.
     * This mirrors the Ice & Fire approach and is safe on server/client.
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

    public boolean isBeaming() { return this.entityData.get(DATA_BEAMING); }
    public void setBeaming(boolean beaming) {
        this.entityData.set(DATA_BEAMING, beaming);
    }

    // (No client/server rider anchor fields; seat uses math-based head-space anchor)

    // ===== BEAM END SYNC + CLIENT LERP =====
    private Vec3 prevClientBeamEnd = null;
    private Vec3 clientBeamEnd = null;

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
        if (!this.entityData.get(DATA_BEAM_END_SET)) return null;
        return new Vec3(
                this.entityData.get(DATA_BEAM_END_X),
                this.entityData.get(DATA_BEAM_END_Y),
                this.entityData.get(DATA_BEAM_END_Z)
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
        if (!this.entityData.get(DATA_BEAM_START_SET)) return null;
        return new Vec3(
                this.entityData.get(DATA_BEAM_START_X),
                this.entityData.get(DATA_BEAM_START_Y),
                this.entityData.get(DATA_BEAM_START_Z)
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
    protected @NotNull PathNavigation createNavigation(@NotNull Level level) {
        return new GroundPathNavigation(this, level);
    }

    // ===== STATE MANAGEMENT =====
    public boolean isFlying() { return this.entityData.get(DATA_FLYING); }

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

    public boolean isTakeoff() { return this.entityData.get(DATA_TAKEOFF); }
    public void setTakeoff(boolean takeoff) {
        if (takeoff && this.isBaby()) takeoff = false;
        this.entityData.set(DATA_TAKEOFF, takeoff);
    }

    public boolean isHovering() { return this.entityData.get(DATA_HOVERING); }
    public void setHovering(boolean hovering) {
        if (hovering && this.isBaby()) hovering = false;
        this.entityData.set(DATA_HOVERING, hovering);
    }


    public boolean isRunning() { return this.entityData.get(DATA_RUNNING); }

    public void setRunning(boolean running) {
        this.entityData.set(DATA_RUNNING, running);
        if (running) {
            runningTicks = 0;
            Objects.requireNonNull(this.getAttribute(Attributes.MOVEMENT_SPEED)).setBaseValue(RUN_SPEED);
        } else {
            Objects.requireNonNull(this.getAttribute(Attributes.MOVEMENT_SPEED)).setBaseValue(WALK_SPEED);
        }
    }

    public boolean isWalking() {
        if (isFlying()) return false;
        int s = level().isClientSide ? getEffectiveGroundState() : this.entityData.get(DATA_GROUND_MOVE_STATE);
        if (s == 1) return true;
        if (s == 2) return false;
        // Fallback for client prediction or early ticks before sync
        if (level().isClientSide && clientGroundOverride == Integer.MIN_VALUE) {
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
        return this.entityData.get(DATA_RUNNING);
    }

    public boolean isLanding() { return this.entityData.get(DATA_LANDING); }

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

    // (Removed unused DATA_ATTACKING flag)

    public void setAttackKind(int kind) { this.entityData.set(DATA_ATTACK_KIND, kind); }

    public void setAttackPhase(int phase) { this.entityData.set(DATA_ATTACK_PHASE, phase); }

    // Riding control states
    public boolean isGoingUp() { return this.entityData.get(DATA_GOING_UP); }
    public void setGoingUp(boolean goingUp) { this.entityData.set(DATA_GOING_UP, goingUp); }

    public boolean isGoingDown() { return this.entityData.get(DATA_GOING_DOWN); }
    public void setGoingDown(boolean goingDown) { this.entityData.set(DATA_GOING_DOWN, goingDown); }

    public boolean isAccelerating() { return this.entityData.get(DATA_ACCELERATING); }
    public void setAccelerating(boolean accelerating) { this.entityData.set(DATA_ACCELERATING, accelerating); }

    // Rider input snapshots for server-side animation sync
    public void setLastRiderForward(float forward) { this.entityData.set(DATA_RIDER_FORWARD, forward); }
    public void setLastRiderStrafe(float strafe) { this.entityData.set(DATA_RIDER_STRAFE, strafe); }

    // Flight mode accessor for controllers (avoids accessing protected entityData outside entity)
    public int getSyncedFlightMode() { return this.entityData.get(DATA_FLIGHT_MODE); }

    // Debug/inspection helper: expose raw ground move state
    public int getGroundMoveState() { return this.entityData.get(DATA_GROUND_MOVE_STATE); }

    // ===== Client animation overrides (for robust observer sync) =====
    private int clientGroundOverride = Integer.MIN_VALUE;
    private int clientFlightOverride = Integer.MIN_VALUE;
    private int clientOverrideExpiry = 0;
    
    public void applyClientAnimState(int groundState, int flightMode) {
        this.clientGroundOverride = groundState;
        this.clientFlightOverride = flightMode;
        this.clientOverrideExpiry = this.tickCount + 40; // Expire after 2 seconds
    }
    public int getEffectiveGroundState() {
        if (level().isClientSide && clientGroundOverride != Integer.MIN_VALUE && tickCount < clientOverrideExpiry) {
            return clientGroundOverride;
        }
        // Clear expired overrides
        if (level().isClientSide && tickCount >= clientOverrideExpiry) {
            clientGroundOverride = Integer.MIN_VALUE;
        }
        return this.entityData.get(DATA_GROUND_MOVE_STATE);
    }
    public int getEffectiveFlightMode() {
        if (level().isClientSide && clientFlightOverride != Integer.MIN_VALUE && tickCount < clientOverrideExpiry) {
            return clientFlightOverride;
        }
        // Clear expired overrides
        if (level().isClientSide && tickCount >= clientOverrideExpiry) {
            clientFlightOverride = Integer.MIN_VALUE;
        }
        int v = this.entityData.get(DATA_FLIGHT_MODE);
        if (v < 0 && isFlying()) {
            // Derive from flags if missing
            if (isTakeoff()) return 3;
            if (isHovering()) return 2;
            double yDelta = this.getY() - this.yo;
            double vh2 = getDeltaMovement().horizontalDistanceSqr();
            if (Math.abs(yDelta) < 0.06 && vh2 > 0.01) return 0; // clear glide
            return 1;
        }
        return v;
    }

    // Expose last-tick vertical delta for robust flight-mode decisions
    public double getYDelta() { return this.getY() - this.yo; }

    // Allow AI goals to set ground move state explicitly
    public void setGroundMoveStateFromAI(int state) {
        if (!this.level().isClientSide) {
            int s = Math.max(0, Math.min(2, state));
            if (this.entityData.get(DATA_GROUND_MOVE_STATE) != s) {
                this.entityData.set(DATA_GROUND_MOVE_STATE, s);
                com.leon.saintsdragons.common.network.NetworkHandler.INSTANCE.send(
                        net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> this),
                        new com.leon.saintsdragons.common.network.MessageDragonAnimState(this.getId(), (byte) s, (byte) getEffectiveFlightMode())
                );
            }
        }
    }


    // Control state system
    private byte controlState = 0;

    public byte getControlState() {
        return controlState;
    }

    public void setControlState(byte controlState) {
        this.controlState = controlState;  // Update local cached control state
        // Only process keybind logic on the server
        if (!level().isClientSide) {
            keybindHandler.setControlState(controlState);
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

    // ===== RIDING SUPPORT =====
    @Override
    public double getPassengersRidingOffset() {
        return riderController.getPassengersRidingOffset();
    }

    @Override
    protected void positionRider(@NotNull Entity passenger, Entity.@NotNull MoveFunction moveFunction) {
        riderController.positionRider(passenger, moveFunction);
    }

    @Override
    public @NotNull Vec3 getDismountLocationForPassenger(@NotNull LivingEntity passenger) {
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

    // (Target validation handled by DragonCombatManager.)

    // ===== MAIN TICK METHOD =====
    @Override
    public void tick() {
        animationController.tick();

        // Client-side animation sync no longer required; standard controller handles timing

        super.tick();

        // Clear sitting state if the dragon is being ridden
        if (!this.level().isClientSide && this.isVehicle() && this.isOrderedToSit()) {
            this.setOrderedToSit(false);
        }

        // Drive pending sound scheduling (both sides)
        this.getSoundHandler().tick();

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

        // Decrement rider takeoff window (no-op while dying)
        if (!level().isClientSide && riderTakeoffTicks > 0 && !isDying()) {
            riderTakeoffTicks--;
        }

        // (No action window/gate ticking)

        // Delegate to controllers (disabled while dying)
        if (!isDying()) {
            flightController.handleFlightLogic();
            combatManager.tick();
        }
        interactionHandler.updateSittingProgress();

        // Cool down hurt sound throttle
        if (hurtSoundCooldown > 0) hurtSoundCooldown--;

        if (!level().isClientSide) {
            // When ridden and flying, never stay in 'hovering' unless explicitly landing or beaming or taking off
            if (isFlying() && getControllingPassenger() != null) {
                if (!isLanding() && !isBeaming() && !isTakeoff() && isHovering()) {
                    setHovering(false);
                }
            }
            // While rider controls are locked (e.g., Summon Storm windup), freeze movement and AI
            if (areRiderControlsLocked()) {
                this.getNavigation().stop();
                this.setTarget(null);
                this.setDeltaMovement(0, 0, 0);
            }
            handleAmbientSounds();
            // no-op
        }

        // Handle sleep enter/exit timers
        if (!level().isClientSide) {
            // Supercharge timer (summon storm)
            if (superchargeTicks > 0) superchargeTicks--;
            // Temporary invulnerability timer
            if (tempInvulnTicks > 0) {
                tempInvulnTicks--;
                if (tempInvulnTicks == 0 && !isDying()) this.setInvulnerable(false);
            }
            // Supercharge VFX: periodic arcs/sparks around the body
            if ((isSupercharged() || this.level().isThundering()) && superchargeVfxCooldown-- <= 0) {
                spawnSuperchargeVfx();
                superchargeVfxCooldown = 6 + this.random.nextInt(6); // pulse every ~0.3-0.6s
            }
            if (sleepTransitionTicks > 0) {
                sleepTransitionTicks--;
                if (sleepTransitionTicks == 0) {
                    if (sleepingEntering) {
                        // Enter finished: mark sleeping
                        setSleeping(true);
                        sleepingEntering = false;
                    } else if (sleepingExiting) {
                        // Exit finished
                        sleepingExiting = false;
                        // Start small ambient cooldown buffer (~0.5s)
                        sleepAmbientCooldownTicks = 10;
                    }
                }
            }
            if (sleepAmbientCooldownTicks > 0) sleepAmbientCooldownTicks--;
            if (sleepReentryCooldownTicks > 0) sleepReentryCooldownTicks--;
            if (sleepCancelTicks > 0) sleepCancelTicks--;
        }

        // Wake up if mounted or target appears/aggression
        if (!level().isClientSide && (isSleeping() || sleepingEntering || sleepingExiting)) {
            if (this.isVehicle()) {
                wakeUpImmediately();
            } else if (this.getTarget() != null || this.isAggressive()) {
                // On aggression/target, clear immediately and suppress re-entry for a short window
                wakeUpImmediately();
                suppressSleep(200); // ~10s; adjust as desired
            } else if (this.isInWaterOrBubble() || this.isInLava()) {
                // Never sleep in fluids; wake and suppress to avoid drowning
                wakeUpImmediately();
                suppressSleep(200);
            }

            // Server-authoritative ground movement state sync for reliable client animation (runs while sleeping transitions)
            // Only consider ground state when not flying
            int moveState = 0; // idle
            if (!isFlying()) {
                // If being ridden, prefer rider inputs for robust state selection
                if (getControllingPassenger() != null) {
                    float fwd = this.entityData.get(DATA_RIDER_FORWARD);
                    float str = this.entityData.get(DATA_RIDER_STRAFE);
                    float mag = Math.abs(fwd) + Math.abs(str);
                    if (mag > 0.05f) {
                        moveState = this.isAccelerating() ? 2 : 1;
                    } else {
                        // Fallback while ridden: use actual velocity so observers still see walk/run
                        double speedSqr = getDeltaMovement().horizontalDistanceSqr();
                        if (speedSqr > 0.08) {
                            moveState = 2; // running-level velocity
                        } else if (speedSqr > 0.005) {
                            moveState = 1; // walking-level velocity
                        }
                    }
                } else {
                    // Use horizontal velocity (matches HUD vel2) for AI classification to avoid position delta spikes
                    double velSqr = getDeltaMovement().horizontalDistanceSqr();

                    // Thresholds tuned so typical AI follow (≈0.0054) is walk, not run
                    final double WALK_MIN = 0.0008;
                    final double RUN_MIN  = 0.0200;

                    if (velSqr > RUN_MIN) {
                        moveState = 2; // run
                    } else if (velSqr > WALK_MIN) {
                        moveState = 1; // walk

                    }
                }
            }
            // Only write when changed to avoid excess sync traffic
            boolean changed = false;
            if (this.entityData.get(DATA_GROUND_MOVE_STATE) != moveState) {
                this.entityData.set(DATA_GROUND_MOVE_STATE, moveState);
                changed = true;
            }
            // Flight mode sync for observers
            int flightMode = -1;
            if (isFlying()) {
                double yDelta = getYDelta();
                if (isTakeoff()) {
                    flightMode = 3; // takeoff
                } else if (getControllingPassenger() != null) {
                    // Ridden: always glide when rider holds descend; flap when holding ascend
                    if (isGoingDown()) flightMode = 0; // glide
                    else if (isGoingUp()) flightMode = 1; // flap
                    else if (yDelta < -0.005) flightMode = 0; // natural descent -> glide
                    else flightMode = 1; // otherwise flap
                } else {
                    // AI: keep original behavior using fractions with small bias
                    float glide = getGlidingFraction();
                    float flap = getFlappingFraction();
                    float hover = getHoveringFraction();
                    if (yDelta > 0.02) flightMode = 1;
                    else if (yDelta < -0.02) flightMode = 0;
                    else if (isHovering() || hover > 0.55f) flightMode = 2;
                    else flightMode = (glide >= flap + 0.10f) ? 0 : 1;
                }
            }
            if (this.entityData.get(DATA_FLIGHT_MODE) != flightMode) {
                this.entityData.set(DATA_FLIGHT_MODE, flightMode);
                changed = true;
            }
            // Pulse an S2C message on changes to nudge late observers
            if (changed && (lastBroadcastGroundState != moveState || lastBroadcastFlightMode != flightMode)) {
                lastBroadcastGroundState = moveState;
                lastBroadcastFlightMode = flightMode;
                com.leon.saintsdragons.common.network.NetworkHandler.INSTANCE.send(
                        net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> this),
                        new com.leon.saintsdragons.common.network.MessageDragonAnimState(this.getId(), (byte) moveState, (byte) flightMode)
                );
            }
            // Reduced frequency redundancy - only every 20 ticks (1 second) for critical states
            boolean needsPulse = this.isVehicle() || flightMode >= 0 || moveState != 0;
            if (needsPulse && (this.tickCount & 19) == 0) {
                com.leon.saintsdragons.common.network.NetworkHandler.INSTANCE.send(
                        net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> this),
                        new com.leon.saintsdragons.common.network.MessageDragonAnimState(this.getId(), (byte) moveState, (byte) flightMode)
                );
            }
            // Decay rider inputs slightly each tick to avoid sticking when packets drop
            if (this.entityData.get(DATA_RIDER_FORWARD) != 0f || this.entityData.get(DATA_RIDER_STRAFE) != 0f) {
                float nf = this.entityData.get(DATA_RIDER_FORWARD) * 0.8f;
                float ns = this.entityData.get(DATA_RIDER_STRAFE) * 0.8f;
                if (Math.abs(nf) < 0.01f) nf = 0f;
                if (Math.abs(ns) < 0.01f) ns = 0f;
                this.entityData.set(DATA_RIDER_FORWARD, nf);
                this.entityData.set(DATA_RIDER_STRAFE, ns);
            }
        }

        // Run the same movement/flight sync during normal ticks (not in sleep transitions)
        if (!level().isClientSide && !(isSleeping() || sleepingEntering || sleepingExiting)) {
            int moveState = 0;
            if (!isFlying()) {
                if (getControllingPassenger() != null) {
                    float fwd = this.entityData.get(DATA_RIDER_FORWARD);
                    float str = this.entityData.get(DATA_RIDER_STRAFE);
                    float mag = Math.abs(fwd) + Math.abs(str);
                    if (mag > 0.05f) {
                        moveState = this.isAccelerating() ? 2 : 1;
                    } else {
                        double velSqr = getDeltaMovement().horizontalDistanceSqr();
                        final double WALK_MIN = 0.0008;
                        final double RUN_MIN  = 0.0200;
                        if (velSqr > RUN_MIN) moveState = 2; else if (velSqr > WALK_MIN) moveState = 1;
                    }
                } else {
                    // Use horizontal velocity (matches HUD) for AI classification
                    double velSqr = getDeltaMovement().horizontalDistanceSqr();
                    final double WALK_MIN = 0.0008;
                    final double RUN_MIN  = 0.0200;
                    if (velSqr > RUN_MIN) moveState = 2; else if (velSqr > WALK_MIN) moveState = 1;
                }
            }
            boolean changed = false;
            if (this.entityData.get(DATA_GROUND_MOVE_STATE) != moveState) {
                this.entityData.set(DATA_GROUND_MOVE_STATE, moveState);
                changed = true;
            }
            int flightMode = -1;
            if (isFlying()) {
                double yDelta = getYDelta();
                if (isTakeoff()) {
                    flightMode = 3; // takeoff
                } else if (getControllingPassenger() != null) {
                    if (isGoingDown()) flightMode = 0;
                    else if (isGoingUp()) flightMode = 1;
                    else if (yDelta < -0.005) flightMode = 0;
                    else flightMode = 1;
                } else {
                    float glide = getGlidingFraction();
                    float flap = getFlappingFraction();
                    float hover = getHoveringFraction();
                    if (yDelta > 0.02) flightMode = 1;
                    else if (yDelta < -0.02) flightMode = 0;
                    else if (isHovering() || hover > 0.55f) flightMode = 2;
                    else flightMode = (glide >= flap + 0.10f) ? 0 : 1;
                }
            }
            if (this.entityData.get(DATA_FLIGHT_MODE) != flightMode) {
                this.entityData.set(DATA_FLIGHT_MODE, flightMode);
                changed = true;
            }
            if (changed && (lastBroadcastGroundState != moveState || lastBroadcastFlightMode != flightMode)) {
                lastBroadcastGroundState = moveState;
                lastBroadcastFlightMode = flightMode;
                com.leon.saintsdragons.common.network.NetworkHandler.INSTANCE.send(
                        net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> this),
                        new com.leon.saintsdragons.common.network.MessageDragonAnimState(this.getId(), (byte) moveState, (byte) flightMode)
                );
            }
        }

        // Handle dodge movement first
        if (!level().isClientSide && this.isDodging()) {
            handleDodgeMovement();
            return;
        }

        // Track running time for animations
        if (this.isRunning()) {
            runningTicks++;
        } else {
            runningTicks = Math.max(0, runningTicks - 2);
        }


        // Update client-side sit progress and lerp beam end from synchronized data
        if (level().isClientSide) {
            prevSitProgress = sitProgress;
            sitProgress = this.entityData.get(DATA_SIT_PROGRESS);

            // Beam end/start lerp
            this.prevClientBeamEnd = this.clientBeamEnd;
            this.clientBeamEnd = getBeamEndPosition();

        }
        if (this.isRunning() && this.getDeltaMovement().horizontalDistanceSqr() < 0.01) {
            this.setRunning(false);
        }
    }

    @Override
    protected void playHurtSound(@NotNull net.minecraft.world.damagesource.DamageSource source) {
        if (isDying()) {
            return;
        }
        // Throttle hurt sound to avoid spam while ridden or under rapid damage
        if (hurtSoundCooldown > 0) {
            return;
        }
        // Custom: activate one-shot hurt ability (plays sound + animation once)
        if (!level().isClientSide) {
            this.tryActivateAbility(com.leon.saintsdragons.common.registry.LightningDragonAbilities.HURT);
        }
        // Short cooldown; extend slightly when being ridden
        this.hurtSoundCooldown = this.isVehicle() ? 15 : 8;
    }
    /**
     * Plays appropriate ambient sound based on dragon's current mood and state
     */
    private void playCustomAmbientSound() {
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
     * Because a silent dragon is a boring dragon
     */
    private void handleAmbientSounds() {
        if (isDying() || isSleeping() || isSleepTransitioning() || sleepAmbientCooldownTicks > 0) return;
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
     * Call this method when dragon gets excited/happy (like when player approaches)
     */
    public void playExcitedSound() {
        if (!level().isClientSide && !isDying() && !isStayOrSitMuted() && !isSleepTransitioning()) {
            this.playSound(ModSounds.DRAGON_EXCITED.get(), 1.0f, 1.0f + getRandom().nextFloat() * 0.3f);
        }
    }

    /**
     * Call this when dragon gets annoyed (like when attacked by something weak)
     */
    public void playAnnoyedSound() {
        if (!level().isClientSide && !isDying() && !isStayOrSitMuted() && !isSleepTransitioning()) {
            this.playSound(ModSounds.DRAGON_ANNOYED.get(), 1.2f, 0.8f + getRandom().nextFloat() * 0.4f);
        }
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
        if (this.isInSittingPose() || this.isDodging() || this.isDying() || this.isSleeping() || this.isSleepTransitioning()) {
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
    public void performRangedAttack(@NotNull LivingEntity target, float distanceFactor) {
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
    public static boolean canSpawnHere(EntityType<LightningDragonEntity> type,
                                       net.minecraft.world.level.LevelAccessor level,
                                       MobSpawnType reason,
                                       BlockPos pos,
                                       net.minecraft.util.RandomSource random) {
        BlockPos below = pos.below();
        boolean solidGround = level.getBlockState(below).isFaceSturdy(level, below, net.minecraft.core.Direction.UP);
        boolean feetFree = level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
        boolean headFree = level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty();
        return solidGround && feetFree && headFree;
    }

    @Override
    public int getMaxHeadXRot() {
        return 180;
    }

    // Help the dragon keep its gaze while running: allow wide, fast head turns
    @Override
    public int getMaxHeadYRot() {
        // Encourage body alignment during beam by limiting head swivel
        if (isBeaming()) return 60;
        return 180; // large horizontal head sweep otherwise
    }

    @Override
    public int getHeadRotSpeed() {
        // Slightly slower head-only snapping while beaming to avoid neck doing all the work
        return isBeaming() ? 90 : 180;
    }

    // ===== LOOK CONTROLLER =====
    public static class DragonLookController extends LookControl {
        private final LightningDragonEntity dragon;

        public DragonLookController(LightningDragonEntity dragon) {
            super(dragon);
            this.dragon = dragon;
        }

        @Override
        public void tick() {
            if (this.dragon.isAlive()) {
                super.tick();
            }
        }
    }
    // ===== AI GOALS =====
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(3, new LightningDragonPanicGoal(this));
        this.goalSelector.addGoal(2, new LightningDragonDodgeGoal(this));
        this.goalSelector.addGoal(4, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(5, new FloatGoal(this));

        // Combat goals (prioritized to avoid conflicts)
        // Using new extensible goal system
        this.goalSelector.addGoal(6, new LightningDragonAirCombatGoal(this));      // Air combat + flight decision
        this.goalSelector.addGoal(8, new LightningDragonMeleeAttackGoal(this));     // Ground melee combat

        // Movement/idle
        // Unified sleep goal: high priority to preempt follow/wander, but calm() prevents overriding combat/aggro
        this.goalSelector.addGoal(0, new LightningDragonSleepGoal(this));         // Higher priority than follow
        this.goalSelector.addGoal(1, new LightningDragonFollowOwnerGoal(this));
        this.goalSelector.addGoal(2, new LightningDragonGroundWanderGoal(this, 1.0, 60));
        this.goalSelector.addGoal(9, new LightningDragonFlightGoal(this));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 8.0F));

        // Target selection
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
        // Neutral behavior: do not proactively target players. Only retaliate when hurt or defend owner.
    }

    @Override
    public boolean hurt(@NotNull DamageSource damageSource, float amount) {
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
        if (isSleeping() || sleepingEntering || sleepingExiting) {
            wakeUpImmediately();
            suppressSleep(200);
        }
        if (damageSource.is(DamageTypes.FALL)) {
            return false;
        }

        // Store previous flying state to restore if being ridden
        boolean wasFlying = isFlying();
        boolean wasRidden = isVehicle();

        // Intercept lethal damage to play custom death ability first
        if (!level().isClientSide && !dying) {
            float remaining = this.getHealth() - amount;
            if (remaining <= 0.0f) {
                // Start death sequence; make dragon briefly invulnerable to suppress further deaths
                this.setInvulnerable(true);
                this.setDying(true);
                
                // Force interrupt any active ability and start death ability
                if (!this.canUseAbility()) {
                    // If we can't use ability, there's an active one - interrupt it
                    this.setActiveAbility(null);
                }
                this.tryActivateAbility(com.leon.saintsdragons.common.registry.LightningDragonAbilities.DIE);
                return true; // handled
            }
        }

        boolean result = super.hurt(damageSource, amount);

        // If the dragon was being ridden and flying before taking damage,
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
    public boolean isInvulnerableTo(@NotNull DamageSource source) {
        if (source.is(DamageTypes.LIGHTNING_BOLT)) return true;
        return super.isInvulnerableTo(source);
    }

    // ===== BREATHING / AIR SUPPLY =====
    // Allow the dragon to hold its breath underwater for ~3 minutes (3600 ticks)
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
    public void thunderHit(@NotNull ServerLevel level, @NotNull LightningBolt lightning) {
        // Do not call super; ignore ignition and effects
        if (this.isOnFire()) this.clearFire();
    }

    public boolean isDying() {
        return dying;
    }
    public void setDying(boolean dying) {
        this.dying = dying;
    }

    // ===== SUPERCHARGE (Summon Storm) =====
    private int superchargeTicks = 0;
    public void startSupercharge(int ticks) {
        this.superchargeTicks = Math.max(this.superchargeTicks, Math.max(0, ticks));
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
        for (int i = 0; i <= steps; i++) {
            // Randomly pick arc type per point and randomly drop some points for a crackly feel
            if (this.random.nextFloat() < 0.7f) {
                if (this.random.nextBoolean()) {
                    server.sendParticles(new LightningArcData(size),
                            pos.x, pos.y, pos.z, 1, dir.x, dir.y, dir.z, 0.0);
                } else {
                    server.sendParticles(new LightningStormData(size),
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

    // ===== SLEEPING =====
    public boolean isSleeping() {
        return this.entityData.get(DATA_SLEEPING);
    }
    public void setSleeping(boolean sleeping) {
        this.entityData.set(DATA_SLEEPING, sleeping);
    }
    public boolean isSleepTransitioning() {
        return sleepingEntering || sleepingExiting;
    }
    public void startSleepEnter() {
        if (isSleeping() || sleepingEntering || sleepingExiting) return;
        sleepingEntering = true;
        sleepTransitionTicks = 81; // ~4.021s (enter)
        triggerAnim("action", "sleep_enter");
    }
    public void startSleepExit() {
        if ((!isSleeping() && !sleepingEntering) || sleepingExiting) return;
        // stop sleep loop and transition out
        this.entityData.set(DATA_SLEEPING, false);
        sleepingEntering = false;
        sleepingExiting = true;
        sleepTransitionTicks = 122; // ~6.075s (exit)
        triggerAnim("action", "sleep_exit");
    }

    /** Immediately cancel any sleep state/transition without playing animations. */
    public void wakeUpImmediately() {
        this.entityData.set(DATA_SLEEPING, false);
        sleepingEntering = false;
        sleepingExiting = false;
        sleepTransitionTicks = 0;
        sleepCancelTicks = 2; // ensure controllers STOP for a couple ticks to flush animation
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
        if (this.isDying()) {
            return InteractionResult.PASS;
        }
        ItemStack itemstack = player.getItemInHand(hand);

        if (!this.isTame()) {
            if (this.isFood(itemstack)) {
                // Taming logic must be server-only to avoid client-only visual state changes
                if (!level().isClientSide) {
                    if (!player.getAbilities().instabuild) itemstack.shrink(1);

                    if (this.random.nextInt(3) == 0) {
                        this.tame(player);
                        this.setOrderedToSit(true);
                        this.level().broadcastEntityEvent(this, (byte) 7);
                        
                        // Trigger advancement for taming Lightning Dragon
                        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                            net.minecraft.advancements.Advancement advancement = serverPlayer.server.getAdvancements()
                                    .getAdvancement(com.leon.saintsdragons.SaintsDragons.rl("tame_lightning_dragon"));
                            if (advancement != null) {
                                serverPlayer.getAdvancements().award(advancement, "tame_lightning_dragon");
                            }
                        }
                    } else {
                        this.level().broadcastEntityEvent(this, (byte) 6);
                    }
                }
                // Let both sides know we handled the interaction without duplicating logic client-side
                return InteractionResult.sidedSuccess(level().isClientSide);
            }
        } else {
            if (player.equals(this.getOwner())) {
                // Command cycling - Shift+Right-click cycles through commands
                if (canOwnerCommand(player) && itemstack.isEmpty() && hand == InteractionHand.MAIN_HAND) {
                    int currentCommand = getCommand();
                    int nextCommand = (currentCommand + 1) % 3; // 0=Follow, 1=Sit, 2=Wander
                    setCommand(nextCommand);

                    // Send feedback message to player (action bar), server-side only to avoid duplicates
                    if (!level().isClientSide) {
                        player.displayClientMessage(
                                net.minecraft.network.chat.Component.translatable(
                                        "entity.saintsdragons.all.command_" + nextCommand,
                                        this.getName()
                                ),
                                true
                        );
                    }

                    return InteractionResult.SUCCESS;
                }
                // Mounting - Right-click without shift
                else if (!player.isCrouching() && itemstack.isEmpty() && hand == InteractionHand.MAIN_HAND && canOwnerMount(player)) {
                    if (!this.isVehicle()) {
                        // Force the dragon to stand if sitting
                        if (this.isOrderedToSit()) {
                            this.setOrderedToSit(false);
                        }
                        // Wake up immediately when mounting (bypass transitions/animations)
                        if (this.isSleeping() || this.sleepingEntering || this.sleepingExiting) {
                            this.wakeUpImmediately();
                        }
                        
                        // Clear all combat and AI states when mounting
                        this.clearAllStatesForMounting();
                        
                        // Start riding
                        if (player.startRiding(this)) {
                            // Play excited sound when mounting
                            this.playExcitedSound();
                            // Player can manually take off using Space key when ready
                            return InteractionResult.sidedSuccess(level().isClientSide);
                        }
                    }
                    return InteractionResult.sidedSuccess(level().isClientSide);
                }
            }
        }
        return super.mobInteract(player, hand);
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
    public boolean isFood(ItemStack stack) {
        return stack.is(Items.SALMON);
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

        // Save lock states
        tag.putInt("RiderControlLockTicks", riderControlLockTicks);
        tag.putInt("TakeoffLockTicks", takeoffLockTicks);

        // Persist combat cooldowns
        this.combatManager.saveToNBT(tag);

        // Persist supercharge timer so logout/login doesn't clear buff early
        tag.putInt("SuperchargeTicks", Math.max(0, this.superchargeTicks));

        // Persist temporary invulnerability timer (e.g., during Summon Storm windup)
        tag.putInt("TempInvulnTicks", Math.max(0, this.tempInvulnTicks));

        // Persist sleep state and transition timers
        tag.putBoolean("Sleeping", this.isSleeping());
        tag.putBoolean("SleepingEntering", this.sleepingEntering);
        tag.putBoolean("SleepingExiting", this.sleepingExiting);
        tag.putInt("SleepTransitionTicks", Math.max(0, this.sleepTransitionTicks));
        tag.putInt("SleepAmbientCooldownTicks", Math.max(0, this.sleepAmbientCooldownTicks));
        tag.putInt("SleepReentryCooldownTicks", Math.max(0, this.sleepReentryCooldownTicks));
        tag.putInt("SleepCancelTicks", Math.max(0, this.sleepCancelTicks));

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
        this.riderTakeoffTicks = tag.contains("RiderTakeoffTicks") ? tag.getInt("RiderTakeoffTicks") : 0;

        // Restore critical flight state variables that were missing
        this.lastLandingGameTime = tag.contains("LastLandingGameTime") ? tag.getLong("LastLandingGameTime") : Long.MIN_VALUE;
        this.landingFlag = tag.contains("LandingFlag") && tag.getBoolean("LandingFlag");
        this.landingTimer = tag.contains("LandingTimer") ? tag.getInt("LandingTimer") : 0;

        // Restore lock states
        this.riderControlLockTicks = tag.contains("RiderControlLockTicks") ? tag.getInt("RiderControlLockTicks") : 0;
        this.takeoffLockTicks = tag.contains("TakeoffLockTicks") ? tag.getInt("TakeoffLockTicks") : 0;

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

        // Restore sleep state and transition timers
        if (tag.contains("Sleeping")) this.setSleeping(tag.getBoolean("Sleeping"));
        this.sleepingEntering = tag.getBoolean("SleepingEntering");
        this.sleepingExiting = tag.getBoolean("SleepingExiting");
        this.sleepTransitionTicks = Math.max(0, tag.getInt("SleepTransitionTicks"));
        this.sleepAmbientCooldownTicks = Math.max(0, tag.getInt("SleepAmbientCooldownTicks"));
        this.sleepReentryCooldownTicks = Math.max(0, tag.getInt("SleepReentryCooldownTicks"));
        this.sleepCancelTicks = Math.max(0, tag.getInt("SleepCancelTicks"));

        animationController.readFromNBT(tag);

        if (this.usingAirNav) {
            switchToAirNavigation();
        } else {
            switchToGroundNavigation();
        }

        // If we saved while flying, keep the dragon in the air briefly after load
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
    }

    // Rider takeoff window accessors for controllers
    public int getRiderTakeoffTicks() { return riderTakeoffTicks; }
    public void setRiderTakeoffTicks(int ticks) { this.riderTakeoffTicks = Math.max(0, ticks); }
    
    /**
     * Clears all states when mounting to ensure clean transition to rider control
     */
    private void clearAllStatesForMounting() {
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
     * Triggers the dodge animation - called when dragon dodges projectiles
     */
    public void triggerDodgeAnimation() {
        // Trigger native GeckoLib action key
        triggerAnim("action", "dodge");
    }

    // Note: We rely on GeckoLib triggerAnim(...) to play action clips.

    // ===== GECKOLIB =====
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Use entity-specific controller names to prevent animation bleeding between dragons
        // Update frequency: run every tick to maintain accurate keyframe timing
        AnimationController<LightningDragonEntity> movementController =
                new AnimationController<>(this, "movement", 1, animationController::handleMovementAnimation);
        AnimationController<LightningDragonEntity> bankingController =
                new AnimationController<>(this, "banking", 12, this::bankingPredicate);
        AnimationController<LightningDragonEntity> pitchingController =
                new AnimationController<>(this, "pitching", 12, this::pitchingPredicate);
        AnimationController<LightningDragonEntity> actionController =
                new AnimationController<>(this, "action", 0, this::actionPredicate);

        // Sound keyframes
        bankingController.setSoundKeyframeHandler(this::onAnimationSound);
        pitchingController.setSoundKeyframeHandler(this::onAnimationSound);
        movementController.setSoundKeyframeHandler(this::onAnimationSound);
        actionController.setSoundKeyframeHandler(this::onAnimationSound);

        // Register triggerable one-shots for server-side triggerAnim()
        registerVocalTriggers(actionController);
        // Register native keys for triggers
        actionController.triggerableAnim("lightning_bite",
                RawAnimation.begin().thenPlay("animation.lightning_dragon.lightning_bite"));
        actionController.triggerableAnim("horn_gore",
                RawAnimation.begin().thenPlay("animation.lightning_dragon.horn_gore"));
        actionController.triggerableAnim("dodge",
                RawAnimation.begin().thenPlay("animation.lightning_dragon.dodge"));
        actionController.triggerableAnim("lightning_beam",
                RawAnimation.begin().thenPlay("animation.lightning_dragon.lightning_beam"));
        // Summon Storm variants
        actionController.triggerableAnim("summon_storm_ground",
                RawAnimation.begin().thenPlay("animation.lightning_dragon.summon_storm_ground"));
        actionController.triggerableAnim("summon_storm_air",
                RawAnimation.begin().thenPlay("animation.lightning_dragon.summon_storm_air"));
        // Sleep transitions
        actionController.triggerableAnim("sleep_enter",
                RawAnimation.begin().thenPlay("animation.lightning_dragon.sleep_enter"));
        actionController.triggerableAnim("sleep_exit",
                RawAnimation.begin().thenPlay("animation.lightning_dragon.sleep_exit"));


        // Add controllers in order
        controllers.add(bankingController);
        controllers.add(pitchingController);
        controllers.add(movementController);
        controllers.add(actionController);
    }

    @Override
    public com.leon.saintsdragons.server.entity.ability.DragonAbilityType<?, ?> getPrimaryAttackAbility() {
        // Lightning Dragon alternates between bite and horn gore attacks
        // Use entity tick count to alternate between attacks (every 2 seconds)
        boolean useHornGore = (tickCount / 40) % 2 == 1; // Switch every 2 seconds
        return useHornGore ? 
            com.leon.saintsdragons.common.registry.LightningDragonAbilities.HORN_GORE : 
            com.leon.saintsdragons.common.registry.LightningDragonAbilities.BITE;
    }

    @Override
    public com.leon.saintsdragons.server.entity.ability.DragonAbilityType<?, ?> getRoarAbility() {
        return com.leon.saintsdragons.common.registry.LightningDragonAbilities.ROAR;
    }

    @Override
    public com.leon.saintsdragons.server.entity.ability.DragonAbilityType<?, ?> getSummonStormAbility() {
        return com.leon.saintsdragons.common.registry.LightningDragonAbilities.SUMMON_STORM;
    }

    private void onAnimationSound(SoundKeyframeEvent<LightningDragonEntity> event) {
        // Delegate all keyframed sounds to the sound handler
        // Pass the raw event data to the sound handler
        this.getSoundHandler().handleAnimationSound(this, event.getKeyframeData(), event.getController());
    }

    // No particle keyframe anchoring needed; beam origin uses computeHeadMouthOrigin

    private void registerVocalTriggers(AnimationController<LightningDragonEntity> action) {
        String[] keys = new String[] {
                "grumble1","grumble2","grumble3","purr","snort","chuff","content","annoyed",
                "growl_warning","roar","roar_ground","roar_air","hurt","die","lightning_bite","dodge"
        };
        for (String key : keys) {
            action.triggerableAnim(key, RawAnimation.begin().thenPlay("animation.lightning_dragon." + key));
        }
    }



    //PREDICATES

    private PlayState bankingPredicate(AnimationState<LightningDragonEntity> state) {
        state.getController().transitionLength(10);
        if (areRiderControlsLocked()) return PlayState.STOP;
        // Only apply banking during flight and when not sitting
        if (!isFlying() || isOrderedToSit()) return PlayState.STOP;

        // Exponential smoothing to avoid jitter
        float yawChange = getYRot() - yRotO;
        bankSmoothedYaw = bankSmoothedYaw * 0.85f + yawChange * 0.15f;

        // Hysteresis thresholds
        float enter = 0.25f;
        float exit = 0.12f;

        int desiredDir = bankDir;
        if (bankSmoothedYaw > enter) desiredDir = 1;
        else if (bankSmoothedYaw < -enter) desiredDir = -1;
        else if (Math.abs(bankSmoothedYaw) < exit) desiredDir = 0;

        // Minimum hold time to prevent flicker
        if (desiredDir != bankDir) {
            if (bankHoldTicks >= 8) {
                bankDir = desiredDir;
                bankHoldTicks = 0;
            } else {
                bankHoldTicks++;
            }
        } else {
            bankHoldTicks = Math.min(bankHoldTicks + 1, 20);
        }

        if (bankDir > 0) {
            state.setAndContinue(RawAnimation.begin().thenLoop("animation.lightning_dragon.banking_right"));
        } else if (bankDir < 0) {
            state.setAndContinue(RawAnimation.begin().thenLoop("animation.lightning_dragon.banking_left"));
        } else {
            state.setAndContinue(RawAnimation.begin().thenLoop("animation.lightning_dragon.banking_off"));
        }
        return PlayState.CONTINUE;
    }

    private PlayState pitchingPredicate(AnimationState<LightningDragonEntity> state) {
        state.getController().transitionLength(10);
        if (areRiderControlsLocked()) return PlayState.STOP;
        // Only apply pitching during flight and when not sitting
        if (!isFlying() || isOrderedToSit()) return PlayState.STOP;

        int desiredDir = pitchDir;

        if (this.isVehicle() && this.getControllingPassenger() instanceof Player) {
            if (isGoingUp()) {
                desiredDir = -1;  // Pitching up
            } else if (isGoingDown()) {
                desiredDir = 1;   // Pitching down
            } else {
                desiredDir = 0;   // No pitching
            }
        } else {
            float pitchChange = getXRot() - xRotO;
            pitchSmoothedPitch = pitchSmoothedPitch * 0.85f + pitchChange * 0.15f;

            // Hysteresis thresholds
            float enter = 0.25f;
            float exit = 0.12f;

            if (pitchSmoothedPitch > enter) desiredDir = 1;
            else if (pitchSmoothedPitch < -enter) desiredDir = -1;
            else if (Math.abs(pitchSmoothedPitch) < exit) desiredDir = 0;
        }

        // Minimum hold time to prevent flicker
        if (desiredDir != pitchDir) {
            if (pitchHoldTicks >= 8) {
                pitchDir = desiredDir;
                pitchHoldTicks = 0;
            } else {
                pitchHoldTicks++;
            }
        } else {
            pitchHoldTicks = Math.min(pitchHoldTicks + 1, 20);
        }

        if (pitchDir > 0) {
            state.setAndContinue(RawAnimation.begin().thenLoop("animation.lightning_dragon.pitching_down"));
        } else if (pitchDir < 0) {
            state.setAndContinue(RawAnimation.begin().thenLoop("animation.lightning_dragon.pitching_up"));
        } else {
            state.setAndContinue(RawAnimation.begin().thenLoop("animation.lightning_dragon.pitching_off"));
        }
        return PlayState.CONTINUE;
    }

    private PlayState actionPredicate(AnimationState<LightningDragonEntity> state) {
        // Native GeckoLib: controller idles until triggerAnim is fired
        state.getController().transitionLength(5);
        // If summoning (controls locked), force the summon clip variant to prevent bleed
        if (areRiderControlsLocked() && !isDying() && !isSleeping() && !sleepingEntering && !sleepingExiting) {
            String clip = isFlying() ?
                    "animation.lightning_dragon.summon_storm_air" :
                    "animation.lightning_dragon.summon_storm_ground";
            state.setAndContinue(RawAnimation.begin().thenPlay(clip));
            return PlayState.CONTINUE;
        }
        // If dying, force the death clip to hold until completion
        if (isDying()) {
            state.setAndContinue(RawAnimation.begin().thenPlay("animation.lightning_dragon.die"));
            return PlayState.CONTINUE;
        }
        // Sleep transitions and loop
        if (sleepingEntering) {
            state.setAndContinue(RawAnimation.begin().thenPlay("animation.lightning_dragon.sleep_enter"));
            return PlayState.CONTINUE;
        }
        if (isSleeping()) {
            state.setAndContinue(RawAnimation.begin().thenLoop("animation.lightning_dragon.sleep"));
            return PlayState.CONTINUE;
        }
        if (sleepingExiting) {
            state.setAndContinue(RawAnimation.begin().thenPlay("animation.lightning_dragon.sleep_exit"));
            return PlayState.CONTINUE;
        }
        // No sleep state: stop action controller to clear any lingering sleep clip
        return PlayState.STOP;
    }

    //NO MORE PREDICATES
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

    // While > 0, rider input is ignored to keep action animation coherent (e.g., roar)
    private int riderControlLockTicks = 0;
    public boolean areRiderControlsLocked() { return riderControlLockTicks > 0; }
    private void tickRiderControlLock() { if (riderControlLockTicks > 0) riderControlLockTicks--; }
    public void lockRiderControls(int ticks) { this.riderControlLockTicks = Math.max(this.riderControlLockTicks, Math.max(0, ticks)); }

    // While > 0, only takeoff is locked (allows running/movement during roar)
    private int takeoffLockTicks = 0;
    public boolean isTakeoffLocked() { return takeoffLockTicks > 0; }
    public void lockTakeoff(int ticks) { this.takeoffLockTicks = Math.max(this.takeoffLockTicks, Math.max(0, ticks)); }
    private void tickTakeoffLock() { if (takeoffLockTicks > 0) takeoffLockTicks--; }

    // ===== RECENT AGGRO TRACKING (for roar lightning targeting) =====
    private final java.util.Map<Integer, Long> recentAggroIds = new java.util.concurrent.ConcurrentHashMap<>();
    private static final int AGGRO_TTL_TICKS = 200; // ~10s

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
    public float getEyeHeight(@NotNull Pose pose) {
        // Always use dynamically calculated eye height when available
        EntityDimensions dimensions = getDimensions(pose);
        return dimensions.height * 0.6f;
    }

    @Override
    protected float getStandingEyeHeight(@NotNull Pose pose, @NotNull EntityDimensions dimensions) {
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
    @Nullable
    public AgeableMob getBreedOffspring(net.minecraft.server.level.@NotNull ServerLevel level, @NotNull AgeableMob otherParent) {
        return null;
    }

    // ===== RIDING INPUT IMPLEMENTATION =====
    @Override
    protected @NotNull Vec3 getRiddenInput(@NotNull Player player, @NotNull Vec3 deltaIn) {
        if (areRiderControlsLocked()) {
            // Ignore rider strafe/forward while locked
            return net.minecraft.world.phys.Vec3.ZERO;
        }
        Vec3 input = riderController.getRiddenInput(player, deltaIn);
        // On the server, mirror inputs into synced data so observers get correct walk/run
        if (!level().isClientSide && !isFlying()) {
            float fwd = (float) Mth.clamp(input.z, -1.0, 1.0);
            float str = (float) Mth.clamp(input.x, -1.0, 1.0);
            this.entityData.set(DATA_RIDER_FORWARD, Math.abs(fwd) > 0.02f ? fwd : 0f);
            this.entityData.set(DATA_RIDER_STRAFE, Math.abs(str) > 0.02f ? str : 0f);
        }
        return input;
    }
    @Override
    protected void tickRidden(@NotNull Player player, @NotNull Vec3 travelVector) {
        super.tickRidden(player, travelVector);
        // Decrement locks on server authority
        if (!this.level().isClientSide) {
            tickRiderControlLock();
            tickTakeoffLock();
        }
        if (!areRiderControlsLocked()) {
            riderController.tickRidden(player, travelVector);
        } else {
            // While locked, keep rider safe and aligned but do not apply rider-driven yaw/pitch changes
            player.fallDistance = 0.0F;
            this.fallDistance = 0.0F;
            this.setTarget(null);
            // Keep body/head coherent
            this.yBodyRot = this.getYRot();
            this.yHeadRot = this.getYRot();
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
    protected float getRiddenSpeed(@NotNull Player rider) {
        if (areRiderControlsLocked()) {
            return 0.0F;
        }
        return riderController.getRiddenSpeed(rider);
    }

    @Override
    protected void removePassenger(@NotNull Entity passenger) {
        // Prevent dismounting while rider controls are locked (e.g., Summon Storm windup)
        if (areRiderControlsLocked() && passenger == getControllingPassenger()) {
            return;
        }
        super.removePassenger(passenger);
        // Reset rider-driven movement states immediately on dismount
        if (!this.level().isClientSide) {
            this.setAccelerating(false);
            this.setRunning(false);
            this.entityData.set(DATA_RIDER_FORWARD, 0f);
            this.entityData.set(DATA_RIDER_STRAFE, 0f);
            this.entityData.set(DATA_GROUND_MOVE_STATE, 0);
            // Nudge observers so animation stops if we dismounted mid-run/walk
            com.leon.saintsdragons.common.network.NetworkHandler.INSTANCE.send(
                    net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> this),
                    new com.leon.saintsdragons.common.network.MessageDragonAnimState(this.getId(), (byte) 0, (byte) getSyncedFlightMode())
            );
        }
    }
    // Cooldown for aggro growl to prevent spam while ridden or under repeated retargeting
    private int aggroGrowlCooldown = 0;

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        LivingEntity previousTarget = this.getTarget();
        super.setTarget(target);

        if (!this.level().isClientSide) {
            // Decrement here too in case tick() hasn't yet
            if (aggroGrowlCooldown > 0) aggroGrowlCooldown--;

            // Play growl when entering combat from idle, but throttle and avoid while being ridden
            if (target != null && previousTarget == null && aggroGrowlCooldown <= 0) {
                // Suppress frequent growls when mounted; lengthen cooldown if mounted
                if (!this.isVehicle() && !isStayOrSitMuted()) {
                    this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                            ModSounds.DRAGON_GROWL_WARNING.get(), SoundSource.HOSTILE,
                            1.2f, 0.8f + this.random.nextFloat() * 0.4f);
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

    // Prevent dragon and its riders from taking fall damage when mounted/landing
    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, @NotNull DamageSource source) {
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
        return 8.0f; // Lightning dragon melee damage
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
        if (attacking) {
            setBeaming(true);
        } else {
            setBeaming(false);
        }
    }
    
    // ===== DRAGON FLIGHT CAPABLE INTERFACE =====
    // Note: Most flight methods already exist in LightningDragonEntity
    
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
            true,  // canSleepAtNight
            true,  // canSleepDuringDay
            true,  // requiresShelter
            false, // avoidsThunderstorms (Lightning Dragons like storms!)
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
            level().addParticle(new LightningStormData(1.0f), 
                position.x, position.y, position.z, 0.0, 0.0, 0.0);
        }
    }
    
    public void playSleepAnimation() {
        // Trigger sleep animation
        triggerAnim("sleep_controller", "sleep");
    }
    
    public void playWakeAnimation() {
        // Trigger wake animation
        triggerAnim("sleep_controller", "wake");
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
}
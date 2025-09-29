/**
 * My name's Zap Van Dink. I'm a lightning dragon.
 */
package com.leon.saintsdragons.server.entity.dragons.lightningdragon;

//Custom stuff
import com.leon.saintsdragons.common.particle.lightningdragon.LightningArcData;
import com.leon.saintsdragons.common.particle.lightningdragon.LightningStormData;
import com.leon.saintsdragons.common.registry.lightningdragon.LightningDragonAbilities;
import com.leon.saintsdragons.server.ai.goals.lightningdragon.LightningDragonDodgeGoal;
import com.leon.saintsdragons.server.ai.goals.lightningdragon.LightningDragonFlightGoal;
import com.leon.saintsdragons.server.ai.goals.lightningdragon.LightningDragonFollowOwnerGoal;
import com.leon.saintsdragons.server.ai.goals.lightningdragon.LightningDragonGroundWanderGoal;
import com.leon.saintsdragons.server.ai.goals.lightningdragon.LightningDragonPanicGoal;
import com.leon.saintsdragons.server.ai.goals.lightningdragon.LightningDragonTemptGoal;
import com.leon.saintsdragons.server.ai.goals.lightningdragon.*;
import com.leon.saintsdragons.server.ai.navigation.DragonFlightMoveHelper;
import com.leon.saintsdragons.server.entity.controller.lightningdragon.LightningDragonPhysicsController;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.interfaces.DragonCombatCapable;
import com.leon.saintsdragons.server.entity.interfaces.DragonFlightCapable;
import com.leon.saintsdragons.server.entity.interfaces.DragonSleepCapable;
import com.leon.saintsdragons.server.entity.interfaces.ShakesScreen;
import com.leon.saintsdragons.server.entity.controller.lightningdragon.LightningDragonFlightController;
import com.leon.saintsdragons.server.entity.handler.DragonInteractionHandler;
import com.leon.saintsdragons.server.entity.handler.DragonKeybindHandler;
import com.leon.saintsdragons.server.entity.dragons.lightningdragon.handlers.LightningDragonInteractionHandler;
import com.leon.saintsdragons.server.entity.dragons.lightningdragon.handlers.LightningDragonAnimationHandler;
import static com.leon.saintsdragons.server.entity.dragons.lightningdragon.handlers.LightningDragonConstantsHandler.*;
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
import org.jetbrains.annotations.NotNull;
import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

//Just everything
public class LightningDragonEntity extends RideableDragonBase implements FlyingAnimal, RangedAttackMob,
        DragonCombatCapable, DragonFlightCapable, DragonSleepCapable, ShakesScreen {
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
    
    // ===== NEW ATTACK STATE SYSTEM (Cataclysm-style) =====
    /** Attack state timing counter */
    public int attackTicks = 0;
    
    /** Attack cooldown timer */
    public int attackCooldown = 0;
    private boolean allowGroundBeamDuringStorm = false;
    // Sleep transition state
    public boolean sleepingEntering = false;
    public boolean sleepingExiting = false;
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
    public final LightningDragonFlightController flightController;
    public final DragonInteractionHandler interactionHandler;
    private final LightningDragonInteractionHandler lightningInteractionHandler;
    private final LightningDragonAnimationHandler animationHandler;

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
        this.flightController = new LightningDragonFlightController(this);
        this.interactionHandler = new DragonInteractionHandler(this);
        this.lightningInteractionHandler = new LightningDragonInteractionHandler(this);
        this.animationHandler = new LightningDragonAnimationHandler(this);

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
    protected void playStepSound(@Nonnull BlockPos pos, @Nonnull BlockState state) {
        // Intentionally empty — step sounds are driven by GeckoLib keyframes (step1/step2)
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 180.0D)
                .add(Attributes.MOVEMENT_SPEED, WALK_SPEED)
                .add(Attributes.FOLLOW_RANGE, 80.0D)
                .add(Attributes.FLYING_SPEED, 1.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
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
        // Define all rideable dragon data keys for LightningDragonEntity
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
        return getEyePosition();
    }
    @Override
    public Vec3 getMouthPosition() {
        // Use the proven computeHeadMouthOrigin method - it does everything correctly!
        return computeHeadMouthOrigin(1.0f);
    }
    /**
     * Compute a mouth origin in world space from head yaw/pitch and a fixed local offset.
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
        setBooleanData(DATA_BEAMING, beaming);
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
     * Check if the dragon is currently in an attack state (Cataclysm-style)
     */
    public boolean isInAttackState() {
        return getAttackState() != ATTACK_STATE_IDLE;
    }
    
    /**
     * Check if the dragon can start a new attack (not on cooldown)
     */
    public boolean canAttack() {
        return attackCooldown <= 0 && !isInAttackState();
    }

    // ===== Lightning Dragon Specific Methods =====
    
    // Flight mode accessor for controllers (avoids accessing protected entityData outside entity)
    public int getSyncedFlightMode() { return getIntegerData(DATA_FLIGHT_MODE); }

    // Debug/inspection helper: expose raw ground move state
    public int getGroundMoveState() { return getIntegerData(DATA_GROUND_MOVE_STATE); }
    
    // ===== RideableDragonBase Abstract Method Implementations =====
    
    @Override
    protected int getFlightMode() {
        if (!isFlying()) return -1; // Ground state
        if (isTakeoff()) return 3;  // Takeoff
        if (isHovering()) return 2; // Hover
        if (isLanding()) return 2;  // Landing (treat as hover)
        
        // Check if gliding (moving horizontally without significant vertical movement)
        double yDelta = this.getY() - this.yo;
        double horizontalSpeed = getDeltaMovement().horizontalDistanceSqr();
        if (Math.abs(yDelta) < 0.06 && horizontalSpeed > 0.01) {
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
        }
        
        // Update banking and pitching logic every tick
        tickBankingLogic();
        tickPitchingLogic();

        // Wake up if mounted or target appears/aggression
        if (!level().isClientSide && (isSleeping() || sleepingEntering || sleepingExiting)) {
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
        if (!level().isClientSide && !(isSleeping() || sleepingEntering || sleepingExiting)) {
            super.tickAnimationStates();
        }

        // Handle dodge movement first
        if (!level().isClientSide && this.isDodging()) {
            handleDodgeMovement();
            return;
        }

        tickRunningTime();

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
        // Clear sitting state if the dragon is being ridden
        if (!this.level().isClientSide && this.isVehicle() && this.isOrderedToSit()) {
            this.setOrderedToSit(false);
        }
    }
    
    private boolean wasVehicleLastTick = false;
    
    private void tickMountingState() {
        // Check if dragon just became a vehicle and clear all states
        if (!this.level().isClientSide && this.isVehicle() && !wasVehicleLastTick) {
            clearAllStatesWhenMounted();
        }
        wasVehicleLastTick = this.isVehicle();
    }
    
    /**
     * Clears all dragon states (sleep, sit) when mounted to ensure full player control
     */
    private void clearAllStatesWhenMounted() {
        if (!this.level().isClientSide && this.isVehicle()) {
            // Clear sleep states aggressively - including any ongoing transitions
            wakeUpImmediately();
            
            // Clear sitting state and sync command value
            if (this.isOrderedToSit()) {
                this.setOrderedToSit(false);
                // If dragon was sitting, set command to Follow (0) when mounted
                if (this.getCommand() == 1) {
                    this.setCommand(0);
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
            combatManager.tick();
        }
        interactionHandler.updateSittingProgress();
    }
    
    private void tickRiderControlLockMovement() {
        // While rider controls are locked (e.g., Summon Storm windup), freeze movement and AI
        if (areRiderControlsLocked()) {
            this.getNavigation().stop();
            this.setTarget(null);
            this.setDeltaMovement(0, 0, 0);
        }
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
    }
    
    private void tickSleepCooldowns() {
        if (sleepAmbientCooldownTicks > 0) sleepAmbientCooldownTicks--;
        if (sleepReentryCooldownTicks > 0) sleepReentryCooldownTicks--;
        if (sleepCancelTicks > 0) sleepCancelTicks--;
    }
    
    private void tickBankingLogic() {
        // Reset banking when not flying or when controls are locked - INSTANT reset
        if (areRiderControlsLocked() || !isFlying() || isOrderedToSit()) {
            if (bankDir != 0) {
                bankDir = 0;
                bankSmoothedYaw = 0f;
                bankHoldTicks = 0;
            }
            return;
        }
        
        // Exponential smoothing to avoid jitter
        float yawChange = getYRot() - yRotO;
        bankSmoothedYaw = bankSmoothedYaw * 0.85f + yawChange * 0.15f;
        float enter = 1.0f; 
        float exit = 5.0f;

        int desiredDir = bankDir;
        if (bankSmoothedYaw > enter) desiredDir = 1;
        else if (bankSmoothedYaw < -enter) desiredDir = -1;
        else if (Math.abs(bankSmoothedYaw) < exit) desiredDir = 0;  // banking_off when flying straight
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
    
    private void tickPitchingLogic() {
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
    protected void playHurtSound(@Nonnull net.minecraft.world.damagesource.DamageSource source) {
        if (isDying()) {
            return;
        }
        // Throttle hurt sound to avoid spam while ridden or under rapid damage
        if (hurtSoundCooldown > 0) {
            return;
        }
        // Custom: activate one-shot hurt ability (plays sound + animation once)
        if (!level().isClientSide) {
            this.tryActivateAbility(LightningDragonAbilities.HURT);
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
        this.goalSelector.addGoal(1, new LightningDragonPanicGoal(this));
        this.goalSelector.addGoal(2, new LightningDragonDodgeGoal(this));
        this.goalSelector.addGoal(5, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(6, new FloatGoal(this));

        // Combat goals (prioritized to avoid conflicts)
        // Using new Cataclysm-style separated goal system
        this.goalSelector.addGoal(7, new LightningDragonAirCombatGoal(this));      // Air combat + flight decision
        
        // New separated combat system (ensure combat decision outranks chase)
        this.goalSelector.addGoal(3, new LightningDragonCombatGoal(this));          // Attack coordination
        this.goalSelector.addGoal(4, new LightningDragonMoveGoal(this, true, 1.4)); // Pure movement - yields when attacking
        
        // Attack execution goals (high priority, interrupt movement)
        this.goalSelector.addGoal(10, new LightningDragonAttackGoal(this, ATTACK_STATE_HORN_WINDUP, ATTACK_STATE_HORN_WINDUP, 15, 10, 4.0f));
        this.goalSelector.addGoal(10, new LightningDragonAttackGoal(this, ATTACK_STATE_BITE_WINDUP, ATTACK_STATE_BITE_WINDUP, 11, 8, 3.0f));
        this.goalSelector.addGoal(10, new LightningDragonAttackGoal(this, ATTACK_STATE_HORN_ACTIVE, ATTACK_STATE_HORN_ACTIVE, 5, 5, 4.0f));
        this.goalSelector.addGoal(10, new LightningDragonAttackGoal(this, ATTACK_STATE_BITE_ACTIVE, ATTACK_STATE_BITE_ACTIVE, 3, 3, 3.0f));
        this.goalSelector.addGoal(10, new LightningDragonAttackGoal(this, ATTACK_STATE_RECOVERY, ATTACK_STATE_RECOVERY, 5, 5, 4.0f));
        
        // State transition goals
        this.goalSelector.addGoal(11, new LightningDragonStateGoal(this, ATTACK_STATE_HORN_WINDUP, ATTACK_STATE_HORN_WINDUP, ATTACK_STATE_HORN_ACTIVE, 10, 10));
        this.goalSelector.addGoal(11, new LightningDragonStateGoal(this, ATTACK_STATE_BITE_WINDUP, ATTACK_STATE_BITE_WINDUP, ATTACK_STATE_BITE_ACTIVE, 8, 8));
        this.goalSelector.addGoal(11, new LightningDragonStateGoal(this, ATTACK_STATE_HORN_ACTIVE, ATTACK_STATE_HORN_ACTIVE, ATTACK_STATE_RECOVERY, 5, 5));
        this.goalSelector.addGoal(11, new LightningDragonStateGoal(this, ATTACK_STATE_BITE_ACTIVE, ATTACK_STATE_BITE_ACTIVE, ATTACK_STATE_RECOVERY, 3, 3));
        this.goalSelector.addGoal(11, new LightningDragonStateGoal(this, ATTACK_STATE_RECOVERY, ATTACK_STATE_RECOVERY, ATTACK_STATE_IDLE, 5, 5));

        // Movement/idle
        // Unified sleep goal: high priority to preempt follow/wander, but calm() prevents overriding combat/aggro
        this.goalSelector.addGoal(0, new LightningDragonSleepGoal(this));         // Higher priority than follow
        this.goalSelector.addGoal(8, new LightningDragonFollowOwnerGoal(this));   // Lower priority than combat
        this.goalSelector.addGoal(9, new LightningDragonGroundWanderGoal(this, 1.0, 60)); // Lower priority than combat
        
        // Item pickup behavior (like foxes eating berries) + ground fish taming
        this.goalSelector.addGoal(10, new LightningDragonTemptGoal(this, 1.2, 
                net.minecraft.world.item.crafting.Ingredient.of(net.minecraft.world.item.Items.SALMON, 
                                                               net.minecraft.world.item.Items.COD, 
                                                               net.minecraft.world.item.Items.TROPICAL_FISH, 
                                                               net.minecraft.world.item.Items.PUFFERFISH), false));
        
        this.goalSelector.addGoal(11, new LightningDragonFlightGoal(this));
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
                this.tryActivateAbility(LightningDragonAbilities.DIE);
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
    public boolean isInvulnerableTo(@Nonnull DamageSource source) {
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
    public void thunderHit(@Nonnull ServerLevel level, @Nonnull LightningBolt lightning) {
        // Do not call super; ignore ignition and effects
        if (this.isOnFire()) this.clearFire();
    }

    public boolean isDying() {
        return dying;
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
     * Gets the current pitch direction for animation purposes
     * @return -1 for up, 0 for none, 1 for down
     */
    public int getPitchDirection() {
        return pitchDir;
    }
    
    /**
     * Checks if the dragon is currently summoning (controls locked for ability)
     * @return true if summoning
     */
    public boolean isSummoning() {
        return false;
    }
    public void setDying(boolean dying) {
        this.dying = dying;
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
        return getBooleanData(DATA_SLEEPING);
    }
    public void setSleeping(boolean sleeping) {
        setBooleanData(DATA_SLEEPING, sleeping);
    }
    public boolean isSleepTransitioning() {
        return sleepingEntering || sleepingExiting;
    }
    public boolean isSleepLocked() {
        return sleepLocked || isSleeping() || sleepingEntering || sleepingExiting;
    }

    private void enterSleepLock() {
        if (!sleepLocked) {
            sleepLocked = true;
            sleepCommandSnapshot = this.getCommand();
        }
        this.setCommand(1);
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
                this.setCommand(1);
                this.setOrderedToSit(true);
            } else {
                this.setCommand(desired);
                this.setOrderedToSit(false);
            }
        }
        this.getNavigation().stop();
        this.setRunning(false);
        this.setGroundMoveStateFromAI(0);
    }

    public void startSleepEnter() {
        if (isSleeping() || sleepingEntering || sleepingExiting) return;
        sleepingEntering = true;
        sleepTransitionTicks = 81;
        animationHandler.triggerSleepEnter();
        if (!level().isClientSide) {
            enterSleepLock();
        }
    }

    public void startSleepExit() {
        if ((!isSleeping() && !sleepingEntering) || sleepingExiting) return;
        this.entityData.set(DATA_SLEEPING, false);
        sleepingEntering = false;
        sleepingExiting = true;
        sleepTransitionTicks = 122;
        animationHandler.triggerSleepExit();
        if (!level().isClientSide) {
            suppressSleep(20);
            releaseSleepLock();
        }
    }

    public void wakeUpImmediately() {
        this.entityData.set(DATA_SLEEPING, false);
        sleepingEntering = false;
        sleepingExiting = false;
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

        // Save lock states
        tag.putInt("RiderControlLockTicks", riderControlLockTicks);
        tag.putInt("TakeoffLockTicks", takeoffLockTicks);

        // Persist combat cooldowns
        this.combatManager.saveToNBT(tag);

        // Persist supercharge timer so logout/login doesn't clear buff early
        tag.putInt("SuperchargeTicks", Math.max(0, this.superchargeTicks));

        // Persist temporary invulnerability timer (e.g., during Summon Storm windup)
        tag.putInt("TempInvulnTicks", Math.max(0, this.tempInvulnTicks));
        tag.putBoolean("AllowGroundBeamStorm", this.allowGroundBeamDuringStorm);

        // Persist sleep state and transition timers
        tag.putBoolean("Sleeping", this.isSleeping());
        tag.putBoolean("SleepingEntering", this.sleepingEntering);
        tag.putBoolean("SleepingExiting", this.sleepingExiting);
        tag.putInt("SleepTransitionTicks", Math.max(0, this.sleepTransitionTicks));
        tag.putInt("SleepAmbientCooldownTicks", Math.max(0, this.sleepAmbientCooldownTicks));
        tag.putInt("SleepReentryCooldownTicks", Math.max(0, this.sleepReentryCooldownTicks));
        tag.putInt("SleepCancelTicks", Math.max(0, this.sleepCancelTicks));
        tag.putBoolean("SleepLock", this.sleepLocked);
        tag.putInt("SleepCommandSnapshot", this.sleepCommandSnapshot);

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

        if (tag.contains("AllowGroundBeamStorm")) {
            this.allowGroundBeamDuringStorm = tag.getBoolean("AllowGroundBeamStorm");
        }

        // Restore sleep state and transition timers
        if (tag.contains("Sleeping")) this.setSleeping(tag.getBoolean("Sleeping"));
        this.sleepingEntering = tag.getBoolean("SleepingEntering");
        this.sleepingExiting = tag.getBoolean("SleepingExiting");
        this.sleepTransitionTicks = Math.max(0, tag.getInt("SleepTransitionTicks"));
        this.sleepAmbientCooldownTicks = Math.max(0, tag.getInt("SleepAmbientCooldownTicks"));
        this.sleepReentryCooldownTicks = Math.max(0, tag.getInt("SleepReentryCooldownTicks"));
        this.sleepCancelTicks = Math.max(0, tag.getInt("SleepCancelTicks"));
        this.sleepLocked = tag.getBoolean("SleepLock");
        this.sleepCommandSnapshot = tag.getInt("SleepCommandSnapshot");

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
            // If dragon was sitting, set command to Follow (0) when mounted
            if (this.getCommand() == 1) {
                this.setCommand(0);
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
     * Triggers the dodge animation - called when dragon dodges projectiles
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
        AnimationController<LightningDragonEntity> movementController =
                new AnimationController<>(this, "movement", 1, animationController::handleMovementAnimation);
        AnimationController<LightningDragonEntity> bankingController =
                new AnimationController<>(this, "banking", 8, animationHandler::bankingPredicate);
        AnimationController<LightningDragonEntity> pitchingController =
                new AnimationController<>(this, "pitching", 6, animationHandler::pitchingPredicate);
        AnimationController<LightningDragonEntity> actionController =
                new AnimationController<>(this, "action", 0, animationHandler::actionPredicate);

        // Sound keyframes
        bankingController.setSoundKeyframeHandler(this::onAnimationSound);
        pitchingController.setSoundKeyframeHandler(this::onAnimationSound);
        movementController.setSoundKeyframeHandler(this::onAnimationSound);
        actionController.setSoundKeyframeHandler(this::onAnimationSound);

        // Setup animation triggers via animation handler
        animationHandler.setupActionController(actionController);


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
            LightningDragonAbilities.HORN_GORE :
            LightningDragonAbilities.BITE;
    }

    @Override
    public com.leon.saintsdragons.server.entity.ability.DragonAbilityType<?, ?> getRoarAbility() {
        return LightningDragonAbilities.ROAR;
    }

    @Override
    public com.leon.saintsdragons.server.entity.ability.DragonAbilityType<?, ?> getSummonStormAbility() {
        return LightningDragonAbilities.SUMMON_STORM;
    }

    public void onAnimationSound(SoundKeyframeEvent<LightningDragonEntity> event) {
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
    @Nullable
    public AgeableMob getBreedOffspring(@Nonnull net.minecraft.server.level.ServerLevel level, @Nonnull AgeableMob otherParent) {
        return null;
    }

    // ===== RIDING INPUT IMPLEMENTATION =====
    @Override
    public @NotNull Vec3 getRiddenInput(@Nonnull Player player, @Nonnull Vec3 deltaIn) {
        if (areRiderControlsLocked()) {
            // Ignore rider strafe/forward while locked
            return net.minecraft.world.phys.Vec3.ZERO;
        }
        Vec3 input = riderController.getRiddenInput(player, deltaIn);
        // Call parent implementation to handle standard rideable dragon input processing
        return super.getRiddenInput(player, input);
    }
    @Override
    protected void tickRidden(@Nonnull Player player, @Nonnull Vec3 travelVector) {
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
        // Call parent implementation to handle standard rideable dragon cleanup
        super.removePassenger(passenger);
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
            level().addParticle(new LightningStormData(1.0f), 
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

    // ===== WATER CONDUCTIVITY SYSTEM =====
    
    /**
     * Check if the dragon is currently underwater or in water
     */
    public boolean isInWater() {
        return super.isInWater() || this.level().getBlockState(this.blockPosition()).getFluidState().is(net.minecraft.world.level.material.Fluids.WATER);
    }
    
    /**
     * Check if this dragon can be bound (not playing dead, not sleeping, etc.)
     */
    public boolean canBeBound() {
        return !isSleeping() && !isDying() && !isCharging() && !isBeaming();
    }
    
    /**
     * Get water conductivity multiplier for lightning damage
     * Water conducts electricity better, so lightning is more effective underwater
     */
    public float getWaterConductivityMultiplier() {
        if (isInWater()) {
            return 1.5f; // 50% damage boost underwater
        }
        return 1.0f; // Normal damage on land/air
    }
    
    /**
     * Get water conductivity multiplier for lightning range/radius
     * Water allows lightning to spread further
     */
    public double getWaterRangeMultiplier() {
        if (isInWater()) {
            return 1.3; // 30% range boost underwater
        }
        return 1.0; // Normal range on land/air
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
        // This is important for dragon riding scenarios
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
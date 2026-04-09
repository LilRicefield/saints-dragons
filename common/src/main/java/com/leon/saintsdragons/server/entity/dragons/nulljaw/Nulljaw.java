package com.leon.saintsdragons.server.entity.dragons.nulljaw;

import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.server.ai.goals.base.DragonDirectAirCombatMovementHelper;
import com.leon.saintsdragons.server.ai.goals.base.DragonFollowOwnerGoal;
import com.leon.saintsdragons.server.ai.goals.base.DragonPackFollowLeaderGoal;
import com.leon.saintsdragons.server.ai.goals.nulljaw.NulljawFloatGoal;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.handlers.NulljawAnimationHandler;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.handlers.NulljawSoundProfile;
import com.leon.saintsdragons.server.entity.handler.DragonSoundHandler;
import com.leon.saintsdragons.server.entity.interfaces.DragonFlightCapable;
import com.leon.saintsdragons.server.entity.interfaces.PackMember;
import com.leon.saintsdragons.server.entity.interfaces.DragonSoundProfile;
import com.leon.saintsdragons.server.entity.interfaces.SoundHandledDragon;
import com.leon.saintsdragons.server.flight.DragonFlightVisuals;
import com.leon.saintsdragons.server.flight.DragonRiderSeat;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Nulljaw extends RideableDragonBase implements DragonFlightCapable, SoundHandledDragon, PackMember<Nulljaw> {
    private static final int MIN_AMBIENT_DELAY = 220;
    private static final int MAX_AMBIENT_DELAY = 420;
    private static final int TAME_CHANCE_DENOMINATOR = 5;
    private static final int DEATH_SOUND_DURATION_TICKS = 44;
    private static final double RIDER_FLIGHT_SPEED = 0.32D;
    private static final double RIDER_ASCEND_SPEED = 0.16D;
    private static final double RIDER_DESCEND_SPEED = 0.18D;
    private static final double AI_CRUISE_SPEED = 0.24D;
    private static final double FLOAT_ACCEL = 0.10D;
    private static final double FLOAT_DRAG = 0.96D;
    private static final float BABY_HITBOX_SCALE = 0.55F;
    private static final int HOVER_VISUAL_GRACE_TICKS = 8;
    private static final double MOUNTED_HOVER_VISUAL_THRESHOLD_SQ = 0.010D;
    private static final int MAX_PACK_SIZE = 4;
    private static final double PACK_SEARCH_RADIUS = 28.0D;
    private static final EntityDataAccessor<Boolean> DATA_HOVER_VISUAL_ACTIVE =
            SynchedEntityData.defineId(Nulljaw.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_FLIGHT_PITCH =
            SynchedEntityData.defineId(Nulljaw.class, EntityDataSerializers.FLOAT);

    private static final Map<String, VocalEntry> VOCAL_ENTRIES = new VocalEntryBuilder()
            .add("grumble1", "actions", "animation.nulljaw.grumble1", ModSounds.NULLJAW_GRUMBLE_1, 1.0f, 1.0f, 0.08f, true, true, false)
            .add("grumble2", "actions", "animation.nulljaw.grumble2", ModSounds.NULLJAW_GRUMBLE_2, 1.0f, 1.0f, 0.08f, true, true, false)
            .add("grumble3", "actions", "animation.nulljaw.grumble3", ModSounds.NULLJAW_GRUMBLE_3, 1.0f, 1.0f, 0.08f, true, true, false)
            .add("eat", "actions", "animation.nulljaw.eat", ModSounds.NULLJAW_EAT, 1.0f, 1.0f, 0.02f, true, true, false)
            .add("nulljaw_hurt", "instant", "animation.nulljaw.hurt", ModSounds.NULLJAW_HURT, 1.1f, 1.0f, 0.04f, true, true, true)
            .add("nulljaw_die", "instant", "animation.nulljaw.die", ModSounds.NULLJAW_DIE, 1.2f, 1.0f, 0.0f, true, true, true)
            .build();

    private final AnimatableInstanceCache dragonCache = GeckoLibUtil.createInstanceCache(this);
    private final NulljawAnimationHandler animationHandler = new NulljawAnimationHandler(this);
    private final DragonSoundHandler soundHandler = new DragonSoundHandler(this);
    private final DragonFlightVisuals.State flightVisualState = new DragonFlightVisuals.State();
    private final Map<String, Vec3> clientLocatorCache = new ConcurrentHashMap<>();

    @Nullable
    private UUID packLeaderUuid;
    private int ambientSoundTimer;
    private int nextAmbientSoundDelay;
    private int hoverVisualTicks;
    private boolean running;
    private boolean hurtSoundQueued;
    private boolean deathSoundQueued;

    public Nulljaw(EntityType<? extends Nulljaw> type, Level level) {
        super(type, level);
        this.setRideable();
        this.setFlying(true);
        this.setHovering(false);
        this.setTakeoff(false);
        this.setLanding(false);
        this.setNoGravity(true);
        RandomSource rng = this.getRandom();
        this.ambientSoundTimer = rng.nextInt(80);
        this.nextAmbientSoundDelay = MIN_AMBIENT_DELAY + rng.nextInt(Math.max(1, MAX_AMBIENT_DELAY - MIN_AMBIENT_DELAY + 1));
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
        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance().getConfig(DragonAttributeConfigLoader.NULLJAW_ID);
        setAttributeBase(Attributes.MAX_HEALTH, config.maxHealth());
        setAttributeBase(Attributes.FLYING_SPEED, config.flyingSpeed());
        setAttributeBase(Attributes.ARMOR, config.armor());

        if (this.getHealth() > this.getMaxHealth()) {
            this.setHealth(this.getMaxHealth());
        }
    }

    private void setAttributeBase(Attribute attribute, double value) {
        AttributeInstance instance = this.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    public static boolean canSpawnHere(EntityType<? extends Nulljaw> type,
                                       LevelAccessor level,
                                       MobSpawnType spawnType,
                                       BlockPos pos,
                                       RandomSource random) {
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
        return stateAt.getCollisionShape(level, pos).isEmpty()
                && stateAbove.getCollisionShape(level, pos.above()).isEmpty();
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
    protected void defineRideableDragonData() {
        this.entityData.define(DATA_HOVER_VISUAL_ACTIVE, false);
        this.entityData.define(DATA_FLIGHT_PITCH, 0f);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(1, new DragonFollowOwnerGoal<>(this, DragonFollowOwnerGoal.FollowConfig.forVolitans()) {
            @Override
            protected void updateFlightState(LivingEntity owner, boolean shouldFly, boolean ownerAirborne, double distance) {
                Nulljaw.this.setFlying(true);
                Nulljaw.this.setTakeoff(false);
                Nulljaw.this.setHovering(false);
                Nulljaw.this.setLanding(false);
            }

            @Override
            protected void startFollowTakeoff() {
                Nulljaw.this.setFlying(true);
                Nulljaw.this.setTakeoff(false);
                Nulljaw.this.setHovering(false);
                Nulljaw.this.setLanding(false);
            }

            @Override
            protected void handleFlightFollowing(LivingEntity owner, boolean ownerAirborne) {
                Nulljaw.this.setFlying(true);
                Nulljaw.this.setTakeoff(false);
                Nulljaw.this.setHovering(false);
                Nulljaw.this.setLanding(false);
                Vec3 targetPos = getFlightFollowTarget(owner, true);
                Nulljaw.this.flyToward(targetPos, getFlightFollowSpeed());
            }
        });
        this.goalSelector.addGoal(2, new DragonPackFollowLeaderGoal<>(this, Nulljaw.class, 0.9D, 18.0D, 9.0D));
        this.goalSelector.addGoal(3, new NulljawFloatGoal(this));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 4, animationHandler::movementPredicate));
        AnimationController<Nulljaw> actions = new AnimationController<>(this, "actions", 2, animationHandler::actionPredicate);
        animationHandler.setupActionController(actions);
        controllers.add(actions);
        controllers.add(new AnimationController<>(this, "mounted", 2, animationHandler::mountedPredicate));
        AnimationController<Nulljaw> instant = new AnimationController<>(this, "instant", 1, animationHandler::instantPredicate);
        animationHandler.setupInstantController(instant);
        controllers.add(instant);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return dragonCache;
    }

    @Override
    public DragonSoundHandler getSoundHandler() {
        return soundHandler;
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

    public boolean shouldUseHoverAnimation() {
        return this.entityData.get(DATA_HOVER_VISUAL_ACTIVE) || this.isActuallyHovering();
    }

    public float getBankAngleDegrees(float partialTick) {
        return Mth.lerp(partialTick, this.flightVisualState.prevBankAngle, this.flightVisualState.bankAngle);
    }

    public float getFlightPitchRadians(float partialTick) {
        return Mth.lerp(partialTick, this.flightVisualState.prevFlightPitchRad, this.flightVisualState.flightPitchRad);
    }

    public float getSmoothedRoll(float partialTick) {
        return 0.0F;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        this.soundHandler.tick();
        tickBankingLogic();
        tickPitchingLogic();
        if (!this.level().isClientSide) {
            this.setFlying(true);
            this.setTakeoff(false);
            this.setLanding(false);
            this.setNoGravity(true);
            tickHoverVisualState();
            this.tickAmbientVocals();
        }
        this.tickAnimationStates();
    }

    public void flyToward(Vec3 destination, double speedScale) {
        this.beginAiFlight();
        markHoverVisualActive();
        DragonDirectAirCombatMovementHelper.flyToward(this, destination, speedScale, FLOAT_ACCEL, FLOAT_DRAG);
    }

    private void markHoverVisualActive() {
        this.hoverVisualTicks = HOVER_VISUAL_GRACE_TICKS;
        if (!this.level().isClientSide) {
            this.entityData.set(DATA_HOVER_VISUAL_ACTIVE, true);
        }
    }

    private void tickHoverVisualState() {
        if (this.isVehicle()) {
            boolean active = this.getDeltaMovement().lengthSqr() > MOUNTED_HOVER_VISUAL_THRESHOLD_SQ;
            this.hoverVisualTicks = 0;
            if (this.entityData.get(DATA_HOVER_VISUAL_ACTIVE) != active) {
                this.entityData.set(DATA_HOVER_VISUAL_ACTIVE, active);
            }
            return;
        }

        if (this.isActuallyHovering()) {
            this.hoverVisualTicks = HOVER_VISUAL_GRACE_TICKS;
        } else if (this.hoverVisualTicks > 0) {
            this.hoverVisualTicks--;
        }

        boolean active = this.hoverVisualTicks > 0;
        if (this.entityData.get(DATA_HOVER_VISUAL_ACTIVE) != active) {
            this.entityData.set(DATA_HOVER_VISUAL_ACTIVE, active);
        }
    }

    private void tickBankingLogic() {
        DragonFlightVisuals.tickBanking(
                this.flightVisualState,
                true,
                this.horizontalCollision,
                this.verticalCollision,
                this.getYRot(),
                this.yRotO
        );
    }

    private void tickPitchingLogic() {
        DragonFlightVisuals.beginPitchTick(this.flightVisualState);

        if (this.level().isClientSide) {
            this.flightVisualState.flightPitchRad = this.entityData.get(DATA_FLIGHT_PITCH);
            return;
        }

        float targetPitchRad;
        if (this.isVehicle() && this.getControllingPassenger() instanceof Player player) {
            float rawPlayerPitchRad = (float) Math.toRadians(player.getXRot());
            targetPitchRad = DragonFlightVisuals.smoothRiderPitchInput(this.flightVisualState, rawPlayerPitchRad);
        } else {
            DragonFlightVisuals.clearRiderPitchInput(this.flightVisualState);
            targetPitchRad = DragonFlightVisuals.computeAiPitchTarget(this.getDeltaMovement());
        }

        this.flightVisualState.flightPitchRad =
                DragonFlightVisuals.approachPitch(this.flightVisualState.flightPitchRad, targetPitchRad);
        this.entityData.set(DATA_FLIGHT_PITCH, this.flightVisualState.flightPitchRad);
    }

    private void tickAmbientVocals() {
        if (this.isDeadOrDying() || this.isVehicle()) {
            return;
        }
        if (++ambientSoundTimer < nextAmbientSoundDelay) {
            return;
        }
        ambientSoundTimer = 0;
        nextAmbientSoundDelay = MIN_AMBIENT_DELAY + this.getRandom().nextInt(Math.max(1, MAX_AMBIENT_DELAY - MIN_AMBIENT_DELAY + 1));
        int choice = this.getRandom().nextInt(3);
        if (choice == 0) {
            soundHandler.playVocal("grumble1");
        } else if (choice == 1) {
            soundHandler.playVocal("grumble2");
        } else {
            soundHandler.playVocal("grumble3");
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
            if (this.getNavigation().getPath() != null) {
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

        float yaw = rider.getYRot();
        float pitch = rider.getXRot();
        this.yRotO = this.getYRot();
        this.yBodyRotO = this.yBodyRot;
        this.yHeadRotO = this.yHeadRot;
        this.xRotO = this.getXRot();
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.setYHeadRot(yaw);
        this.setXRot(Mth.clamp(pitch * 0.5F, -45.0F, 45.0F));

        float yawRad = yaw * Mth.DEG_TO_RAD;
        float pitchRad = this.getXRot() * Mth.DEG_TO_RAD;
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

    public boolean shouldRiderSit() {
        return false;
    }

    @Override
    public double getPassengersRidingOffset() {
        return this.getBbHeight() * 0.45D;
    }

    @Override
    protected void positionRider(@NotNull Entity passenger, @NotNull Entity.MoveFunction moveFunction) {
        DragonRiderSeat.positionLocatorRider(
                this,
                passenger,
                moveFunction,
                getPassengersRidingOffset(),
                this.level().isClientSide ? this.getClientLocatorPosition("passengerLocator") : null
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
        ItemStack heldItem = player.getItemInHand(hand);

        if (heldItem.is(Items.CHORUS_FRUIT)) {
            if (!this.level().isClientSide) {
                if (!player.getAbilities().instabuild) {
                    heldItem.shrink(1);
                }
                this.soundHandler.playVocal("eat");

                if (!this.isTame()) {
                    if (this.getRandom().nextInt(TAME_CHANCE_DENOMINATOR) == 0) {
                        this.tame(player);
                        this.navigation.stop();
                        this.setTarget(null);
                        this.level().broadcastEntityEvent(this, (byte) 7);
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
            if (player.isCrouching() && hand == InteractionHand.MAIN_HAND && heldItem.isEmpty()) {
                if (!this.level().isClientSide) {
                    int next = (this.getCommand() + 1) % 3;
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

            if (!player.isCrouching() && hand == InteractionHand.MAIN_HAND) {
                if (!this.level().isClientSide) {
                    player.startRiding(this);
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
        }

        return super.mobInteract(player, hand);
    }

    @Override
    public boolean isFood(@NotNull ItemStack stack) {
        return stack.is(Items.CHORUS_FRUIT);
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        boolean result = super.hurt(source, amount);
        if (result && this.isAlive() && !this.level().isClientSide) {
            if (!hurtSoundQueued) {
                hurtSoundQueued = true;
                this.soundHandler.playVocal("nulljaw_hurt");
                hurtSoundQueued = false;
            }
        }
        return result;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damageMultiplier, @NotNull DamageSource source) {
        return false;
    }

    @Override
    public void die(@NotNull DamageSource cause) {
        if (!this.dead && !this.level().isClientSide && !deathSoundQueued) {
            deathSoundQueued = true;
            this.triggerAnim("instant", "nulljaw_die");
            VocalEntry deathEntry = VOCAL_ENTRIES.get("nulljaw_die");
            if (deathEntry != null && deathEntry.soundSupplier() != null) {
                float pitch = deathEntry.basePitch();
                if (deathEntry.pitchVariance() != 0.0F) {
                    pitch += (this.getRandom().nextFloat() - 0.5F) * deathEntry.pitchVariance() * 2.0F;
                }
                this.playSound(deathEntry.soundSupplier().get(), deathEntry.volume(), pitch);
            }
        }
        super.die(cause);
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
    public @NotNull EntityDimensions getDimensions(@NotNull Pose pose) {
        EntityDimensions base = super.getDimensions(pose);
        return isBaby() ? base.scale(BABY_HITBOX_SCALE) : base;
    }

    @Override
    public boolean canMate(@NotNull Animal otherAnimal) {
        return otherAnimal instanceof Nulljaw && super.canMate(otherAnimal);
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(@NotNull ServerLevel level, @NotNull AgeableMob otherParent) {
        return ModEntities.NULLJAW.get().create(level);
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
    public boolean canParticipateInPack() {
        if (this.isTame()) {
            return false;
        }
        if (this.isBaby() || this.isDying()) {
            return false;
        }
        if (!this.isAlive() || this.isRemoved()) {
            return false;
        }
        return !this.isOrderedToSit() && this.getCommand() != 1;
    }

    @Override
    public boolean canLeadPack() {
        return canParticipateInPack() && !this.isFemale();
    }

    @Override
    public int getPackLeadershipPriority() {
        return (int) Math.round((this.getHealth() / Math.max(1.0F, this.getMaxHealth())) * 100.0F);
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
    public int getPackLeaderRefreshIntervalTicks() {
        return 60;
    }

    @Override
    public boolean handleDirectAirPackFollow(Vec3 target, double speed) {
        this.flyToward(target, speed);
        return true;
    }

    @Override
    protected int getFlightMode() {
        return isActuallyHovering() ? 2 : 5;
    }

    @Override
    protected boolean isDragonFlying() {
        return this.entityData.get(DATA_FLYING);
    }

    @Override
    public boolean isTakeoff() {
        return this.entityData.get(DATA_TAKEOFF);
    }

    @Override
    public boolean isLanding() {
        return this.entityData.get(DATA_LANDING);
    }

    @Override
    public boolean isHovering() {
        return this.entityData.get(DATA_HOVERING);
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
    public void setFlying(boolean flying) {
        this.entityData.set(DATA_FLYING, flying);
    }

    @Override
    public void setTakeoff(boolean takeoff) {
        this.entityData.set(DATA_TAKEOFF, takeoff);
    }

    @Override
    public void setHovering(boolean hovering) {
        this.entityData.set(DATA_HOVERING, hovering);
    }

    @Override
    public void setLanding(boolean landing) {
        this.entityData.set(DATA_LANDING, landing);
    }

    @Override
    public float getFlightSpeed() {
        return (float) AI_CRUISE_SPEED;
    }

    @Override
    public double getPreferredFlightAltitude() {
        return 8.0D;
    }

    @Override
    public boolean canTakeoff() {
        return !this.isBaby();
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
    }

    @Override
    public void beginAiLanding() {
        beginAiFlight();
    }

    public void handleAiLandingComplete() {
        markLandedNow();
    }

    @Override
    public void markLandedNow() {
        this.setFlying(true);
        this.setTakeoff(false);
        this.setLanding(false);
        this.setHovering(false);
    }

    @Override
    public DragonAbilityType<?, ?> getPrimaryAttackAbility() {
        return null;
    }

    @Override
    public Vec3 getHeadPosition() {
        Vec3 forward = this.getLookAngle().normalize().scale(this.getBbWidth() * 0.55D);
        return this.position().add(0.0D, this.getBbHeight() * 0.72D, 0.0D).add(forward);
    }

    @Override
    public Vec3 getMouthPosition() {
        Vec3 forward = this.getLookAngle().normalize().scale(this.getBbWidth() * 0.8D);
        return this.position().add(0.0D, this.getBbHeight() * 0.66D, 0.0D).add(forward);
    }

    public void setClientLocatorPosition(String name, Vec3 pos) {
        if (name == null || pos == null) {
            return;
        }
        this.clientLocatorCache.put(name, pos);
    }

    @Override
    public Vec3 getClientLocatorPosition(String name) {
        if (name == null) {
            return null;
        }
        return this.clientLocatorCache.get(name);
    }
}

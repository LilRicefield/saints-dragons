package com.leon.saintsdragons.server.entity.dragons.amphithere;

import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.server.ai.goals.amphithere.AmphithereFlightGoal;
import com.leon.saintsdragons.server.ai.navigation.DragonFlightMoveHelper;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.dragons.amphithere.handlers.AmphithereAnimationHandler;
import com.leon.saintsdragons.server.entity.handler.DragonSoundHandler;
import com.leon.saintsdragons.server.entity.interfaces.DragonFlightCapable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.keyframe.event.SoundKeyframeEvent;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nonnull;

public class AmphithereEntity extends DragonEntity implements FlyingAnimal, DragonFlightCapable {
    private static final EntityDataAccessor<Boolean> DATA_FLYING =
            SynchedEntityData.defineId(AmphithereEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_TAKEOFF =
            SynchedEntityData.defineId(AmphithereEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_HOVERING =
            SynchedEntityData.defineId(AmphithereEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_LANDING =
            SynchedEntityData.defineId(AmphithereEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final AmphithereAnimationHandler animationHandler = new AmphithereAnimationHandler(this);
    private final DragonSoundHandler soundHandler = new DragonSoundHandler(this);

    private final GroundPathNavigation groundNav;
    private final FlyingPathNavigation airNav;
    private boolean usingAirNav;

    private Vec3 currentFlightTarget;
    private int targetCooldown;
    private int airTicks;
    private int groundTicks;

    private float bankSmoothedYaw = 0f;
    private int bankHoldTicks = 0;
    private int bankDir = 0;

    private float pitchSmoothedPitch = 0f;
    private int pitchHoldTicks = 0;
    private int pitchDir = 0;

    public AmphithereEntity(EntityType<? extends AmphithereEntity> type, Level level) {
        super(type, level);
        this.setMaxUpStep(1.1F);

        this.groundNav = new GroundPathNavigation(this, level);
        this.airNav = new FlyingPathNavigation(this, level) {
            @Override
            public boolean isStableDestination(@Nonnull BlockPos pos) {
                BlockState below = this.level.getBlockState(pos.below());
                return !below.isAir();
            }
        };
        this.airNav.setCanOpenDoors(false);
        this.airNav.setCanFloat(false);
        this.airNav.setCanPassDoors(false);

        this.navigation = groundNav;
        this.moveControl = new MoveControl(this);
        this.usingAirNav = false;

        this.setPathfindingMalus(BlockPathTypes.LEAVES, -1.0F);
        this.setPathfindingMalus(BlockPathTypes.DANGER_FIRE, -1.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 60.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.FLYING_SPEED, 0.50D)
                .add(Attributes.ARMOR, 4.0D);
    }

    public static boolean canSpawnHere(EntityType<? extends AmphithereEntity> type, LevelAccessor level, net.minecraft.world.entity.MobSpawnType spawnType, BlockPos pos, RandomSource rng) {
        return net.minecraft.world.entity.animal.Animal.checkAnimalSpawnRules(type, level, spawnType, pos, rng);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_FLYING, false);
        this.entityData.define(DATA_TAKEOFF, false);
        this.entityData.define(DATA_HOVERING, false);
        this.entityData.define(DATA_LANDING, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new AmphithereFlightGoal(this));
        this.goalSelector.addGoal(4, new FollowOwnerGoal(this, 1.1D, 10.0F, 3.0F, false));
        this.goalSelector.addGoal(6, new RandomStrollGoal(this, 0.6D, 160));

        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.level().isClientSide) {
            if (isFlying()) {
                airTicks++;
                groundTicks = 0;
                this.fallDistance = 0.0F;
            } else {
                groundTicks++;
                airTicks = 0;
            }

            if (isFlying() && this.onGround()) {
                setLanding(true);
                setFlying(false);
            }
        }

        this.setNoGravity(isFlying() || isHovering());
        if (!isFlying() && usingAirNav) {
            switchToGroundNavigation();
        }
    }
    public void tick() {
        super.tick();

        tickBankingLogic();
        tickPitchingLogic();

        if (!level().isClientSide && targetCooldown > 0) {
            targetCooldown--;
        }
    }

    private void tickBankingLogic() {
        if (!isFlying() || isLanding() || isHovering()) {
            if (bankDir != 0 || bankSmoothedYaw != 0f) {
                bankDir = 0;
                bankSmoothedYaw = 0f;
                bankHoldTicks = 0;
            }
            return;
        }

        float yawChange = getYRot() - yRotO;
        bankSmoothedYaw = bankSmoothedYaw * 0.85f + yawChange * 0.15f;

        float enter = 0.8f;
        float exit = 3.5f;

        int desiredDir = bankDir;
        if (bankSmoothedYaw > enter) {
            desiredDir = 1;
        } else if (bankSmoothedYaw < -enter) {
            desiredDir = -1;
        } else if (Math.abs(bankSmoothedYaw) < exit) {
            desiredDir = 0;
        }

        if (desiredDir != bankDir) {
            int holdTime = (desiredDir == 0) ? 1 : 2;
            if (bankHoldTicks >= holdTime) {
                bankDir = desiredDir;
                bankHoldTicks = 0;
            } else {
                bankHoldTicks++;
            }
        } else {
            bankHoldTicks = Math.min(bankHoldTicks + 1, 10);
        }
    }

    private void tickPitchingLogic() {
        if (!isFlying() || isLanding() || isHovering()) {
            if (pitchDir != 0) {
                pitchDir = 0;
                pitchSmoothedPitch = 0f;
                pitchHoldTicks = 0;
            }
            return;
        }

        float pitchChange = getXRot() - xRotO;
        pitchSmoothedPitch = pitchSmoothedPitch * 0.85f + pitchChange * 0.15f;

        int desiredDir = pitchDir;
        float enter = 3.0f;
        float exit = 3.0f;

        if (pitchSmoothedPitch > enter) {
            desiredDir = 1;
        } else if (pitchSmoothedPitch < -enter) {
            desiredDir = -1;
        } else if (Math.abs(pitchSmoothedPitch) < exit) {
            desiredDir = 0;
        }

        if (desiredDir != pitchDir) {
            int holdTime = (desiredDir == 0) ? 1 : 2;
            if (pitchHoldTicks >= holdTime) {
                pitchDir = desiredDir;
                pitchHoldTicks = 0;
            } else {
                pitchHoldTicks++;
            }
        } else {
            pitchHoldTicks = Math.min(pitchHoldTicks + 1, 10);
        }
    }

    public int getBankDirection() {
        return bankDir;
    }

    public int getPitchDirection() {
        return pitchDir;
    }
    public DragonSoundHandler getSoundHandler() {
        return soundHandler;
    }

    public boolean needsNewFlightTarget() {
        return currentFlightTarget == null || targetCooldown <= 0;
    }

    public void assignFlightTarget(Vec3 target) {
        this.currentFlightTarget = target;
        this.targetCooldown = 50 + this.getRandom().nextInt(40);
    }

    @Nullable
    public Vec3 getFlightTarget() {
        return currentFlightTarget;
    }

    public int getAirTicks() {
        return airTicks;
    }

    public int getGroundTicks() {
        return groundTicks;
    }

    public void switchToAirNavigation() {
        if (!usingAirNav) {
            this.navigation = this.airNav;
            this.moveControl = new DragonFlightMoveHelper(this);
            this.usingAirNav = true;
        }
    }

    public void switchToGroundNavigation() {
        if (usingAirNav) {
            this.navigation = this.groundNav;
            this.moveControl = new MoveControl(this);
            this.usingAirNav = false;
        }
    }

    @Override
    protected @NotNull PathNavigation createNavigation(@Nonnull Level level) {
        return new GroundPathNavigation(this, level);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        AnimationController<AmphithereEntity> movement = new AnimationController<>(this, "movement", 5, animationHandler::handleMovementAnimation);
        movement.setSoundKeyframeHandler(this::onAnimationSound);
        controllers.add(movement);

        AnimationController<AmphithereEntity> banking = new AnimationController<>(this, "banking", 5, animationHandler::bankingPredicate);
        controllers.add(banking);

        AnimationController<AmphithereEntity> pitching = new AnimationController<>(this, "pitching", 5, animationHandler::pitchingPredicate);
        controllers.add(pitching);

        AnimationController<AmphithereEntity> actions = new AnimationController<>(this, "actions", 10, animationHandler::actionPredicate);
        animationHandler.setupActionController(actions);
        actions.setSoundKeyframeHandler(this::onAnimationSound);
        controllers.add(actions);
    }
    private void onAnimationSound(SoundKeyframeEvent<AmphithereEntity> event) {
        soundHandler.handleAnimationSound(this, event.getKeyframeData(), event.getController());
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public DragonAbilityType<?, ?> getPrimaryAttackAbility() {
        return null;
    }

    @Override
    public Vec3 getHeadPosition() {
        return this.getEyePosition();
    }

    @Override
    public Vec3 getMouthPosition() {
        return this.position().add(0.0D, this.getBbHeight() * 0.8D, 0.0D);
    }

    @Override
    public boolean isFood(@NotNull ItemStack stack) {
        return stack.is(Items.COD) || stack.is(Items.SALMON) || stack.is(Items.TROPICAL_FISH) || stack.is(Items.CHICKEN);
    }

    @Override
    public @NotNull InteractionResult mobInteract(@NotNull Player player, @NotNull InteractionHand hand) {
        return super.mobInteract(player, hand);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(@Nonnull ServerLevel level, @Nonnull AgeableMob partner) {
        return ModEntities.AMPHITHERE.get().create(level);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Flying", isFlying());
        tag.putBoolean("Hovering", isHovering());
        tag.putBoolean("Landing", isLanding());
        tag.putBoolean("Takeoff", isTakeoff());
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Flying")) setFlying(tag.getBoolean("Flying"));
        if (tag.contains("Hovering")) setHovering(tag.getBoolean("Hovering"));
        if (tag.contains("Landing")) setLanding(tag.getBoolean("Landing"));
        if (tag.contains("Takeoff")) setTakeoff(tag.getBoolean("Takeoff"));
    }

    // ===== DragonFlightCapable =====
    @Override
    public boolean isFlying() {
        return this.entityData.get(DATA_FLYING);
    }

    @Override
    public void setFlying(boolean flying) {
        if (flying == isFlying()) {
            return;
        }
        this.entityData.set(DATA_FLYING, flying);
        if (flying) {
            switchToAirNavigation();
            setLanding(false);
            setTakeoff(true);
            this.getNavigation().stop();
        } else {
            switchToGroundNavigation();
            setHovering(false);
            setTakeoff(false);
        }
    }

    @Override
    public boolean isTakeoff() {
        return this.entityData.get(DATA_TAKEOFF);
    }

    @Override
    public void setTakeoff(boolean takeoff) {
        this.entityData.set(DATA_TAKEOFF, takeoff);
    }

    @Override
    public boolean isHovering() {
        return this.entityData.get(DATA_HOVERING);
    }

    @Override
    public void setHovering(boolean hovering) {
        this.entityData.set(DATA_HOVERING, hovering);
    }

    @Override
    public boolean isLanding() {
        return this.entityData.get(DATA_LANDING);
    }

    @Override
    public void setLanding(boolean landing) {
        this.entityData.set(DATA_LANDING, landing);
        if (landing) {
            setHovering(false);
            setTakeoff(false);
        }
    }

    @Override
    public float getFlightSpeed() {
        return 0.5F;
    }

    @Override
    public double getPreferredFlightAltitude() {
        double base = 24.0D + this.getRandom().nextDouble() * 10.0D;
        return Mth.clamp(base, 20.0D, 45.0D);
    }

    @Override
    public boolean canTakeoff() {
        return !this.isBaby() && !this.isOrderedToSit() && this.isAlive() && (this.onGround() || this.isInWater());
    }

    @Override
    public void markLandedNow() {
        setLanding(false);
        setTakeoff(false);
    }

    // ===== FlyingAnimal =====
    @Override
    public boolean isFlapping() {
        return isFlying() && this.getDeltaMovement().y > -0.1D;
    }
}





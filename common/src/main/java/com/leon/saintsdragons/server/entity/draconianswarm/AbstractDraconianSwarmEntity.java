package com.leon.saintsdragons.server.entity.draconianswarm;

import com.leon.saintsdragons.common.block.DraconianNucleusBlockEntity;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.ai.goals.draconianswarm.DraconianSwarmCombatMovementGoal;
import com.leon.saintsdragons.server.ai.goals.draconianswarm.DraconianSwarmCoordinator;
import com.leon.saintsdragons.server.ai.goals.SuspendedFloatWanderGoal;
import com.leon.saintsdragons.server.ai.navigation.async.AsyncSwarmFlightController;
import com.leon.saintsdragons.server.ai.navigation.async.AsyncSwarmFlightMoveControl;
import com.leon.saintsdragons.server.ai.navigation.async.AsyncSwarmFlyingPathNavigation;
import com.leon.saintsdragons.server.entity.controller.DragonBodyControl;
import com.leon.saintsdragons.server.entity.controller.GenericLookControl;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

public abstract class AbstractDraconianSwarmEntity extends Monster implements GeoEntity {
    private static final double VISUAL_PITCH_MIN_SPEED_SQ = 0.0025D;
    private static final double VISUAL_PITCH_VERTICAL_DEADZONE = 0.015D;
    private static final float VISUAL_PITCH_LERP = 0.28F;

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private final AsyncSwarmFlightController swarmFlightController;
    private float prevFlightPitchRad;
    private float flightPitchRad;
    private float prevTailDragYawDeg;
    private float tailDragYawDeg;
    private boolean deathAnimationStarted;
    private boolean combatAttackWindow;
    private boolean combatRetreatRequested;
    private double combatRetreatDistance;
    @Nullable
    private BlockPos nucleusPos;
    @Nullable
    private UUID encounterId;
    private int encounterWave;
    private boolean nucleusDeathReported;
    private boolean initialConfiguredAttributesApplied;

    protected AbstractDraconianSwarmEntity(EntityType<? extends AbstractDraconianSwarmEntity> entityType, Level level) {
        super(entityType, level);
        this.swarmFlightController = new AsyncSwarmFlightController(this);
        this.lookControl = new GenericLookControl(this);
        this.navigation = createSwarmNavigation(level);
        this.moveControl = new AsyncSwarmFlightMoveControl(this, this.swarmFlightController);
        this.setNoGravity(true);
        this.setPathfindingMalus(BlockPathTypes.WALKABLE, -1.0F);
        this.setPathfindingMalus(BlockPathTypes.WATER, -1.0F);
        this.setPathfindingMalus(BlockPathTypes.WATER_BORDER, -1.0F);
        this.setPathfindingMalus(BlockPathTypes.FENCE, -1.0F);
    }

    protected static DragonAttributeConfig swarmConfig() {
        return DragonAttributeConfigLoader.getInstance().getConfig(DragonAttributeConfigLoader.DRACONIAN_SWARM_ID);
    }

    protected static double swarmHealth(String creatureKey, double fallback) {
        DragonAttributeConfig config = swarmConfig();
        if ("latcher".equals(creatureKey)) {
            return config.extraDouble("latcher_max_health", config.maxHealth());
        }
        return config.extraDouble(creatureKey + "_max_health", fallback);
    }

    protected static double swarmArmor(String creatureKey, double fallback) {
        DragonAttributeConfig config = swarmConfig();
        if ("latcher".equals(creatureKey)) {
            return config.extraDouble("latcher_armor", config.armor());
        }
        return config.extraDouble(creatureKey + "_armor", fallback);
    }

    protected static double swarmAbilityDamage(String abilityKey, double fallback) {
        return swarmConfig().abilityDamage(abilityKey, fallback);
    }

    protected static double swarmChaseSpeed(String creatureKey, double fallback) {
        return Math.max(0.0D, swarmConfig().extraDouble(creatureKey + "_chase_speed", fallback));
    }

    public void applyConfiguredAttributes() {
        applyConfiguredAttributes(false);
    }

    private void applyInitialConfiguredAttributes() {
        if (this.initialConfiguredAttributesApplied) {
            return;
        }
        this.initialConfiguredAttributesApplied = true;
        applyConfiguredAttributes(true);
    }

    private void applyConfiguredAttributes(boolean fillHealth) {
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(getConfiguredMaxHealth());
        getAttribute(Attributes.ARMOR).setBaseValue(getConfiguredArmor());
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(getConfiguredAttackDamage());
        if (fillHealth) {
            setHealth(getMaxHealth());
        } else if (getHealth() > getMaxHealth()) {
            setHealth(getMaxHealth());
        }
    }

    protected abstract double getConfiguredMaxHealth();

    protected abstract double getConfiguredArmor();

    protected abstract double getConfiguredAttackDamage();

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(3, createCombatMovementGoal());
        this.goalSelector.addGoal(7, new SuspendedFloatWanderGoal(
                this,
                new SuspendedFloatWanderGoal.Movement() {
                    @Override
                    public boolean isIdle() {
                        return AbstractDraconianSwarmEntity.this.swarmFlightController.isIdle();
                    }

                    @Override
                    public void moveTo(Vec3 target, double speed) {
                        AbstractDraconianSwarmEntity.this.swarmFlightController.setWaypoint(target, speed);
                    }

                    @Override
                    public void stop() {
                        AbstractDraconianSwarmEntity.this.swarmFlightController.clearWaypoint();
                    }
                },
                () -> this.isAlive() && this.getTarget() == null,
                getWanderFlightSpeed(),
                32
        ));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(
                this, LivingEntity.class, 10, true, false, this::canTargetFromSwarm));
    }

    protected Goal createCombatMovementGoal() {
        return new DraconianSwarmCombatMovementGoal(this);
    }

    @Override
    public @NotNull SpawnGroupData finalizeSpawn(@NotNull ServerLevelAccessor level,
                                                 @NotNull DifficultyInstance difficulty,
                                                 @NotNull MobSpawnType spawnType,
                                                 @Nullable SpawnGroupData spawnGroupData,
                                                 @Nullable CompoundTag tag) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData, tag);
        applyInitialConfiguredAttributes();
        return data;
    }

    @Override
    public void tick() {
        if (!level().isClientSide) {
            applyInitialConfiguredAttributes();
        }
        super.tick();
        tickVisualFlightPitch();
        tickTailDragYaw();
        this.setNoGravity(true);
        if (!level().isClientSide) {
            tickNucleusLeash();
            this.swarmFlightController.serverTick();
        }
    }

    @Override
    protected @NotNull BodyRotationControl createBodyControl() {
        return new DragonBodyControl(this, getBodyTurnSpeed());
    }

    @Override
    protected @NotNull PathNavigation createNavigation(@NotNull Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanPassDoors(false);
        navigation.setCanFloat(false);
        return navigation;
    }

    protected PathNavigation createSwarmNavigation(Level level) {
        AsyncSwarmFlyingPathNavigation navigation = new AsyncSwarmFlyingPathNavigation(this, level, this.swarmFlightController);
        navigation.setCanOpenDoors(false);
        navigation.setCanPassDoors(false);
        navigation.setCanFloat(false);
        return navigation;
    }

    @Override
    public float getWalkTargetValue(@NotNull BlockPos pos, @NotNull LevelReader level) {
        return level.getBlockState(pos).isAir() ? 10.0F : 0.0F;
    }

    @Override
    public boolean causeFallDamage(float distance, float damageMultiplier, @NotNull DamageSource source) {
        return false;
    }

    @Override
    protected SoundEvent getHurtSound(@NotNull DamageSource source) {
        return ModSounds.DRACONIAN_SWARM_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.DRACONIAN_SWARM_DEATH.get();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return switch (getRandom().nextInt(3)) {
            case 0 -> ModSounds.DRACONIAN_SWARM_IDLE_0.get();
            case 1 -> ModSounds.DRACONIAN_SWARM_IDLE_1.get();
            default -> ModSounds.DRACONIAN_SWARM_IDLE_2.get();
        };
    }

    @Override
    public void die(@NotNull DamageSource source) {
        super.die(source);
        reportDefeatToNucleus();
        if (!this.deathAnimationStarted) {
            this.deathAnimationStarted = true;
            releaseCombatAttack();
            playDeathAnimation();
            this.swarmFlightController.clearWaypoint();
            setDeltaMovement(Vec3.ZERO);
        }
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && isAlive() && retreatsAfterTakingDamage()) {
            requestCombatRetreat();
        }
        return hurt;
    }

    protected boolean retreatsAfterTakingDamage() {
        return false;
    }

    protected void playDeathAnimation() {
    }

    public void playSpawnAnimation() {
    }

    public void assignNucleusEncounter(BlockPos nucleusPos, UUID encounterId, int wave) {
        this.nucleusPos = nucleusPos.immutable();
        this.encounterId = encounterId;
        this.encounterWave = wave;
        this.nucleusDeathReported = false;
        this.setPersistenceRequired();
    }

    @Nullable
    public BlockPos getNucleusPos() {
        return this.nucleusPos;
    }

    public boolean belongsToEncounter(BlockPos nucleusPos, UUID encounterId) {
        return nucleusPos.equals(this.nucleusPos) && encounterId.equals(this.encounterId);
    }

    private void tickNucleusLeash() {
        if (this.nucleusPos == null || this.encounterId == null) {
            return;
        }
        if (level() instanceof ServerLevel serverLevel
                && (!(serverLevel.getBlockEntity(this.nucleusPos)
                instanceof DraconianNucleusBlockEntity nucleus)
                || !nucleus.isActiveEncounter(this.encounterId))) {
            discard();
            return;
        }

        Vec3 nucleusCenter = Vec3.atCenterOf(this.nucleusPos);
        LivingEntity target = getTarget();
        if (target != null && (target.distanceToSqr(nucleusCenter) > 64.0D * 64.0D
                || position().distanceToSqr(nucleusCenter) > 64.0D * 64.0D)) {
            setTarget(null);
            setCombatAttackWindow(false);
            releaseCombatAttack();
            this.swarmFlightController.clearWaypoint();
        }

        if (getTarget() == null && position().distanceToSqr(nucleusCenter) > 20.0D * 20.0D) {
            double angle = getRandom().nextDouble() * Math.PI * 2.0D;
            double radius = 10.0D + getRandom().nextDouble() * 6.0D;
            Vec3 returnPoint = nucleusCenter.add(
                    Math.cos(angle) * radius,
                    3.0D + getRandom().nextDouble() * 4.0D,
                    Math.sin(angle) * radius);
            if (level().noCollision(getBoundingBox().move(returnPoint.subtract(position())))) {
                this.swarmFlightController.setWaypoint(returnPoint, getRetreatSpeed());
            }
        }
    }

    private void reportDefeatToNucleus() {
        if (this.nucleusDeathReported || this.nucleusPos == null || this.encounterId == null
                || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        this.nucleusDeathReported = true;
        if (serverLevel.getBlockEntity(this.nucleusPos)
                instanceof DraconianNucleusBlockEntity nucleus) {
            nucleus.onSwarmDefeated(this.encounterId, this.encounterWave, getUUID());
        }
    }

    @Override
    public boolean canAttack(@NotNull LivingEntity target) {
        if (!canHitWithSwarmAttack(target)) {
            return false;
        }
        if (this.nucleusPos != null && target.distanceToSqr(Vec3.atCenterOf(this.nucleusPos)) > 64.0D * 64.0D) {
            return false;
        }
        return !(target instanceof AbstractDraconianSwarmEntity) && super.canAttack(target);
    }

    public boolean canHitWithSwarmAttack(LivingEntity target) {
        return target.isAlive()
                && target != this
                && !(target instanceof AbstractDraconianSwarmEntity)
                && !(target instanceof EnderMan);
    }

    @Override
    public boolean doHurtTarget(@NotNull Entity target) {
        return !(target instanceof EnderMan) && super.doHurtTarget(target);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return this.encounterId == null && super.removeWhenFarAway(distanceToClosestPlayer);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.nucleusPos != null && this.encounterId != null) {
            tag.putLong("NucleusPos", this.nucleusPos.asLong());
            tag.putUUID("NucleusEncounter", this.encounterId);
            tag.putInt("NucleusWave", this.encounterWave);
            tag.putBoolean("NucleusDeathReported", this.nucleusDeathReported);
        }
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.initialConfiguredAttributesApplied = true;
        applyConfiguredAttributes();
        if (tag.contains("NucleusPos") && tag.hasUUID("NucleusEncounter")) {
            this.nucleusPos = BlockPos.of(tag.getLong("NucleusPos"));
            this.encounterId = tag.getUUID("NucleusEncounter");
            this.encounterWave = tag.getInt("NucleusWave");
            this.nucleusDeathReported = tag.getBoolean("NucleusDeathReported");
            this.setPersistenceRequired();
        }
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, @NotNull net.minecraft.world.level.block.state.BlockState state,
                                   @NotNull BlockPos pos) {
    }

    @Override
    public boolean onClimbable() {
        return false;
    }

    public AsyncSwarmFlightController getSwarmFlightController() {
        return this.swarmFlightController;
    }

    protected double getWanderFlightSpeed() {
        return 0.20D;
    }

    protected double getChaseFlightSpeed() {
        return 0.28D;
    }

    public double getChaseSpeed() {
        return getChaseFlightSpeed();
    }

    public CombatStyle getCombatStyle() {
        return CombatStyle.ORBIT_THEN_ATTACK;
    }

    public double getCombatOrbitRadius() {
        return 6.0D;
    }

    public double getCombatOrbitHeight() {
        return 1.5D;
    }

    public int getOrbitDurationTicks() {
        return 35 + getRandom().nextInt(25);
    }

    public double getOrbitSpeed() {
        return getChaseSpeed() * 0.85D;
    }

    public double getRetreatSpeed() {
        return getChaseSpeed() * 1.1D;
    }

    public double getCombatRetreatDistance() {
        return 8.0D;
    }

    public boolean canStartCombatAttack() {
        return this.combatAttackWindow;
    }

    public boolean tryClaimCombatAttack() {
        LivingEntity target = getTarget();
        return this.combatAttackWindow
                && target != null
                && DraconianSwarmCoordinator.tryClaimAttack(this, target);
    }

    public void releaseCombatAttack() {
        DraconianSwarmCoordinator.releaseAttack(this);
    }

    public void setCombatAttackWindow(boolean attackWindow) {
        this.combatAttackWindow = attackWindow;
    }

    public void requestCombatRetreat() {
        releaseCombatAttack();
        this.combatRetreatRequested = true;
        this.combatRetreatDistance = getCombatRetreatDistance();
    }

    public boolean hasCombatRetreatRequest() {
        return this.combatRetreatRequested;
    }

    public double consumeCombatRetreatDistance() {
        this.combatRetreatRequested = false;
        return this.combatRetreatDistance > 0.0D ? this.combatRetreatDistance : getCombatRetreatDistance();
    }

    protected float getBodyTurnSpeed() {
        return 0.35F;
    }

    protected boolean canTargetFromSwarm(LivingEntity target) {
        return canHitWithSwarmAttack(target)
                && !(target instanceof Mob mob && mob.getType() == getType());
    }

    public enum CombatStyle {
        ORBIT_THEN_ATTACK,
        PRECISE
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animationCache;
    }

    public float getFlightPitchRadians(float partialTick) {
        return Mth.lerp(partialTick, this.prevFlightPitchRad, this.flightPitchRad);
    }

    public float getTailDragYawRadians(float partialTick) {
        return Mth.lerp(partialTick, this.prevTailDragYawDeg, this.tailDragYawDeg) * Mth.DEG_TO_RAD;
    }

    private void tickVisualFlightPitch() {
        this.prevFlightPitchRad = this.flightPitchRad;

        Vec3 velocity = getDeltaMovement();
        if (velocity.lengthSqr() < VISUAL_PITCH_MIN_SPEED_SQ) {
            this.flightPitchRad = Mth.lerp(VISUAL_PITCH_LERP, this.flightPitchRad, 0.0F);
            return;
        }

        double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        double verticalSpeed = Math.abs(velocity.y) < VISUAL_PITCH_VERTICAL_DEADZONE ? 0.0D : velocity.y;
        float targetPitchRad = horizontalSpeed <= 1.0E-4D
                ? (verticalSpeed > 0.0D ? Mth.HALF_PI : -Mth.HALF_PI)
                : (float) Math.atan2(verticalSpeed, horizontalSpeed);
        targetPitchRad = Mth.clamp(targetPitchRad, -0.95F, 0.95F);
        this.flightPitchRad = Mth.lerp(VISUAL_PITCH_LERP, this.flightPitchRad, targetPitchRad);
        if (Math.abs(this.flightPitchRad) < 0.001F) {
            this.flightPitchRad = 0.0F;
        }
    }

    private void tickTailDragYaw() {
        this.prevTailDragYawDeg = this.tailDragYawDeg;

        float bodyYawDelta = Mth.wrapDegrees(this.yBodyRot - this.yBodyRotO);
        float entityYawDelta = Mth.wrapDegrees(this.getYRot() - this.yRotO);
        float targetDelta = Math.abs(bodyYawDelta) > 0.01F ? bodyYawDelta : entityYawDelta;
        float targetYaw = Mth.clamp(targetDelta * 4.0F, -55.0F, 55.0F);
        this.tailDragYawDeg = Mth.lerp(0.32F, this.tailDragYawDeg, targetYaw);
        if (Math.abs(this.tailDragYawDeg) < 0.01F) {
            this.tailDragYawDeg = 0.0F;
        }
    }
}

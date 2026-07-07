package com.leon.saintsdragons.server.entity.draconianswarm;

import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.ai.goals.draconianswarm.WhettledHornChargeGoal;
import com.leon.saintsdragons.server.ai.goals.draconianswarm.WhettledClawAttackGoal;
import com.leon.saintsdragons.server.entity.controller.CombatBodyFacingLock;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

public class Whettled extends AbstractDraconianSwarmEntity implements CombatBodyFacingLock {
    private static final EntityDataAccessor<Boolean> DATA_COMBAT_FACING =
            SynchedEntityData.defineId(Whettled.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_HORN_CHARGING =
            SynchedEntityData.defineId(Whettled.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_HORN_CHARGE_YAW =
            SynchedEntityData.defineId(Whettled.class, EntityDataSerializers.FLOAT);
    private static final String MOVEMENT_CONTROLLER = "movement";
    private static final String ACTION_CONTROLLER = "action";
    private static final String EYE_CONTROLLER = "eyes";
    private static final String CLAW_TRIGGER = "clawattack";
    private static final String HORN_TRIGGER = "movehornattack";
    private static final String DIE_TRIGGER = "die";
    private static final String SPAWN_TRIGGER = "spawn";

    private static final RawAnimation IDLE_MOVE =
            RawAnimation.begin().thenLoop("whettled.animation.idleandslowmove");
    private static final RawAnimation IDLE_EYE = RawAnimation.begin().thenLoop("whettled.animation.idleeye");
    private static final RawAnimation CLAW = RawAnimation.begin().thenPlay("whettled.animation.clawattack");
    private static final RawAnimation HORN = RawAnimation.begin().thenPlay("whettled.animation.movehornattack");
    private static final RawAnimation DIE = RawAnimation.begin().thenPlay("whettled.animation.die");
    private static final RawAnimation SPAWN = RawAnimation.begin().thenPlay("whettled.animation.spawn");

    private boolean swooping;
    public Whettled(EntityType<? extends Whettled> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_COMBAT_FACING, false);
        this.entityData.define(DATA_HORN_CHARGING, false);
        this.entityData.define(DATA_HORN_CHARGE_YAW, 0.0F);
    }

    @Override
    public void tick() {
        if (!level().isClientSide) {
            LivingEntity target = getTarget();
            this.entityData.set(DATA_COMBAT_FACING,
                    target != null && target.isAlive() && !isHornCharging());
        }
        super.tick();
        if (!level().isClientSide) {
            LivingEntity target = getTarget();
            boolean combatFacing = target != null && target.isAlive() && !isHornCharging();
            this.entityData.set(DATA_COMBAT_FACING, combatFacing);
            if (combatFacing) {
                lockCombatFacing(target);
            }
        }
    }

    @Override
    public boolean isCombatBodyFacingLocked() {
        return this.entityData.get(DATA_COMBAT_FACING) || isHornCharging();
    }

    @Override
    public float getCombatBodyFacingYaw() {
        return isHornCharging() ? this.entityData.get(DATA_HORN_CHARGE_YAW) : getYRot();
    }

    @Override
    public float getCombatBodyFacingTurnSpeed() {
        return isHornCharging() ? 180.0F : 18.0F;
    }

    private void lockCombatFacing(LivingEntity target) {
        double dx = target.getX() - getX();
        double dz = target.getZ() - getZ();
        if (dx * dx + dz * dz > 1.0E-4D) {
            float targetYaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
            float lockedYaw = Mth.approachDegrees(getYRot(), targetYaw, 14.0F);
            setYRot(lockedYaw);
            this.yHeadRot = Mth.approachDegrees(this.yHeadRot, targetYaw, 24.0F);
        }
        getLookControl().setLookAt(target, 100.0F, 100.0F);
        lookAt(target, 100.0F, 100.0F);
    }

    public static AttributeSupplier.@NotNull Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 16.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new WhettledHornChargeGoal(this));
        this.goalSelector.addGoal(2, new WhettledClawAttackGoal(this));
    }

    @Override
    protected double getWanderFlightSpeed() {
        return 0.38D;
    }

    @Override
    protected double getChaseFlightSpeed() {
        return 0.60D;
    }

    @Override
    protected boolean retreatsAfterTakingDamage() {
        return true;
    }

    @Override
    public CombatStyle getCombatStyle() {
        return CombatStyle.PRECISE;
    }

    @Override
    public double getCombatRetreatDistance() {
        return 7.0D;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, MOVEMENT_CONTROLLER, 4, state -> {
            state.setAndContinue(IDLE_MOVE);
            return PlayState.CONTINUE;
        }).triggerableAnim(CLAW_TRIGGER, CLAW)
                .triggerableAnim(HORN_TRIGGER, HORN));
        controllers.add(new AnimationController<>(this, ACTION_CONTROLLER, 1, state -> PlayState.STOP)
                .triggerableAnim(SPAWN_TRIGGER, SPAWN)
                .triggerableAnim(DIE_TRIGGER, DIE));
        controllers.add(new AnimationController<>(this, EYE_CONTROLLER, 1, state -> {
            state.setAndContinue(IDLE_EYE);
            return PlayState.CONTINUE;
        }));
    }

    public void performClawAnimation() {
        triggerAnim(MOVEMENT_CONTROLLER, CLAW_TRIGGER);
        playSound(ModSounds.WHETTLED_STRIKE.get(), 1.0F, 0.95F + getRandom().nextFloat() * 0.1F);
    }

    public void performSwoopAnimation() {
        triggerAnim(MOVEMENT_CONTROLLER, HORN_TRIGGER);
        playSound(ModSounds.WHETTLED_STRIKE_2.get(), 1.0F, 0.95F + getRandom().nextFloat() * 0.1F);
    }

    public boolean isSwooping() {
        return this.swooping;
    }

    public void setSwooping(boolean swooping) {
        this.swooping = swooping;
    }

    public boolean isHornCharging() {
        return this.entityData.get(DATA_HORN_CHARGING);
    }

    public void setHornCharging(boolean hornCharging) {
        this.entityData.set(DATA_HORN_CHARGING, hornCharging);
    }

    public void lockHornChargeDirection(Vec3 direction) {
        float yaw = (float) (Mth.atan2(direction.z, direction.x) * Mth.RAD_TO_DEG) - 90.0F;
        this.entityData.set(DATA_HORN_CHARGE_YAW, yaw);
        setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
    }

    @Override
    public void playSpawnAnimation() {
        triggerAnim(ACTION_CONTROLLER, SPAWN_TRIGGER);
    }

    @Override
    protected void playDeathAnimation() {
        triggerAnim(ACTION_CONTROLLER, DIE_TRIGGER);
    }
}

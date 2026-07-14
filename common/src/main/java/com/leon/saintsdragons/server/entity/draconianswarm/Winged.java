package com.leon.saintsdragons.server.entity.draconianswarm;

import com.leon.saintsdragons.server.ai.goals.draconianswarm.WingedDiveBombGoal;
import com.leon.saintsdragons.server.ai.goals.draconianswarm.WingedPullAttackGoal;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

public class Winged extends AbstractDraconianSwarmEntity {
    private static final String MOVEMENT_CONTROLLER = "movement";
    public static final String ACTION_CONTROLLER = "action";
    public static final String ATTACK_TRIGGER = "attack";
    public static final String SWOOP_TRIGGER = "attack2";
    public static final String DIE_TRIGGER = "die";
    public static final String SPAWN_TRIGGER = "spawn";

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("winged.animation.idle");
    private static final RawAnimation MOVE = RawAnimation.begin().thenLoop("winged.animation.move");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("winged.animation.attack");
    private static final RawAnimation SWOOP = RawAnimation.begin().thenPlay("winged.animation.attack2");
    private static final RawAnimation DIE = RawAnimation.begin().thenPlay("winged.animation.die");
    private static final RawAnimation SPAWN = RawAnimation.begin().thenPlay("winged.animation.spawn");
    private boolean swooping;
    private int pullAttackHits;

    public Winged(EntityType<? extends Winged> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.@NotNull Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, swarmHealth("winged", 8.0D))
                .add(Attributes.MOVEMENT_SPEED, 0.50D)
                .add(Attributes.FOLLOW_RANGE, 64.0D)
                .add(Attributes.ATTACK_DAMAGE, swarmAbilityDamage("winged_attack", 1.5D))
                .add(Attributes.ARMOR, swarmArmor("winged", 0.0D));
    }

    @Override
    protected double getConfiguredMaxHealth() {
        return swarmHealth("winged", 8.0D);
    }

    @Override
    protected double getConfiguredArmor() {
        return swarmArmor("winged", 0.0D);
    }

    @Override
    protected double getConfiguredAttackDamage() {
        return swarmAbilityDamage("winged_attack", 1.5D);
    }

    public double getDiveBombDamage() {
        return swarmAbilityDamage("winged_attack2", 2.025D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new WingedDiveBombGoal(this));
        this.goalSelector.addGoal(2, new WingedPullAttackGoal(this));
    }

    @Override
    protected double getWanderFlightSpeed() {
        return 0.43D;
    }

    @Override
    protected double getChaseFlightSpeed() {
        return swarmChaseSpeed("winged", 0.76D);
    }

    @Override
    protected boolean retreatsAfterTakingDamage() {
        return true;
    }

    @Override
    public double getCombatOrbitRadius() {
        return 8.0D;
    }

    @Override
    public double getCombatOrbitHeight() {
        return 3.0D;
    }

    @Override
    public int getOrbitDurationTicks() {
        return 25 + getRandom().nextInt(25);
    }

    @Override
    public double getCombatRetreatDistance() {
        return 9.0D;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, MOVEMENT_CONTROLLER, 4, state -> {
            state.setAndContinue(isMovingForAnimation() ? MOVE : IDLE);
            return PlayState.CONTINUE;
        }).triggerableAnim(ATTACK_TRIGGER, ATTACK)
                .triggerableAnim(SWOOP_TRIGGER, SWOOP));

        controllers.add(new AnimationController<>(this, ACTION_CONTROLLER, 4, state -> PlayState.STOP)
                .triggerableAnim(SPAWN_TRIGGER, SPAWN)
                .triggerableAnim(DIE_TRIGGER, DIE));
    }

    public boolean isMovingForAnimation() {
        return getDeltaMovement().lengthSqr() > 0.003D;
    }

    public void performPullAttackAnimation() {
        triggerAnim(MOVEMENT_CONTROLLER, ATTACK_TRIGGER);
    }

    public void recordPullAttackHit() {
        this.pullAttackHits = Math.min(2, this.pullAttackHits + 1);
    }

    public boolean isDiveBombReady() {
        return this.pullAttackHits >= 2;
    }

    public void consumeDiveBomb() {
        this.pullAttackHits = 0;
    }

    public void performSwoopAnimation() {
        triggerAnim(MOVEMENT_CONTROLLER, SWOOP_TRIGGER);
    }

    public boolean isSwooping() {
        return this.swooping;
    }

    public void setSwooping(boolean swooping) {
        this.swooping = swooping;
    }

    @Override
    public void playSpawnAnimation() {
        triggerAnim(ACTION_CONTROLLER, SPAWN_TRIGGER);
    }

    @Override
    protected void playDeathAnimation() {
        triggerAnim(ACTION_CONTROLLER, DIE_TRIGGER);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("PullAttackHits", this.pullAttackHits);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.pullAttackHits = Mth.clamp(tag.getInt("PullAttackHits"), 0, 2);
    }
}

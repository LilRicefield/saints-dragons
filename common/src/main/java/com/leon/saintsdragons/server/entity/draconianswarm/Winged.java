package com.leon.saintsdragons.server.entity.draconianswarm;

import com.leon.saintsdragons.server.ai.goals.draconianswarm.WingedPullAttackGoal;
import com.leon.saintsdragons.server.ai.goals.draconianswarm.DraconianSwarmSwoopAttackGoal;
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

public class Winged extends AbstractDraconianSwarmEntity implements SwoopingSwarmEntity {
    private static final String MOVEMENT_CONTROLLER = "movement";
    public static final String ACTION_CONTROLLER = "action";
    public static final String ATTACK_TRIGGER = "attack";
    public static final String SWOOP_TRIGGER = "attack2";
    public static final String DIE_TRIGGER = "die";

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("winged.animation.idle");
    private static final RawAnimation MOVE = RawAnimation.begin().thenLoop("winged.animation.move");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("winged.animation.attack");
    private static final RawAnimation SWOOP = RawAnimation.begin().thenPlay("winged.animation.attack2");
    private static final RawAnimation DIE = RawAnimation.begin().thenPlay("winged.animation.die");
    private boolean swooping;

    public Winged(EntityType<? extends Winged> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.@NotNull Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.50D)
                .add(Attributes.FOLLOW_RANGE, 28.0D)
                .add(Attributes.ATTACK_DAMAGE, 1.5D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new DraconianSwarmSwoopAttackGoal<>(this, 0.82D));
        this.goalSelector.addGoal(2, new WingedPullAttackGoal(this));
    }

    @Override
    protected double getWanderFlightSpeed() {
        return 0.30D;
    }

    @Override
    protected double getChaseFlightSpeed() {
        return 0.55;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, MOVEMENT_CONTROLLER, 4, state -> {
            state.setAndContinue(isMovingForAnimation() ? MOVE : IDLE);
            return PlayState.CONTINUE;
        }).triggerableAnim(ATTACK_TRIGGER, ATTACK)
                .triggerableAnim(SWOOP_TRIGGER, SWOOP));

        controllers.add(new AnimationController<>(this, ACTION_CONTROLLER, 4, state -> PlayState.STOP)
                .triggerableAnim(DIE_TRIGGER, DIE));
    }

    public boolean isMovingForAnimation() {
        return getDeltaMovement().lengthSqr() > 0.003D;
    }

    public void performPullAttackAnimation() {
        triggerAnim(MOVEMENT_CONTROLLER, ATTACK_TRIGGER);
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
    protected void playDeathAnimation() {
        triggerAnim(ACTION_CONTROLLER, DIE_TRIGGER);
    }
}

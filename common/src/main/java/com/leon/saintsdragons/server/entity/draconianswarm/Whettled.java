package com.leon.saintsdragons.server.entity.draconianswarm;

import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.ai.goals.draconianswarm.DraconianSwarmSwoopAttackGoal;
import com.leon.saintsdragons.server.ai.goals.draconianswarm.WhettledClawAttackGoal;
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

public class Whettled extends AbstractDraconianSwarmEntity implements SwoopingSwarmEntity {
    private static final String MOVEMENT_CONTROLLER = "movement";
    private static final String ACTION_CONTROLLER = "action";
    private static final String EYE_CONTROLLER = "eyes";
    private static final String CLAW_TRIGGER = "clawattack";
    private static final String HORN_TRIGGER = "movehornattack";
    private static final String DIE_TRIGGER = "die";

    private static final RawAnimation IDLE_MOVE =
            RawAnimation.begin().thenLoop("whettled.animation.idleandslowmove");
    private static final RawAnimation IDLE_EYE = RawAnimation.begin().thenLoop("whettled.animation.idleeye");
    private static final RawAnimation CLAW = RawAnimation.begin().thenPlay("whettled.animation.clawattack");
    private static final RawAnimation HORN = RawAnimation.begin().thenPlay("whettled.animation.movehornattack");
    private static final RawAnimation DIE = RawAnimation.begin().thenPlay("whettled.animation.die");

    private boolean swooping;

    public Whettled(EntityType<? extends Whettled> entityType, Level level) {
        super(entityType, level);
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
        this.goalSelector.addGoal(1, new DraconianSwarmSwoopAttackGoal<>(this, 0.72D, 0, 55, 1));
        this.goalSelector.addGoal(2, new WhettledClawAttackGoal(this));
    }

    @Override
    protected double getWanderFlightSpeed() {
        return 0.33D;
    }

    @Override
    protected double getChaseFlightSpeed() {
        return 0.53D;
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

    @Override
    public void performSwoopAnimation() {
        triggerAnim(MOVEMENT_CONTROLLER, HORN_TRIGGER);
        playSound(ModSounds.WHETTLED_STRIKE_2.get(), 1.0F, 0.95F + getRandom().nextFloat() * 0.1F);
    }

    @Override
    public boolean isSwooping() {
        return this.swooping;
    }

    @Override
    public void setSwooping(boolean swooping) {
        this.swooping = swooping;
    }

    @Override
    protected void playDeathAnimation() {
        triggerAnim(ACTION_CONTROLLER, DIE_TRIGGER);
    }
}

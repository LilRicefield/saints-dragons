package com.leon.saintsdragons.server.entity.draconianswarm;

import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.ai.goals.draconianswarm.LatcherBiteGoal;
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

public class Latcher extends AbstractDraconianSwarmEntity {
    private static final String MOVEMENT_CONTROLLER = "movement";
    public static final String ACTION_CONTROLLER = "action";
    public static final String BITE_TRIGGER = "bite";
    public static final String BITE_MOVE_TRIGGER = "bite_move";
    public static final String DIE_TRIGGER = "die";
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("latcher.animation.idle");
    private static final RawAnimation MOVE = RawAnimation.begin().thenLoop("latcher.animation.move");
    private static final RawAnimation BITE = RawAnimation.begin().thenPlay("latcher.animation.bite");
    private static final RawAnimation BITE_MOVE = RawAnimation.begin().thenPlay("latcher.animation.bite_move");
    private static final RawAnimation DIE = RawAnimation.begin().thenPlay("latcher.animation.die");

    public Latcher(EntityType<? extends Latcher> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.@NotNull Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 12.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(2, new LatcherBiteGoal(this));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, MOVEMENT_CONTROLLER, 4, state -> {
            state.setAndContinue(isMovingForAnimation() ? MOVE : IDLE);
            return PlayState.CONTINUE;
        }));
        controllers.add(new AnimationController<>(this, ACTION_CONTROLLER, 1, state -> PlayState.STOP)
                .triggerableAnim(BITE_TRIGGER, BITE)
                .triggerableAnim(BITE_MOVE_TRIGGER, BITE_MOVE)
                .triggerableAnim(DIE_TRIGGER, DIE));
    }

    public void performBiteAnimation(boolean moving) {
        triggerAnim(ACTION_CONTROLLER, moving ? BITE_MOVE_TRIGGER : BITE_TRIGGER);
        playSound(ModSounds.LATCHER_BITE.get(), 1.0F, 0.95F + getRandom().nextFloat() * 0.1F);
    }

    @Override
    protected void playDeathAnimation() {
        triggerAnim(ACTION_CONTROLLER, DIE_TRIGGER);
    }

    public boolean isMovingForAnimation() {
        return getDeltaMovement().lengthSqr() > 0.003D;
    }
}

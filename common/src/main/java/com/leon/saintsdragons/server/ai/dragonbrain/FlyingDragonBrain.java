package com.leon.saintsdragons.server.ai.dragonbrain;

import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.AirToGroundTransitionBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonFlightMovementRecoveryBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonRescueFallingOwnerBehaviour;
import com.leon.saintsdragons.server.entity.base.RideableFlyingDragon;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.schedule.Activity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public abstract class FlyingDragonBrain<T extends RideableFlyingDragon> extends DragonBrainProfile<T> {
    @Override
    public boolean usesDragonScent() {
        return true;
    }

    protected @Nullable DragonRescueFallingOwnerBehaviour.Config getRescueConfig() {
        return null;
    }

    @Override
    public final List<DragonBehaviour<T>> getDragonBrainBehaviours(DragonBehaviourGroup<T> group) {
        List<DragonBehaviour<T>> behaviours = new ArrayList<>(super.getDragonBrainBehaviours(group));
        if (group.activity() == Activity.FIGHT) {
            behaviours.add(0, new AirToGroundTransitionBehaviour<>());
            behaviours.add(new DragonFlightMovementRecoveryBehaviour<>());
        }
        return behaviours;
    }

    @Override
    protected final void addFeatureGroups(List<DragonBehaviourGroup<T>> groups) {
        var rescue = getRescueConfig();
        if (rescue == null) {
            return;
        }
        // Keep rescue ahead of ordinary idle behaviours, as in the original species profiles
        int index = 0;
        while (index < groups.size() && groups.get(index).activity() != Activity.IDLE) {
            index++;
        }
        groups.add(index, rescueGroup(rescue));
    }

    @Override
    protected boolean selectPriorityActivity(Brain<T> brain, T dragon) {
        var rescue = getRescueConfig();
        if (rescue != null && DragonRescueFallingOwnerBehaviour.updateRescueTarget(brain, dragon, rescue)) {
            brain.setActiveActivityIfPossible(Activity.PANIC);
            return true;
        }
        return false;
    }

    /** Species can add ability locks and engagement ranges after this common eligibility check. */
    @Override
    protected boolean canFight(T dragon, @Nullable LivingEntity target) {
        return super.canFight(dragon, target) && !dragon.isBaby() && !dragon.isPassenger();
    }

    private DragonBehaviourGroup<T> rescueGroup(DragonRescueFallingOwnerBehaviour.Config config) {
        return DragonBehaviourGroup.<T>activity(Activity.PANIC)
                .behaviours(new DragonRescueFallingOwnerBehaviour<>(config))
                .clearWhenStopped(DragonMemories.RESCUE_TARGET)
                .clearMovementWhenStopped()
                .build();
    }
}

package com.leon.saintsdragons.server.ai.dragonbrain;

import com.leon.saintsdragons.common.registry.ModSensorTypes;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.FirstApplicableDragonBehaviour;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.world.entity.schedule.Activity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class DragonBrainValidation {
    private DragonBrainValidation() {
    }

    static <T extends DragonEntity> void validate(DragonBrainOwner<T> profile, List<DragonBehaviourGroup<T>> groups) {
        Set<Activity> activities = new HashSet<>();
        for (DragonBehaviourGroup<T> group : groups) {
            if (!activities.add(group.activity())) {
                throw invalid(profile, "duplicate activity " + group.activity()
                        + "; feature activities must not also be declared in the species list");
            }
        }
        if (!activities.contains(Activity.CORE)) {
            throw invalid(profile, "missing CORE activity");
        }
        Activity fallback = profile.getDragonBrainFallbackActivity();
        if (fallback == Activity.CORE || !activities.contains(fallback)) {
            throw invalid(profile, "default activity must be a registered non-CORE activity: " + fallback);
        }
        Set<Activity> priorities = new HashSet<>();
        for (Activity activity : profile.getDragonBrainActivityPriority()) {
            if (!priorities.add(activity) || activity == Activity.CORE || !activities.contains(activity)) {
                throw invalid(profile, "invalid or duplicate activity priority " + activity);
            }
        }
        var sensors = profile.getDragonBrainSensors();
        if (new HashSet<>(sensors).size() != sensors.size()) {
            throw invalid(profile, "duplicate sensor registration");
        }
        boolean hasScentSensor = sensors.contains(ModSensorTypes.DRAGON_SCENT.get());
        if (hasScentSensor != profile.usesDragonScent()) {
            throw invalid(profile, "scent assessment and DRAGON_SCENT sensor registration disagree");
        }
    }

    static void validateBehaviours(DragonBrainOwner<?> profile, Activity activity,
                                  List<? extends DragonBehaviour<?>> behaviours) {
        for (DragonBehaviour<?> behaviour : behaviours) {
            if (!behaviour.claimBrainRegistration()) {
                throw invalid(profile, "reused " + behaviour.getClass().getSimpleName() + " in " + activity
                        + "; create fresh behaviour instances for each Brain and activity");
            }
            if (behaviour instanceof FirstApplicableDragonBehaviour<?> composite) {
                validateBehaviours(profile, activity, composite.childBehaviours());
            }
        }
    }

    private static IllegalStateException invalid(DragonBrainOwner<?> profile, String reason) {
        return new IllegalStateException("Invalid dragon Brain profile " + profile.getClass().getSimpleName() + ": " + reason);
    }
}

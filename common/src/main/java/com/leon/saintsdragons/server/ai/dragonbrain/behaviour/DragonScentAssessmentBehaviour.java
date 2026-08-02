package com.leon.saintsdragons.server.ai.dragonbrain.behaviour;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMovementIntent;
import com.leon.saintsdragons.server.ai.dragonbrain.perception.DragonInvestigation;
import com.leon.saintsdragons.server.ai.dragonbrain.perception.DragonScentEligibility;
import com.leon.saintsdragons.server.ai.dragonbrain.perception.DragonScentProfile;
import com.leon.saintsdragons.server.ai.dragonbrain.perception.DragonSensoryObservation;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.RideableDragonBase;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DragonScentAssessmentBehaviour<T extends DragonEntity> extends DragonBehaviour<T> {
    private static final DragonMovementIntent SCENT_STOP = DragonMovementIntent.stop("scent-assessment");

    private DragonSensoryObservation candidate;
    private int assessmentTicks;
    private int assessmentDuration;
    private String outcome = "idle";

    @Override
    protected boolean canStart(DragonBrainContext<T> context) {
        return canAssess(context);
    }

    @Override
    protected boolean canContinue(DragonBrainContext<T> context) {
        return assessmentTicks < assessmentDuration && canAssess(context);
    }

    @Override
    protected void start(DragonBrainContext<T> context) {
        candidate = context.memories().get(DragonMemories.SCENT_CANDIDATE).orElse(null);
        assessmentTicks = 0;
        outcome = "assessing";

        DragonScentProfile profile = DragonScentProfile.forDragon(context.dragon());
        assessmentDuration = randomBetween(
                context,
                profile.minAssessmentTicks(),
                profile.maxAssessmentTicks()
        );
        int cooldown = randomBetween(context, profile.minCooldownTicks(), profile.maxCooldownTicks());
        context.memories().set(DragonMemories.SCENT_COOLDOWN, true, cooldown);
        clearExistingMovement(context);
        claimMovement(context);
    }

    @Override
    protected void tick(DragonBrainContext<T> context) {
        if (candidate == null) {
            return;
        }
        claimMovement(context);
        context.dragon().getLookControl().setLookAt(
                candidate.position().x,
                candidate.position().y,
                candidate.position().z,
                10.0F,
                context.dragon().getMaxHeadXRot()
        );
        assessmentTicks++;
        if (assessmentTicks < assessmentDuration) {
            return;
        }

        context.memories().erase(DragonMemories.SCENT_CANDIDATE);
        if (context.memories().has(DragonMemories.ATTACK_TARGET)) {
            outcome = "combat-interrupted";
            return;
        }
        Entity source = candidate.sourceUuid() == null
                ? null
                : context.level().getEntity(candidate.sourceUuid());
        if (source instanceof LivingEntity living
                && living.isAlive()
                && context.dragon().getSensing().hasLineOfSight(living)) {
            outcome = "source-visible";
            return;
        }
        outcome = DragonInvestigation.remember(context.dragon(), candidate)
                ? "investigate"
                : "superseded";
    }

    @Override
    protected void stop(DragonBrainContext<T> context) {
        context.memories().erase(DragonMemories.SCENT_CANDIDATE);
        DragonMovementIntent intent = context.memories().get(DragonMemories.MOVEMENT_INTENT).orElse(null);
        if (SCENT_STOP.equals(intent)) {
            context.memories().erase(DragonMemories.MOVEMENT_INTENT);
        }
        candidate = null;
        assessmentTicks = 0;
        assessmentDuration = 0;
    }

    private boolean canAssess(DragonBrainContext<T> context) {
        return context.memories().has(DragonMemories.SCENT_CANDIDATE)
                && DragonScentEligibility.isAvailable(context.dragon(), context.memories());
    }

    private void claimMovement(DragonBrainContext<T> context) {
        context.memories().set(DragonMemories.MOVEMENT_INTENT, SCENT_STOP);
    }

    private void clearExistingMovement(DragonBrainContext<T> context) {
        context.dragon().getNavigation().stop();
        if (context.dragon() instanceof RideableDragonBase dragon) {
            dragon.getAIMovement().stopAndClearAllMovement();
        }
    }

    private int randomBetween(DragonBrainContext<T> context, int minimum, int maximum) {
        return minimum + context.dragon().getRandom().nextInt(maximum - minimum + 1);
    }

    @Override
    public Map<String, String> getDragonBrainDebugDetails() {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("candidate", candidate == null ? "none" : candidate.position().toString());
        details.put("progress", assessmentTicks + "/" + assessmentDuration);
        details.put("outcome", outcome);
        return Map.copyOf(details);
    }
}

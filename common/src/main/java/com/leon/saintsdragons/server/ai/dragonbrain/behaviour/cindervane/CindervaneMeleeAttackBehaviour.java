package com.leon.saintsdragons.server.ai.dragonbrain.behaviour.cindervane;

import com.leon.saintsdragons.common.registry.ModAbilities;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.entity.base.DragonLocomotionMode;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

import java.util.Map;

public class CindervaneMeleeAttackBehaviour extends DragonBehaviour<Cindervane> {
    private static final double ATTACK_RANGE = 4.5D;
    private static final double AIR_ATTACK_RANGE = 6.0D;
    private static final double FIRE_BODY_ACTIVATION_RANGE = 8.0D;
    private int attackCooldown;
    private int fireBodyCheckCooldown;

    public CindervaneMeleeAttackBehaviour() {
        super(Map.of(DragonMemories.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT));
    }

    @Override
    protected boolean canStart(DragonBrainContext<Cindervane> context) {
        boolean targetAirborne = context.memories().get(DragonMemories.TARGET_AIRBORNE).orElse(false);
        boolean routeAbandoned = context.memories()
                .get(DragonMemories.GROUND_ROUTE_ABANDONED)
                .orElse(false);
        boolean aerialEngagement = targetAirborne || routeAbandoned;
        DragonLocomotionMode mode = context.memories()
                .get(DragonMemories.LOCOMOTION_MODE)
                .orElse(context.dragon().getLocomotionMode());
        if (aerialEngagement && mode != DragonLocomotionMode.AIR) {
            return false;
        }
        if (!aerialEngagement
                && mode != DragonLocomotionMode.GROUND
                && !context.dragon().isInWaterOrBubble()) {
            return false;
        }
        return context.memories().get(DragonMemories.ATTACK_TARGET)
                .filter(context.dragon()::isTargetValid)
                .isPresent();
    }

    @Override
    protected boolean canContinue(DragonBrainContext<Cindervane> context) {
        return canStart(context);
    }

    @Override
    protected void tick(DragonBrainContext<Cindervane> context) {
        if (attackCooldown > 0) {
            attackCooldown--;
        }
        if (fireBodyCheckCooldown > 0) {
            fireBodyCheckCooldown--;
        }

        Cindervane dragon = context.dragon();
        LivingEntity target = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        if (target == null) {
            deactivateFireBodyIfActive(dragon);
            return;
        }

        boolean aerialEngagement = context.memories().get(DragonMemories.TARGET_AIRBORNE).orElse(false)
                || context.memories().get(DragonMemories.GROUND_ROUTE_ABANDONED).orElse(false);
        double attackRangeSqr = aerialEngagement
                ? AIR_ATTACK_RANGE * AIR_ATTACK_RANGE
                : getAttackReachSqr(dragon, target);
        if (dragon.distanceToSqr(target) <= attackRangeSqr
                && dragon.getSensing().hasLineOfSight(target)) {
            tryPerformBite(dragon, target);
        }

        if (dragon.isInWaterOrBubble()) {
            deactivateFireBodyIfActive(dragon);
            return;
        }
        handleFireBodyActivation(dragon, target);
    }

    @Override
    protected void stop(DragonBrainContext<Cindervane> context) {
        attackCooldown = 0;
        fireBodyCheckCooldown = 0;
        deactivateFireBodyIfActive(context.dragon());
    }

    private void tryPerformBite(Cindervane dragon, LivingEntity target) {
        if (attackCooldown > 0
                || dragon.getAiCombatPacing().getCadenceCooldownTicks() > 0
                || dragon.isAbilityActive(ModAbilities.CINDERVANE_BITE)
                || !dragon.getSensing().hasLineOfSight(target)
                || !dragon.combatManager.canStart(ModAbilities.CINDERVANE_BITE)
                || !dragon.getAiCombatPacing().canUse(ModAbilities.CINDERVANE_BITE, false)) {
            return;
        }

        dragon.combatManager.tryUseAbility(ModAbilities.CINDERVANE_BITE);
        dragon.getAiCombatPacing().recordUse(ModAbilities.CINDERVANE_BITE, 40, 40, false, 0, 28);
        attackCooldown = 40;
    }

    private void handleFireBodyActivation(Cindervane dragon, LivingEntity target) {
        if (fireBodyCheckCooldown > 0 || dragon.isVehicle()) {
            return;
        }

        boolean fireBodyActive = dragon.isAbilityActive(ModAbilities.CINDERVANE_FIRE_BODY);
        double distanceToTarget = dragon.distanceTo(target);
        if (!fireBodyActive && distanceToTarget < FIRE_BODY_ACTIVATION_RANGE) {
            dragon.combatManager.tryUseAbility(ModAbilities.CINDERVANE_FIRE_BODY);
            fireBodyCheckCooldown = 40;
        } else if (fireBodyActive && distanceToTarget > FIRE_BODY_ACTIVATION_RANGE * 1.5D) {
            dragon.forceEndAbility(ModAbilities.CINDERVANE_FIRE_BODY);
            fireBodyCheckCooldown = 40;
        }
    }

    private void deactivateFireBodyIfActive(Cindervane dragon) {
        if (!dragon.isVehicle() && dragon.isAbilityActive(ModAbilities.CINDERVANE_FIRE_BODY)) {
            dragon.forceEndAbility(ModAbilities.CINDERVANE_FIRE_BODY);
        }
    }

    private double getAttackReachSqr(Cindervane dragon, LivingEntity target) {
        double combinedRadii = (dragon.getBbWidth() + target.getBbWidth()) * 0.5D;
        double reach = ATTACK_RANGE + combinedRadii;
        return reach * reach;
    }

    public static double groundStopRange(LivingEntity target) {
        return ATTACK_RANGE;
    }
}

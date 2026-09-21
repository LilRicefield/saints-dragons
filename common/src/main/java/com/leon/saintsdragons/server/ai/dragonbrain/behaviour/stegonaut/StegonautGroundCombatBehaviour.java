package com.leon.saintsdragons.server.ai.dragonbrain.behaviour.stegonaut;

import com.leon.saintsdragons.common.config.dragon.profile.StegonautStatProfile;

import com.leon.saintsdragons.common.registry.ModAbilities;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

import java.util.Map;

public class StegonautGroundCombatBehaviour extends DragonBehaviour<Stegonaut> {
    public static final double GROUND_ATTACK_RANGE = StegonautStatProfile.GroundCombatBehaviour.GROUND_ATTACK_RANGE;
    public static final double WATER_ATTACK_RANGE = StegonautStatProfile.GroundCombatBehaviour.WATER_ATTACK_RANGE;
    private static final int ATTACK_COOLDOWN_TICKS = StegonautStatProfile.GroundCombatBehaviour.ATTACK_COOLDOWN_TICKS;

    private int attackCooldown;
    private String lastDecision = "idle";

    public StegonautGroundCombatBehaviour() {
        super(Map.of(DragonMemories.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT));
    }

    @Override
    protected boolean canStart(DragonBrainContext<Stegonaut> context) {
        LivingEntity target = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        Stegonaut dragon = context.dragon();
        return target != null
                && dragon.isTargetValid(target)
                && dragon.canTarget(target)
                && !dragon.isVehicle()
                && !dragon.isOrderedToSit();
    }

    @Override
    protected boolean canContinue(DragonBrainContext<Stegonaut> context) {
        return canStart(context);
    }

    @Override
    protected void tick(DragonBrainContext<Stegonaut> context) {
        if (attackCooldown > 0) {
            attackCooldown--;
        }

        Stegonaut dragon = context.dragon();
        LivingEntity target = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        if (target == null) {
            decision(dragon, "no-target");
            return;
        }
        if (isAttacking(dragon)) {
            decision(dragon, "attack-committed");
            return;
        }
        if (attackCooldown > 0 || dragon.getAiCombatPacing().getCadenceCooldownTicks() > 0) {
            decision(dragon, "cadence-cooldown");
            return;
        }
        if (!dragon.getSensing().hasLineOfSight(target)) {
            decision(dragon, "no-line-of-sight");
            return;
        }
        if (dragon.distanceToSqr(target) > attackReachSqr(dragon, target)) {
            decision(dragon, "out-of-range");
            return;
        }

        boolean biteReady = dragon.combatManager.canStartAiAbility(ModAbilities.STEGONAUT_BITE, false);
        boolean slamReady = dragon.combatManager.canStartAiAbility(ModAbilities.STEGONAUT_CHIN_SLAM, false);
        if (!biteReady && !slamReady) {
            decision(dragon, "no-usable-attack");
            return;
        }

        var ability = biteReady && slamReady ? dragon.getRandomAiAttackAbility()
                : biteReady ? ModAbilities.STEGONAUT_BITE : ModAbilities.STEGONAUT_CHIN_SLAM;
        if (dragon.combatManager.tryUseAiAbility(
                ability,
                false,
                ATTACK_COOLDOWN_TICKS,
                ATTACK_COOLDOWN_TICKS,
                0,
                22
        )) {
            attackCooldown = ATTACK_COOLDOWN_TICKS;
            lastDecision = "attack-started";
        } else {
            lastDecision = "startup-rejected";
        }
    }

    @Override
    protected void stop(DragonBrainContext<Stegonaut> context) {
        attackCooldown = 0;
        lastDecision = "stopped";
    }

    private void decision(Stegonaut dragon, String reason) {
        lastDecision = reason;
        dragon.combatManager.recordAiDecision("stegonaut-combat", reason);
    }

    @Override
    public Map<String, String> getDragonBrainDebugDetails() {
        return Map.of("decision", lastDecision, "attack_cooldown", Integer.toString(attackCooldown));
    }

    public static boolean isAttacking(Stegonaut dragon) {
        return dragon.combatManager.isAbilityActive(ModAbilities.STEGONAUT_BITE)
                || dragon.combatManager.isAbilityActive(ModAbilities.STEGONAUT_CHIN_SLAM);
    }

    public static double attackRange(Stegonaut dragon) {
        return dragon.isInWaterOrBubble() ? WATER_ATTACK_RANGE : GROUND_ATTACK_RANGE;
    }

    private static double attackReachSqr(Stegonaut dragon, LivingEntity target) {
        double combinedRadii = (dragon.getBbWidth() + target.getBbWidth()) * 0.5D;
        double reach = attackRange(dragon) + combinedRadii;
        return reach * reach;
    }
}

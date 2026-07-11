package com.leon.saintsdragons.server.ai.dragonbrain.behaviour.stegonaut;

import com.leon.saintsdragons.common.registry.ModAbilities;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

import java.util.Map;

public class StegonautGroundCombatBehaviour extends DragonBehaviour<Stegonaut> {
    public static final double GROUND_ATTACK_RANGE = 3.4D;
    public static final double WATER_ATTACK_RANGE = 6.0D;
    private static final int ATTACK_COOLDOWN_TICKS = 26;

    private int attackCooldown;

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
        if (target == null
                || attackCooldown > 0
                || dragon.getAiCombatPacing().getCadenceCooldownTicks() > 0
                || isAttacking(dragon)
                || !dragon.getSensing().hasLineOfSight(target)
                || dragon.distanceToSqr(target) > attackReachSqr(dragon, target)) {
            return;
        }

        var ability = dragon.getRandomAiAttackAbility();
        if (!dragon.combatManager.canStart(ability)
                || !dragon.getAiCombatPacing().canUse(ability, false)) {
            return;
        }

        dragon.combatManager.tryUseAbility(ability);
        dragon.getAiCombatPacing().recordUse(
                ability,
                ATTACK_COOLDOWN_TICKS,
                ATTACK_COOLDOWN_TICKS,
                false,
                0,
                22
        );
        attackCooldown = ATTACK_COOLDOWN_TICKS;
    }

    @Override
    protected void stop(DragonBrainContext<Stegonaut> context) {
        attackCooldown = 0;
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

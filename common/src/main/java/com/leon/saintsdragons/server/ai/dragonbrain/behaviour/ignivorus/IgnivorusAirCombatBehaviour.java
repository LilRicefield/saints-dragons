package com.leon.saintsdragons.server.ai.dragonbrain.behaviour.ignivorus;

import com.leon.saintsdragons.common.registry.ModAbilities;
import com.leon.saintsdragons.server.ai.RangedAirCombatSettings;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.RangedAirCombatBehaviour;
import com.leon.saintsdragons.server.ai.DragonTargetingHelper;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.entity.interfaces.DragonFlightCapable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;

public class IgnivorusAirCombatBehaviour extends RangedAirCombatBehaviour<Ignivorus> {
    private static final int BREATH_COOLDOWN_TICKS = 2400;
    private static final double MOUNTED_FLIGHT_CLEARANCE = 3.0D;
    private static final RangedAirCombatSettings COMBAT_SETTINGS = new RangedAirCombatSettings(
            3.75D,
            5.5D,
            7.0D,
            42.0D,
            16.0D,
            3.5D,
            20.0D,
            64.0D,
            25.0D,
            30,
            60,
            BREATH_COOLDOWN_TICKS
    );

    public IgnivorusAirCombatBehaviour() {
        super(COMBAT_SETTINGS);
    }

    @Override
    protected void prepareStartConditions(Ignivorus dragon, LivingEntity target) {
        if (shouldExitPhase2ForAirPursuit(dragon, target)) {
            dragon.exitWildPhase2ForAirPursuit();
        }
    }

    private boolean shouldExitPhase2ForAirPursuit(Ignivorus dragon, LivingEntity target) {
        if (!dragon.isPhase2Active() || target == null) {
            return false;
        }
        if (target instanceof Player player && player.isFallFlying()) {
            return true;
        }
        if (!(target.getVehicle() instanceof LivingEntity vehicle)
                || !(vehicle instanceof DragonFlightCapable flightCapable)) {
            return false;
        }
        if (flightCapable.isTakeoff()) {
            return true;
        }
        if ((!flightCapable.isFlying() && !flightCapable.isHovering())
                || flightCapable.isLanding()
                || vehicle.onGround()) {
            return false;
        }

        double groundY = vehicle.level()
                .getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, vehicle.blockPosition())
                .getY();
        return vehicle.getY() - groundY > MOUNTED_FLIGHT_CLEARANCE;
    }

    @Override
    protected boolean checkExtraStartConditions(Ignivorus dragon, LivingEntity target) {
        return canUseAirCombat(dragon);
    }

    @Override
    protected boolean checkExtraContinueConditions(Ignivorus dragon, LivingEntity target) {
        return canUseAirCombat(dragon);
    }

    @Override
    protected boolean isMeleeAttackActive(Ignivorus dragon) {
        return dragon.isAbilityActive(ModAbilities.IGNIVORUS_BITE);
    }

    @Override
    protected boolean isRangedAttackActive(Ignivorus dragon) {
        return dragon.isAbilityActive(ModAbilities.IGNIVORUS_FIRE_BREATH);
    }

    @Override
    protected boolean isAdditionalAttackActive(Ignivorus dragon) {
        return dragon.isLeaping() || dragon.isLeapImpactRecovering();
    }

    @Override
    protected boolean canUseRangedAttack(Ignivorus dragon, LivingEntity target) {
        return !DragonTargetingHelper.isBiteOnlyPreyTarget(dragon, target);
    }

    @Override
    protected boolean tryStartMeleeAttack(Ignivorus dragon, LivingEntity target) {
        if (!canUseAiAbility(dragon, ModAbilities.IGNIVORUS_BITE, false)
                || !dragon.combatManager.tryUseAbility(ModAbilities.IGNIVORUS_BITE)) {
            return false;
        }
        dragon.getAiCombatPacing().recordUse(ModAbilities.IGNIVORUS_BITE, 30, 30, false, 0, 24);
        return true;
    }

    @Override
    protected boolean tryStartRangedAttack(Ignivorus dragon, LivingEntity target) {
        if (!canUseAiAbility(dragon, ModAbilities.IGNIVORUS_FIRE_BREATH, true)
                || !dragon.combatManager.tryUseAbility(ModAbilities.IGNIVORUS_FIRE_BREATH)) {
            return false;
        }
        dragon.getAiCombatPacing().recordUse(
                ModAbilities.IGNIVORUS_FIRE_BREATH,
                60,
                BREATH_COOLDOWN_TICKS,
                true,
                180,
                80
        );
        return true;
    }

    private boolean canUseAirCombat(Ignivorus dragon) {
        return !dragon.isBaby()
                && !dragon.isAiSpecialCombatActive()
                && !dragon.areRiderControlsLocked()
                && !dragon.isLeaping()
                && !dragon.isLeapImpactRecovering();
    }

    private boolean canUseAiAbility(Ignivorus dragon,
                                    com.leon.saintsdragons.server.entity.ability.DragonAbilityType<?, ?> abilityType,
                                    boolean majorAbility) {
        return dragon.combatManager.canStart(abilityType)
                && dragon.getAiCombatPacing().canUse(abilityType, majorAbility);
    }
}

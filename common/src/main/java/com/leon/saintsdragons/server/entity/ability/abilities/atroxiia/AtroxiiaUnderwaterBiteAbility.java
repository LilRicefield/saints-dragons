package com.leon.saintsdragons.server.entity.ability.abilities.atroxiia;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.DragonMeleeGeometry;
import com.leon.saintsdragons.server.entity.dragons.atroxiia.Atroxiia;
import com.leon.saintsdragons.server.entity.dragons.atroxiia.handlers.AtroxiiaAnimationHandler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.RECOVERY;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.STARTUP;

public final class AtroxiiaUnderwaterBiteAbility extends DragonAbility<Atroxiia> {
    private static final float BASE_DAMAGE = 10.0F;
    private static final double RANGE = 5.0D;
    private static final double HITBOX_HALF_WIDTH = 3.0D;
    private static final double HITBOX_HALF_HEIGHT = 2.75D;
    private static final double ANGLE_DEGREES = 95.0D;
    private static final double CLOSE_HIT_RANGE = 2.5D;

    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(STARTUP, 9),
            new AbilitySectionDuration(ACTIVE, 2),
            new AbilitySectionDuration(RECOVERY, 8)
    };

    private boolean appliedHit;

    public AtroxiiaUnderwaterBiteAbility(
            DragonAbilityType<Atroxiia, AtroxiiaUnderwaterBiteAbility> type,
            Atroxiia user) {
        super(type, user, TRACK, 12);
    }

    @Override
    public boolean tryAbility() {
        Atroxiia dragon = getUser();
        if (dragon.isBaby() || !dragon.isInWaterOrBubble()) {
            return false;
        }
        if (dragon.getControllingPassenger() != null) {
            return true;
        }
        LivingEntity target = dragon.getTarget();
        return !dragon.isVehicle()
                && !dragon.isPassenger()
                && dragon.isTargetValid(target)
                && dragon.canTarget(target);
    }

    @Override
    public boolean isOverlayAbility() {
        return true;
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section != null && section.sectionType == STARTUP) {
            getUser().triggerAnim(AtroxiiaAnimationHandler.FAST_ACTION_CONTROLLER, "underwater_bite");
            appliedHit = false;
        }
    }

    @Override
    public void tickUsing() {
        DragonAbilitySection section = getCurrentSection();
        if (section == null || section.sectionType != ACTIVE || appliedHit) {
            return;
        }

        Atroxiia dragon = getUser();
        for (LivingEntity target : findTargets(dragon)) {
            applyHit(dragon, target);
        }
        appliedHit = true;
    }

    private List<LivingEntity> findTargets(Atroxiia dragon) {
        if (dragon.getControllingPassenger() == null) {
            LivingEntity target = dragon.getTarget();
            double combinedRadii = target == null
                    ? 0.0D
                    : (dragon.getBbWidth() + target.getBbWidth()) * 0.5D;
            double gap = target == null
                    ? Double.MAX_VALUE
                    : Math.max(0.0D, dragon.distanceTo(target) - combinedRadii);
            return DragonMeleeGeometry.isDirectAiTargetValid(dragon, target) && gap <= RANGE
                    ? List.of(target)
                    : List.of();
        }

        return DragonMeleeGeometry.findForwardTargets(
                dragon,
                RANGE,
                HITBOX_HALF_WIDTH,
                HITBOX_HALF_HEIGHT,
                ANGLE_DEGREES,
                CLOSE_HIT_RANGE,
                entity -> !dragon.isAlly(entity)
        );
    }

    private void applyHit(Atroxiia dragon, LivingEntity target) {
        float damage = (float) DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.ATROXIIA_ID)
                .abilityDamage("underwater_bite", BASE_DAMAGE);
        damage *= dragon.getHungerMeleeDamageMultiplier();

        DamageSource source = dragon.level().damageSources().mobAttack(dragon);
        target.hurt(source, damage);

        Vec3 push = DragonMeleeGeometry.forwardAttack(dragon).forward().scale(0.2D);
        target.push(push.x, 0.05D, push.z);
    }
}

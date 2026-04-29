package com.leon.saintsdragons.server.entity.ability.abilities.cindervane;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.DragonMeleeGeometry;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.RECOVERY;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.STARTUP;

public class CindervaneBiteAbility extends DragonAbility<Cindervane> {
    private static final float BASE_DAMAGE = 12.0f;
    private static final double RANGE = 10.0;
    private static final double AIR_RANGE_BONUS = 0.6;
    private static final double ANGLE_DEGREES = 35.0;

    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(STARTUP, 5),
            new AbilitySectionDuration(ACTIVE, 2),
            new AbilitySectionDuration(RECOVERY, 5)
    };

    private boolean appliedHit;

    public CindervaneBiteAbility(DragonAbilityType<Cindervane, CindervaneBiteAbility> type,
                                 Cindervane user) {
        super(type, user, TRACK, 15);
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) {
            return;
        }

        if (section.sectionType == STARTUP) {
            Cindervane dragon = getUser();
            String animation = dragon.isFlying() ? "bite_air" : "bite";
            dragon.triggerAnim("actions", animation);
            if (!dragon.level().isClientSide) {
                dragon.getSoundHandler().playMovingEntitySound(ModSounds.CINDERVANE_BITE.get(), 1.0f, 0.95f, 25);
            }
            appliedHit = false;
        }
    }

    @Override
    public void tickUsing() {
        DragonAbilitySection section = getCurrentSection();
        if (section == null) {
            return;
        }

        if (section.sectionType == ACTIVE && !appliedHit) {
            Cindervane dragon = getUser();

            List<LivingEntity> targets = selectTargets();

            if (targets.isEmpty()) {
                LivingEntity currentTarget = dragon.getTarget();
                if (currentTarget != null && currentTarget.isAlive() && !dragon.isAlly(currentTarget)) {
                    targets = List.of(currentTarget);
                }
            }

            for (LivingEntity target : targets) {
                applyHit(dragon, target);
            }

            appliedHit = true;
        }
    }

    private void applyHit(Cindervane dragon, LivingEntity target) {
        float damage = (float) DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.CINDERVANE_ID)
                .abilityDamage("bite", BASE_DAMAGE);
        damage *= dragon.getHungerMeleeDamageMultiplier();
        DamageSource source = dragon.level().damageSources().mobAttack(dragon);
        target.hurt(source, damage);

        Vec3 push = dragon.getLookAngle().scale(0.3);
        target.push(push.x, dragon.isFlying() ? 0.15 : 0.05, push.z);
    }

    private List<LivingEntity> selectTargets() {
        Cindervane dragon = getUser();
        double range = RANGE;
        if (dragon.isFlying()) {
            range += AIR_RANGE_BONUS;
        }

        if (dragon.getControllingPassenger() == null) {
            LivingEntity target = dragon.getTarget();
            if (DragonMeleeGeometry.isDirectAiTargetValid(dragon, target, 1.5D)) {
                return List.of(target);
            }
            return List.of();
        }

        return DragonMeleeGeometry.findForwardTargets(
                dragon,
                range,
                ANGLE_DEGREES,
                entity -> !dragon.isAlly(entity)
        );
    }

}

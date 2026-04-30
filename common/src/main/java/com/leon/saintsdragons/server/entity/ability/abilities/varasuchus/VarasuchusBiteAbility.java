package com.leon.saintsdragons.server.entity.ability.abilities.varasuchus;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.DragonMeleeGeometry;
import com.leon.saintsdragons.server.entity.dragons.varasuchus.Varasuchus;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.RECOVERY;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.STARTUP;

public class VarasuchusBiteAbility extends DragonAbility<Varasuchus> {
    private static final int SOUND_TICKS = 24;
    private static final float BASE_DAMAGE = 15.0f;
    private static final float DEFAULT_ATTACK_DAMAGE = 10.0f;
    private static final double RANGE = 5.0;
    private static final double HITBOX_HALF_WIDTH = 3.0;
    private static final double HITBOX_HALF_HEIGHT = 1.35;
    private static final double CLOSE_HIT_RANGE = 2.5;
    private static final double ANGLE_DEGREES = 90.0;

    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(STARTUP, 5),
            new AbilitySectionDuration(ACTIVE, 6),
            new AbilitySectionDuration(RECOVERY, 6)
    };

    private boolean appliedHit;

    public VarasuchusBiteAbility(DragonAbilityType<Varasuchus, VarasuchusBiteAbility> type,
                                 Varasuchus user) {
        super(type, user, TRACK, 15);
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) {
            return;
        }

        if (section.sectionType == STARTUP) {
            Varasuchus dragon = getUser();
            dragon.triggerAnim("action", "bite");
            if (!dragon.level().isClientSide) {
                dragon.getSoundHandler().playMovingEntitySound(ModSounds.VARASUCHUS_BITE1.get(), 1.0f, 1.0f, SOUND_TICKS);
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
            Varasuchus dragon = getUser();
            List<LivingEntity> targets = findAllTargetsInCone();

            for (LivingEntity target : targets) {
                applyHit(dragon, target);
            }

            appliedHit = true;
        }
    }

    private void applyHit(Varasuchus dragon, LivingEntity target) {
        float damage = resolveBaseDamage();
        AttributeInstance attackAttr = dragon.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackAttr != null && DEFAULT_ATTACK_DAMAGE > 0.0f) {
            double value = attackAttr.getValue();
            if (value > 0) {
                damage *= value / DEFAULT_ATTACK_DAMAGE;
            }
        }

        damage *= dragon.getHungerMeleeDamageMultiplier();
        DamageSource source = dragon.level().damageSources().mobAttack(dragon);
        target.hurt(source, damage);

        Vec3 push = dragon.getLookAngle().scale(0.3);
        target.push(push.x, dragon.isSwimming() ? 0.15 : 0.05, push.z);
    }

    private List<LivingEntity> findAllTargetsInCone() {
        Varasuchus dragon = getUser();
        boolean ridden = dragon.getControllingPassenger() != null;

        if (!ridden) {
            LivingEntity target = dragon.getTarget();
            if (DragonMeleeGeometry.isDirectAiTargetValid(dragon, target, 1.5D)) {
                return List.of(target);
            }
            return List.of();
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

    private float resolveBaseDamage() {
        return (float) DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.VARASUCHUS_ID)
                .abilityDamage("bite_phase1", BASE_DAMAGE);
    }
}

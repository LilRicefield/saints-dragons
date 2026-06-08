package com.leon.saintsdragons.server.entity.ability.abilities.stegonaut;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.DragonMeleeGeometry;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.handlers.StegonautAnimationHandler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.RECOVERY;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.STARTUP;

public class StegonautBiteAbility extends DragonAbility<Stegonaut> {
    private static final float BASE_DAMAGE = 5.0f;
    private static final int HIT_TICK = 13;
    private static final double RANGE = 4.0;
    private static final double ANGLE_DEGREES = 80.0;

    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(STARTUP, 7),
            new AbilitySectionDuration(ACTIVE, 3),
            new AbilitySectionDuration(RECOVERY, 11)
    };

    private boolean appliedHit;

    public StegonautBiteAbility(DragonAbilityType<Stegonaut, StegonautBiteAbility> type, Stegonaut user) {
        super(type, user, TRACK, 16);
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) {
            return;
        }
        if (section.sectionType == STARTUP) {
            getUser().triggerAnim(StegonautAnimationHandler.ACTION_CONTROLLER, "bite");
            if (!getUser().level().isClientSide) {
                getUser().getSoundHandler().playMovingEntitySound(ModSounds.STEGONAUT_BITE.get(), 1.0f, getUser().isBaby() ? 1.6f : 1.0f, 59);
            }
            appliedHit = false;
        }
    }

    @Override
    public void tickUsing() {
        if (!appliedHit && getTicksInUse() >= HIT_TICK) {
            List<LivingEntity> targets = findTargets();
            for (LivingEntity target : targets) {
                applyHit(target);
            }
            appliedHit = true;
        }
    }

    private void applyHit(LivingEntity target) {
        Stegonaut dragon = getUser();
        float damage = resolveDamage() * dragon.getHungerMeleeDamageMultiplier();
        DamageSource source = dragon.level().damageSources().mobAttack(dragon);
        target.hurt(source, damage);

        Vec3 push = dragon.getLookAngle().scale(0.25);
        target.push(push.x, 0.06, push.z);
    }

    private List<LivingEntity> findTargets() {
        Stegonaut dragon = getUser();

        if (dragon.getControllingPassenger() == null) {
            LivingEntity target = dragon.getTarget();
            if (DragonMeleeGeometry.isDirectAiTargetValid(dragon, target)) {
                return List.of(target);
            }
            return List.of();
        }

        return DragonMeleeGeometry.findForwardTargets(
                dragon,
                RANGE,
                ANGLE_DEGREES,
                entity -> !dragon.isAlly(entity)
        );
    }

    private float resolveDamage() {
        return (float) DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.STEGONAUT_ID)
                .abilityDamage("bite", BASE_DAMAGE);
    }
}

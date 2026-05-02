package com.leon.saintsdragons.server.entity.ability.abilities.stegonaut;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.DragonMeleeGeometry;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.RECOVERY;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.STARTUP;

public class StegonautChinSlamAbility extends DragonAbility<Stegonaut> {
    private static final float BASE_DAMAGE = 8.0f;
    private static final int HIT_TICK = 13;
    private static final float ARMOR_PENETRATION = 4.0f;
    private static final double RANGE = 5.0;
    private static final double SLAM_ANGLE_DEG = 95.0;
    private static final double SWEEP_HORIZONTAL = 4.5;
    private static final double SWEEP_VERTICAL = 4.5;

    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(STARTUP, 6),
            new AbilitySectionDuration(ACTIVE, 3),
            new AbilitySectionDuration(RECOVERY, 8)
    };

    private boolean appliedHit;

    public StegonautChinSlamAbility(DragonAbilityType<Stegonaut, StegonautChinSlamAbility> type, Stegonaut user) {
        super(type, user, TRACK, 18);
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) {
            return;
        }
        if (section.sectionType == STARTUP) {
            getUser().triggerAnim("action", "chin_slam");
            if (!getUser().level().isClientSide) {
                getUser().getSoundHandler().playMovingEntitySound(ModSounds.STEGONAUT_CHIN_SLAM.get(), 1.0f, getUser().isBaby() ? 1.6f : 1.0f, 51);
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
        float damage = (resolveDamage() + ARMOR_PENETRATION) * dragon.getHungerMeleeDamageMultiplier();
        DamageSource source = dragon.level().damageSources().mobAttack(dragon);
        target.hurt(source, damage);

        Vec3 push = dragon.getLookAngle().scale(0.3);
        target.push(push.x, 0.14, push.z);
    }

    private List<LivingEntity> findTargets() {
        Stegonaut dragon = getUser();
        double range = RANGE;

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
                SWEEP_HORIZONTAL,
                SWEEP_VERTICAL,
                SLAM_ANGLE_DEG,
                range * 0.4D,
                entity -> !dragon.isAlly(entity)
        );
    }

    private float resolveDamage() {
        return (float) DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.STEGONAUT_ID)
                .abilityDamage("chin_slam", BASE_DAMAGE);
    }
}
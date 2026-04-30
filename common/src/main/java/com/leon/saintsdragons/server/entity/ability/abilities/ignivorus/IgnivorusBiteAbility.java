package com.leon.saintsdragons.server.entity.ability.abilities.ignivorus;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.DragonMeleeGeometry;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.RECOVERY;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.STARTUP;

public class IgnivorusBiteAbility extends DragonAbility<Ignivorus> {
    private static final float BASE_DAMAGE = 50.0f;
    private static final float ARMOR_PENETRATION = 5.0f;
    private static final double RANGE = 10.0;
    private static final double AIR_RANGE_BONUS = 2.0;
    private static final double HITBOX_HALF_WIDTH = 7.5;
    private static final double HITBOX_HALF_HEIGHT = 2.6;
    private static final double CLOSE_HIT_RANGE = 4.25;
    private static final double ANGLE_DEGREES = 80.0;

    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(STARTUP, 6),
            new AbilitySectionDuration(ACTIVE, 3),
            new AbilitySectionDuration(RECOVERY, 8)
    };

    private boolean appliedHit;

    public IgnivorusBiteAbility(DragonAbilityType<Ignivorus, IgnivorusBiteAbility> type,
                                Ignivorus user) {
        super(type, user, TRACK, 20);
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) {
            return;
        }

        if (section.sectionType == STARTUP) {
            Ignivorus dragon = getUser();
            dragon.triggerAnim("action", "bite");
            if (!dragon.level().isClientSide) {
                dragon.getSoundHandler().playMovingEntitySound(ModSounds.IGNIVORUS_BITE.get(), 1.0f, 1.0f, 60);
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
            Ignivorus dragon = getUser();

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

    private void applyHit(Ignivorus dragon, LivingEntity target) {
        float damage = resolveBiteDamage();
        float hungerMult = dragon.getHungerMeleeDamageMultiplier();
        DamageSource physicalSource = dragon.level().damageSources().mobAttack(dragon);
        float armorPenDamage = (damage + ARMOR_PENETRATION) * hungerMult;

        target.hurt(physicalSource, armorPenDamage);
        target.setSecondsOnFire(3);

        Vec3 push = dragon.getLookAngle().scale(dragon.isFlying() ? 0.4 : 0.25);
        target.push(push.x, dragon.isFlying() ? 0.2 : 0.08, push.z);
    }

    private float resolveBiteDamage() {
        return (float) DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.IGNIVORUS_ID)
                .abilityDamage("bite", BASE_DAMAGE);
    }

    private List<LivingEntity> selectTargets() {
        Ignivorus dragon = getUser();

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
                HITBOX_HALF_WIDTH,
                HITBOX_HALF_HEIGHT,
                ANGLE_DEGREES,
                CLOSE_HIT_RANGE,
                entity -> !dragon.isAlly(entity)
        );
    }

}

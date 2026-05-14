package com.leon.saintsdragons.server.entity.ability.abilities.varasuchus;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.varasuchus.Varasuchus;
import com.leon.saintsdragons.server.entity.dragons.varasuchus.handlers.VarasuchusAnimationHandler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.RECOVERY;

public class VarasuchusTailguardAbility extends DragonAbility<Varasuchus> {
    private static final int GUARD_TICKS = (int) Math.round(1.6667D * 20.0D);
    private static final int PARRY_TICKS = (int) Math.round(0.8333D * 20.0D);
    private static final int GUARD_COOLDOWN_TICKS = 4 * 20;
    private static final int PARRY_COOLDOWN_TICKS = 15 * 20;
    private static final float DEFAULT_PARRY_DAMAGE = 10.0F;
    private static final double PARRY_RANGE_SQR = 8.0D * 8.0D;
    private static final double PARRY_SWEEP_HORIZONTAL = 6.5D;
    private static final double PARRY_SWEEP_VERTICAL = 3.0D;
    private static final double KNOCKBACK_STRENGTH = 1.5D;

    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(ACTIVE, GUARD_TICKS),
            new AbilitySectionDuration(RECOVERY, PARRY_TICKS)
    };

    private boolean parried;
    private LivingEntity parriedTarget;

    public VarasuchusTailguardAbility(DragonAbilityType<Varasuchus, VarasuchusTailguardAbility> type,
                                      Varasuchus user) {
        super(type, user, TRACK, GUARD_COOLDOWN_TICKS);
    }

    @Override
    public boolean tryAbility() {
        Varasuchus dragon = getUser();
        return !dragon.isPhaseTwoActive();
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) {
            return;
        }

        Varasuchus dragon = getUser();
        if (section.sectionType == ACTIVE) {
            parried = false;
            parriedTarget = null;
            dragon.lockRiderControls(GUARD_TICKS);
            dragon.triggerAnim(VarasuchusAnimationHandler.FAST_ACTION_CONTROLLER, "tailguard");
        } else if (section.sectionType == RECOVERY) {
            if (!parried) {
                end();
                return;
            }
            dragon.lockRiderControls(PARRY_TICKS);
            dragon.triggerAnim(VarasuchusAnimationHandler.FAST_ACTION_CONTROLLER, "tailguard_parry");
            applyParryHit();
        }
    }

    public boolean tryParry(DamageSource source) {
        if (parried || getCurrentSection() == null || getCurrentSection().sectionType != ACTIVE) {
            return false;
        }

        Entity attacker = source.getEntity();
        if (!(attacker instanceof LivingEntity livingAttacker) || attacker == getUser() || getUser().isAlly(livingAttacker)) {
            return false;
        }
        if (livingAttacker.distanceToSqr(getUser()) > PARRY_RANGE_SQR) {
            return false;
        }

        parried = true;
        parriedTarget = livingAttacker;
        jumpToSection(1);
        return true;
    }

    private void applyParryHit() {
        Varasuchus dragon = getUser();
        if (dragon.level().isClientSide) {
            return;
        }

        if (dragon.isEffectiveAi() || dragon.getControllingPassenger() == null) {
            applyParryHitTo(parriedTarget);
            return;
        }

        AABB sweepBox = dragon.getBoundingBox().inflate(PARRY_SWEEP_HORIZONTAL, PARRY_SWEEP_VERTICAL, PARRY_SWEEP_HORIZONTAL);
        List<LivingEntity> targets = dragon.level().getEntitiesOfClass(
                LivingEntity.class,
                sweepBox,
                target -> target != dragon && target.isAlive() && target.attackable() && !dragon.isAlly(target)
        );
        for (LivingEntity target : targets) {
            applyParryHitTo(target);
        }
    }

    private void applyParryHitTo(LivingEntity target) {
        Varasuchus dragon = getUser();
        if (target == null || !target.isAlive()) {
            return;
        }
        target.hurt(dragon.damageSources().mobAttack(dragon), resolveParryDamage() * dragon.getHungerMeleeDamageMultiplier());

        Vec3 direction = target.position().subtract(dragon.position());
        if (direction.lengthSqr() < 1.0E-4D) {
            direction = dragon.getLookAngle();
        }
        Vec3 push = direction.normalize().scale(KNOCKBACK_STRENGTH);
        target.push(push.x, 0.35D, push.z);
        target.hurtMarked = true;
    }

    private float resolveParryDamage() {
        return (float) DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.VARASUCHUS_ID)
                .abilityDamage("tailguard_parry", DEFAULT_PARRY_DAMAGE);
    }

    @Override
    public int getMaxCooldown() {
        return parried ? PARRY_COOLDOWN_TICKS : GUARD_COOLDOWN_TICKS;
    }
}

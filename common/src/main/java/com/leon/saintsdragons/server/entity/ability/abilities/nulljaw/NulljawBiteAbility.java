package com.leon.saintsdragons.server.entity.ability.abilities.nulljaw;

import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.DragonMeleeGeometry;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.Nulljaw;
import com.leon.saintsdragons.server.entity.dragons.nulljaw.handlers.NulljawAnimationHandler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.RECOVERY;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.STARTUP;

public final class NulljawBiteAbility extends DragonAbility<Nulljaw> {
    private static final float BASE_DAMAGE = 8.0F;
    private static final double RANGE = 2.75D;
    private static final double HITBOX_HALF_WIDTH = 3.3D;
    private static final double HITBOX_HALF_HEIGHT = 3.3D;
    private static final double HITBOX_FORWARD_OFFSET = 1.75D;

    private static final DragonAbilitySection[] TRACK = {
            new DragonAbilitySection.AbilitySectionDuration(STARTUP, 5),
            new DragonAbilitySection.AbilitySectionDuration(ACTIVE, 2),
            new DragonAbilitySection.AbilitySectionDuration(RECOVERY, 10)
    };

    private boolean appliedHit;

    public NulljawBiteAbility(DragonAbilityType<Nulljaw, NulljawBiteAbility> type, Nulljaw user) {
        super(type, user, TRACK, 12);
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section != null && section.sectionType == STARTUP) {
            Nulljaw dragon = getUser();
            dragon.triggerAnim(NulljawAnimationHandler.ACTION_CONTROLLER, NulljawAnimationHandler.BITE_TRIGGER);
            if (!dragon.level().isClientSide) {
                dragon.getSoundHandler().playMovingEntitySound(ModSounds.NULLJAW_BITE.get(), 1.0F, 1.0F, 40);
            }
            this.appliedHit = false;
        }
    }

    @Override
    public void tickUsing() {
        DragonAbilitySection section = getCurrentSection();
        if (section == null || section.sectionType != ACTIVE || this.appliedHit) {
            return;
        }

        for (LivingEntity target : findTargets()) {
            applyHit(target);
        }
        this.appliedHit = true;
    }

    private List<LivingEntity> findTargets() {
        Nulljaw dragon = getUser();
        if (dragon.getControllingPassenger() == null) {
            LivingEntity target = dragon.getTarget();
            double reach = RANGE + dragon.getBbWidth() * 0.5D
                    + (target == null ? 0.0D : target.getBbWidth() * 0.5D);
            if (DragonMeleeGeometry.isDirectAiTargetValid(dragon, target)
                    && dragon.distanceToSqr(target) <= reach * reach) {
                return List.of(target);
            }
            return List.of();
        }

        return DragonMeleeGeometry.findBodySweepTargets(
                dragon,
                RANGE,
                HITBOX_HALF_WIDTH,
                HITBOX_HALF_HEIGHT,
                HITBOX_FORWARD_OFFSET,
                entity -> !dragon.isAlly(entity)
        );
    }

    private void applyHit(LivingEntity target) {
        Nulljaw dragon = getUser();
        DamageSource source = dragon.level().damageSources().mobAttack(dragon);
        float damage = dragon.getConfiguredAbilityDamage("bite", BASE_DAMAGE)
                * dragon.getHungerMeleeDamageMultiplier();
        if (target.hurt(source, damage)) {
            Vec3 push = dragon.getLookAngle().scale(0.2D);
            target.push(push.x, 0.05D, push.z);
        }
    }
}

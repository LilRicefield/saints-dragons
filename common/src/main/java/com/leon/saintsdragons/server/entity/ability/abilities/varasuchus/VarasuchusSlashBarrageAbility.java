package com.leon.saintsdragons.server.entity.ability.abilities.varasuchus;

import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.DragonMeleeGeometry;
import com.leon.saintsdragons.server.entity.dragons.varasuchus.Varasuchus;
import com.leon.saintsdragons.server.entity.dragons.varasuchus.handlers.VarasuchusAnimationHandler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;

public class VarasuchusSlashBarrageAbility extends DragonAbility<Varasuchus> {
    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(ACTIVE, 93)
    };

    private static final int TOTAL_TICKS = 93;
    private static final int SOUND_TICKS = 140;
    private static final int[] HIT_TICKS = new int[] {2, 8, 14, 20, 24, 30, 36, 41, 46, 51, 61, 71, 78, 82};
    private static final float HIT_DAMAGE = 15.0F;
    private static final double CLAW_RANGE = 6.5;
    private static final double CLAW_HORIZONTAL = 3.0;
    private static final double CLAW_VERTICAL = 4.0;
    private static final double CLAW_ANGLE_DEG = 100.0;

    private final boolean[] hitsApplied = new boolean[HIT_TICKS.length];

    public VarasuchusSlashBarrageAbility(DragonAbilityType<Varasuchus, VarasuchusSlashBarrageAbility> type, Varasuchus user) {
        super(type, user, TRACK, 16);
    }

    @Override
    public boolean tryAbility() {
        return getUser().isPhaseTwoActive();
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) {
            return;
        }

        if (section.sectionType == ACTIVE) {
            Varasuchus dragon = getUser();
            dragon.triggerAnim(VarasuchusAnimationHandler.FAST_ACTION_CONTROLLER, "slash_barrage");
            dragon.lockAbilities(TOTAL_TICKS);
            if (!dragon.level().isClientSide) {
                dragon.getSoundHandler().playMovingEntitySound(ModSounds.VARASUCHUS_SLASH_BARRAGE.get(), 1.0f, 1.0f, SOUND_TICKS);
            }
            enforceWalkOnly(dragon);
        }
    }

    @Override
    public void tickUsing() {
        DragonAbilitySection section = getCurrentSection();
        if (section == null || section.sectionType != ACTIVE || getUser().level().isClientSide) {
            return;
        }

        Varasuchus dragon = getUser();
        int ticks = getTicksInSection();
        enforceWalkOnly(dragon);

        for (int i = 0; i < HIT_TICKS.length; i++) {
            if (!hitsApplied[i] && ticks >= HIT_TICKS[i]) {
                applyHit(dragon);
                hitsApplied[i] = true;
            }
        }
    }

    private void enforceWalkOnly(Varasuchus dragon) {
        dragon.setAccelerating(false);

        if (dragon.isVehicle()) {
            boolean moving = Math.abs(dragon.getLastRiderForward()) > 0.05F || Math.abs(dragon.getLastRiderStrafe()) > 0.05F;
            dragon.setGroundMoveStateFromRider(moving ? 1 : 0);
            return;
        }

        LivingEntity target = dragon.getTarget();
        if (target != null && target.isAlive()) {
            dragon.getAIMovement().moveToGroundTarget(target, 1.0D, false);
            dragon.setGroundMoveStateFromAI(1);
        } else {
            dragon.setGroundMoveStateFromAI(0);
        }
    }

    private void applyHit(Varasuchus dragon) {
        List<LivingEntity> targets = findClawTargets(dragon);
        if (targets.isEmpty()) {
            return;
        }

        float damage = HIT_DAMAGE * dragon.getHungerMeleeDamageMultiplier();
        DamageSource source = dragon.level().damageSources().mobAttack(dragon);
        Vec3 push = dragon.getLookAngle().scale(0.25);

        for (LivingEntity target : targets) {
            target.hurt(source, damage);
            target.setDeltaMovement(Vec3.ZERO);
            target.push(push.x * 0.1D, 0.0D, push.z * 0.1D);
            target.hurtMarked = true;
            target.hasImpulse = true;
        }
    }

    private List<LivingEntity> findClawTargets(Varasuchus dragon) {
        boolean ridden = dragon.getControllingPassenger() != null;

        double range = CLAW_RANGE;

        if (!ridden) {
            LivingEntity target = dragon.getTarget();
            if (DragonMeleeGeometry.isDirectAiTargetValid(dragon, target)) {
                return List.of(target);
            }
            return List.of();
        }

        return DragonMeleeGeometry.findForwardTargets(
                dragon,
                range,
                CLAW_HORIZONTAL,
                CLAW_VERTICAL,
                CLAW_ANGLE_DEG,
                range * 0.4D,
                entity -> !dragon.isAlly(entity)
        );
    }

}

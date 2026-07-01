package com.leon.saintsdragons.server.entity.ability.abilities.atroxiia;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.DragonMeleeGeometry;
import com.leon.saintsdragons.server.entity.dragons.atroxiia.Atroxiia;
import com.leon.saintsdragons.util.animation.AnimationHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.RECOVERY;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.STARTUP;

public class AtroxiiaSlamAbility extends DragonAbility<Atroxiia> {
    private static final float BASE_DAMAGE = 16.0F;
    private static final double RANGE = 5.5D;
    private static final double SWEEP_HORIZONTAL = 4.0D;
    private static final double SWEEP_VERTICAL = 4.0D;
    private static final double ANGLE_DEG = 100.0D;
    private static final int HIT_TICK = 10;
    private static final int ACTIVE_TICKS = 2;
    private static final int RECOVERY_TICKS = 13;


    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(STARTUP, HIT_TICK),
            new AbilitySectionDuration(ACTIVE, ACTIVE_TICKS),
            new AbilitySectionDuration(RECOVERY, RECOVERY_TICKS)
    };

    private boolean appliedHit;
    private final boolean rightSide;

    public AtroxiiaSlamAbility(DragonAbilityType<Atroxiia, AtroxiiaSlamAbility> type, Atroxiia user) {
        super(type, user, TRACK, 12);
        this.rightSide = user.useRightMeleeSide();
    }

    @Override
    public boolean tryAbility() {
        Atroxiia dragon = getUser();
        return dragon.getControllingPassenger() != null && dragon.onGround() && !dragon.isBaby();
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section != null && section.sectionType == STARTUP) {
            Atroxiia dragon = getUser();
            dragon.triggerAnim(AnimationHelper.MOVEMENT_CONTROLLER, rightSide ? "slam_right" : "slam_left");
            dragon.getSoundHandler().playMovingEntitySound(ModSounds.ATROXIIA_SLAM.get(), 1.0f, 1.0f, 30);
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
        List<LivingEntity> targets = DragonMeleeGeometry.findForwardTargets(
                dragon,
                RANGE,
                SWEEP_HORIZONTAL,
                SWEEP_VERTICAL,
                ANGLE_DEG,
                RANGE * 0.45D,
                entity -> !dragon.isAlly(entity)
        );

        for (LivingEntity target : targets) {
            applyHit(dragon, target);
        }
        appliedHit = true;
    }

    private void applyHit(Atroxiia dragon, LivingEntity target) {
        float damage = (float) DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.ATROXIIA_ID)
                .abilityDamage("slam", BASE_DAMAGE);
        damage *= dragon.getHungerMeleeDamageMultiplier();

        DamageSource source = dragon.level().damageSources().mobAttack(dragon);
        target.hurt(source, damage);

        Vec3 push = DragonMeleeGeometry.forwardAttack(dragon).forward().scale(0.45D);
        target.push(push.x, 0.18D, push.z);
    }
}

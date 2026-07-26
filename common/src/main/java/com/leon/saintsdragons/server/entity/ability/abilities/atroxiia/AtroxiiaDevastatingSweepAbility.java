package com.leon.saintsdragons.server.entity.ability.abilities.atroxiia;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.atroxiia.Atroxiia;
import com.leon.saintsdragons.util.animation.AnimationHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.RECOVERY;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.STARTUP;

public class AtroxiiaDevastatingSweepAbility extends DragonAbility<Atroxiia> {
    private static final float BASE_DAMAGE = 13.0F;
    private static final int HIT_TICK = 10;
    private static final int ACTIVE_TICKS = 2;
    private static final int RECOVERY_TICKS = 14;
    private static final double RADIUS = 12.0D;
    private static final double VERTICAL_RANGE = 6.0D;
    private static final double KNOCKBACK = 1.65D;
    private static final double KNOCKBACK_Y = 0.32D;

    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(STARTUP, HIT_TICK),
            new AbilitySectionDuration(ACTIVE, ACTIVE_TICKS),
            new AbilitySectionDuration(RECOVERY, RECOVERY_TICKS)
    };

    private boolean appliedHit;

    public AtroxiiaDevastatingSweepAbility(DragonAbilityType<Atroxiia, AtroxiiaDevastatingSweepAbility> type, Atroxiia user) {
        super(type, user, TRACK, 18);
    }

    @Override
    public boolean tryAbility() {
        return getUser().canUseGroundCombatAbility();
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section != null && section.sectionType == STARTUP) {
            Atroxiia dragon = getUser();
            dragon.triggerAnim(AnimationHelper.MOVEMENT_CONTROLLER, "devastating_sweep");
            dragon.getSoundHandler().playMovingEntitySound(ModSounds.ATROXIIA_DEVASTATING_SWEEP.get(), 1.4f, 1.0f, 40);
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

    private void applyHit(Atroxiia dragon, LivingEntity target) {
        float damage = (float) DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.ATROXIIA_ID)
                .abilityDamage("devastating_sweep", BASE_DAMAGE);
        damage *= dragon.getHungerMeleeDamageMultiplier();

        DamageSource source = dragon.level().damageSources().mobAttack(dragon);
        target.hurt(source, damage);

        Vec3 direction = target.getBoundingBox().getCenter().subtract(dragon.getBoundingBox().getCenter());
        Vec3 horizontalDirection = new Vec3(direction.x, 0.0D, direction.z);
        if (horizontalDirection.lengthSqr() < 1.0E-6D) {
            horizontalDirection = Vec3.directionFromRotation(0.0F, dragon.getYRot());
        }

        Vec3 push = horizontalDirection.normalize().scale(KNOCKBACK);
        target.push(push.x, KNOCKBACK_Y, push.z);
        target.hurtMarked = true;
    }

    private List<LivingEntity> findTargets(Atroxiia dragon) {
        Vec3 center = dragon.getBoundingBox().getCenter();
        AABB area = dragon.getBoundingBox().inflate(RADIUS, VERTICAL_RANGE, RADIUS);
        double radiusSqr = RADIUS * RADIUS;
        return dragon.level().getEntitiesOfClass(LivingEntity.class, area, entity -> {
            if (entity == dragon || !entity.isAlive() || !entity.attackable() || dragon.isAlly(entity)) {
                return false;
            }
            Vec3 targetCenter = entity.getBoundingBox().getCenter();
            double dx = targetCenter.x - center.x;
            double dz = targetCenter.z - center.z;
            return dx * dx + dz * dz <= radiusSqr;
        });
    }
}

package com.leon.saintsdragons.server.entity.ability.abilities.ignivorus;

import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.RECOVERY;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.STARTUP;

/**
 * Secondary melee for Ignivorus: a ground-only body slam that locks rider controls briefly,
 * then damages and knocks back everything overlapping the dragon's hitbox.
 */
public class IgnivorusBodySlamAbility extends DragonAbility<Ignivorus> {
    private static final int STARTUP_TICKS = 15; // ~0.75s
    private static final int ACTIVE_TICKS = 6;
    private static final int RECOVERY_TICKS = 8;
    private static final int CONTROL_LOCK_TICKS = 29; // ~1.46s
    private static final int COOLDOWN_TICKS = 40;

    private static final float BASE_DAMAGE = 40.0f;
    private static final double PUSH_STRENGTH = 1.1D;
    private static final double LIFT_FORCE = 0.6D;

    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[]{
            new AbilitySectionDuration(STARTUP, STARTUP_TICKS),
            new AbilitySectionDuration(ACTIVE, ACTIVE_TICKS),
            new AbilitySectionDuration(RECOVERY, RECOVERY_TICKS)
    };

    private boolean impactApplied;

    public IgnivorusBodySlamAbility(DragonAbilityType<Ignivorus, IgnivorusBodySlamAbility> type, Ignivorus user) {
        super(type, user, TRACK, COOLDOWN_TICKS);
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) {
            return;
        }
        Ignivorus dragon = getUser();
        if (section.sectionType == STARTUP) {
            impactApplied = false;
            dragon.triggerAnim("action", "body_slam");
            dragon.lockRiderControls(CONTROL_LOCK_TICKS);
        }
    }

    @Override
    public void tickUsing() {
        DragonAbilitySection section = getCurrentSection();
        if (section == null) {
            return;
        }

        Ignivorus dragon = getUser();
        if (dragon == null) {
            return;
        }

        // Abort if the dragon somehow takes off mid-ability
        if (dragon.isFlying()) {
            interrupt();
            return;
        }

        if (section.sectionType == ACTIVE && !impactApplied) {
            impactApplied = true;
            applySlam(dragon);
        }
    }

    private void applySlam(Ignivorus dragon) {
        Level level = dragon.level();
        if (!(level instanceof ServerLevel server)) {
            return;
        }

        double inflateXZ = Math.max(1.5D, dragon.getBbWidth()) * 0.75D;
        double inflateY = Math.max(1.5D, dragon.getBbHeight() * 0.35D);
        AABB slamArea = dragon.getBoundingBox().inflate(inflateXZ, inflateY, inflateXZ);

        List<LivingEntity> targets = server.getEntitiesOfClass(LivingEntity.class, slamArea,
                entity -> entity != dragon && entity.isAlive() && entity.attackable() && !dragon.isAlly(entity));

        if (targets.isEmpty()) {
            return;
        }

        float damage = computeDamage(dragon);
        DamageSource source = server.damageSources().mobAttack(dragon);

        for (LivingEntity target : targets) {
            target.hurt(source, damage);

            Vec3 push = target.position().subtract(dragon.position());
            if (push.lengthSqr() < 1.0E-4) {
                push = new Vec3(0, 0, 1);
            }
            push = push.normalize();
            double scaledPush = PUSH_STRENGTH + dragon.getBbWidth() * 0.15D;
            target.push(push.x * scaledPush, LIFT_FORCE, push.z * scaledPush);
            target.hasImpulse = true;
        }
    }

    private static float computeDamage(Ignivorus dragon) {
        double attack = dragon.getAttributeValue(Attributes.ATTACK_DAMAGE);
        return (float) (BASE_DAMAGE + attack * 0.75D);
    }

    @Override
    public boolean tryAbility() {
        Ignivorus dragon = getUser();
        if (dragon == null || dragon.isBaby() || dragon.isFlying() || !dragon.onGround() || dragon.areRiderControlsLocked()) {
            return false;
        }
        return super.tryAbility();
    }

    @Override
    protected void endSection(DragonAbilitySection section) {
        if (section != null && section.sectionType == RECOVERY) {
            impactApplied = false;
        }
    }

    @Override
    public void interrupt() {
        impactApplied = false;
        super.interrupt();
    }
}

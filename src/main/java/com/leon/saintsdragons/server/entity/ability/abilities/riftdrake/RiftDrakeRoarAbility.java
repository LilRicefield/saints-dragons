package com.leon.saintsdragons.server.entity.ability.abilities.riftdrake;

import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.riftdrake.RiftDrakeEntity;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.RECOVERY;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.STARTUP;

import java.util.ArrayList;
import java.util.List;

/**
 * Cosmetic roar for the Rift Drake. Plays roar animation and sound.
 * Locks abilities for the full animation duration (~5s) but allows movement.
 */
public class RiftDrakeRoarAbility extends DragonAbility<RiftDrakeEntity> {

    // Animation length: ~4.75s = 95 ticks. Round up to 100 ticks for 5 seconds.
    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(STARTUP, 6),
            new AbilitySectionDuration(ACTIVE, 85),
            new AbilitySectionDuration(RECOVERY, 9)
    };

    private static final int SOUND_DELAY_TICKS = 3;
    private static final int ROAR_TOTAL_TICKS = 100; // 5 seconds @ 20 TPS

    private static final int FIRST_SWIPE_TICK = 6;
    private static final int SECOND_SWIPE_TICK = 22;
    private static final float CLAW_DAMAGE_MULTIPLIER = 2.0f;
    private static final float BASE_CLAW_DAMAGE = 12.0f;
    private static final double CLAW_RANGE = 5.0;
    private static final double CLAW_RANGE_RIDDEN_BONUS = 1.5;
    private static final double CLAW_HORIZONTAL = 4.0;
    private static final double CLAW_HORIZONTAL_RIDDEN = 3.0;
    private static final double CLAW_VERTICAL = 4.0;
    private static final double CLAW_ANGLE_DEG = 100.0;

    private boolean soundQueued = false;
    private boolean firstSwipeApplied = false;
    private boolean secondSwipeApplied = false;

    public RiftDrakeRoarAbility(DragonAbilityType<RiftDrakeEntity, RiftDrakeRoarAbility> type,
                                RiftDrakeEntity user) {
        super(type, user, TRACK, 20);
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) {
            return;
        }
        if (section.sectionType == STARTUP) {
            RiftDrakeEntity dragon = getUser();
            // Use different roar animation based on phase
            String trigger = dragon.isPhaseTwoActive() ? "roar2" : "roar";
            dragon.triggerAnim("action", trigger);
            soundQueued = true;

            // Lock abilities for full animation duration but allow walking/running
            dragon.lockAbilities(ROAR_TOTAL_TICKS);
        }
    }

    @Override
    protected void endSection(DragonAbilitySection section) {
        if (section != null && section.sectionType == STARTUP) {
            soundQueued = false;
        }
    }

    @Override
    public void tickUsing() {
        DragonAbilitySection section = getCurrentSection();
        if (section == null) {
            return;
        }

        if (soundQueued && section.sectionType == STARTUP && getTicksInSection() >= SOUND_DELAY_TICKS) {
            RiftDrakeEntity dragon = getUser();
            if (!dragon.level().isClientSide) {
                Vec3 mouth = dragon.getMouthPosition();
                boolean phaseTwo = dragon.isPhaseTwoActive();
                // Phase 2 roar is deeper and louder
                float basePitch = phaseTwo ? 0.8f : 1.0f;
                float volume = phaseTwo ? 1.8f : 1.4f;
                float pitch = basePitch + dragon.getRandom().nextFloat() * 0.1f;

                dragon.level().playSound(null,
                        mouth.x, mouth.y, mouth.z,
                        ModSounds.RIFTDRAKE_ROAR.get(),
                        SoundSource.NEUTRAL,
                        volume,
                        pitch);
            }
            soundQueued = false;
        }

        if (section.sectionType == ACTIVE) {
            RiftDrakeEntity dragon = getUser();
            if (!dragon.level().isClientSide && dragon.isPhaseTwoActive()) {
                int ticks = getTicksInSection();
                if (!firstSwipeApplied && ticks >= FIRST_SWIPE_TICK) {
                    applyRoarSwipe(dragon);
                    firstSwipeApplied = true;
                }
                if (!secondSwipeApplied && ticks >= SECOND_SWIPE_TICK) {
                    applyRoarSwipe(dragon);
                    secondSwipeApplied = true;
                }
            }
        }
    }

    private void applyRoarSwipe(RiftDrakeEntity dragon) {
        List<LivingEntity> targets = findClawTargets(dragon);
        if (targets.isEmpty()) {
            return;
        }

        float damage = computeClawDamage(dragon) * CLAW_DAMAGE_MULTIPLIER;
        DamageSource source = dragon.level().damageSources().mobAttack(dragon);
        Vec3 push = dragon.getLookAngle().scale(0.5);

        for (LivingEntity target : targets) {
            target.hurt(source, damage);
            target.push(push.x, 0.15, push.z);
        }
    }

    private float computeClawDamage(RiftDrakeEntity dragon) {
        float damage = BASE_CLAW_DAMAGE;
        AttributeInstance attack = dragon.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) {
            double value = attack.getValue();
            if (value > 0.0) {
                damage = (float) (value * 1.2);
            }
        }
        return damage;
    }

    private List<LivingEntity> findClawTargets(RiftDrakeEntity dragon) {
        Vec3 origin = dragon.getMouthPosition();
        Vec3 forward = dragon.getLookAngle().normalize();
        boolean ridden = dragon.getControllingPassenger() != null;

        double range = CLAW_RANGE + (ridden ? CLAW_RANGE_RIDDEN_BONUS : 0.0);
        double horizontal = ridden ? CLAW_HORIZONTAL_RIDDEN : CLAW_HORIZONTAL;

        AABB sweep = new AABB(origin, origin.add(forward.scale(range)))
                .inflate(horizontal, CLAW_VERTICAL, horizontal);

        List<LivingEntity> candidates = dragon.level().getEntitiesOfClass(LivingEntity.class, sweep,
                entity -> entity != dragon && entity.isAlive() && entity.attackable() && !dragon.isAlly(entity));

        double cosLimit = Math.cos(Math.toRadians(CLAW_ANGLE_DEG));
        List<LivingEntity> valid = new ArrayList<>();

        for (LivingEntity candidate : candidates) {
            double distance = distancePointToAABB(origin, candidate.getBoundingBox());
            if (distance > range + 0.5) {
                continue;
            }

            Vec3 toward = closestPointOnAABB(origin, candidate.getBoundingBox()).subtract(origin);
            double len = toward.length();
            if (len <= 1.0e-4) {
                continue;
            }
            Vec3 dir = toward.scale(1.0 / len);
            double dot = dir.dot(forward);
            if (dot <= 0.0) {
                continue;
            }

            boolean veryClose = distance < (range * 0.4);
            boolean goodAngle = dot >= cosLimit;
            if (ridden) {
                goodAngle = goodAngle || dot >= (cosLimit * 0.7);
            }

            if (veryClose || goodAngle) {
                valid.add(candidate);
            }
        }

        return valid;
    }

    private static double distancePointToAABB(Vec3 point, AABB box) {
        double dx = Math.max(Math.max(box.minX - point.x, 0.0), point.x - box.maxX);
        double dy = Math.max(Math.max(box.minY - point.y, 0.0), point.y - box.maxY);
        double dz = Math.max(Math.max(box.minZ - point.z, 0.0), point.z - box.maxZ);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static Vec3 closestPointOnAABB(Vec3 point, AABB box) {
        double cx = Mth.clamp(point.x, box.minX, box.maxX);
        double cy = Mth.clamp(point.y, box.minY, box.maxY);
        double cz = Mth.clamp(point.z, box.minZ, box.maxZ);
        return new Vec3(cx, cy, cz);
    }
}

package com.leon.saintsdragons.server.entity.ability.abilities.ignivorus;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.RECOVERY;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.STARTUP;

/**
 * Wing swipe attack for Ignivorus Phase 2.
 * Deals heavy damage with massive knockback in a wide arc.
 * Alternates between left and right wing.
 */
public class IgnivorusWingSwipeAbility extends DragonAbility<Ignivorus> {
    // "10 health" = 10 damage points (5 hearts)
    private static final float DEFAULT_DAMAGE = 15.0f;

    // Massive AoE radius for wing swipe - hits everything around the dragon
    private static final double AOE_RADIUS = 22.0;

    // Massive knockback multiplier
    private static final double KNOCKBACK_STRENGTH = 4.0;

    // Animation timing: 1.25 seconds = 25 ticks total
    // Visual hit lands at 0.58 seconds (≈ tick 12)
    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(STARTUP, 12),   // Windup (0.6s - damage lands here)
            new AbilitySectionDuration(ACTIVE, 2),     // Hit window (0.1s)
            new AbilitySectionDuration(RECOVERY, 11)   // Recovery (0.55s)
    };

    private boolean appliedHit;

    public IgnivorusWingSwipeAbility(DragonAbilityType<Ignivorus, IgnivorusWingSwipeAbility> type,
                                     Ignivorus user) {
        super(type, user, TRACK, 3);
    }

    @Override
    public boolean tryAbility() {
        // Wing swipe only works in Phase 2 while grounded
        // When flying, falls back to bite attack
        return getUser().isPhase2Active() && !getUser().isFlying();
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) {
            return;
        }

        if (section.sectionType == STARTUP) {
            Ignivorus dragon = getUser();

            // Lock controls for the full animation duration (1.25 seconds = 25 ticks)
            dragon.lockRiderControls(25);

            // Alternate between left and right wing swipe
            boolean useRight = dragon.shouldUseRightWingSwipe();
            String animationName = useRight ? "wing_swipe_right" : "wing_swipe_left";

            // Trigger wing swipe animation as a one-shot on the instant controller.
            dragon.triggerAnim("instant", animationName);
            if (!dragon.level().isClientSide) {
                dragon.getSoundHandler().playMovingEntitySound(ModSounds.IGNIVORUS_WING_SWIPE.get(), 1.0f, 1.0f, 55);
            }

            // Toggle for next time
            dragon.toggleWingSwipeSide();

            appliedHit = false;
        }
    }

    @Override
    public void tickUsing() {
        DragonAbilitySection section = getCurrentSection();
        if (section == null) {
            return;
        }

        // Apply damage during ACTIVE window (hit frame)
        if (section.sectionType == ACTIVE && !appliedHit) {
            Ignivorus dragon = getUser();

            List<LivingEntity> targets = selectTargets();

            // Apply wing swipe to all valid targets
            for (LivingEntity target : targets) {
                applyHit(dragon, target);
            }

            appliedHit = true;
        }
    }

    private void applyHit(Ignivorus dragon, LivingEntity target) {
        // Apply damage as a direct melee hit
        DamageSource physicalSource = dragon.level().damageSources().mobAttack(dragon);
        target.hurt(physicalSource, resolveDamage() * dragon.getHungerMeleeDamageMultiplier());

        // Apply massive knockback
        Vec3 knockbackDir = target.position().subtract(dragon.position()).normalize();
        Vec3 push = knockbackDir.scale(KNOCKBACK_STRENGTH);
        target.push(push.x, 0.5, push.z); // Extra vertical launch
        target.hurtMarked = true; // Force velocity sync to client
    }

    private float resolveDamage() {
        return (float) DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.IGNIVORUS_ID)
                .abilityDamage("wing_swipe", DEFAULT_DAMAGE);
    }

    private List<LivingEntity> selectTargets() {
        Ignivorus dragon = getUser();

        // Get dragon center position at body height for wing swipe
        Vec3 dragonPos = dragon.position().add(0, dragon.getBbHeight() * 0.5, 0);

        // Get dragon's look direction for determining wing swipe arc
        Vec3 lookDir = dragon.getLookAngle();
        double dragonYaw = Math.atan2(lookDir.z, lookDir.x);

        // Determine which wing is swinging (left or right)
        boolean isRightWing = dragon.shouldUseRightWingSwipe();

        // Create wide detection area
        AABB detectionBox = new AABB(dragonPos, dragonPos).inflate(AOE_RADIUS);

        // Find targets in a wide arc to the side of the dragon
        // Wing swipes hit in a 180-degree arc on the side of the swinging wing
        List<LivingEntity> candidates = dragon.level().getEntitiesOfClass(LivingEntity.class, detectionBox,
                entity -> {
                    if (entity == dragon || !entity.isAlive() || !entity.attackable() || dragon.isAlly(entity)) {
                        return false;
                    }

                    Vec3 entityCenter = entity.getBoundingBox().getCenter();

                    // Check if within radius
                    double distSqr = entityCenter.distanceToSqr(dragonPos);
                    if (distSqr > (AOE_RADIUS * AOE_RADIUS)) {
                        return false;
                    }

                    // Calculate angle to entity relative to dragon's facing
                    Vec3 toEntity = entityCenter.subtract(dragonPos);
                    double angleToEntity = Math.atan2(toEntity.z, toEntity.x);
                    double relativeAngle = angleToEntity - dragonYaw;

                    // Normalize angle to -PI to PI range
                    while (relativeAngle > Math.PI) relativeAngle -= 2 * Math.PI;
                    while (relativeAngle < -Math.PI) relativeAngle += 2 * Math.PI;

                    // Right wing swipes hit right side (angles 0 to PI/-PI to -PI/2)
                    // Left wing swipes hit left side (angles -PI/2 to PI/2)
                    // Both include front arc for overlap
                    if (isRightWing) {
                        // Right side: -135° to +135° (270° arc covering right + front)
                        return relativeAngle >= -2.356 && relativeAngle <= 2.356;
                    } else {
                        // Left side: -45° to +225° (270° arc covering left + front)
                        return relativeAngle >= -0.785 || relativeAngle <= 2.356;
                    }
                });

        // Sort by distance (closest first) for consistent behavior
        candidates.sort(Comparator.comparingDouble(e ->
            e.getBoundingBox().getCenter().distanceToSqr(dragonPos)
        ));

        return candidates;
    }
}

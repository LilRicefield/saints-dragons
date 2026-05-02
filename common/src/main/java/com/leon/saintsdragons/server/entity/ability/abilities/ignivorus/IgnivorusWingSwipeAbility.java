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


public class IgnivorusWingSwipeAbility extends DragonAbility<Ignivorus> {

    private static final float DEFAULT_DAMAGE = 15.0f;
    private static final double AOE_RADIUS = 22.0;
    private static final double KNOCKBACK_STRENGTH = 4.0;
    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(STARTUP, 12),
            new AbilitySectionDuration(ACTIVE, 2),
            new AbilitySectionDuration(RECOVERY, 11)
    };

    private boolean appliedHit;

    public IgnivorusWingSwipeAbility(DragonAbilityType<Ignivorus, IgnivorusWingSwipeAbility> type,
                                     Ignivorus user) {
        super(type, user, TRACK, 3);
    }

    @Override
    public boolean tryAbility() {
        return getUser().isPhase2Active() && !getUser().isFlying();
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) {
            return;
        }

        if (section.sectionType == STARTUP) {
            Ignivorus dragon = getUser();
            dragon.lockRiderControls(25);
            boolean useRight = dragon.shouldUseRightWingSwipe();
            String animationName = useRight ? "wing_swipe_right" : "wing_swipe_left";
            dragon.triggerAnim("instant", animationName);
            if (!dragon.level().isClientSide) {
                dragon.getSoundHandler().playMovingEntitySound(ModSounds.IGNIVORUS_WING_SWIPE.get(), 1.0f, 1.0f, 55);
            }
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
        if (section.sectionType == ACTIVE && !appliedHit) {
            Ignivorus dragon = getUser();
            List<LivingEntity> targets = selectTargets();

            for (LivingEntity target : targets) {
                applyHit(dragon, target);
            }

            appliedHit = true;
        }
    }

    private void applyHit(Ignivorus dragon, LivingEntity target) {
        DamageSource physicalSource = dragon.level().damageSources().mobAttack(dragon);
        target.hurt(physicalSource, resolveDamage() * dragon.getHungerMeleeDamageMultiplier());
        Vec3 knockbackDir = target.position().subtract(dragon.position()).normalize();
        Vec3 push = knockbackDir.scale(KNOCKBACK_STRENGTH);
        target.push(push.x, 0.5, push.z);
        target.hurtMarked = true;
    }

    private float resolveDamage() {
        return (float) DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.IGNIVORUS_ID)
                .abilityDamage("wing_swipe", DEFAULT_DAMAGE);
    }

    private List<LivingEntity> selectTargets() {
        Ignivorus dragon = getUser();

        Vec3 dragonPos = dragon.position().add(0, dragon.getBbHeight() * 0.5, 0);
        Vec3 lookDir = dragon.getLookAngle();
        double dragonYaw = Math.atan2(lookDir.z, lookDir.x);
        boolean isRightWing = dragon.shouldUseRightWingSwipe();
        AABB detectionBox = new AABB(dragonPos, dragonPos).inflate(AOE_RADIUS);
        List<LivingEntity> candidates = dragon.level().getEntitiesOfClass(LivingEntity.class, detectionBox,
                entity -> {
                    if (entity == dragon || !entity.isAlive() || !entity.attackable() || dragon.isAlly(entity)) {
                        return false;
                    }

                    Vec3 entityCenter = entity.getBoundingBox().getCenter();
                    double distSqr = entityCenter.distanceToSqr(dragonPos);
                    if (distSqr > (AOE_RADIUS * AOE_RADIUS)) {
                        return false;
                    }

                    Vec3 toEntity = entityCenter.subtract(dragonPos);
                    double angleToEntity = Math.atan2(toEntity.z, toEntity.x);
                    double relativeAngle = angleToEntity - dragonYaw;
                    while (relativeAngle > Math.PI) relativeAngle -= 2 * Math.PI;
                    while (relativeAngle < -Math.PI) relativeAngle += 2 * Math.PI;
                    if (isRightWing) {
                        return relativeAngle >= -2.356 && relativeAngle <= 2.356;
                    } else {
                        return relativeAngle >= -0.785 || relativeAngle <= 2.356;
                    }
                });
        candidates.sort(Comparator.comparingDouble(e ->
            e.getBoundingBox().getCenter().distanceToSqr(dragonPos)
        ));

        return candidates;
    }
}
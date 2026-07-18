package com.leon.saintsdragons.server.ai.dragonbrain.behaviour.varasuchus;

import com.leon.saintsdragons.common.registry.ModTags;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonTargetingBehaviour;
import com.leon.saintsdragons.server.ai.DragonTargetingHelper;
import com.leon.saintsdragons.server.entity.dragons.varasuchus.Varasuchus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

public final class VarasuchusTargetingBehaviour extends DragonTargetingBehaviour<Varasuchus> {
    private static final double BABY_PROTECTION_RANGE = 16.0D;
    private static final double COMMITTED_RETENTION_MULTIPLIER = 2.0D;

    private int lastOwnerHurtTimestamp;
    private int lastOwnerAttackTimestamp;
    private int lastSelfHurtTimestamp;
    private int playerPollCooldown;
    private int raidPollCooldown;
    private int huntPollCooldown;
    private boolean protectingBabies;

    @Nullable
    @Override
    protected TargetChoice findPriorityTarget(DragonBrainContext<Varasuchus> context) {
        Varasuchus dragon = context.dragon();

        if (dragon.shouldSuspendRoostWandering()) {
            protectingBabies = false;
            LivingEntity attacker = dragon.getLastHurtByMob();
            int hurtTimestamp = dragon.getLastHurtByMobTimestamp();
            if (hurtTimestamp != lastSelfHurtTimestamp
                    && isRecentAttacker(dragon, attacker, hurtTimestamp)
                    && isUsableTarget(dragon, attacker)) {
                lastSelfHurtTimestamp = hurtTimestamp;
                return choice(attacker, Source.RETALIATION);
            }
            return null;
        }

        LivingEntity owner = dragon.getOwner();
        if (owner != null) {
            LivingEntity threat = owner.getLastHurtByMob();
            int timestamp = owner.getLastHurtByMobTimestamp();
            if (timestamp != lastOwnerHurtTimestamp && isUsableTarget(dragon, threat)) {
                lastOwnerHurtTimestamp = timestamp;
                return choice(threat, Source.OWNER_HURT);
            }

            threat = owner.getLastHurtMob();
            timestamp = owner.getLastHurtMobTimestamp();
            if (timestamp != lastOwnerAttackTimestamp && isUsableTarget(dragon, threat)) {
                lastOwnerAttackTimestamp = timestamp;
                return choice(threat, Source.OWNER_ATTACKED);
            }
        }

        List<Varasuchus> babies = protectableBabies(dragon);
        protectingBabies = dragon.isWildAggressionEnabled() && !babies.isEmpty();
        if (protectingBabies) {
            LivingEntity threat = newestBabyThreat(dragon, babies);
            if (threat != null) {
                return choice(threat, Source.PROTECT_BABY);
            }
            return null;
        }

        LivingEntity attacker = dragon.getLastHurtByMob();
        int hurtTimestamp = dragon.getLastHurtByMobTimestamp();
        if (hurtTimestamp != lastSelfHurtTimestamp && isUsableTarget(dragon, attacker)) {
            lastSelfHurtTimestamp = hurtTimestamp;
            return choice(attacker, Source.RETALIATION);
        }

        if (playerPollCooldown-- <= 0) {
            playerPollCooldown = 10;
            if (dragon.isWildAggressionEnabled() && dragon.hasNearbyWildBaby()) {
                Player player = nearest(context.level(), dragon, Player.class,
                        candidate -> !candidate.isCreative() && !candidate.isSpectator());
                if (player != null) {
                    return choice(player, Source.NEARBY_BABY_PLAYER);
                }
            }
        }

        if (raidPollCooldown-- <= 0) {
            raidPollCooldown = 10;
            Raider raider = nearest(context.level(), dragon, Raider.class,
                    DragonTargetingHelper::isActiveRaidTarget);
            if (raider != null) {
                return choice(raider, Source.RAID_DEFENSE);
            }
        }

        if (huntPollCooldown-- <= 0) {
            huntPollCooldown = 80;
            if (!dragon.isTame()) {
                LivingEntity prey = nearest(context.level(), dragon, LivingEntity.class,
                        candidate -> DragonTargetingHelper.isTaggedHuntTarget(
                                candidate, ModTags.EntityTypes.VARASUCHUS_TARGETS));
                if (prey != null) {
                    return choice(prey, Source.HUNT);
                }
            }
        }
        return null;
    }

    @Override
    protected boolean canAcquireTargets(Varasuchus dragon) {
        return dragon.isAlive()
                && !dragon.isDying()
                && !dragon.isBaby()
                && !dragon.isVehicle()
                && !dragon.isPassenger()
                && !dragon.isOrderedToSit()
                && !dragon.isSleepLocked();
    }

    @Override
    protected boolean isUsableTarget(Varasuchus dragon, @Nullable LivingEntity target) {
        return target != null
                && target.isAlive()
                && dragon.isTargetValid(target)
                && dragon.canTarget(target)
                && (!dragon.hasRoostTerritory() || dragon.isWithinRoostTerritory(target.position()));
    }

    @Override
    protected boolean canRetainTarget(Varasuchus dragon, LivingEntity target, String source) {
        if (dragon.shouldSuspendRoostWandering()
                && (!Source.RETALIATION.debugName.equals(source)
                || !isRecentAttacker(dragon, target, dragon.getLastHurtByMobTimestamp()))) {
            return false;
        }
        double range = Math.max(16.0D, dragon.getAttributeValue(Attributes.FOLLOW_RANGE));
        if (Source.RETALIATION.debugName.equals(source)
                || Source.OWNER_HURT.debugName.equals(source)
                || Source.OWNER_ATTACKED.debugName.equals(source)
                || dragon.isInWaterOrBubble() && target.isInWaterOrBubble()) {
            range *= COMMITTED_RETENTION_MULTIPLIER;
        }
        return dragon.distanceToSqr(target) <= range * range;
    }

    @Override
    protected boolean suppressesTargetRetention(DragonBrainContext<Varasuchus> context) {
        return protectingBabies;
    }

    @Override
    protected void prepareTargetChange(Varasuchus dragon,
                                       @Nullable LivingEntity oldTarget,
                                       LivingEntity newTarget,
                                       String oldSource,
                                       String newSource) {
        if (Source.NEARBY_BABY_PLAYER.debugName.equals(newSource)) {
            dragon.markBabyProtectionAggroTarget(newTarget);
        } else {
            dragon.clearBabyProtectionAggroTarget();
        }
    }

    @Override
    protected void targetChanged(Varasuchus dragon,
                                 @Nullable LivingEntity oldTarget,
                                 LivingEntity newTarget,
                                 String oldSource,
                                 String newSource) {
        if (Source.HUNT.debugName.equals(oldSource)
                && (oldTarget != newTarget || !Source.HUNT.debugName.equals(newSource))) {
            dragon.clearPassiveHuntTarget();
        }
        if (Source.HUNT.debugName.equals(newSource)
                && DragonTargetingHelper.isPassivePreyType(newTarget)) {
            dragon.markPassiveHuntTarget(newTarget);
        }
    }

    @Override
    protected void targetCleared(Varasuchus dragon,
                                 @Nullable LivingEntity oldTarget,
                                 String oldSource) {
        if (Source.HUNT.debugName.equals(oldSource)) {
            dragon.clearPassiveHuntTarget();
        }
        dragon.clearBabyProtectionAggroTarget();
    }

    @Override
    protected Map<String, String> additionalDebugDetails() {
        return Map.of("protecting_babies", Boolean.toString(protectingBabies));
    }

    private List<Varasuchus> protectableBabies(Varasuchus dragon) {
        if (dragon.isBaby() || !dragon.isFemale()) {
            return List.of();
        }
        return dragon.level().getEntitiesOfClass(
                Varasuchus.class,
                dragon.getBoundingBox().inflate(BABY_PROTECTION_RANGE),
                baby -> baby.isBaby() && baby.isAlive() && assignedTo(dragon, baby)
        );
    }

    private boolean assignedTo(Varasuchus mother, Varasuchus baby) {
        UUID assigned = baby.getAssignedParentUuid();
        if (assigned != null) {
            return mother.getUUID().equals(assigned);
        }
        Varasuchus nearest = baby.level().getEntitiesOfClass(
                        Varasuchus.class,
                        baby.getBoundingBox().inflate(12.0D, 6.0D, 12.0D),
                        adult -> !adult.isBaby() && adult.isAlive() && adult.isFemale())
                .stream()
                .min(Comparator.comparingDouble(baby::distanceToSqr))
                .orElse(null);
        if (nearest != null) {
            baby.setAssignedParentUuid(nearest.getUUID());
        }
        return nearest == mother;
    }

    @Nullable
    private LivingEntity newestBabyThreat(Varasuchus dragon, List<Varasuchus> babies) {
        LivingEntity newest = null;
        int newestTimestamp = Integer.MIN_VALUE;
        for (Varasuchus baby : babies) {
            LivingEntity threat = baby.getLastDamager();
            int timestamp = baby.getLastDamagerTimestamp();
            if (threat == null) {
                threat = baby.getLastHurtByMob();
                timestamp = baby.getLastHurtByMobTimestamp();
            }
            if (threat != null
                    && timestamp > newestTimestamp
                    && !(threat instanceof Varasuchus)
                    && threat != dragon.getOwner()
                    && dragon.distanceToSqr(threat) <= BABY_PROTECTION_RANGE * BABY_PROTECTION_RANGE
                    && isUsableTarget(dragon, threat)) {
                newest = threat;
                newestTimestamp = timestamp;
            }
        }
        return newest;
    }

    private boolean isRecentAttacker(Varasuchus dragon,
                                     @Nullable LivingEntity target,
                                     int attackTimestamp) {
        if (target == null || dragon.getLastHurtByMob() != target || attackTimestamp <= 0) {
            return false;
        }
        int age = dragon.tickCount - attackTimestamp;
        return age >= 0 && age < 20 * 30;
    }

    @Nullable
    private <E extends LivingEntity> E nearest(ServerLevel level,
                                                Varasuchus dragon,
                                                Class<E> type,
                                                Predicate<E> predicate) {
        double range = Math.max(16.0D, dragon.getAttributeValue(Attributes.FOLLOW_RANGE));
        TargetingConditions conditions = TargetingConditions.forCombat()
                .range(range)
                .selector(entity -> predicate.test(type.cast(entity)) && dragon.canTarget(entity));
        return level.getNearestEntity(type, conditions, dragon,
                dragon.getX(), dragon.getEyeY(), dragon.getZ(), dragon.getBoundingBox().inflate(range));
    }

    private TargetChoice choice(LivingEntity target, Source source) {
        return targetChoice(target, source.debugName, source.priority);
    }

    private enum Source {
        OWNER_HURT("owner_hurt", 0),
        OWNER_ATTACKED("owner_attacked", 1),
        PROTECT_BABY("protect_baby", 2),
        RETALIATION("retaliation", 3),
        NEARBY_BABY_PLAYER("nearby_baby_player", 4),
        RAID_DEFENSE("raid_defense", 5),
        HUNT("hunt", 6);

        private final String debugName;
        private final int priority;

        Source(String debugName, int priority) {
            this.debugName = debugName;
            this.priority = priority;
        }
    }
}

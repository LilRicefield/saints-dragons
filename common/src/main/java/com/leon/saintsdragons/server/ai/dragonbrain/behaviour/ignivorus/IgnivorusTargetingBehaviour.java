package com.leon.saintsdragons.server.ai.dragonbrain.behaviour.ignivorus;

import com.leon.saintsdragons.common.registry.ModTags;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonHuntAndEatBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonTargetingBehaviour;
import com.leon.saintsdragons.server.ai.DragonTargetingHelper;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
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

public final class IgnivorusTargetingBehaviour extends DragonTargetingBehaviour<Ignivorus> {
    private static final double BABY_PROTECTION_RANGE = 16.0D;

    private int lastOwnerHurtTimestamp;
    private int lastOwnerAttackTimestamp;
    private int lastSelfHurtTimestamp;
    private int playerPollCooldown;
    private int raidPollCooldown;
    private int huntPollCooldown;
    private boolean protectingBabies;

    @Nullable
    @Override
    protected TargetChoice findPriorityTarget(DragonBrainContext<Ignivorus> context) {
        Ignivorus dragon = context.dragon();
        List<Ignivorus> babies = protectableBabies(dragon);
        protectingBabies = !babies.isEmpty();

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

        if (protectingBabies) {
            keepNearBabies(dragon);
            LivingEntity threat = newestBabyThreat(dragon, babies);
            if (threat != null) return choice(threat, Source.PROTECT_BABY);
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
            if (dragon.isWildAggressionEnabled()) {
                Player player = nearest(
                        context.level(),
                        dragon,
                        Player.class,
                        candidate -> !candidate.isCreative() && !candidate.isSpectator()
                );
                if (player != null) return choice(player, Source.AGGRESSIVE_WILD);
            }
        }

        if (raidPollCooldown-- <= 0) {
            raidPollCooldown = 10;
            Raider raider = nearest(
                    context.level(),
                    dragon,
                    Raider.class,
                    DragonTargetingHelper::isActiveRaidTarget
            );
            if (raider != null) return choice(raider, Source.RAID_DEFENSE);
        }

        if (huntPollCooldown-- <= 0) {
            huntPollCooldown = 80;
            if (DragonHuntAndEatBehaviour.shouldAcquirePrey(dragon)) {
                LivingEntity prey = nearest(
                        context.level(),
                        dragon,
                        LivingEntity.class,
                        candidate -> DragonTargetingHelper.isTaggedHuntTarget(
                                candidate,
                                ModTags.EntityTypes.IGNIVORUS_HUNT_PREY
                        )
                );
                if (prey != null) return choice(prey, Source.HUNT);
            }
        }
        return null;
    }

    @Override
    protected boolean canAcquireTargets(Ignivorus dragon) {
        return dragon.isAlive()
                && !dragon.isDying()
                && !dragon.isBaby()
                && !dragon.isVehicle()
                && !dragon.isPassenger()
                && !dragon.isOrderedToSit()
                && !dragon.isSleepLocked()
                && !dragon.isTamingStunned();
    }

    @Override
    protected boolean isUsableTarget(Ignivorus dragon, @Nullable LivingEntity target) {
        return target != null
                && target.isAlive()
                && dragon.isTargetValid(target)
                && dragon.canTarget(target);
    }

    @Override
    protected boolean canRetainTarget(Ignivorus dragon, LivingEntity target, String source) {
        if (Source.HUNT.debugName.equals(source)
                && !DragonHuntAndEatBehaviour.shouldAcquirePrey(dragon)) {
            return false;
        }
        double range = Source.PROTECT_BABY.debugName.equals(source)
                ? BABY_PROTECTION_RANGE
                : Math.max(32.0D, dragon.getAttributeValue(Attributes.FOLLOW_RANGE));
        return dragon.distanceToSqr(target) <= range * range;
    }

    @Override
    protected boolean suppressesTargetRetention(DragonBrainContext<Ignivorus> context) {
        if (!protectingBabies) return false;
        LivingEntity current = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        LivingEntity owner = context.dragon().getOwner();
        return owner == null
                || current != owner.getLastHurtByMob() && current != owner.getLastHurtMob();
    }

    @Override
    protected void targetChanged(Ignivorus dragon,
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
    protected void targetCleared(Ignivorus dragon,
                                 @Nullable LivingEntity oldTarget,
                                 String oldSource) {
        if (Source.HUNT.debugName.equals(oldSource)) dragon.clearPassiveHuntTarget();
    }

    private void keepNearBabies(Ignivorus dragon) {
        if (dragon.isAerial() && !dragon.isLanding()) dragon.beginAiLanding();
    }

    private List<Ignivorus> protectableBabies(Ignivorus dragon) {
        if (dragon.isBaby() || !dragon.isFemale()) return List.of();
        return dragon.level().getEntitiesOfClass(
                Ignivorus.class,
                dragon.getBoundingBox().inflate(BABY_PROTECTION_RANGE),
                baby -> baby.isBaby() && baby.isAlive() && assignedTo(dragon, baby)
        );
    }

    private boolean assignedTo(Ignivorus mother, Ignivorus baby) {
        UUID assigned = baby.getAssignedParentUuid();
        if (assigned != null) return mother.getUUID().equals(assigned);
        Ignivorus nearest = baby.level().getEntitiesOfClass(
                        Ignivorus.class,
                        baby.getBoundingBox().inflate(12.0D, 6.0D, 12.0D),
                        adult -> !adult.isBaby() && adult.isAlive() && adult.isFemale()
                )
                .stream()
                .min(Comparator.comparingDouble(baby::distanceToSqr))
                .orElse(null);
        if (nearest != null) baby.setAssignedParentUuid(nearest.getUUID());
        return nearest == mother;
    }

    @Nullable
    private LivingEntity newestBabyThreat(Ignivorus dragon, List<Ignivorus> babies) {
        LivingEntity newest = null;
        int newestTimestamp = Integer.MIN_VALUE;
        for (Ignivorus baby : babies) {
            LivingEntity threat = baby.getLastDamager();
            int timestamp = baby.getLastDamagerTimestamp();
            if (threat == null) {
                threat = baby.getLastHurtByMob();
                timestamp = baby.getLastHurtByMobTimestamp();
            }
            if (threat != null
                    && timestamp > newestTimestamp
                    && !(threat instanceof Ignivorus)
                    && threat != dragon.getOwner()
                    && dragon.distanceToSqr(threat) <= BABY_PROTECTION_RANGE * BABY_PROTECTION_RANGE
                    && isUsableTarget(dragon, threat)) {
                newest = threat;
                newestTimestamp = timestamp;
            }
        }
        return newest;
    }

    @Nullable
    private <E extends LivingEntity> E nearest(ServerLevel level,
                                                Ignivorus dragon,
                                                Class<E> type,
                                                Predicate<E> predicate) {
        double range = Math.max(32.0D, dragon.getAttributeValue(Attributes.FOLLOW_RANGE));
        TargetingConditions conditions = TargetingConditions.forCombat()
                .range(range)
                .selector(entity -> predicate.test(type.cast(entity)) && dragon.canTarget(entity));
        return level.getNearestEntity(
                type,
                conditions,
                dragon,
                dragon.getX(),
                dragon.getEyeY(),
                dragon.getZ(),
                dragon.getBoundingBox().inflate(range)
        );
    }

    private TargetChoice choice(LivingEntity target, Source source) {
        return targetChoice(target, source.debugName, source.priority);
    }

    @Override
    protected Map<String, String> additionalDebugDetails() {
        return Map.of("protecting_babies", Boolean.toString(protectingBabies));
    }

    private enum Source {
        OWNER_HURT("owner_hurt", 0),
        OWNER_ATTACKED("owner_attacked", 1),
        PROTECT_BABY("protect_baby", 2),
        RETALIATION("retaliation", 3),
        AGGRESSIVE_WILD("aggressive_wild", 4),
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

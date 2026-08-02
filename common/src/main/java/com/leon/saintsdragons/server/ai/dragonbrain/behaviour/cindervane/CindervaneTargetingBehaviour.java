package com.leon.saintsdragons.server.ai.dragonbrain.behaviour.cindervane;

import com.leon.saintsdragons.common.registry.ModTags;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonHuntAndEatBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonTargetingBehaviour;
import com.leon.saintsdragons.server.ai.DragonTargetingHelper;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
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

public final class CindervaneTargetingBehaviour extends DragonTargetingBehaviour<Cindervane> {
    private static final double BABY_PROTECTION_RANGE = 16.0D;
    private static final double PACK_ASSIST_RANGE = 36.0D;

    private int lastOwnerHurtTimestamp;
    private int lastOwnerAttackTimestamp;
    private int lastSelfHurtTimestamp;
    private int packPollCooldown;
    private int playerPollCooldown;
    private int raidPollCooldown;
    private int huntPollCooldown;
    private boolean protectingBabies;

    @Nullable
    @Override
    protected TargetChoice findPriorityTarget(DragonBrainContext<Cindervane> context) {
        Cindervane dragon = context.dragon();
        List<Cindervane> babies = protectableBabies(dragon);
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

        if (packPollCooldown-- <= 0) {
            LivingEntity threat = packThreat(context.level(), dragon);
            if (threat != null) {
                packPollCooldown = 20 + dragon.getRandom().nextInt(20);
                return choice(threat, Source.PACK_DEFENSE);
            }
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
                                ModTags.EntityTypes.CINDERVANE_HUNT_PREY
                        )
                );
                if (prey != null) return choice(prey, Source.HUNT);
            }
        }
        return null;
    }

    @Override
    protected boolean canAcquireTargets(Cindervane dragon) {
        return dragon.isAlive()
                && !dragon.isDying()
                && !dragon.isBaby()
                && !dragon.isVehicle()
                && !dragon.isPassenger()
                && !dragon.isOrderedToSit()
                && !dragon.isSleepLocked();
    }

    @Override
    protected boolean canRetainTarget(Cindervane dragon, LivingEntity target, String source) {
        if (Source.HUNT.debugName.equals(source)
                && !DragonHuntAndEatBehaviour.shouldAcquirePrey(dragon)) {
            return false;
        }
        double range = Source.PROTECT_BABY.debugName.equals(source)
                ? BABY_PROTECTION_RANGE
                : Source.PACK_DEFENSE.debugName.equals(source)
                ? PACK_ASSIST_RANGE * 2.0D
                : followRange(dragon);
        return dragon.distanceToSqr(target) <= range * range;
    }

    @Override
    protected boolean suppressesTargetRetention(DragonBrainContext<Cindervane> context) {
        if (!protectingBabies) return false;
        LivingEntity current = context.memories().get(DragonMemories.ATTACK_TARGET).orElse(null);
        LivingEntity owner = context.dragon().getOwner();
        return owner == null
                || current != owner.getLastHurtByMob() && current != owner.getLastHurtMob();
    }

    @Override
    protected void targetChanged(Cindervane dragon,
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
    protected void targetCleared(Cindervane dragon,
                                 @Nullable LivingEntity oldTarget,
                                 String oldSource) {
        if (Source.HUNT.debugName.equals(oldSource)) dragon.clearPassiveHuntTarget();
    }

    private void keepNearBabies(Cindervane dragon) {
        if (dragon.isAerial() && !dragon.isLanding()) dragon.beginAiLanding();
    }

    private List<Cindervane> protectableBabies(Cindervane dragon) {
        if (dragon.isBaby() || !dragon.isFemale()) return List.of();
        return dragon.level().getEntitiesOfClass(
                Cindervane.class,
                dragon.getBoundingBox().inflate(BABY_PROTECTION_RANGE),
                baby -> baby.isBaby() && baby.isAlive() && assignedTo(dragon, baby)
        );
    }

    private boolean assignedTo(Cindervane mother, Cindervane baby) {
        UUID assigned = baby.getAssignedParentUuid();
        if (assigned != null) return mother.getUUID().equals(assigned);
        Cindervane nearest = baby.level().getEntitiesOfClass(
                        Cindervane.class,
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
    private LivingEntity newestBabyThreat(Cindervane dragon, List<Cindervane> babies) {
        LivingEntity newest = null;
        int newestTimestamp = Integer.MIN_VALUE;
        for (Cindervane baby : babies) {
            LivingEntity threat = baby.getLastDamager();
            int timestamp = baby.getLastDamagerTimestamp();
            if (threat == null) {
                threat = baby.getLastHurtByMob();
                timestamp = baby.getLastHurtByMobTimestamp();
            }
            if (threat != null
                    && timestamp > newestTimestamp
                    && !(threat instanceof Cindervane)
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
    private LivingEntity packThreat(ServerLevel level, Cindervane dragon) {
        if (!dragon.canParticipateInPack()) {
            dragon.setPackLeaderUuid(null);
            return null;
        }
        UUID leaderUuid = dragon.getPackLeaderUuid();
        if (leaderUuid != null) {
            Entity entity = level.getEntity(leaderUuid);
            if (entity instanceof Cindervane leader && compatible(dragon, leader)) {
                LivingEntity threat = firstThreat(dragon, leader.getTarget(), leader.getLastHurtByMob());
                if (threat != null) return threat;
            }
        }
        double range = Math.max(8.0D, dragon.getPackSearchRadius());
        List<Cindervane> nearby = level.getEntitiesOfClass(
                Cindervane.class,
                dragon.getBoundingBox().inflate(range),
                candidate -> compatible(dragon, candidate)
        );
        for (Cindervane packmate : nearby) {
            LivingEntity threat = firstThreat(dragon, packmate.getTarget(), packmate.getLastHurtByMob());
            if (threat != null) return threat;
        }
        return null;
    }

    private boolean compatible(Cindervane dragon, Cindervane other) {
        if (other == dragon || !other.canParticipateInPack() || dragon.isTame() != other.isTame()) return false;
        return !dragon.isTame() || dragon.getOwner() != null && other.isOwnedBy(dragon.getOwner());
    }

    @Nullable
    private LivingEntity firstThreat(Cindervane dragon,
                                     @Nullable LivingEntity first,
                                     @Nullable LivingEntity second) {
        if (isUsableTarget(dragon, first)) return first;
        return isUsableTarget(dragon, second) ? second : null;
    }

    @Nullable
    private <E extends LivingEntity> E nearest(ServerLevel level,
                                                Cindervane dragon,
                                                Class<E> type,
                                                Predicate<E> predicate) {
        double range = followRange(dragon);
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

    private double followRange(Cindervane dragon) {
        return Math.max(32.0D, dragon.getAttributeValue(Attributes.FOLLOW_RANGE));
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
        PACK_DEFENSE("pack_defense", 3),
        RETALIATION("retaliation", 4),
        AGGRESSIVE_WILD("aggressive_wild", 5),
        RAID_DEFENSE("raid_defense", 6),
        HUNT("hunt", 7);

        private final String debugName;
        private final int priority;

        Source(String debugName, int priority) {
            this.debugName = debugName;
            this.priority = priority;
        }
    }
}

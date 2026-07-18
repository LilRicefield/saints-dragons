package com.leon.saintsdragons.server.ai.dragonbrain.behaviour.stegonaut;

import com.leon.saintsdragons.common.registry.ModTags;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonTargetingBehaviour;
import com.leon.saintsdragons.server.ai.DragonTargetingHelper;
import com.leon.saintsdragons.server.entity.dragons.stegonaut.Stegonaut;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

public final class StegonautTargetingBehaviour extends DragonTargetingBehaviour<Stegonaut> {
    private static final double BABY_PROTECTION_RANGE = 16.0D;
    private static final double PACK_ASSIST_RANGE = 36.0D;

    private int lastOwnerHurtTimestamp;
    private int lastOwnerAttackTimestamp;
    private int lastSelfHurtTimestamp;
    private int packPollCooldown;
    private int raidPollCooldown;
    private int playerPollCooldown;
    private int huntPollCooldown;
    private boolean protectingBabies;

    @Nullable
    @Override
    protected TargetChoice findPriorityTarget(DragonBrainContext<Stegonaut> context) {
        Stegonaut dragon = context.dragon();

        List<Stegonaut> babies = protectableBabies(dragon);
        protectingBabies = !babies.isEmpty();
        if (protectingBabies) {
            LivingEntity threat = newestBabyThreat(dragon, babies);
            if (threat != null) {
                return choice(threat, Source.PROTECT_BABY);
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

        if (packPollCooldown-- <= 0) {
            packPollCooldown = 20 + dragon.getRandom().nextInt(20);
            LivingEntity packThreat = findPackThreat(context.level(), dragon);
            if (packThreat != null) {
                return choice(packThreat, Source.PACK_DEFENSE);
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
            if (dragon.isAggressiveWild()) {
                Player player = nearest(context.level(), dragon, Player.class,
                        candidate -> !candidate.isCreative() && !candidate.isSpectator());
                if (player != null) {
                    return choice(player, Source.AGGRESSIVE_WILD);
                }
            }
        }

        if (raidPollCooldown-- <= 0) {
            raidPollCooldown = 10;
            if (canDefendRaid(dragon)) {
                Raider raider = nearest(context.level(), dragon, Raider.class,
                        DragonTargetingHelper::isActiveRaidTarget);
                if (raider != null) {
                    return choice(raider, Source.RAID_DEFENSE);
                }
            }
        }

        if (huntPollCooldown-- <= 0) {
            huntPollCooldown = 80;
            if (canHunt(dragon)) {
                LivingEntity prey = nearest(context.level(), dragon, LivingEntity.class,
                        candidate -> DragonTargetingHelper.isTaggedHuntTarget(
                                candidate, ModTags.EntityTypes.STEGONAUT_TARGETS));
                if (prey != null) {
                    return choice(prey, Source.HUNT);
                }
            }
        }
        return null;
    }

    @Override
    protected boolean canAcquireTargets(Stegonaut dragon) {
        return dragon.isAlive()
                && !dragon.isDying()
                && !dragon.isVehicle()
                && !dragon.isPassenger()
                && !dragon.isOrderedToSit()
                && !dragon.isSleepLocked();
    }

    private boolean canDefendRaid(Stegonaut dragon) {
        return !dragon.isBaby();
    }

    private boolean canHunt(Stegonaut dragon) {
        return !dragon.isTame() && !dragon.isBaby();
    }

    @Nullable
    private LivingEntity findPackThreat(ServerLevel level, Stegonaut dragon) {
        if (!dragon.canParticipateInPack()) {
            dragon.setPackLeaderUuid(null);
            return null;
        }

        LivingEntity direct = firstUsable(dragon, dragon.getLastHurtByMob());
        if (direct != null) {
            return direct;
        }

        UUID leaderId = dragon.getPackLeaderUuid();
        if (leaderId != null) {
            Entity entity = level.getEntity(leaderId);
            if (entity instanceof Stegonaut leader && compatiblePackmate(dragon, leader)) {
                LivingEntity threat = firstUsable(dragon, leader.getTarget(), leader.getLastHurtByMob());
                if (threat != null) {
                    return threat;
                }
            }
        }

        AABB box = dragon.getBoundingBox().inflate(Math.max(8.0D, dragon.getPackSearchRadius()));
        for (Stegonaut packmate : level.getEntitiesOfClass(Stegonaut.class, box,
                other -> compatiblePackmate(dragon, other))) {
            LivingEntity threat = firstUsable(dragon, packmate.getTarget(), packmate.getLastHurtByMob());
            if (threat != null && dragon.distanceToSqr(threat) <= PACK_ASSIST_RANGE * PACK_ASSIST_RANGE) {
                return threat;
            }
        }
        return null;
    }

    private boolean compatiblePackmate(Stegonaut dragon, Stegonaut other) {
        return other != dragon && other.canParticipateInPack() && dragon.isTame() == other.isTame();
    }

    @Nullable
    private LivingEntity firstUsable(Stegonaut dragon, LivingEntity... candidates) {
        for (LivingEntity candidate : candidates) {
            if (isUsableTarget(dragon, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private List<Stegonaut> protectableBabies(Stegonaut dragon) {
        if (dragon.isBaby() || !dragon.isFemale()) {
            return List.of();
        }
        return dragon.level().getEntitiesOfClass(
                Stegonaut.class,
                dragon.getBoundingBox().inflate(BABY_PROTECTION_RANGE),
                baby -> baby.isBaby() && baby.isAlive() && assignedTo(dragon, baby)
        );
    }

    private boolean assignedTo(Stegonaut mother, Stegonaut baby) {
        UUID assigned = baby.getAssignedParentUuid();
        if (assigned != null) {
            return mother.getUUID().equals(assigned);
        }

        Stegonaut nearest = baby.level().getEntitiesOfClass(
                        Stegonaut.class,
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
    private LivingEntity newestBabyThreat(Stegonaut dragon, List<Stegonaut> babies) {
        LivingEntity newest = null;
        int newestTimestamp = Integer.MIN_VALUE;
        for (Stegonaut baby : babies) {
            LivingEntity threat = baby.getLastDamager();
            int timestamp = baby.getLastDamagerTimestamp();
            if (threat == null) {
                threat = baby.getLastHurtByMob();
                timestamp = baby.getLastHurtByMobTimestamp();
            }
            if (timestamp > newestTimestamp
                    && !(threat instanceof Stegonaut)
                    && threat != dragon.getOwner()
                    && isUsableTarget(dragon, threat)) {
                newest = threat;
                newestTimestamp = timestamp;
            }
        }
        return newest;
    }

    @Nullable
    private <E extends LivingEntity> E nearest(ServerLevel level,
                                                Stegonaut dragon,
                                                Class<E> type,
                                                Predicate<E> predicate) {
        double range = followRange(dragon);
        TargetingConditions conditions = TargetingConditions.forCombat()
                .range(range)
                .selector(entity -> predicate.test(type.cast(entity)) && dragon.canTarget(entity));
        return level.getNearestEntity(type, conditions, dragon,
                dragon.getX(), dragon.getEyeY(), dragon.getZ(), dragon.getBoundingBox().inflate(range));
    }

    @Override
    protected boolean isUsableTarget(Stegonaut dragon, @Nullable LivingEntity target) {
        return target != null && target.isAlive() && dragon.isTargetValid(target) && dragon.canTarget(target);
    }

    @Override
    protected boolean canRetainTarget(Stegonaut dragon, LivingEntity target, String source) {
        double range = Source.PACK_DEFENSE.debugName.equals(source)
                ? PACK_ASSIST_RANGE * 2.0D
                : followRange(dragon);
        return dragon.distanceToSqr(target) <= range * range;
    }

    private double followRange(Stegonaut dragon) {
        return Math.max(16.0D, dragon.getAttributeValue(Attributes.FOLLOW_RANGE));
    }

    @Override
    protected boolean suppressesTargetRetention(DragonBrainContext<Stegonaut> context) {
        return protectingBabies;
    }

    @Override
    protected void targetChanged(Stegonaut dragon,
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
    protected void targetCleared(Stegonaut dragon,
                                 @Nullable LivingEntity oldTarget,
                                 String oldSource) {
        if (Source.HUNT.debugName.equals(oldSource)) {
            dragon.clearPassiveHuntTarget();
        }
    }

    @Override
    protected Map<String, String> additionalDebugDetails() {
        return Map.of("protecting_babies", Boolean.toString(protectingBabies));
    }

    private TargetChoice choice(LivingEntity target, Source source) {
        return targetChoice(target, source.debugName, source.priority);
    }

    private enum Source {
        PROTECT_BABY("protect_baby", 0),
        OWNER_HURT("owner_hurt", 1),
        OWNER_ATTACKED("owner_attacked", 2),
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

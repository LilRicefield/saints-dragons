package com.leon.saintsdragons.server.ai.dragonbrain.behaviour.atroxiia;

import com.leon.saintsdragons.server.ai.dragonbrain.DragonBrainContext;
import com.leon.saintsdragons.server.ai.dragonbrain.behaviour.DragonTargetingBehaviour;
import com.leon.saintsdragons.server.entity.dragons.atroxiia.Atroxiia;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public final class AtroxiiaTargetingBehaviour extends DragonTargetingBehaviour<Atroxiia> {
    private int lastOwnerHurtTimestamp;
    private int lastOwnerAttackTimestamp;
    private int lastSelfHurtTimestamp;
    private int playerPollCooldown;

    @Nullable
    @Override
    protected TargetChoice findPriorityTarget(DragonBrainContext<Atroxiia> context) {
        Atroxiia dragon = context.dragon();
        LivingEntity owner = dragon.getOwner();
        if (owner != null) {
            LivingEntity threat = owner.getLastHurtByMob();
            int timestamp = owner.getLastHurtByMobTimestamp();
            if (timestamp != lastOwnerHurtTimestamp && isUsableTarget(dragon, threat)) {
                lastOwnerHurtTimestamp = timestamp;
                return targetChoice(threat, "owner_hurt", 0);
            }

            threat = owner.getLastHurtMob();
            timestamp = owner.getLastHurtMobTimestamp();
            if (timestamp != lastOwnerAttackTimestamp && isUsableTarget(dragon, threat)) {
                lastOwnerAttackTimestamp = timestamp;
                return targetChoice(threat, "owner_attacked", 1);
            }
        }

        LivingEntity attacker = dragon.getLastHurtByMob();
        int hurtTimestamp = dragon.getLastHurtByMobTimestamp();
        if (hurtTimestamp != lastSelfHurtTimestamp && isUsableTarget(dragon, attacker)) {
            lastSelfHurtTimestamp = hurtTimestamp;
            return targetChoice(attacker, "retaliation", 2);
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
                if (player != null) {
                    return targetChoice(player, "aggressive_wild", 3);
                }
            }
        }
        return null;
    }

    @Override
    protected boolean canAcquireTargets(Atroxiia dragon) {
        return dragon.isAlive()
                && !dragon.isDying()
                && !dragon.isBaby()
                && !dragon.isTamingStunned()
                && !dragon.isVehicle()
                && !dragon.isPassenger()
                && !dragon.isOrderedToSit()
                && !dragon.isSleepLocked();
    }

    @Override
    protected boolean canRetainTarget(Atroxiia dragon, LivingEntity target, String source) {
        double range = Math.max(32.0D, dragon.getAttributeValue(Attributes.FOLLOW_RANGE));
        return dragon.distanceToSqr(target) <= range * range;
    }

    @Nullable
    private <E extends LivingEntity> E nearest(ServerLevel level,
                                                Atroxiia dragon,
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
}

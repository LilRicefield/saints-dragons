package com.leon.saintsdragons.server.entity.ability.abilities.primitivedrake;

import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.dragons.primitivedrake.PrimitiveDrakeEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Provides the Primitive Drake's passive aura: Resistance and Absorption to allies.
 */
public class PrimitiveDrakePassiveBuffAbility {
    private static final double BUFF_RANGE = 8.0;
    private static final int UPDATE_INTERVAL = 20;
    private static final int BUFF_DURATION_TICKS = 40;
    private static final int RESISTANCE_AMPLIFIER = 0;
    private static final int ABSORPTION_AMPLIFIER = 0;

    private final PrimitiveDrakeEntity drake;
    private final Level level;

    private int tickCounter = 0;
    private Set<UUID> buffedEntityIds = new HashSet<>();

    public PrimitiveDrakePassiveBuffAbility(PrimitiveDrakeEntity drake) {
        this.drake = drake;
        this.level = drake.level();
    }

    public void tick() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (++tickCounter < UPDATE_INTERVAL) {
            return;
        }
        tickCounter = 0;

        if (!drake.isAlive() || drake.isPlayingDead()) {
            clearTrackedBuffs(serverLevel);
            return;
        }

        applyBuffs(serverLevel);
    }

    private void applyBuffs(ServerLevel serverLevel) {
        List<LivingEntity> nearbyEntities = serverLevel.getEntitiesOfClass(
            LivingEntity.class,
            drake.getBoundingBox().inflate(BUFF_RANGE),
            this::isEligibleForBuff
        );

        Set<UUID> currentNearby = new HashSet<>();

        for (LivingEntity entity : nearbyEntities) {
            applyResistance(entity);
            applyAbsorption(entity);
            currentNearby.add(entity.getUUID());
        }

        buffedEntityIds = currentNearby;
    }

    private boolean isEligibleForBuff(LivingEntity entity) {
        if (entity == drake || !entity.isAlive()) {
            return false;
        }

        if (entity instanceof Player player) {
            return isPlayerEligible(player);
        }

        if (entity instanceof DragonEntity dragon) {
            return isDragonEligible(dragon);
        }

        return false;
    }

    private boolean isPlayerEligible(Player player) {
        if (!drake.isTame()) {
            return false;
        }

        LivingEntity owner = drake.getOwner();
        if (!(owner instanceof Player ownerPlayer)) {
            return false;
        }

        if (ownerPlayer.getUUID().equals(player.getUUID())) {
            return true;
        }

        return drake.allyManager.isAlly(player);
    }

    private boolean isDragonEligible(DragonEntity dragon) {
        if (!drake.isTame()) {
            return false;
        }

        LivingEntity owner = drake.getOwner();
        if (!(owner instanceof Player ownerPlayer)) {
            return false;
        }

        if (dragon.isOwnedBy(ownerPlayer)) {
            return true;
        }

        return dragon instanceof PrimitiveDrakeEntity alliedDrake && alliedDrake.allyManager.isAlly(ownerPlayer);
    }

    private void applyResistance(LivingEntity entity) {
        entity.addEffect(new MobEffectInstance(
            MobEffects.DAMAGE_RESISTANCE,
            BUFF_DURATION_TICKS,
            RESISTANCE_AMPLIFIER,
            false,
            false,
            true
        ));
    }

    private void applyAbsorption(LivingEntity entity) {
        entity.addEffect(new MobEffectInstance(
            MobEffects.ABSORPTION,
            BUFF_DURATION_TICKS,
            ABSORPTION_AMPLIFIER,
            false,
            false,
            true
        ));
    }

    public void cleanup() {
        if (level instanceof ServerLevel serverLevel) {
            clearTrackedBuffs(serverLevel);
        } else {
            buffedEntityIds.clear();
        }
    }

    private void clearTrackedBuffs(ServerLevel serverLevel) {
        for (UUID uuid : buffedEntityIds) {
            Entity entity = serverLevel.getEntity(uuid);
            if (entity instanceof LivingEntity livingEntity) {
                livingEntity.removeEffect(MobEffects.DAMAGE_RESISTANCE);
                livingEntity.removeEffect(MobEffects.ABSORPTION);
            }
        }
        buffedEntityIds.clear();
    }

    public static double getBuffRange() {
        return BUFF_RANGE;
    }

    public static int getResistanceAmplifier() {
        return RESISTANCE_AMPLIFIER;
    }

    public static int getAbsorptionAmplifier() {
        return ABSORPTION_AMPLIFIER;
    }
}


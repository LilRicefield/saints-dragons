package com.leon.saintsdragons.common.item;

import com.leon.saintsdragons.common.registry.ModAttributes;
import com.leon.saintsdragons.common.registry.ModParticles;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.data.DragonlordPlayerSavedData;
import com.leon.saintsdragons.server.entity.effect.ImpactRingEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class DragonlordArmorSetBonus {
    private static final double DOUBLE_JUMP_VELOCITY = 0.85D;
    private static final double LANDING_SHOCKWAVE_TOUCH_RADIUS = 7.5D;
    private static final double LANDING_SHOCKWAVE_FALLOFF_RADIUS = 4.0D;
    private static final double LANDING_SHOCKWAVE_Y_RADIUS = 5.0D;
    private static final double LANDING_SHOCKWAVE_STRENGTH = 2.15D;
    private static final double LANDING_SHOCKWAVE_LIFT = 0.75D;
    private static final double LANDING_SHOCKWAVE_MIN_DROP = 4.0D;
    private static final float LANDING_IMPACT_RING_SCALE = 0.25F;
    private static final int LANDING_IMPACT_DUST_COUNT = 18;
    private static final int DOUBLE_JUMP_FLAME_COUNT = 20;
    private static final float FALL_DAMAGE_BLOCK_THRESHOLD = 16.0F;
    private static final float VANILLA_PLAYER_MAX_HEALTH = 20.0F;
    private static final UUID DOUBLE_JUMP_MODIFIER_UUID = UUID.fromString("8e8d7d4f-14b7-4df2-aeaf-4b47e6c4f617");
    private static final AttributeModifier DOUBLE_JUMP_MODIFIER = new AttributeModifier(
            DOUBLE_JUMP_MODIFIER_UUID,
            "Dragonlord double jump",
            1.0D,
            AttributeModifier.Operation.ADDITION
    );
    private static final Set<UUID> USED_MIDAIR_JUMP = new HashSet<>();
    private static final Set<UUID> PENDING_LANDING_SHOCKWAVE = new HashSet<>();
    private static final Map<UUID, Double> DOUBLE_JUMP_PEAK_Y = new HashMap<>();
    private static final Set<UUID> PENDING_HEALTH_RESTORE = new HashSet<>();

    private DragonlordArmorSetBonus() {
    }

    public static void tick(ServerPlayer player) {
        if (player == null) {
            return;
        }

        boolean fullSet = isWearingFullSet(player);
        AttributeInstance doubleJump = player.getAttribute(ModAttributes.DOUBLE_JUMP.get());
        if (doubleJump != null) {
            if (fullSet) {
                if (doubleJump.getModifier(DOUBLE_JUMP_MODIFIER_UUID) == null) {
                    doubleJump.addTransientModifier(DOUBLE_JUMP_MODIFIER);
                }
            } else {
                doubleJump.removeModifier(DOUBLE_JUMP_MODIFIER_UUID);
            }
        }

        if (!fullSet || player.onClimbable() || player.isInWaterOrBubble()) {
            USED_MIDAIR_JUMP.remove(player.getUUID());
            PENDING_LANDING_SHOCKWAVE.remove(player.getUUID());
            DOUBLE_JUMP_PEAK_Y.remove(player.getUUID());
            if (!fullSet) {
                PENDING_HEALTH_RESTORE.remove(player.getUUID());
            }
            return;
        }

        restoreSavedHealthIfNeeded(player);
        if (!PENDING_HEALTH_RESTORE.contains(player.getUUID()) && player.tickCount % 100 == 0) {
            saveHealthForReload(player);
        }

        if (player.onGround()) {
            if (PENDING_LANDING_SHOCKWAVE.remove(player.getUUID())) {
                tryLandingShockwave(player);
            }
            USED_MIDAIR_JUMP.remove(player.getUUID());
            DOUBLE_JUMP_PEAK_Y.remove(player.getUUID());
        } else if (PENDING_LANDING_SHOCKWAVE.contains(player.getUUID())) {
            DOUBLE_JUMP_PEAK_Y.merge(player.getUUID(), player.getY(), Math::max);
        }
    }

    public static boolean tryDoubleJump(ServerPlayer player) {
        if (player == null || player.isPassenger() || player.onGround() || player.onClimbable() || player.isInWaterOrBubble()) {
            return false;
        }
        if (player.getAbilities().flying || player.isFallFlying()) {
            return false;
        }
        if (USED_MIDAIR_JUMP.contains(player.getUUID())) {
            return false;
        }
        AttributeInstance doubleJump = player.getAttribute(ModAttributes.DOUBLE_JUMP.get());
        if (doubleJump == null || doubleJump.getValue() < 1.0D) {
            return false;
        }

        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(motion.x, Math.max(DOUBLE_JUMP_VELOCITY, motion.y + DOUBLE_JUMP_VELOCITY), motion.z);
        player.hurtMarked = true;
        player.resetFallDistance();
        USED_MIDAIR_JUMP.add(player.getUUID());
        PENDING_LANDING_SHOCKWAVE.add(player.getUUID());
        DOUBLE_JUMP_PEAK_Y.put(player.getUUID(), player.getY());
        spawnDoubleJumpEffects(player);
        return true;
    }

    public static boolean blocksDamage(ServerPlayer player, DamageSource source) {
        if (player == null || source == null || !isWearingFullSet(player)) {
            return false;
        }
        if (source.is(DamageTypeTags.IS_FALL) && player.fallDistance <= FALL_DAMAGE_BLOCK_THRESHOLD) {
            player.fallDistance = 0.0F;
            return true;
        }

        boolean blocked = (source.is(DamageTypeTags.IS_FIRE) && player.getAttributeValue(ModAttributes.FIRE_RESISTANCE.get()) >= 100.0D)
                || (source.is(DamageTypeTags.IS_EXPLOSION) && player.getAttributeValue(ModAttributes.BLAST_RESISTANCE.get()) >= 100.0D);
        if (blocked && source.is(DamageTypeTags.IS_FIRE)) {
            player.clearFire();
        }
        return blocked;
    }

    public static void queueHealthRestore(ServerPlayer player) {
        if (player != null) {
            PENDING_HEALTH_RESTORE.add(player.getUUID());
        }
    }

    public static void saveHealthForReload(ServerPlayer player) {
        if (player == null) {
            return;
        }
        DragonlordPlayerSavedData data = DragonlordPlayerSavedData.get(player.serverLevel());
        if (isWearingFullSet(player)) {
            data.saveHealth(player.getUUID(), player.getHealth());
        } else {
            data.clearHealth(player.getUUID());
        }
    }

    public static boolean isWearingFullSet(ServerPlayer player) {
        return isDragonlord(player.getItemBySlot(EquipmentSlot.HEAD))
                && isDragonlord(player.getItemBySlot(EquipmentSlot.CHEST))
                && isDragonlord(player.getItemBySlot(EquipmentSlot.LEGS))
                && isDragonlord(player.getItemBySlot(EquipmentSlot.FEET));
    }

    private static boolean isDragonlord(ItemStack stack) {
        return stack.getItem() instanceof DragonlordArmorItem;
    }

    private static void tryLandingShockwave(ServerPlayer player) {
        double peakY = DOUBLE_JUMP_PEAK_Y.getOrDefault(player.getUUID(), player.getY());
        if (peakY - player.getY() < LANDING_SHOCKWAVE_MIN_DROP) {
            return;
        }
        knockBackNearbyEntities(player);
        spawnLandingImpactEffects(player);
    }

    private static void knockBackNearbyEntities(ServerPlayer player) {
        AABB hitbox = player.getBoundingBox().inflate(
                LANDING_SHOCKWAVE_TOUCH_RADIUS,
                LANDING_SHOCKWAVE_Y_RADIUS,
                LANDING_SHOCKWAVE_TOUCH_RADIUS
        );
        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, hitbox, DragonlordArmorSetBonus::canShockwaveTarget)) {
            if (target == player) {
                continue;
            }

            Vec3 offset = target.position().subtract(player.position());
            Vec3 horizontal = new Vec3(offset.x, 0.0D, offset.z);
            double distance = horizontal.length();
            if (distance > LANDING_SHOCKWAVE_TOUCH_RADIUS) {
                continue;
            }

            Vec3 direction = distance > 1.0E-4D
                    ? horizontal.scale(1.0D / distance)
                    : new Vec3(player.getLookAngle().x, 0.0D, player.getLookAngle().z).normalize();
            if (direction.lengthSqr() < 1.0E-4D) {
                direction = new Vec3(0.0D, 0.0D, 1.0D);
            }

            double falloff = 1.0D - Math.min(distance / LANDING_SHOCKWAVE_FALLOFF_RADIUS, 0.75D);
            double strength = LANDING_SHOCKWAVE_STRENGTH * falloff;
            target.push(direction.x * strength, LANDING_SHOCKWAVE_LIFT, direction.z * strength);
            target.hurtMarked = true;
        }
    }

    private static void spawnLandingImpactEffects(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel server)) {
            return;
        }

        Vec3 origin = player.position();
        server.addFreshEntity(new ImpactRingEntity(server, origin.add(0.0D, 0.08D, 0.0D), LANDING_IMPACT_RING_SCALE));

        double y = player.getBoundingBox().minY + 0.08D;
        for (int i = 0; i < LANDING_IMPACT_DUST_COUNT; i++) {
            double angle = (Math.PI * 2.0D * i) / LANDING_IMPACT_DUST_COUNT;
            server.sendParticles(ModParticles.DRAGON_DUST.get(),
                    origin.x, y, origin.z,
                    0, Math.cos(angle) * 0.18D, 0.08D,
                    Math.sin(angle) * 0.18D, 1.0D);
        }

        server.playSound(null, player.blockPosition(), ModSounds.DRAGONLORD_ARMOR_IMPACT.get(),
                SoundSource.PLAYERS, 0.9F, 0.95F + player.getRandom().nextFloat() * 0.1F);
    }

    private static void spawnDoubleJumpEffects(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel server)) {
            return;
        }

        Vec3 origin = player.position();
        double y = player.getY() + 0.35D;
        for (int i = 0; i < DOUBLE_JUMP_FLAME_COUNT; i++) {
            double angle = (Math.PI * 2.0D * i) / DOUBLE_JUMP_FLAME_COUNT;
            server.sendParticles(ParticleTypes.FLAME,
                    origin.x, y, origin.z,
                    0, Math.cos(angle) * 0.18D, 0.02D,
                    Math.sin(angle) * 0.18D, 1.0D);
        }

        server.playSound(null, player.blockPosition(), ModSounds.DRAGONLORD_ARMOR_DOUBLE_JUMP.get(),
                SoundSource.PLAYERS, 0.75F, 0.95F + player.getRandom().nextFloat() * 0.1F);
    }

    private static void restoreSavedHealthIfNeeded(ServerPlayer player) {
        UUID playerId = player.getUUID();
        if (!PENDING_HEALTH_RESTORE.contains(playerId)) {
            return;
        }

        DragonlordPlayerSavedData data = DragonlordPlayerSavedData.get(player.serverLevel());
        var savedHealth = data.getHealth(playerId);
        if (savedHealth.isEmpty()) {
            PENDING_HEALTH_RESTORE.remove(playerId);
            return;
        }

        float health = savedHealth.get();
        float maxHealth = player.getMaxHealth();
        if (health > VANILLA_PLAYER_MAX_HEALTH && maxHealth <= VANILLA_PLAYER_MAX_HEALTH + 0.01F) {
            return;
        }

        data.consumeHealth(playerId);
        PENDING_HEALTH_RESTORE.remove(playerId);
        player.setHealth(Math.min(health, maxHealth));
    }

    private static boolean canShockwaveTarget(LivingEntity entity) {
        return entity.isAlive() && !(entity instanceof ServerPlayer player && player.isSpectator());
    }
}

package com.leon.saintsdragons.common.item;

import com.leon.saintsdragons.common.registry.ModAttributes;
import com.leon.saintsdragons.server.data.DragonlordPlayerSavedData;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class DragonlordArmorSetBonus {
    private static final double DOUBLE_JUMP_VELOCITY = 0.85D;
    private static final double LANDING_SHOCKWAVE_TOUCH_RADIUS = 7.5D;
    private static final double LANDING_SHOCKWAVE_FALLOFF_RADIUS = 4.0D;
    private static final double LANDING_SHOCKWAVE_Y_RADIUS = 5.0D;
    private static final double LANDING_SHOCKWAVE_STRENGTH = 2.15D;
    private static final double LANDING_SHOCKWAVE_LIFT = 0.75D;
    private static final float FALL_DAMAGE_BLOCK_THRESHOLD = 16.0F;
    private static final UUID DOUBLE_JUMP_MODIFIER_UUID = UUID.fromString("8e8d7d4f-14b7-4df2-aeaf-4b47e6c4f617");
    private static final AttributeModifier DOUBLE_JUMP_MODIFIER = new AttributeModifier(
            DOUBLE_JUMP_MODIFIER_UUID,
            "Dragonlord double jump",
            1.0D,
            AttributeModifier.Operation.ADDITION
    );
    private static final Set<UUID> USED_MIDAIR_JUMP = new HashSet<>();
    private static final Set<UUID> PENDING_LANDING_SHOCKWAVE = new HashSet<>();
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
            if (!fullSet) {
                PENDING_HEALTH_RESTORE.remove(player.getUUID());
            }
            return;
        }

        restoreSavedHealthIfNeeded(player);
        if (player.tickCount % 100 == 0) {
            saveHealthForReload(player);
        }

        if (player.onGround()) {
            if (PENDING_LANDING_SHOCKWAVE.remove(player.getUUID())) {
                knockBackNearbyEntities(player);
            }
            USED_MIDAIR_JUMP.remove(player.getUUID());
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

    private static void restoreSavedHealthIfNeeded(ServerPlayer player) {
        if (!PENDING_HEALTH_RESTORE.remove(player.getUUID())) {
            return;
        }
        DragonlordPlayerSavedData.get(player.serverLevel())
                .consumeHealth(player.getUUID())
                .ifPresent(health -> player.setHealth(Math.min(health, player.getMaxHealth())));
    }

    private static boolean canShockwaveTarget(LivingEntity entity) {
        return entity.isAlive() && !(entity instanceof ServerPlayer player && player.isSpectator());
    }
}

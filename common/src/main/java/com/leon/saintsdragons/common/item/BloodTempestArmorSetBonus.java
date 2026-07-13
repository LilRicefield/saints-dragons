package com.leon.saintsdragons.common.item;

import com.leon.saintsdragons.common.network.BloodTempestDodgeDirection;
import com.leon.saintsdragons.common.network.MessageBloodTempestDodgeEffect;
import com.leon.saintsdragons.common.network.NetworkHandler;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.component.DragonMotionMath;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class BloodTempestArmorSetBonus {
    private static final int DODGE_COOLDOWN_TICKS = 30;
    private static final int DODGE_IFRAMES_TICKS = 8;
    private static final double FORWARD_DASH_SPEED = 4.0D;
    private static final double DODGE_SPEED = 4.0D;

    private static final Map<UUID, Integer> DODGE_COOLDOWNS = new HashMap<>();
    private static final Map<UUID, Integer> DODGE_IFRAMES = new HashMap<>();

    private BloodTempestArmorSetBonus() {
    }

    public static void tick(ServerPlayer player) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUUID();
        if (!player.isAlive()) {
            clear(player);
            return;
        }

        decrement(DODGE_COOLDOWNS, playerId);
        if (!isWearingFullSet(player)) {
            DODGE_IFRAMES.remove(playerId);
            return;
        }
        decrement(DODGE_IFRAMES, playerId);
    }

    public static boolean tryDodge(ServerPlayer player, BloodTempestDodgeDirection direction) {
        if (player == null
                || direction == null
                || !player.isAlive()
                || player.isSpectator()
                || player.isPassenger()
                || player.getAbilities().flying
                || player.isFallFlying()
                || player.onClimbable()
                || player.isInWaterOrBubble()
                || !isWearingFullSet(player)
                || DODGE_COOLDOWNS.containsKey(player.getUUID())) {
            return false;
        }

        Vec3 forward = DragonMotionMath.horizontalForward(player.getYRot());
        Vec3 left = DragonMotionMath.horizontalRight(player.getYRot());
        Vec3 dodge = switch (direction) {
            case FORWARD -> forward.scale(FORWARD_DASH_SPEED);
            case LEFT -> left.scale(DODGE_SPEED);
            case BACKWARD -> forward.scale(-DODGE_SPEED);
            case RIGHT -> left.scale(-DODGE_SPEED);
        };

        Vec3 current = player.getDeltaMovement();
        player.setDeltaMovement(dodge.x, current.y, dodge.z);
        player.hasImpulse = true;
        player.hurtMarked = true;
        player.connection.send(new ClientboundSetEntityMotionPacket(player));

        MessageBloodTempestDodgeEffect effect = new MessageBloodTempestDodgeEffect(player.getId());
        NetworkHandler.sendToTracking(player, effect);
        NetworkHandler.sendToPlayer(player, effect);

        UUID playerId = player.getUUID();
        DODGE_COOLDOWNS.put(playerId, DODGE_COOLDOWN_TICKS);
        DODGE_IFRAMES.put(playerId, DODGE_IFRAMES_TICKS);
        player.level().playSound(
                null,
                player.blockPosition(),
                ModSounds.BLOOD_TEMPEST_ARMOR_DODGE.get(),
                SoundSource.PLAYERS,
                0.8F,
                1.1F
        );
        return true;
    }

    public static boolean blocksDamage(ServerPlayer player, DamageSource source) {
        return player != null
                && source != null
                && isWearingFullSet(player)
                && DODGE_IFRAMES.containsKey(player.getUUID())
                && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    public static boolean isWearingFullSet(LivingEntity wearer) {
        return wearer != null
                && isBloodTempest(wearer.getItemBySlot(EquipmentSlot.HEAD))
                && isBloodTempest(wearer.getItemBySlot(EquipmentSlot.CHEST))
                && isBloodTempest(wearer.getItemBySlot(EquipmentSlot.LEGS))
                && isBloodTempest(wearer.getItemBySlot(EquipmentSlot.FEET));
    }

    public static void clear(ServerPlayer player) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUUID();
        DODGE_COOLDOWNS.remove(playerId);
        DODGE_IFRAMES.remove(playerId);
    }

    private static boolean isBloodTempest(ItemStack stack) {
        return stack.getItem() instanceof BloodTempestArmorItem;
    }

    private static void decrement(Map<UUID, Integer> timers, UUID playerId) {
        Integer ticks = timers.get(playerId);
        if (ticks == null) {
            return;
        }
        if (ticks <= 1) {
            timers.remove(playerId);
        } else {
            timers.put(playerId, ticks - 1);
        }
    }
}

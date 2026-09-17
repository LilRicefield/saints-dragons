package com.leon.saintsdragons.common.network;

import com.leon.saintsdragons.platform.Services;
import com.leon.saintsdragons.server.entity.base.DragonPartEntity;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.entity.part.DragonPartProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public record MessageDragonPartAttack(int dragonId, UUID dragonUuid, int partIndex) {
    private static final Map<ServerPlayer, Integer> LAST_ATTACK = new WeakHashMap<>();

    public static void encode(MessageDragonPartAttack message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.dragonId);
        buffer.writeUUID(message.dragonUuid);
        buffer.writeVarInt(message.partIndex);
    }

    public static MessageDragonPartAttack decode(FriendlyByteBuf buffer) {
        return new MessageDragonPartAttack(buffer.readVarInt(), buffer.readUUID(), buffer.readVarInt());
    }

    public static void handle(MessageDragonPartAttack message, ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator()) return;
        Entity parent = player.serverLevel().getEntity(message.dragonId);
        if (!(parent instanceof Ignivorus dragon) || !parent.getUUID().equals(message.dragonUuid)
                || !dragon.isAlive() || dragon.isRemoved() || dragon.isBaby()
                || !(parent instanceof DragonPartProvider provider)) return;
        Entity[] parts = provider.dragonParts();
        if (message.partIndex < 0 || message.partIndex >= parts.length) return;
        double reach = Services.PLATFORM.getPlayerAttackReach(player);
        if (!Double.isFinite(reach) || reach <= 0) return;
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(reach));
        Vec3 wall = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player)).getLocation();
        double nearest = eye.distanceToSqr(wall) + 1.0E-6D;
        Entity selected = null;
        for (Entity part : parts) {
            if (!(part instanceof DragonPartEntity region) || !part.isPickable()) continue;
            var box = dragon.getCollisionState().bounds(region.getPartIndex()).inflate(0.15D);
            Vec3 hit = box.contains(eye) ? eye : box.clip(eye, end).orElse(null);
            if (hit == null) continue;
            double distance = eye.distanceToSqr(hit);
            if (distance < nearest) { nearest = distance; selected = part; }
        }
        if (selected == null || !player.level().getWorldBorder().isWithinBounds(selected.blockPosition())) return;
        Integer last = LAST_ATTACK.get(player);
        if (last != null && last == player.tickCount) return;
        LAST_ATTACK.put(player, player.tickCount);
        player.resetLastActionTime();
        player.attack(selected);
    }
}

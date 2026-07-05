package com.leon.saintsdragons.server.ai.goals.draconianswarm;

import com.leon.saintsdragons.server.entity.draconianswarm.AbstractDraconianSwarmEntity;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class DraconianSwarmCoordinator {
    private static final int ATTACK_RESERVATION_TICKS = 80;
    private static final Map<UUID, AttackReservation> ATTACK_RESERVATIONS = new HashMap<>();

    private DraconianSwarmCoordinator() {
    }

    public static OrbitSlot getOrbitSlot(AbstractDraconianSwarmEntity swarm, LivingEntity target) {
        List<AbstractDraconianSwarmEntity> members = swarm.level().getEntitiesOfClass(
                AbstractDraconianSwarmEntity.class,
                target.getBoundingBox().inflate(40.0D),
                member -> member.isAlive() && member.getTarget() == target);
        members.sort(Comparator.comparingInt(AbstractDraconianSwarmEntity::getId));
        int index = Math.max(0, members.indexOf(swarm));
        return new OrbitSlot(index, Math.max(1, members.size()));
    }

    public static boolean tryClaimAttack(AbstractDraconianSwarmEntity swarm, LivingEntity target) {
        long now = swarm.level().getGameTime();
        AttackReservation reservation = ATTACK_RESERVATIONS.get(target.getUUID());
        if (reservation != null && reservation.expiresAt > now && !reservation.attacker.equals(swarm.getUUID())) {
            return false;
        }
        ATTACK_RESERVATIONS.put(target.getUUID(),
                new AttackReservation(swarm.getUUID(), now + ATTACK_RESERVATION_TICKS));
        return true;
    }

    public static boolean isAttackReservedByOther(AbstractDraconianSwarmEntity swarm, LivingEntity target) {
        AttackReservation reservation = ATTACK_RESERVATIONS.get(target.getUUID());
        return reservation != null
                && reservation.expiresAt > swarm.level().getGameTime()
                && !reservation.attacker.equals(swarm.getUUID());
    }

    public static void releaseAttack(AbstractDraconianSwarmEntity swarm) {
        ATTACK_RESERVATIONS.entrySet().removeIf(entry -> entry.getValue().attacker.equals(swarm.getUUID()));
    }

    public static boolean isPlayerFocusing(AbstractDraconianSwarmEntity swarm, LivingEntity target) {
        if (!(target instanceof Player player) || !player.hasLineOfSight(swarm)) {
            return false;
        }
        Vec3 toSwarm = swarm.getBoundingBox().getCenter().subtract(player.getEyePosition());
        if (toSwarm.lengthSqr() > 324.0D || toSwarm.lengthSqr() < 1.0E-4D) {
            return false;
        }
        return player.getViewVector(1.0F).dot(toSwarm.normalize()) >= 0.94D;
    }

    public record OrbitSlot(int index, int count) {
        public double angleOffset() {
            return index * MATH_TAU / count;
        }
    }

    private record AttackReservation(UUID attacker, long expiresAt) {
    }

    private static final double MATH_TAU = Math.PI * 2.0D;
}

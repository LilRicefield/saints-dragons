package com.leon.saintsdragons.server.ai.goals.draconianswarm;

import com.leon.saintsdragons.server.entity.draconianswarm.AbstractDraconianSwarmEntity;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.entity.LivingEntity;

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
        AttackReservation reservation;
        synchronized (ATTACK_RESERVATIONS) {
            reservation = ATTACK_RESERVATIONS.get(target.getUUID());
        }
        if (reservation != null && reservation.expiresAt > now && !reservation.attacker.equals(swarm.getUUID())) {
            return false;
        }
        synchronized (ATTACK_RESERVATIONS) {
            ATTACK_RESERVATIONS.put(target.getUUID(),
                    new AttackReservation(swarm.getUUID(), now + ATTACK_RESERVATION_TICKS));
        }
        return true;
    }

    public static boolean isAttackReservedByOther(AbstractDraconianSwarmEntity swarm, LivingEntity target) {
        AttackReservation reservation;
        synchronized (ATTACK_RESERVATIONS) {
            reservation = ATTACK_RESERVATIONS.get(target.getUUID());
        }
        return reservation != null
                && reservation.expiresAt > swarm.level().getGameTime()
                && !reservation.attacker.equals(swarm.getUUID());
    }

    public static void releaseAttack(AbstractDraconianSwarmEntity swarm) {
        UUID attacker = swarm.getUUID();
        synchronized (ATTACK_RESERVATIONS) {
            // iterate over a snapshot since death callbacks can reenter releaseAttack while damage is being applied and mutating the live HashMap iterator causes cme
            List<UUID> reservationsToRemove = new ArrayList<>();
            for (Map.Entry<UUID, AttackReservation> entry : ATTACK_RESERVATIONS.entrySet()) {
                if (entry.getValue().attacker.equals(attacker)) {
                    reservationsToRemove.add(entry.getKey());
                }
            }
            reservationsToRemove.forEach(ATTACK_RESERVATIONS::remove);
        }
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

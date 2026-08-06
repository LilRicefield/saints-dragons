package com.leon.saintsdragons.common.item.tools;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayDeque;
import java.util.Deque;

public final class DragonMeleeHitContext {
    private static final ThreadLocal<Deque<HitContext>> ACTIVE_HITS = ThreadLocal.withInitial(ArrayDeque::new);

    private DragonMeleeHitContext() {
    }

    public static void begin(Player attacker, Entity target) {
        ACTIVE_HITS.get().push(new HitContext(attacker, target));
    }

    public static void end(Player attacker, Entity target) {
        Deque<HitContext> hits = ACTIVE_HITS.get();
        HitContext active = hits.peek();
        if (active != null && active.attacker == attacker && active.target == target) {
            hits.pop();
        } else {
            hits.clear();
        }
        if (hits.isEmpty()) {
            ACTIVE_HITS.remove();
        }
    }

    public static float modifyDamage(LivingEntity target, DamageSource source, float damage) {
        HitContext active = activeHit(target, source);
        if (active == null || active.damageModified) {
            return damage;
        }

        active.damageModified = true;
        return DragonWeaponDamage.applyDirectMeleeMultiplier(active.attacker, target, damage);
    }

    public static void observeResult(LivingEntity target, DamageSource source, boolean hurt) {
        if (!hurt) {
            return;
        }

        HitContext active = activeHit(target, source);
        if (active == null || active.successHandled) {
            return;
        }

        active.successHandled = true;
        if (active.attacker instanceof ServerPlayer serverPlayer) {
            BloodTempestKatanaAbility.onSuccessfulKatanaHit(serverPlayer, target);
        }
    }

    private static HitContext activeHit(LivingEntity target, DamageSource source) {
        HitContext active = ACTIVE_HITS.get().peek();
        if (active == null
                || active.target != target
                || source.getEntity() != active.attacker
                || source.getDirectEntity() != active.attacker) {
            return null;
        }
        return active;
    }

    private static final class HitContext {
        private final Player attacker;
        private final Entity target;
        private boolean damageModified;
        private boolean successHandled;

        private HitContext(Player attacker, Entity target) {
            this.attacker = attacker;
            this.target = target;
        }
    }
}

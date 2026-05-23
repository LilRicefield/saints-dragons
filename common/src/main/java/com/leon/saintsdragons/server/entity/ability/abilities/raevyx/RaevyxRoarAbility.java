package com.leon.saintsdragons.server.entity.ability.abilities.raevyx;

import com.leon.saintsdragons.common.particle.raevyx.RaevyxLightningStormData;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.*;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.*;

public class RaevyxRoarAbility extends DragonAbility<Raevyx> {
    private static final float LIGHTNING_DAMAGE = 5.0F;

    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(AbilitySectionType.STARTUP, 6),
            new AbilitySectionDuration(AbilitySectionType.ACTIVE, 28),
            new AbilitySectionDuration(AbilitySectionType.RECOVERY, 12)
    };

    private int strikesLeft = 0;
    private int strikeCooldown = 0;
    private java.util.List<Integer> targetIds = java.util.Collections.emptyList();
    private int targetCursor = 0;

    public RaevyxRoarAbility(DragonAbilityType<Raevyx, RaevyxRoarAbility> type, Raevyx user) {
        super(type, user, TRACK, 40);
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) return;
        if (section.sectionType == AbilitySectionType.STARTUP) {
            getUser().getSoundHandler().playVocal("roar");
            selectLightningTargets();
            int count = targetIds.size();
            boolean isSupercharged = getUser().isSupercharged();
            
            if (count > 1) {
                strikesLeft = Math.min(6, Math.max(3, count * 2));
            } else {
                strikesLeft = 2 + getUser().getRandom().nextInt(2);
            }
            if (isSupercharged) {
                strikesLeft *= 2;
            }
            strikeCooldown = 0;
        }
    }

    @Override
    public void tickUsing() {
        var section = getCurrentSection();
        if (section == null) return;

        if (!getUser().level().isClientSide) {
            getUser().triggerScreenShake(1.0F);
        }

        if (section.sectionType == AbilitySectionType.ACTIVE && strikesLeft > 0 && !getUser().level().isClientSide) {
            if (strikeCooldown > 0) {
                strikeCooldown--;
            } else {
                spawnLightningStrike();
                strikesLeft--;
                strikeCooldown = 6 + getUser().getRandom().nextInt(6);
            }
        }
    }

    private void selectLightningTargets() {
        Raevyx dragon = getUser();
        LivingEntity rider = dragon.getControllingPassenger();

        Set<Integer> ids = new LinkedHashSet<>();
        if (dragon.level() instanceof ServerLevel server) {
            var box = dragon.getBoundingBox().inflate(24.0);
            var chasers = server.getEntitiesOfClass(Mob.class, box, m -> {
                var t = m.getTarget();
                return m.isAlive() && (t == dragon || (rider != null && t == rider));
            });
            chasers.sort(Comparator.comparingDouble(m -> m.distanceToSqr(dragon)));
            for (var m : chasers) ids.add(m.getId());
        }

        if (dragon.level() instanceof ServerLevel) {
            List<LivingEntity> recent = dragon.getRecentAggro();
            recent.sort(Comparator.comparingDouble(e -> e.distanceToSqr(dragon)));
            for (var le : recent) if (le != null && le.isAlive()) ids.add(le.getId());
            var ct = dragon.getTarget();
            if (ct != null && ct.isAlive()) ids.add(ct.getId());
        }

        this.targetIds = new ArrayList<>();
        int added = 0;
        for (Integer id : ids) {
            this.targetIds.add(id);
            if (++added >= 6) break;
        }
        this.targetCursor = 0;
    }

    private void spawnLightningStrike() {
        Raevyx dragon = getUser();
        if (!(dragon.level() instanceof ServerLevel server)) return;
        LivingEntity target = nextValidTarget(server);
        if (target == null) return;

        double ox = (dragon.getRandom().nextDouble() - 0.5) * 2.0;
        double oz = (dragon.getRandom().nextDouble() - 0.5) * 2.0;
        double x = target.getX() + ox;
        double z = target.getZ() + oz;
        double y = target.getY();
        var bolt = EntityType.LIGHTNING_BOLT.create(server);
        if (bolt != null) {
            bolt.moveTo(x, y, z);
            bolt.setVisualOnly(true);
            var owner = dragon.getOwner();
            if (owner instanceof ServerPlayer sp) {
                bolt.setCause(sp);
            }
            server.addFreshEntity(bolt);
        }
        damageTarget(target);
        spawnElectrocuteArcs(server, target);
        applyStun(target);
    }

    private void damageTarget(LivingEntity target) {
        Raevyx dragon = getUser();
        if (!dragon.canTarget(target)) {
            return;
        }

        DamageSource source = dragon.damageSources().lightningBolt();
        target.hurt(source, LIGHTNING_DAMAGE);
    }


    private void spawnElectrocuteArcs(ServerLevel server, LivingEntity target) {
        Raevyx dragon = getUser();
        Random rnd = new Random(dragon.getRandom().nextLong());
        boolean female = dragon.isFemale();
        Vec3 center = target.position().add(0, target.getBbHeight() * 0.5, 0);
        double radius = Math.max(target.getBoundingBox().getXsize(), target.getBoundingBox().getZsize()) * 0.6;
        int count = 6 + dragon.getRandom().nextInt(5);
        for (int i = 0; i < count; i++) {
            Vec3 a = randomUnit(rnd).scale(radius * (0.4 + rnd.nextDouble() * 0.6));
            Vec3 b = randomUnit(rnd).scale(radius * (0.4 + rnd.nextDouble() * 0.6));
            Vec3 from = center.add(a);
            Vec3 to = center.add(b);
            spawnRoarArc(server, from, to);
        }
    }

    private void spawnRoarArc(ServerLevel server, Vec3 from, Vec3 to) {
        Vec3 delta = to.subtract(from);
        int steps = Math.max(2, (int) (delta.length() * 4));
        Vec3 step = delta.scale(1.0 / steps);
        Vec3 pos = from;
        Vec3 dir = step.normalize();
        float size = 0.8f;
        for (int i = 0; i <= steps; i++) {
            server.sendParticles(new RaevyxLightningStormData(size),
                    pos.x, pos.y, pos.z,
                    1, dir.x, dir.y, dir.z, 0.0);
            pos = pos.add(step);
        }
    }

    private static Vec3 randomUnit(java.util.Random rnd) {
        double u = rnd.nextDouble();
        double v = rnd.nextDouble();
        double theta = 2 * Math.PI * u;
        double z = 2 * v - 1;
        double r = Math.sqrt(1 - z * z);
        return new Vec3(r * Math.cos(theta), z, r * Math.sin(theta));
    }

private static void applyStun(LivingEntity target) {
    final int durationTicks = 30;

        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, durationTicks, 5, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, durationTicks, 0, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, Math.min(durationTicks, 20), 0, false, true));
    }

    private LivingEntity nextValidTarget(ServerLevel server) {
        int n = targetIds != null ? targetIds.size() : 0;
        for (int i = 0; i < n; i++) {
            int idx = (targetCursor + i) % n;
            Entity e = server.getEntity(targetIds.get(idx));
            if (e instanceof LivingEntity le && le.isAlive() && getUser().canTarget(le)) {
                targetCursor = (idx + 1) % n;
                return le;
            }
        }
        return null;
    }
}

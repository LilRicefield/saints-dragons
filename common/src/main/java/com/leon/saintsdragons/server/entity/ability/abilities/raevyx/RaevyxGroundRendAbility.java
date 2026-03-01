package com.leon.saintsdragons.server.entity.ability.abilities.raevyx;

import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType;

public class RaevyxGroundRendAbility extends DragonAbility<Raevyx> {
    private static final int STARTUP_TICKS = 20;
    private static final int ACTIVE_TICKS = 40;
    private static final int RECOVERY_TICKS = 8;
    private static final int COOLDOWN_TICKS = 32;
    private static final double FORWARD_SPEED = 2.0D;
    private static final double RECOVERY_DAMPING = 0.84D;
    private static final float HIT_DAMAGE = 5.0F;
    private static final double HIT_KNOCKBACK = 0.55D;
    private static final int HIT_COOLDOWN_TICKS = 5;

    private final Map<Integer, Integer> hitCooldowns = new HashMap<>();

    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(AbilitySectionType.STARTUP, STARTUP_TICKS),
            new AbilitySectionDuration(AbilitySectionType.ACTIVE, ACTIVE_TICKS),
            new AbilitySectionDuration(AbilitySectionType.RECOVERY, RECOVERY_TICKS)
    };

    public RaevyxGroundRendAbility(DragonAbilityType<Raevyx, RaevyxGroundRendAbility> type, Raevyx user) {
        super(type, user, TRACK, COOLDOWN_TICKS);
    }

    @Override
    public boolean tryAbility() {
        Raevyx wyvern = getUser();
        return !wyvern.isFlying()
                && !wyvern.isTakeoff()
                && !wyvern.isLanding()
                && !wyvern.isHovering()
                && !wyvern.isInWaterOrBubble()
                && wyvern.onGround();
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) {
            return;
        }
        if (section.sectionType == AbilitySectionType.STARTUP) {
            // Play GroundRend animation immediately and delay movement until ACTIVE section.
            getUser().triggerAnim("action", "ground_rend");
            if (!getUser().level().isClientSide) {
                getUser().getSoundHandler().playMovingEntitySound(
                        ModSounds.RAEVYX_GROUND_REND.get(),
                        1.4f,
                        1.0f,
                        100
                );
            }
            getUser().setAccelerating(false);
            // Enable ground rend state to bypass normal travel logic
            getUser().setGroundRending(true);
        }
    }
    @Override
    public void tickUsing() {
        hitCooldowns.entrySet().removeIf(entry -> {
            int next = entry.getValue() - 1;
            if (next <= 0) {
                return true;
            }
            entry.setValue(next);
            return false;
        });

        DragonAbilitySection section = getCurrentSection();
        if (section == null) {
            return;
        }

        Raevyx wyvern = getUser();
        if (wyvern.isFlying() || wyvern.isInWaterOrBubble()) {
            interrupt();
            return;
        }

        if (section.sectionType == AbilitySectionType.RECOVERY) {
            Vec3 current = wyvern.getDeltaMovement();
            wyvern.setGroundRendVelocity(new Vec3(current.x * RECOVERY_DAMPING, current.y, current.z * RECOVERY_DAMPING));
            return;
        }

        if (section.sectionType == AbilitySectionType.ACTIVE) {
            // GroundRend is rider-input driven now: no forward input = no movement.
            float riderForward = Math.max(0.0F, getUser().getRiderForwardInput());
            if (riderForward <= 0.01F) {
                wyvern.setGroundRendVelocity(Vec3.ZERO);
                return;
            }

            Vec3 look = wyvern.getLookAngle();
            Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
            if (horizontal.lengthSqr() < 1.0E-6) {
                return;
            }

            // Set velocity - entity will apply it in handleGroundRendMovement().
            // Scale by rider forward intent so pressing less than full forward still works.
            Vec3 target = horizontal.normalize().scale(FORWARD_SPEED * riderForward);
            Vec3 current = wyvern.getDeltaMovement();
            wyvern.setGroundRendVelocity(new Vec3(target.x, current.y, target.z));
            wyvern.getNavigation().stop();
            applyGroundRendHits(wyvern, horizontal.normalize());
        }
    }

    @Override
    public void end() {
        getUser().setGroundRending(false);
        getUser().clearRiderControlLock();
        hitCooldowns.clear();
        super.end();
    }

    @Override
    public void interrupt() {
        getUser().setGroundRending(false);
        getUser().clearRiderControlLock();
        hitCooldowns.clear();
        super.interrupt();
    }

    private void applyGroundRendHits(Raevyx wyvern, Vec3 forwardDir) {
        if (wyvern.level().isClientSide) {
            return;
        }

        Vec3 mouthPos = wyvern.getMouthPosition();
        AABB dragonBox = wyvern.getBoundingBox().inflate(0.9D);
        AABB mouthBox = new AABB(mouthPos, mouthPos).inflate(1.35D);
        AABB combinedBox = dragonBox.minmax(mouthBox);

        for (LivingEntity target : wyvern.level().getEntitiesOfClass(
                LivingEntity.class,
                combinedBox,
                entity -> entity != wyvern
                        && entity != wyvern.getControllingPassenger()
                        && entity.isAlive()
                        && entity.attackable()
                        && !wyvern.isAlly(entity))) {
            int entityId = target.getId();
            if (hitCooldowns.containsKey(entityId)) {
                continue;
            }

            target.hurt(wyvern.damageSources().mobAttack(wyvern), HIT_DAMAGE);
            wyvern.noteAggroFrom(target);
            target.knockback((float) HIT_KNOCKBACK, -forwardDir.x, -forwardDir.z);
            hitCooldowns.put(entityId, HIT_COOLDOWN_TICKS);
        }
    }
}

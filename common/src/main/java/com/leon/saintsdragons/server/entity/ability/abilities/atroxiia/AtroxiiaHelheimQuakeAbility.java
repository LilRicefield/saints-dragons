package com.leon.saintsdragons.server.entity.ability.abilities.atroxiia;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfig;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.common.registry.ModParticles;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.atroxiia.Atroxiia;
import com.leon.saintsdragons.server.entity.dragons.atroxiia.handlers.AtroxiiaAnimationHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionInfinite;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;

public class AtroxiiaHelheimQuakeAbility extends DragonAbility<Atroxiia> {
    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionInfinite(ACTIVE)
    };

    private static final int QUAKE_ONE_TICKS = (int) Math.round(1.7083D * 20.0D);
    private static final int QUAKE_TWO_TICKS = (int) Math.round(0.85 * 20.0D);
    private static final int CHAIN_TICK = (int) Math.round(1.1D * 20.0D);
    private static final int QUAKE_ONE_IMPACT_TICK = (int) Math.round(0.90D * 20.0D);
    private static final int QUAKE_TWO_IMPACT_TICK = (int) Math.round(0.70D * 20.0D);
    private static final int QUAKE_TWO_TAIL_FLASH_TICK = (int) Math.round(0.33D * 20.0D);
    private static final int COOLDOWN_TICKS = 50;
    private static final float DEFAULT_QUAKE_DAMAGE = 25.0F;
    private static final double QUAKE_RADIUS = 20.0D;
    private static final double QUAKE_VERTICAL_RADIUS = 6.0D;
    private static final double DEFAULT_QUAKE_ONE_KNOCKBACK = 0.75D;
    private static final double DEFAULT_QUAKE_TWO_KNOCKBACK = 1.8D;
    private static final double QUAKE_ONE_LIFT = 0.2D;
    private static final double QUAKE_TWO_LIFT = 0.5D;
    private static final int DEFAULT_QUAKE_STUN_TICKS = 5 * 20;
    private static final float QUAKE_ONE_SCREEN_SHAKE = 0.55F;
    private static final int QUAKE_ONE_SCREEN_SHAKE_TICKS = 6;
    private static final float QUAKE_TWO_SCREEN_SHAKE = 1.1F;
    private static final int QUAKE_TWO_SCREEN_SHAKE_TICKS = 10;
    private static final int FROST_WALKER_LEVEL = 10;

    private Phase phase = Phase.QUAKE_ONE;
    private int phaseTicks;
    private boolean releaseRequested;
    private boolean chainRequested;
    private boolean appliedQuakeOneImpact;
    private boolean appliedQuakeTwoImpact;

    private enum Phase {
        QUAKE_ONE,
        QUAKE_TWO
    }

    public AtroxiiaHelheimQuakeAbility(DragonAbilityType<Atroxiia, AtroxiiaHelheimQuakeAbility> type,
                                       Atroxiia user) {
        super(type, user, TRACK, COOLDOWN_TICKS);
    }

    @Override
    public boolean tryAbility() {
        return getUser().canUseGroundCombatAbility();
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null || section.sectionType != ACTIVE) {
            return;
        }

        phase = Phase.QUAKE_ONE;
        phaseTicks = 0;
        releaseRequested = false;
        chainRequested = false;
        appliedQuakeOneImpact = false;
        appliedQuakeTwoImpact = false;
        getUser().lockRiderControls(3);
        getUser().triggerAnim(AtroxiiaAnimationHandler.MOVEMENT_CONTROLLER, "helheim_quake1");
        getUser().getSoundHandler().playMovingEntitySound(
                ModSounds.ATROXIIA_HELHEIM_QUAKE_1.get(), 1.5F, 1.0F, 90
        );
    }

    @Override
    public void tickUsing() {
        getUser().lockRiderControls(3);
        phaseTicks++;

        if (phase == Phase.QUAKE_ONE) {
            if (!appliedQuakeOneImpact && phaseTicks >= QUAKE_ONE_IMPACT_TICK) {
                appliedQuakeOneImpact = true;
                getUser().triggerScreenShake(QUAKE_ONE_SCREEN_SHAKE, QUAKE_ONE_SCREEN_SHAKE_TICKS);
                AtroxiiaFrostWalker.freezeNearbyWater(getUser(), FROST_WALKER_LEVEL);
                performQuakeImpact();
            }
            if ((!releaseRequested || chainRequested) && phaseTicks >= CHAIN_TICK) {
                beginQuakeTwo();
            } else if (releaseRequested && !chainRequested && phaseTicks >= QUAKE_ONE_TICKS) {
                end();
            }
        } else {
            if (phaseTicks == QUAKE_TWO_TAIL_FLASH_TICK && getUser().level() instanceof ServerLevel server) {
                server.broadcastEntityEvent(getUser(), Atroxiia.QUAKE_TAIL_FLASH_EVENT);
            }
            if (!appliedQuakeTwoImpact && phaseTicks >= QUAKE_TWO_IMPACT_TICK) {
                appliedQuakeTwoImpact = true;
                getUser().triggerScreenShake(QUAKE_TWO_SCREEN_SHAKE, QUAKE_TWO_SCREEN_SHAKE_TICKS);
                performQuakeImpact();
            }
            if (phaseTicks >= QUAKE_TWO_TICKS) {
                end();
            }
        }
    }

    public void requestRelease() {
        releaseRequested = true;
    }

    public void requestChain() {
        if (phase == Phase.QUAKE_ONE) {
            chainRequested = true;
        }
    }

    private void beginQuakeTwo() {
        phase = Phase.QUAKE_TWO;
        phaseTicks = 0;
        getUser().triggerAnim(AtroxiiaAnimationHandler.MOVEMENT_CONTROLLER, "helheim_quake2");
        getUser().getSoundHandler().playMovingEntitySound(
                ModSounds.ATROXIIA_HELHEIM_QUAKE_2.get(), 1.5F, 1.0F, 70
        );
    }

    private void performQuakeImpact() {
        Atroxiia dragon = getUser();
        if (dragon.level() instanceof ServerLevel server) {
            Vec3 origin = new Vec3(dragon.getX(), dragon.getBoundingBox().minY + 0.12, dragon.getZ());
            for (var viewer : server.players()) {
                if (viewer.distanceToSqr(origin) <= 256.0 * 256.0) {
                    server.sendParticles(viewer, ModParticles.ATROXIIA_ICE_BURST.get(), true,
                            origin.x, origin.y, origin.z, 0, 0, 0, 0, 0);
                }
            }
        }
        applyQuakeDamage();
    }

    private void applyQuakeDamage() {
        Atroxiia dragon = getUser();
        if (!(dragon.level() instanceof ServerLevel server)) {
            return;
        }

        Entity rider = dragon.getControllingPassenger();
        AABB area = dragon.getBoundingBox().inflate(
                QUAKE_RADIUS, QUAKE_VERTICAL_RADIUS, QUAKE_RADIUS
        );
        List<LivingEntity> targets = server.getEntitiesOfClass(
                LivingEntity.class,
                area,
                target -> target != dragon
                        && target != rider
                        && target.isAlive()
                        && target.attackable()
                        && !dragon.isAlly(target)
        );

        DragonAttributeConfig config = DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.ATROXIIA_ID);
        float damage = (float) config.abilityDamage("helheim_quake", DEFAULT_QUAKE_DAMAGE);
        damage *= dragon.getHungerMeleeDamageMultiplier();
        boolean firstQuake = phase == Phase.QUAKE_ONE;
        double knockback = firstQuake
                ? config.abilityKnockback("helheim_quake", DEFAULT_QUAKE_ONE_KNOCKBACK)
                : config.abilitySecondaryKnockback("helheim_quake", DEFAULT_QUAKE_TWO_KNOCKBACK);
        int stunTicks = config.abilityStunDurationTicks("helheim_quake", DEFAULT_QUAKE_STUN_TICKS);
        double lift = firstQuake ? QUAKE_ONE_LIFT : QUAKE_TWO_LIFT;
        double radiusSqr = QUAKE_RADIUS * QUAKE_RADIUS;

        for (LivingEntity target : targets) {
            if (target.distanceToSqr(dragon) > radiusSqr
                    || !target.hurt(server.damageSources().mobAttack(dragon), damage)) {
                continue;
            }

            AtroxiiaFrostImpact.apply(dragon, target, stunTicks, false);
            Vec3 direction = target.position().subtract(dragon.position());
            direction = new Vec3(direction.x, 0.0D, direction.z);
            if (direction.lengthSqr() < 1.0E-4D) {
                Vec3 look = dragon.getLookAngle();
                direction = new Vec3(look.x, 0.0D, look.z);
            }
            if (direction.lengthSqr() < 1.0E-4D) {
                direction = Vec3.directionFromRotation(0.0F, dragon.getYRot());
            }
            Vec3 push = direction.normalize().scale(knockback);
            target.push(push.x, lift, push.z);
            target.hurtMarked = true;
        }
    }

}

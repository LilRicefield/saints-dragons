package com.leon.saintsdragons.server.entity.ability.abilities.ignivorus;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.entity.effect.ignivorus.IgnivorusMagmaBlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.STARTUP;

/**
 * Phase 2 fireball ability for Ignivorus.
 * Press R to fire a single magma block projectile.
 */
public class IgnivorusFireballAbility extends DragonAbility<Ignivorus> {
    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(STARTUP, 31)
    };

    private static final int COOLDOWN_TICKS = 15;
    private static final int FIRE_AT_TICK = 15;
    private static final int MAGMA_LIFETIME_TICKS = 200;
    private static final float FIREBALL_SCALE = 4.0F;
    private static final double FIREBALL_SPEED = 3.5D;
    private static final double IMPACT_RADIUS = 8.0D;
    private static final float DEFAULT_IMPACT_DAMAGE = 70.0F;

    private boolean hasFired;

    public IgnivorusFireballAbility(DragonAbilityType<Ignivorus, IgnivorusFireballAbility> type,
                                    Ignivorus user) {
        super(type, user, TRACK, COOLDOWN_TICKS);
    }

    @Override
    public boolean tryAbility() {
        Ignivorus dragon = getUser();
        return dragon.isPhase2Active() && dragon.getControllingPassenger() != null;
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) {
            return;
        }
        if (section.sectionType == STARTUP) {
            getUser().triggerAnim("action", "fireball_shoots");
            hasFired = false;
        }
    }

    @Override
    protected boolean canContinueUsing() {
        Ignivorus dragon = getUser();
        return dragon.isAlive() && !dragon.isRemoved() && !dragon.isInWaterOrBubble();
    }

    @Override
    public void tickUsing() {
        if (!hasFired && getTicksInUse() >= FIRE_AT_TICK) {
            fireFireball();
            hasFired = true;
        }
    }

    private void fireFireball() {
        Ignivorus dragon = getUser();
        if (!(dragon.level() instanceof ServerLevel server)) {
            return;
        }

        Vec3 direction = getAimDirection(dragon);
        Vec3 spawnPos = getMouthPosition(dragon);

        float damage = resolveImpactDamage();
        IgnivorusMagmaBlockEntity fireball = new IgnivorusMagmaBlockEntity(server, spawnPos, dragon,
                IMPACT_RADIUS, damage, MAGMA_LIFETIME_TICKS);
        fireball.setNoGravity(true);
        fireball.setDeltaMovement(direction.scale(FIREBALL_SPEED));
        fireball.setVisualScale(FIREBALL_SCALE);
        fireball.hasImpulse = true;
        server.addFreshEntity(fireball);
    }

    private Vec3 getMouthPosition(Ignivorus dragon) {
        Vec3 mouth = dragon.getFireBreathStartAnchor(1.0f);
        return mouth != null ? mouth : dragon.getEyePosition();
    }

    private Vec3 getAimDirection(Ignivorus dragon) {
        Entity rider = dragon.getControllingPassenger();
        if (rider instanceof Player player) {
            Vec3 view = player.getViewVector(1.0f);
            if (view.lengthSqr() > 1.0E-6) {
                return view.normalize();
            }
        }
        Vec3 look = dragon.getLookAngle();
        return look.lengthSqr() > 1.0E-6 ? look.normalize() : new Vec3(0.0D, 0.0D, 1.0D);
    }

    private float resolveImpactDamage() {
        return (float) DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.IGNIVORUS_ID)
                .abilityDamage("fireball", DEFAULT_IMPACT_DAMAGE);
    }
}

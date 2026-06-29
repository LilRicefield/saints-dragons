package com.leon.saintsdragons.server.entity.ability.abilities.raevyx;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import com.leon.saintsdragons.server.entity.dragons.raevyx.handlers.RaevyxAnimationHandler;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.ability.DragonMeleeGeometry;
import com.leon.saintsdragons.server.entity.ability.debug.DragonAbilityDebug;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.*;

public class RaevyxBiteAbility extends DragonAbility<Raevyx> {
    private static final float BASE_DAMAGE = 15.0f;
    private static final double RANGE = 6.0;
    private static final double HITBOX_HALF_WIDTH = 3.75;
    private static final double HITBOX_HALF_HEIGHT = 3.4;
    private static final double HITBOX_FORWARD_OFFSET = 2.0;
    private static final int DEBUG_COLOR = 0x33D1FF;
    private static final int DEBUG_TICKS = 20;

    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(AbilitySectionType.STARTUP, 3),
            new AbilitySectionDuration(AbilitySectionType.ACTIVE, 2),
            new AbilitySectionDuration(AbilitySectionType.RECOVERY, 3)
    };

    private boolean didHitThisActive = false;

    public RaevyxBiteAbility(DragonAbilityType<Raevyx, RaevyxBiteAbility> type, Raevyx user) {
        super(type, user, TRACK, 3);
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) return;
        if (section.sectionType == AbilitySectionType.STARTUP) {
            getUser().triggerAnim(RaevyxAnimationHandler.FAST_ACTION_CONTROLLER, "lightning_bite");
            if (!getUser().level().isClientSide) {
                float pitch = 0.95f + getUser().getRandom().nextFloat() * 0.10f;
                getUser().getSoundHandler().playMovingEntitySound(ModSounds.RAEVYX_BITE.get(), 1.0f, pitch, 50);
            }
            didHitThisActive = false;
        }
    }

    @Override
    public void tickUsing() {
        DragonAbilitySection section = getCurrentSection();
        if (section == null) return;


        if (section.sectionType == AbilitySectionType.ACTIVE && !didHitThisActive) {
            LivingEntity primary = findPrimaryTarget();
            if (primary != null) {
                bitePrimary(primary);
                RaevyxChainLightningAbility.chainFromBite(getUser(), primary);
            }
            didHitThisActive = true;
        }
    }

    private LivingEntity findPrimaryTarget() {
        Raevyx wyvern = getUser();
        boolean ridden = wyvern.getControllingPassenger() != null;

        if (!ridden) {
            LivingEntity target = wyvern.getTarget();
            if (DragonMeleeGeometry.isDirectAiTargetValid(wyvern, target)) {
                sendDebugBox(wyvern);
                return target;
            }
        }

        List<LivingEntity> candidates = DragonMeleeGeometry.findBodySweepTargets(
                wyvern,
                RANGE,
                HITBOX_HALF_WIDTH,
                HITBOX_HALF_HEIGHT,
                HITBOX_FORWARD_OFFSET,
                entity -> !isAllied(wyvern, entity)
        );
        sendDebugBox(wyvern);

        return candidates.isEmpty() ? null : candidates.get(0);
    }

    private void sendDebugBox(Raevyx wyvern) {
        if (wyvern.level().isClientSide) {
            return;
        }
        DragonMeleeGeometry.ForwardAttack attack = DragonMeleeGeometry.bodyForwardAttack(wyvern).offset(HITBOX_FORWARD_OFFSET);
        DragonAbilityDebug.sendBox(wyvern, attack.sweep(RANGE, HITBOX_HALF_WIDTH, HITBOX_HALF_HEIGHT), DEBUG_COLOR, DEBUG_TICKS);
    }

    private void bitePrimary(LivingEntity primary) {
        Raevyx wyvern = getUser();
        DamageSource src = wyvern.level().damageSources().mobAttack(wyvern);
        float mult = wyvern.getDamageMultiplier() * wyvern.getHungerMeleeDamageMultiplier();
        primary.hurt(src, resolveBiteDamage() * mult);
        wyvern.noteAggroFrom(primary);
    }

    private float resolveBiteDamage() {
        return (float) DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.RAEVYX_ID)
                .abilityDamage("bite", BASE_DAMAGE);
    }

    private boolean isAllied(Raevyx wyvern, Entity other) {
        return wyvern.isAlly(other);
    }
}

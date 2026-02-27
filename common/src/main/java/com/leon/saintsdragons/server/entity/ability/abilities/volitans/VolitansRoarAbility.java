package com.leon.saintsdragons.server.entity.ability.abilities.volitans;

import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.RECOVERY;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.STARTUP;

public class VolitansRoarAbility extends DragonAbility<Volitans> {
    private static final int STARTUP_TICKS = 8;
    private static final int ACTIVE_TICKS = 46;
    private static final int RECOVERY_TICKS = 16;
    private static final int ROAR_ANIM_TOTAL_TICKS = STARTUP_TICKS + ACTIVE_TICKS + RECOVERY_TICKS; // 70 ticks (3.5s)
    private static final int SOUND_DURATION_TICKS = 100; // 5s
    private static final int ROAR_EFFECT_START_TICK = 23;
    private static final int ROAR_EFFECT_DURATION_TICKS = 40;
    private static final float ROAR_DAMAGE = 10.0F;
    private static final float ROAR_SHAKE_INTENSITY = 0.85F;
    private static final int POISON_DURATION_TICKS = 9000;
    private static final int POISON_AMPLIFIER = 3;
    private static final double HIT_RADIUS = 24.0D;

    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(STARTUP, STARTUP_TICKS),
            new AbilitySectionDuration(DragonAbilitySection.AbilitySectionType.ACTIVE, ACTIVE_TICKS),
            new AbilitySectionDuration(RECOVERY, RECOVERY_TICKS)
    };

    private final Set<Integer> hitTargetIds = new HashSet<>();
    private boolean shakeTriggered;

    public VolitansRoarAbility(DragonAbilityType<Volitans, VolitansRoarAbility> type, Volitans user) {
        super(type, user, TRACK, 30);
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) {
            return;
        }
        if (section.sectionType == STARTUP) {
            Volitans dragon = getUser();
            dragon.triggerAnim("actions", "roar");
            dragon.lockRiderControls(ROAR_ANIM_TOTAL_TICKS);
            hitTargetIds.clear();
            shakeTriggered = false;

            if (!dragon.level().isClientSide) {
                dragon.getSoundHandler().playMovingEntitySound(ModSounds.VOLITANS_ROAR.get(), 1.6f, 1.0f, SOUND_DURATION_TICKS);
            }
        }
    }

    @Override
    public void tickUsing() {
        DragonAbilitySection section = getCurrentSection();
        if (section == null || getUser().level().isClientSide) {
            return;
        }

        int ticksInUse = getTicksInUse();
        boolean inEffectWindow = ticksInUse >= ROAR_EFFECT_START_TICK
                && ticksInUse < ROAR_EFFECT_START_TICK + ROAR_EFFECT_DURATION_TICKS;
        if (inEffectWindow) {
            Volitans dragon = getUser();
            if (!shakeTriggered) {
                dragon.triggerScreenShake(ROAR_SHAKE_INTENSITY, ROAR_EFFECT_DURATION_TICKS);
                shakeTriggered = true;
            }
            applyRoarPulse();
        }
    }

    private void applyRoarPulse() {
        Volitans dragon = getUser();
        Vec3 origin = dragon.position();
        AABB hitBox = new AABB(
                origin.x - HIT_RADIUS,
                origin.y - HIT_RADIUS,
                origin.z - HIT_RADIUS,
                origin.x + HIT_RADIUS,
                origin.y + HIT_RADIUS,
                origin.z + HIT_RADIUS
        );
        DamageSource source = dragon.level().damageSources().mobAttack(dragon);

        List<LivingEntity> targets = dragon.level().getEntitiesOfClass(LivingEntity.class, hitBox,
                entity -> entity != dragon
                        && entity.isAlive()
                        && entity.attackable()
                        && !dragon.isAlly(entity)
                        && entity.distanceToSqr(dragon) <= (HIT_RADIUS * HIT_RADIUS));

        for (LivingEntity target : targets) {
            if (!hitTargetIds.add(target.getId())) {
                continue;
            }
            target.hurt(source, ROAR_DAMAGE);
            target.addEffect(new MobEffectInstance(MobEffects.POISON, POISON_DURATION_TICKS, POISON_AMPLIFIER));
        }
    }
}

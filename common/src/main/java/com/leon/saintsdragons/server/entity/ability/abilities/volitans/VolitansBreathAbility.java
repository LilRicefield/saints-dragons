package com.leon.saintsdragons.server.entity.ability.abilities.volitans;

import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import com.leon.saintsdragons.server.entity.effect.volitans.VolitansWaterBreathEntity;
import net.minecraft.world.phys.Vec3;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionInfinite;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.STARTUP;

public class VolitansBreathAbility extends DragonAbility<Volitans> {
    // animation.volitans.breath_start = 0.8333s -> ~16.666 ticks, rounded to 17
    private static final int STARTUP_TICKS = 17;
    private static final int COOLDOWN_TICKS = 20;

    private static final float WATER_PROJECTILE_DAMAGE = 1.8F;
    private static final float WATER_PROJECTILE_PUSH = 0.14F;
    private static final float POISON_PROJECTILE_DAMAGE = 1.4F;
    private static final float POISON_PROJECTILE_PUSH = 0.0F;
    private static final float PROJECTILE_SPEED = 2.35F;
    private static final int PROJECTILE_LIFETIME = 28;

    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(STARTUP, STARTUP_TICKS),
            new AbilitySectionInfinite(ACTIVE)
    };

    public VolitansBreathAbility(DragonAbilityType<Volitans, VolitansBreathAbility> type, Volitans user) {
        super(type, user, TRACK, COOLDOWN_TICKS);
    }

    @Override
    protected void endSection(DragonAbilitySection section) {
        if (section != null && section.sectionType == ACTIVE) {
            getUser().triggerAnim("actions", "breath_end");
            getUser().setBreathMode(0); // always revert to water when breath stops
        }
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) {
            return;
        }
        Volitans dragon = getUser();
        if (section.sectionType == STARTUP) {
            dragon.triggerAnim("actions", "breath_start");
            return;
        }
        if (section.sectionType == ACTIVE) {
            dragon.triggerAnim("actions", "breathing");
        }
    }

    @Override
    public void tickUsing() {
        DragonAbilitySection section = getCurrentSection();
        Volitans dragon = getUser();
        if (section == null || section.sectionType != ACTIVE || dragon.level().isClientSide) {
            return;
        }

        // Emit every other tick so the stream is readable and not too spammy.
        if ((dragon.tickCount & 1) != 0) {
            return;
        }

        Vec3 origin = dragon.getMouthPosition();
        Vec3 look = dragon.getLookAngle();
        if (look.lengthSqr() < 1.0E-6) {
            return;
        }
        Vec3 direction = look.normalize();
        boolean poisonMode = dragon.isPoisonBreathMode();
        float damage = poisonMode ? POISON_PROJECTILE_DAMAGE : WATER_PROJECTILE_DAMAGE;
        float push = poisonMode ? POISON_PROJECTILE_PUSH : WATER_PROJECTILE_PUSH;

        for (int i = 0; i < 8; i++) {
            double spread = 0.20D;
            Vec3 randomized = direction.add(
                    (dragon.getRandom().nextDouble() - 0.5D) * spread,
                    (dragon.getRandom().nextDouble() - 0.5D) * spread,
                    (dragon.getRandom().nextDouble() - 0.5D) * spread
            ).normalize();

            Vec3 spawn = origin.add(randomized.scale(0.5D + i * 0.22D));
            VolitansWaterBreathEntity projectile = new VolitansWaterBreathEntity(
                    dragon.level(),
                    spawn,
                    randomized.scale(PROJECTILE_SPEED),
                    dragon,
                    damage,
                    push,
                    PROJECTILE_LIFETIME,
                    poisonMode
            );
            dragon.level().addFreshEntity(projectile);
        }
    }

    @Override
    protected boolean canContinueUsing() {
        Volitans dragon = getUser();
        return dragon.isAlive() && !dragon.isRemoved();
    }

    @Override
    public void interrupt() {
        getUser().triggerAnim("actions", "breath_end");
        getUser().setBreathMode(0); // always revert to water when breath is interrupted/released
        super.interrupt();
    }
}

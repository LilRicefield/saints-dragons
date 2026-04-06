package com.leon.saintsdragons.server.entity.ability.abilities.volitans;

import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.volitans.Volitans;
import com.leon.saintsdragons.server.entity.effect.volitans.VolitansWaterBreathEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.STARTUP;

public class VolitansBreathAbility extends DragonAbility<Volitans> {
    // animation.volitans.breath_start = 0.8333s -> ~16.666 ticks, rounded to 17
    private static final int STARTUP_TICKS = 17;
    private static final int ACTIVE_TICKS_MAX = 20 * 12; // 12s
    private static final int COOLDOWN_TICKS = 20;
    private static final float BREATH_DRAIN_PER_TICK = 1.0F / ACTIVE_TICKS_MAX;
    private static final int BREATH_START_SOUND_TICKS = 20; // 1.0s
    private static final int BREATH_END_SOUND_TICKS = 50;   // 2.5s
    private static final float BREATH_VOLUME = 2.0F;

    private static final float WATER_PROJECTILE_DAMAGE = 1.8F;
    private static final float WATER_PROJECTILE_PUSH = 0.14F;
    private static final float POISON_PROJECTILE_DAMAGE = 1.4F;
    private static final float POISON_PROJECTILE_PUSH = 0.0F;
    private static final float PROJECTILE_SPEED = 1.60F;
    private static final int PROJECTILE_LIFETIME = 28;

    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(STARTUP, STARTUP_TICKS),
            new AbilitySectionDuration(ACTIVE, ACTIVE_TICKS_MAX)
    };

    public VolitansBreathAbility(DragonAbilityType<Volitans, VolitansBreathAbility> type, Volitans user) {
        super(type, user, TRACK, COOLDOWN_TICKS);
    }

    @Override
    public boolean canUse() {
        return super.canUse() && getUser().canUseCurrentBreathMode();
    }

    @Override
    protected void endSection(DragonAbilitySection section) {
        if (section != null && section.sectionType == ACTIVE) {
            getUser().triggerAnim("actions", "breath_end");
            getUser().setBreathing(false);
            playBreathEndSound();
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
            if (!dragon.canUseCurrentBreathMode()) {
                interrupt();
                return;
            }
            dragon.triggerAnim("actions", "breath_start");
            dragon.setBreathing(false);
            playBreathStartSound();
            return;
        }
        if (section.sectionType == ACTIVE) {
            dragon.triggerAnim("actions", "breathing");
            dragon.setBreathing(true);
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

        updateAiBreathTracking(dragon);
        Vec3 origin = dragon.getMouthPosition();
        Vec3 direction = getBreathDirection(dragon, origin);
        if (direction.lengthSqr() < 1.0E-6) {
            return;
        }

        float energy = Math.max(0.0F, dragon.getCurrentBreathEnergy() - BREATH_DRAIN_PER_TICK);
        dragon.setCurrentBreathEnergy(energy);
        if (!dragon.hasCurrentBreathEnergy()) {
            dragon.setCurrentBreathDepleted(true);
            interrupt();
            return;
        }
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

    private Vec3 getBreathDirection(Volitans dragon, Vec3 origin) {
        Entity rider = dragon.getControllingPassenger();
        if (rider instanceof Player player) {
            Vec3 view = player.getViewVector(1.0F);
            if (view.lengthSqr() > 1.0E-6) {
                return view.normalize();
            }
        }

        LivingEntity target = dragon.getTarget();
        if (dragon.isTargetValid(target)) {
            Vec3 aimPoint = target.getEyePosition().add(target.getDeltaMovement().scale(0.35D));
            Vec3 targetDir = aimPoint.subtract(origin);
            if (targetDir.lengthSqr() > 1.0E-6) {
                return targetDir.normalize();
            }
        }

        Vec3 look = dragon.getLookAngle();
        if (look.lengthSqr() > 1.0E-6) {
            return look.normalize();
        }
        return Vec3.ZERO;
    }

    private void updateAiBreathTracking(Volitans dragon) {
        if (dragon.getControllingPassenger() instanceof Player) {
            return;
        }
        LivingEntity target = dragon.getTarget();
        if (!dragon.isTargetValid(target)) {
            return;
        }

        Vec3 origin = dragon.getMouthPosition();
        Vec3 aimPoint = target.getEyePosition().add(target.getDeltaMovement().scale(0.35D));
        Vec3 toTarget = aimPoint.subtract(origin);
        if (toTarget.lengthSqr() <= 1.0E-6) {
            return;
        }

        dragon.getLookControl().setLookAt(target, 30.0F, 30.0F);

        Vec3 horizontal = new Vec3(toTarget.x, 0.0D, toTarget.z);
        if (horizontal.lengthSqr() > 1.0E-6D) {
            float targetYaw = (float) (Mth.atan2(horizontal.z, horizontal.x) * (180.0D / Math.PI)) - 90.0F;
            float newYaw = Mth.approachDegrees(dragon.getYRot(), targetYaw, 8.0F);
            dragon.setYRot(newYaw);
            dragon.yBodyRot = Mth.approachDegrees(dragon.yBodyRot, targetYaw, 10.0F);
            dragon.yHeadRot = Mth.approachDegrees(dragon.yHeadRot, targetYaw, 14.0F);
        }
    }

    @Override
    public void interrupt() {
        getUser().triggerAnim("actions", "breath_end");
        getUser().setBreathing(false);
        playBreathEndSound();
        getUser().setBreathMode(0); // always revert to water when breath is interrupted/released
        super.interrupt();
    }

    private void playBreathStartSound() {
        Volitans dragon = getUser();
        if (dragon.level().isClientSide) {
            return;
        }
        float pitch = 0.96f + dragon.getRandom().nextFloat() * 0.08f;
        dragon.getSoundHandler().playMovingEntitySound(
                ModSounds.VOLITANS_BREATH_START.get(),
                BREATH_VOLUME,
                pitch,
                BREATH_START_SOUND_TICKS
        );
    }

    private void playBreathEndSound() {
        Volitans dragon = getUser();
        if (dragon.level().isClientSide) {
            return;
        }
        float pitch = 0.96f + dragon.getRandom().nextFloat() * 0.08f;
        dragon.getSoundHandler().playMovingEntitySound(
                ModSounds.VOLITANS_BREATH_END.get(),
                BREATH_VOLUME,
                pitch,
                BREATH_END_SOUND_TICKS
        );
    }
}

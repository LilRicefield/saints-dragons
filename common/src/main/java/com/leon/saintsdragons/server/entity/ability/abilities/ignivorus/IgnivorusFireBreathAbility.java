package com.leon.saintsdragons.server.entity.ability.abilities.ignivorus;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.entity.effect.ignivorus.IgnivorusFlameEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.STARTUP;


public class IgnivorusFireBreathAbility extends DragonAbility<Ignivorus> {

    private static final int STARTUP_TICKS = 9;
    private static final int RIDER_ACTIVE_TICKS = 160;
    private static final int AI_ACTIVE_TICKS = 80;
    private static final int COOLDOWN_TICKS = 40;
    private static final float DEFAULT_FIRE_BREATH_DRAIN_PER_TICK = 1.0f / RIDER_ACTIVE_TICKS;
    private static final double VISUAL_RANGE = 24.0D;
    private static final float DEFAULT_DAMAGE_PER_SECOND = 80.0F;
    private static final double FLAME_SPAWN_OFFSET_FORWARD = 0.0;
    private static final double FLAME_SPAWN_OFFSET_UP = 0.0;
    private static final double FLAME_SPAWN_OFFSET_RIGHT = 0.0;
    private static final int FLAME_SPAWN_MIN = 3;
    private static final int FLAME_SPAWN_MAX = 5;
    private static final double DEFAULT_FLAME_SPAWN_MULTIPLIER = 1.0D;
    private static final double DEFAULT_FLAME_SPEED_MULTIPLIER = 1.0D;
    private static final double DEFAULT_FLAME_LIFETIME_MULTIPLIER = 1.0D;

    private static final DragonAbilitySection[] RIDER_TRACK = new DragonAbilitySection[]{
        new AbilitySectionDuration(STARTUP, STARTUP_TICKS),
        new AbilitySectionDuration(ACTIVE, RIDER_ACTIVE_TICKS)
    };

    private static final DragonAbilitySection[] AI_TRACK = new DragonAbilitySection[]{
        new AbilitySectionDuration(STARTUP, STARTUP_TICKS),
        new AbilitySectionDuration(ACTIVE, AI_ACTIVE_TICKS)
    };

    private boolean breathStartPlayed = false;
    private boolean breathLoopActive = false;

    public IgnivorusFireBreathAbility(DragonAbilityType<Ignivorus, IgnivorusFireBreathAbility> type,
                                      Ignivorus user) {
        super(type, user, user.getControllingPassenger() != null ? RIDER_TRACK : AI_TRACK, COOLDOWN_TICKS);
    }

    @Override
    protected void beginSection(@Nullable DragonAbilitySection section) {
        if (section == null) {
            return;
        }

        Ignivorus dragon = getUser();

        if (!dragon.isTame() && dragon.getControllingPassenger() == null) {
            if (!isValidTarget(dragon.getTarget())) {
                interrupt();
                return;
            }
        }

        if (section.sectionType == STARTUP) {
            if (!dragon.canUseFireBreath()) {
                interrupt();
                return;
            }
            breathStartPlayed = true;
            breathLoopActive = false;
            dragon.setBreathingFire(false);
            dragon.setFireBreathProgress(0);
            dragon.clearFireBreathPath();
            dragon.triggerAnim("action", "fire_breath_start");
            if (!dragon.level().isClientSide) {
                float pitch = 0.92f + dragon.getRandom().nextFloat() * 0.15f;
                dragon.playSound(ModSounds.IGNIVORUS_FIRE_BREATH_START.get(), 2.0f, pitch);
            }

        } else if (section.sectionType == ACTIVE) {
            dragon.setBreathingFire(true);
            dragon.triggerAnim("action", "fire_breathing");
            breathLoopActive = true;
        }
    }

    @Override
    protected void endSection(@Nullable DragonAbilitySection section) {
        if (section == null) {
            return;
        }

        if (section.sectionType == ACTIVE) {
            Ignivorus dragon = getUser();
            dragon.setBreathingFire(false);
            dragon.clearFireBreathPath();
            triggerBreathStop(dragon);
        }
    }

    @Override
    public void interrupt() {
        Ignivorus dragon = getUser();
        dragon.setBreathingFire(false);
        dragon.setFireBreathProgress(0);
        dragon.clearFireBreathPath();
        triggerBreathStop(dragon);
        super.interrupt();
    }

    private void triggerBreathStop(Ignivorus dragon) {
        if (breathLoopActive || breathStartPlayed) {
            dragon.triggerAnim("action", "fire_breath_stop");
            if (!dragon.level().isClientSide) {
                float pitch = 0.92f + dragon.getRandom().nextFloat() * 0.15f;
                dragon.playSound(ModSounds.IGNIVORUS_FIRE_BREATH_END.get(), 2.0f, pitch);
            }
        }
        breathStartPlayed = false;
        breathLoopActive = false;
    }

    @Override
    protected boolean canContinueUsing() {
        Ignivorus dragon = getUser();
        if (!dragon.isAlive() || dragon.isRemoved()) {
            return false;
        }
        if (dragon.isInWaterOrBubble()) {
            return false;
        }
        return true;
    }

    @Override
    public void tickUsing() {
        DragonAbilitySection section = getCurrentSection();
        if (section == null || section.sectionType != ACTIVE) {
            return;
        }

        Ignivorus dragon = getUser();
        if (!dragon.isTame() && dragon.getControllingPassenger() == null) {
            if (!isValidTarget(dragon.getTarget())) {
                interrupt();
                return;
            }
        }
        if (!dragon.level().isClientSide) {
            float drain = (float) DragonAttributeConfigLoader.getInstance()
                    .getConfig(DragonAttributeConfigLoader.IGNIVORUS_ID)
                    .extraDouble("fire_breath_drain_per_tick", DEFAULT_FIRE_BREATH_DRAIN_PER_TICK);
            if (!dragon.drainFireBreathEnergy(drain)) {
                interrupt();
                return;
            }
        }
        int currentProgress = dragon.getFireBreathProgress();
        if (currentProgress < 40) {
            dragon.setFireBreathProgress(currentProgress + 1);
        }

        Vec3 origin = dragon.getFireBreathStartAnchor(1.0f);
        if (origin == null) {
            dragon.clearFireBreathPath();
            return;
        }

        Vec3 aim = dragon.refreshFireAimDirection(origin, false);
        if (aim == null || aim.lengthSqr() < 1.0E-6) {
            dragon.clearFireBreathPath();
            return;
        }
        dragon.syncFireBreathPath(origin, origin.add(aim.normalize().scale(VISUAL_RANGE)));
        if (dragon.level() instanceof ServerLevel serverLevel) {
            spawnFlameProjectiles(serverLevel, dragon, origin, aim);
        }
    }

    private static float computeDamage(Ignivorus dragon, double sizeScale) {
        float configDamagePerSecond = (float) DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.IGNIVORUS_ID)
                .abilityDamage("fire_breath", DEFAULT_DAMAGE_PER_SECOND);
        return configDamagePerSecond / 20.0F;
    }

    private int spawnFlameProjectiles(ServerLevel level, Ignivorus dragon, Vec3 origin, Vec3 direction) {
        RandomSource random = dragon.getRandom();
        double sizeScale = Math.max(0.8D, dragon.getBbWidth());
        float damagePerProjectile = computeDamage(dragon, sizeScale) * 4.0F;
        var config = DragonAttributeConfigLoader.getInstance()
                .getConfig(DragonAttributeConfigLoader.IGNIVORUS_ID);
        double spawnMultiplier = config.extraDouble("fire_breath_flame_spawn_multiplier",
                DEFAULT_FLAME_SPAWN_MULTIPLIER);
        double speedMultiplier = config.extraDouble("fire_breath_flame_speed_multiplier",
                DEFAULT_FLAME_SPEED_MULTIPLIER);
        double lifetimeMultiplier = config.extraDouble("fire_breath_flame_lifetime_multiplier",
                DEFAULT_FLAME_LIFETIME_MULTIPLIER);
        if (spawnMultiplier <= 0.0D) {
            return 0;
        }

        int minCount = Math.max(1, (int) Math.round(FLAME_SPAWN_MIN * spawnMultiplier));
        int maxCount = Math.max(minCount, (int) Math.round(FLAME_SPAWN_MAX * spawnMultiplier));
        int count = minCount + random.nextInt(maxCount - minCount + 1);
        int spawnedCount = 0;

        for (int i = 0; i < count; i++) {
            double spreadAmount = 0.28 + random.nextDouble() * 0.22;
            Vec3 spread = new Vec3(
                    (random.nextDouble() - 0.5) * spreadAmount,
                    (random.nextDouble() - 0.5) * spreadAmount,
                    (random.nextDouble() - 0.5) * spreadAmount
            );
            double baseSpeed = 4.6 + random.nextDouble() * 1.8;
            double speed = Math.max(0.1D, baseSpeed * speedMultiplier);
            Vec3 velocity = direction.normalize().scale(speed).add(spread);
            float scale = 1.5F + random.nextFloat() * 0.5F;
            int baseLifetime = 24 + random.nextInt(12);
            int projectileLifetime = Math.max(1, (int) Math.round(baseLifetime * lifetimeMultiplier));
            double forwardBias = 0.6 + random.nextDouble() * 0.8;
            Vec3 spawnPos = origin.add(direction.normalize().scale(forwardBias));
            spawnPos = applyLocalOffset(spawnPos, direction, FLAME_SPAWN_OFFSET_FORWARD,
                                        FLAME_SPAWN_OFFSET_UP, FLAME_SPAWN_OFFSET_RIGHT);

            IgnivorusFlameEntity flame = new IgnivorusFlameEntity(
                    level, spawnPos, velocity, dragon, damagePerProjectile, scale, projectileLifetime
            );

            if (level.addFreshEntity(flame)) {
                spawnedCount++;
            }
        }
        return spawnedCount;
    }

    private Vec3 applyLocalOffset(Vec3 origin, Vec3 lookDirection, double forward, double up, double right) {
        if (forward == 0.0 && up == 0.0 && right == 0.0) {
            return origin;
        }

        Vec3 forwardVec = lookDirection.normalize();
        Vec3 rightVec = new Vec3(-forwardVec.z, 0, forwardVec.x).normalize();
        Vec3 upVec = rightVec.cross(forwardVec).normalize();
        Vec3 offset = forwardVec.scale(forward)
                .add(upVec.scale(up))
                .add(rightVec.scale(right));

        return origin.add(offset);
    }

    private boolean isValidTarget(LivingEntity target) {
        if (target == null) return false;
        if (!target.isAlive()) return false;
        if (target.isRemoved()) return false;
        if (target instanceof Player player) {
            if (player.isCreative() || player.isSpectator()) {
                return false;
            }
        }

        return true;
    }
}

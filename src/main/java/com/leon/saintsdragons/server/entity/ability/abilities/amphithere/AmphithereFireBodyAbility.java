package com.leon.saintsdragons.server.entity.ability.abilities.amphithere;

import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.amphithere.AmphithereEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.STARTUP;

/**
 * Sustained fire aura ability for the Amphithere.
 */
public class AmphithereFireBodyAbility extends DragonAbility<AmphithereEntity> {
    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(STARTUP, 1),
            new AbilitySectionDuration(ACTIVE, 1000)
    };

    private static final double AURA_RADIUS = 3.5D;
    private static final double AURA_VERTICAL = 2.5D;
    private static final float BASE_DAMAGE = 3.0F;
    private static final int FIRE_SECONDS = 4;
    private static final int ALLY_FIRE_RESIST_TICKS = 60;
    private static final int ALLY_DAMAGE_RESIST_TICKS = 40;

    private int activeTicks;

    public AmphithereFireBodyAbility(DragonAbilityType<AmphithereEntity, AmphithereFireBodyAbility> type,
                                     AmphithereEntity user) {
        super(type, user, TRACK, 40);
    }

    @Override
    public boolean isOverlayAbility() {
        return true;
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) {
            return;
        }
        if (section.sectionType == STARTUP) {
            activeTicks = 0;
            getUser().setBreathingFire(true);
            Level level = getLevel();
            level.playSound(null, getUser().blockPosition(), SoundEvents.FIRECHARGE_USE, getUser().getSoundSource(), 1.2F, 1.0F + getUser().getRandom().nextFloat() * 0.2F);
        } else if (section.sectionType == ACTIVE) {
            getUser().setBreathingFire(true);
        }
    }

    @Override
    protected void endSection(DragonAbilitySection section) {
        if (section != null && section.sectionType == ACTIVE) {
            getUser().setBreathingFire(false);
        }
    }

    @Override
    public void interrupt() {
        getUser().setBreathingFire(false);
        super.interrupt();
    }

    @Override
    protected boolean canContinueUsing() {
        AmphithereEntity dragon = getUser();
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
        AmphithereEntity dragon = getUser();
        Level level = dragon.level();
        if (!level.isClientSide) {
            activeTicks++;
            applyFireAura((ServerLevel) level, dragon);
            if (activeTicks % 20 == 0) {
                level.playSound(null, dragon.blockPosition(), SoundEvents.BLAZE_SHOOT, dragon.getSoundSource(), 0.6F, 0.9F + dragon.getRandom().nextFloat() * 0.2F);
            }
        }

    }

    private void applyFireAura(ServerLevel level, AmphithereEntity dragon) {
        Vec3 center = dragon.position().add(0.0D, dragon.getBbHeight() * 0.5D, 0.0D);
        AABB area = dragon.getBoundingBox().inflate(AURA_RADIUS, AURA_VERTICAL, AURA_RADIUS);

        protectAllies(level, dragon, area);

        Set<LivingEntity> hitThisTick = new HashSet<>();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != dragon && e.isAlive() && e.attackable() && !dragon.isAlly(e))) {
            if (!hitThisTick.add(target)) {
                continue;
            }
            float damage = BASE_DAMAGE;
            target.hurt(level.damageSources().dragonBreath(), damage);
            target.setSecondsOnFire(FIRE_SECONDS);

            Vec3 pushDir = target.position().subtract(center);
            if (pushDir.lengthSqr() > 1.0E-4) {
                pushDir = pushDir.normalize().scale(0.15D);
                target.push(pushDir.x, 0.05D, pushDir.z);
            }
        }

        var rng = dragon.getRandom();
        for (int i = 0; i < 12; i++) {
            double angle = rng.nextDouble() * (Math.PI * 2.0);
            double radius = 0.5D + rng.nextDouble() * (AURA_RADIUS - 0.5D);
            double height = rng.nextDouble() * AURA_VERTICAL;
            Vec3 sample = center.add(Math.cos(angle) * radius, -AURA_VERTICAL * 0.5D + height, Math.sin(angle) * radius);
            spawnParticles(level, sample);
            maybeIgnite(level, sample, dragon);
        }
    }

    private void spawnParticles(ServerLevel level, Vec3 sample) {
        double spread = 0.6D;
        int flameCount = 12;
        int emberCount = 9;
        int smokeCount = 6;

        level.sendParticles(ParticleTypes.FLAME, sample.x, sample.y, sample.z, flameCount,
                spread, spread * 0.6D, spread, 0.05D);
        level.sendParticles(ParticleTypes.SMALL_FLAME, sample.x, sample.y, sample.z, emberCount,
                spread * 0.4D, spread * 0.25D, spread * 0.4D, 0.02D);
        level.sendParticles(ParticleTypes.LAVA, sample.x, sample.y, sample.z, 3,
                spread * 0.2D, spread * 0.2D, spread * 0.2D, 0.07D);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, sample.x, sample.y, sample.z, smokeCount,
                spread * 0.8D, spread * 0.4D, spread * 0.8D, 0.0D);
    }

    private void maybeIgnite(ServerLevel level, Vec3 sample, AmphithereEntity dragon) {
        if (dragon.getRandom().nextFloat() > 0.12F) {
            return;
        }
        BlockPos pos = BlockPos.containing(sample.x, sample.y - 0.5D, sample.z);
        if (!level.isLoaded(pos) || !level.isEmptyBlock(pos)) {
            return;
        }
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        if (belowState.isAir()) {
            return;
        }
        if (Blocks.FIRE.defaultBlockState().canSurvive(level, pos) && belowState.isFaceSturdy(level, below, Direction.UP)) {
            level.setBlock(pos, Blocks.FIRE.defaultBlockState(), 11);
        }
    }

    private void protectAllies(ServerLevel level, AmphithereEntity dragon, AABB area) {
        AABB expanded = area.inflate(1.5D, 0.75D, 1.5D);
        for (LivingEntity ally : level.getEntitiesOfClass(LivingEntity.class, expanded,
                entity -> entity != dragon && entity.isAlive() && dragon.isAlly(entity))) {
            ally.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, ALLY_FIRE_RESIST_TICKS, 0, true, false, false));
            ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ALLY_DAMAGE_RESIST_TICKS, 4, true, false, false));
            ally.setRemainingFireTicks(0);
        }
    }
}

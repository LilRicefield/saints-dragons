package com.leon.saintsdragons.server.entity.ability.abilities.cindervane;

import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import com.leon.saintsdragons.server.entity.dragons.util.DragonDestructionManager;
import com.leon.saintsdragons.server.entity.dragons.util.DragonElementalImmunity;
import com.leon.saintsdragons.server.entity.dragons.util.DragonGriefingRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

public class CindervaneFireBodyAbility extends DragonAbility<Cindervane> {
    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(STARTUP, 1),
            new AbilitySectionDuration(ACTIVE, 1000)
    };

    private static final double AURA_RADIUS = 3.5D;
    private static final double AURA_VERTICAL = 2.5D;
    private static final float BASE_DAMAGE = 3.0F;
    private static final int FIRE_SECONDS = 4;
    private static final double COOKING_RADIUS = 3.5D;
    private static final int ALLY_FIRE_RESIST_TICKS = 60;
    private static final int ALLY_DAMAGE_RESIST_TICKS = 40;
    private static final double PARTICLE_VIEW_DISTANCE = 64.0D;

    private int activeTicks;

    public CindervaneFireBodyAbility(DragonAbilityType<Cindervane, CindervaneFireBodyAbility> type,
                                     Cindervane user) {
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
        Cindervane dragon = getUser();
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
        Cindervane dragon = getUser();
        Level level = dragon.level();
        if (!level.isClientSide) {
            activeTicks++;
            applyFireAura((ServerLevel) level, dragon);
            if (dragon.isGroundedForAction() && dragon.getControllingPassenger() != null) {
                DragonDestructionManager.applyFireBodyCookingAura((ServerLevel) level, dragon, dragon.position(), COOKING_RADIUS);
            }
            if (activeTicks % 20 == 0) {
                level.playSound(null, dragon.blockPosition(), SoundEvents.BLAZE_SHOOT, dragon.getSoundSource(), 0.6F, 0.9F + dragon.getRandom().nextFloat() * 0.2F);
            }
        }

    }

    private void applyFireAura(ServerLevel level, Cindervane dragon) {
        Vec3 center = dragon.position().add(0.0D, dragon.getBbHeight() * 0.5D, 0.0D);
        AABB area = dragon.getBoundingBox().inflate(AURA_RADIUS, AURA_VERTICAL, AURA_RADIUS);

        protectAllies(level, dragon, area);

        Set<LivingEntity> hitThisTick = new HashSet<>();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != dragon
                        && e.isAlive()
                        && e.attackable()
                        && !dragon.isAlly(e)
                        && !DragonElementalImmunity.isFireImmune(e))) {
            if (!hitThisTick.add(target)) {
                continue;
            }
            float damage = (float) DragonAttributeConfigLoader.getInstance()
                    .getConfig(DragonAttributeConfigLoader.CINDERVANE_ID)
                    .abilityDamage("fire_body", BASE_DAMAGE);
            target.hurt(level.damageSources().dragonBreath(), damage);
            target.setSecondsOnFire(FIRE_SECONDS);

            Vec3 pushDir = target.position().subtract(center);
            if (pushDir.lengthSqr() > 1.0E-4) {
                pushDir = pushDir.normalize().scale(0.15D);
                target.push(pushDir.x, 0.05D, pushDir.z);
            }
        }

        var rng = dragon.getRandom();
        for (int i = 0; i < 6; i++) {
            double angle = rng.nextDouble() * (Math.PI * 2.0);
            double radius = 0.5D + rng.nextDouble() * (AURA_RADIUS - 0.5D);
            double height = rng.nextDouble() * AURA_VERTICAL;
            Vec3 sample = center.add(Math.cos(angle) * radius, -AURA_VERTICAL * 0.5D + height, Math.sin(angle) * radius);
            spawnParticles(level, sample, dragon);
            maybeIgnite(level, sample, dragon);
        }
    }

    private void spawnParticles(ServerLevel level, Vec3 sample, Cindervane dragon) {
        double spread = 0.6D;
        int flameCount = 6;
        int smokeCount = 3;
        ParticleOptions flame = dragon.isAlbinoVariant() ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME;

        sendParticles(level, dragon, flame, sample, flameCount,
                spread, spread * 0.6D, spread, 0.05D);
        sendParticles(level, dragon, ParticleTypes.LARGE_SMOKE, sample, smokeCount,
                spread * 0.8D, spread * 0.4D, spread * 0.8D, 0.0D);
    }

    private void sendParticles(ServerLevel level, Cindervane dragon, ParticleOptions particle, Vec3 sample,
                               int count, double xSpread, double ySpread, double zSpread, double speed) {
        double maxDistanceSqr = PARTICLE_VIEW_DISTANCE * PARTICLE_VIEW_DISTANCE;
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(sample.x, sample.y, sample.z) <= maxDistanceSqr
                    || player.distanceToSqr(dragon) <= maxDistanceSqr) {
                level.sendParticles(player, particle, true,
                        sample.x, sample.y, sample.z, count, xSpread, ySpread, zSpread, speed);
            }
        }
    }

    private void maybeIgnite(ServerLevel level, Vec3 sample, Cindervane dragon) {
        if (!DragonGriefingRules.canSetBlocksOnFire(level)) {
            return;
        }
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
        BlockState fire = Blocks.FIRE.defaultBlockState();
        if (fire.canSurvive(level, pos) && belowState.isFaceSturdy(level, below, Direction.UP)) {
            level.setBlock(pos, fire, 11);
        }
    }

    private void protectAllies(ServerLevel level, Cindervane dragon, AABB area) {
        AABB expanded = area.inflate(1.5D, 0.75D, 1.5D);
        for (LivingEntity ally : level.getEntitiesOfClass(LivingEntity.class, expanded,
                entity -> entity != dragon && entity.isAlive() && dragon.isAlly(entity))) {
            ally.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, ALLY_FIRE_RESIST_TICKS, 0, true, false, false));
            ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ALLY_DAMAGE_RESIST_TICKS, 4, true, false, false));
            ally.setRemainingFireTicks(0);
        }
    }
}

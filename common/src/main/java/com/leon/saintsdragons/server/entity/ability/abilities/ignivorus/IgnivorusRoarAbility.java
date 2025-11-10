package com.leon.saintsdragons.server.entity.ability.abilities.ignivorus;

import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.ability.DragonAbility;
import com.leon.saintsdragons.server.entity.ability.DragonAbilitySection;
import com.leon.saintsdragons.server.entity.ability.DragonAbilityType;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.entity.effect.ignivorus.IgnivorusMagmaPillarEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionDuration;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.ACTIVE;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.RECOVERY;
import static com.leon.saintsdragons.server.entity.ability.DragonAbilitySection.AbilitySectionType.STARTUP;

/**
 * Roar ability for Ignivorus that seeds magma pillars in three lanes.
 */
public class IgnivorusRoarAbility extends DragonAbility<Ignivorus> {
    private static final int STARTUP_TICKS = 8;
    private static final int ACTIVE_TICKS = 36;
    private static final int RECOVERY_TICKS = 16;

    private static final DragonAbilitySection[] TRACK = new DragonAbilitySection[] {
            new AbilitySectionDuration(STARTUP, STARTUP_TICKS),
            new AbilitySectionDuration(ACTIVE, ACTIVE_TICKS),
            new AbilitySectionDuration(RECOVERY, RECOVERY_TICKS)
    };

    private static final int SOUND_DELAY_TICKS = 4;
    private static final int WAVES = 3;
    private static final int FIRST_WAVE_TICK = STARTUP_TICKS + 2;
    private static final int WAVE_INTERVAL_TICKS = 6;

    private static final double LANE_SPACING = 5.0D;
    private static final double BASE_FORWARD_OFFSET = 20.0D;
    private static final double FORWARD_STEP = 10.0D;

    private static final float BASE_DAMAGE = 18.0f;
    private static final float DAMAGE_PER_WAVE = 4.0f;
    private static final double BASE_KNOCKBACK = 0.9D;
    private static final double KNOCKBACK_PER_WAVE = 0.2D;
    private static final int PILLAR_WARMUP_TICKS = 5;
    private static final int PILLAR_LIFETIME_TICKS = 34;

    private boolean soundQueued;
    private int wavesSpawned;

    public IgnivorusRoarAbility(DragonAbilityType<Ignivorus, IgnivorusRoarAbility> type, Ignivorus user) {
        super(type, user, TRACK, 80);
    }

    @Override
    protected void beginSection(DragonAbilitySection section) {
        if (section == null) {
            return;
        }
        if (section.sectionType == STARTUP) {
            Ignivorus dragon = getUser();
            dragon.triggerAnim("action", "roar");
            dragon.lockAbilities(STARTUP_TICKS + ACTIVE_TICKS + RECOVERY_TICKS);
            soundQueued = true;
            wavesSpawned = 0;
        }
    }

    @Override
    protected void endSection(DragonAbilitySection section) {
        if (section != null && section.sectionType == STARTUP) {
            soundQueued = false;
        }
    }

    @Override
    public void tickUsing() {
        DragonAbilitySection section = getCurrentSection();
        if (section == null) {
            return;
        }

        Ignivorus dragon = getUser();

        if (section.sectionType == STARTUP && soundQueued && getTicksInSection() >= SOUND_DELAY_TICKS) {
            if (!dragon.level().isClientSide) {
                float pitch = 0.85f + dragon.getRandom().nextFloat() * 0.2f;
                dragon.playSound(ModSounds.IGNIVORUS_ROAR.get(), 2.0f, pitch);
            }
            soundQueued = false;
        }

        if (section.sectionType != ACTIVE || dragon.level().isClientSide) {
            return;
        }

        int totalTicks = getTicksInUse();
        while (wavesSpawned < WAVES &&
                totalTicks >= FIRST_WAVE_TICK + (wavesSpawned * WAVE_INTERVAL_TICKS)) {
            spawnWave(dragon, wavesSpawned);
            wavesSpawned++;
        }
    }

    private void spawnWave(Ignivorus dragon, int waveIndex) {
        if (!(dragon.level() instanceof ServerLevel server)) {
            return;
        }

        Vec3 forward = dragon.getLookAngle();
        Vec3 horizontalForward = new Vec3(forward.x, 0.0D, forward.z);
        if (horizontalForward.lengthSqr() < 1.0E-4D) {
            horizontalForward = new Vec3(dragon.getForward().x, 0.0D, dragon.getForward().z);
        }
        if (horizontalForward.lengthSqr() < 1.0E-4D) {
            horizontalForward = new Vec3(0.0D, 0.0D, 1.0D);
        }
        horizontalForward = horizontalForward.normalize();

        Vec3 right = new Vec3(-horizontalForward.z, 0.0D, horizontalForward.x).normalize();

        double forwardOffset = BASE_FORWARD_OFFSET + waveIndex * FORWARD_STEP;

        for (int lane = -1; lane <= 1; lane++) {
            double lateral = lane * LANE_SPACING;
            Vec3 laneOffset = horizontalForward.scale(forwardOffset).add(right.scale(lateral));
            Vec3 base = new Vec3(dragon.getX(), dragon.getY(), dragon.getZ()).add(laneOffset);

            BlockPos column = BlockPos.containing(base.x, base.y, base.z);
            BlockPos ground = server.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
            Vec3 spawnPos = new Vec3(base.x, ground.getY(), base.z);

            IgnivorusMagmaPillarEntity pillar = new IgnivorusMagmaPillarEntity(
                    server, spawnPos, dragon, waveIndex,
                    BASE_DAMAGE + waveIndex * DAMAGE_PER_WAVE,
                    BASE_KNOCKBACK + waveIndex * KNOCKBACK_PER_WAVE,
                    PILLAR_WARMUP_TICKS,
                    PILLAR_LIFETIME_TICKS);

            server.addFreshEntity(pillar);
            server.playSound(null, spawnPos.x, spawnPos.y, spawnPos.z,
                    ModSounds.IGNIVORUS_MAGMA_PILLAR.get(), SoundSource.HOSTILE,
                    1.2F, 0.9F + server.random.nextFloat() * 0.2F);
        }
    }

    @Override
    public boolean tryAbility() {
        Ignivorus dragon = getUser();
        return dragon != null
                && !dragon.isBaby()
                && !dragon.isFlying()
                && dragon.onGround()
                && super.tryAbility();
    }
}

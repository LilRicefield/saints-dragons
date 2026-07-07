package com.leon.saintsdragons.common.block;

import com.leon.saintsdragons.common.registry.ModBlockEntities;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.common.registry.ModParticles;
import com.leon.saintsdragons.server.entity.draconianswarm.AbstractDraconianSwarmEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DraconianNucleusBlockEntity extends BlockEntity {
    private static final int PLAYER_CHECK_INTERVAL = 20;
    private static final int SPAWN_INTERVAL = 10;
    private static final int WAVE_INTERMISSION = 100;
    private static final double ACTIVATION_RADIUS = 20.0D;
    private static final int[][] WAVE_COMPOSITIONS = {
            {1, 1, 1},
            {2, 2, 2},
            {2, 3, 3}
    };
    private static final BlockPos[] SPAWN_OFFSETS = {
            new BlockPos(0, 3, 0),
            new BlockPos(2, 3, 0),
            new BlockPos(-2, 3, 0),
            new BlockPos(0, 3, 2),
            new BlockPos(0, 3, -2),
            new BlockPos(2, 4, 2),
            new BlockPos(-2, 4, -2),
            new BlockPos(0, 5, 0)
    };

    private long animationTicks;
    private EncounterState encounterState = EncounterState.DORMANT;
    private UUID encounterId;
    private int currentWave;
    private int spawnedThisWave;
    private int remainingThisWave;
    private long nextActionGameTime;
    private final Set<UUID> activeSwarms = new HashSet<>();

    public DraconianNucleusBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DRACONIAN_NUCLEUS.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DraconianNucleusBlockEntity nucleus) {
        nucleus.animationTicks++;
        if (level.isClientSide) {
            spawnNucleusSmoke(level, pos);
            return;
        }
        if (level instanceof ServerLevel serverLevel) {
            nucleus.tickEncounter(serverLevel, pos);
        }
    }

    private void tickEncounter(ServerLevel level, BlockPos pos) {
        long gameTime = level.getGameTime();
        switch (this.encounterState) {
            case DORMANT -> {
                if (gameTime % PLAYER_CHECK_INTERVAL == 0 && hasSurvivalPlayer(level, pos)) {
                    beginEncounter(gameTime);
                }
            }
            case SPAWNING -> tickSpawning(level, pos, gameTime);
            case ACTIVE -> {
                if (this.spawnedThisWave >= getWaveSize() && this.remainingThisWave <= 0) {
                    finishWave(gameTime);
                }
            }
            case INTERMISSION -> {
                if (gameTime >= this.nextActionGameTime) {
                    beginNextWave(gameTime);
                }
            }
            case COMPLETE -> {
            }
        }
    }

    private void beginEncounter(long gameTime) {
        this.encounterId = UUID.randomUUID();
        this.currentWave = 1;
        this.spawnedThisWave = 0;
        this.remainingThisWave = 0;
        this.activeSwarms.clear();
        this.encounterState = EncounterState.SPAWNING;
        this.nextActionGameTime = gameTime;
        setChanged();
    }

    private void tickSpawning(ServerLevel level, BlockPos pos, long gameTime) {
        if (gameTime < this.nextActionGameTime || this.encounterId == null) {
            return;
        }
        if (this.spawnedThisWave >= getWaveSize()) {
            this.encounterState = EncounterState.ACTIVE;
            setChanged();
            return;
        }

        EntityType<? extends AbstractDraconianSwarmEntity> type = getNextSwarmType();
        AbstractDraconianSwarmEntity swarm = type.create(level);
        if (swarm != null && spawnSwarm(level, pos, swarm)) {
            this.spawnedThisWave++;
            this.remainingThisWave++;
            this.activeSwarms.add(swarm.getUUID());
            this.nextActionGameTime = gameTime + SPAWN_INTERVAL;
            if (this.spawnedThisWave >= getWaveSize()) {
                this.encounterState = EncounterState.ACTIVE;
            }
            setChanged();
        } else {
            this.nextActionGameTime = gameTime + SPAWN_INTERVAL;
        }
    }

    private boolean spawnSwarm(ServerLevel level, BlockPos nucleusPos, AbstractDraconianSwarmEntity swarm) {
        int offsetStart = this.spawnedThisWave % SPAWN_OFFSETS.length;
        for (int index = 0; index < SPAWN_OFFSETS.length; index++) {
            BlockPos spawnPos = nucleusPos.offset(SPAWN_OFFSETS[(offsetStart + index) % SPAWN_OFFSETS.length]);
            swarm.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D,
                    level.random.nextFloat() * 360.0F, 0.0F);
            if (!level.noCollision(swarm, swarm.getBoundingBox())) {
                continue;
            }

            swarm.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.TRIGGERED, null, null);
            swarm.assignNucleusEncounter(nucleusPos, this.encounterId, this.currentWave);
            if (level.addFreshEntity(swarm)) {
                swarm.playSpawnAnimation();
                return true;
            }
            return false;
        }
        return false;
    }

    private EntityType<? extends AbstractDraconianSwarmEntity> getNextSwarmType() {
        int[] composition = WAVE_COMPOSITIONS[this.currentWave - 1];
        if (this.spawnedThisWave < composition[0]) {
            return ModEntities.LATCHER.get();
        }
        if (this.spawnedThisWave < composition[0] + composition[1]) {
            return ModEntities.WINGED.get();
        }
        return ModEntities.WHETTLED.get();
    }

    private int getWaveSize() {
        int[] composition = WAVE_COMPOSITIONS[this.currentWave - 1];
        return composition[0] + composition[1] + composition[2];
    }

    private void finishWave(long gameTime) {
        this.activeSwarms.clear();
        if (this.currentWave >= WAVE_COMPOSITIONS.length) {
            this.encounterState = EncounterState.COMPLETE;
        } else {
            this.encounterState = EncounterState.INTERMISSION;
            this.nextActionGameTime = gameTime + WAVE_INTERMISSION;
        }
        setChanged();
    }

    private void beginNextWave(long gameTime) {
        this.currentWave++;
        this.spawnedThisWave = 0;
        this.remainingThisWave = 0;
        this.activeSwarms.clear();
        this.encounterState = EncounterState.SPAWNING;
        this.nextActionGameTime = gameTime;
        setChanged();
    }

    public void onSwarmDefeated(UUID encounterId, int wave, UUID swarmId) {
        if (this.encounterId == null || !this.encounterId.equals(encounterId)
                || this.currentWave != wave || !this.activeSwarms.remove(swarmId)) {
            return;
        }
        this.remainingThisWave = Math.max(0, this.remainingThisWave - 1);
        setChanged();
    }

    private static void spawnNucleusSmoke(Level level, BlockPos pos) {
        if (level.random.nextInt(5) != 0) {
            return;
        }
        double x = pos.getX() + 0.5D + (level.random.nextDouble() - 0.5D) * 0.18D;
        double y = pos.getY() + 1.05D;
        double z = pos.getZ() + 0.5D + (level.random.nextDouble() - 0.5D) * 0.18D;
        double driftX = (level.random.nextDouble() - 0.5D) * 0.006D;
        double driftZ = (level.random.nextDouble() - 0.5D) * 0.006D;
        level.addParticle(ModParticles.DRACONIAN_NUCLEUS_PARTICLE.get(), x, y, z, driftX, 0.035D, driftZ);
    }

    public long getAnimationTimeMillis(float partialTick) {
        return (long) ((this.animationTicks + partialTick) * 50.0F);
    }

    private static boolean hasSurvivalPlayer(ServerLevel level, BlockPos pos) {
        AABB activationArea = new AABB(pos).inflate(ACTIVATION_RADIUS);
        return !level.getEntitiesOfClass(ServerPlayer.class, activationArea, player ->
                player.isAlive() && player.gameMode.getGameModeForPlayer() == GameType.SURVIVAL).isEmpty();
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("EncounterState", this.encounterState.name());
        if (this.encounterId != null) {
            tag.putUUID("EncounterId", this.encounterId);
        }
        tag.putInt("CurrentWave", this.currentWave);
        tag.putInt("SpawnedThisWave", this.spawnedThisWave);
        tag.putInt("RemainingThisWave", this.remainingThisWave);
        tag.putLong("NextActionGameTime", this.nextActionGameTime);
        long[] swarmIds = new long[this.activeSwarms.size() * 2];
        int index = 0;
        for (UUID id : this.activeSwarms) {
            swarmIds[index++] = id.getMostSignificantBits();
            swarmIds[index++] = id.getLeastSignificantBits();
        }
        tag.putLongArray("ActiveSwarms", swarmIds);
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        try {
            this.encounterState = EncounterState.valueOf(tag.getString("EncounterState"));
        } catch (IllegalArgumentException ignored) {
            this.encounterState = EncounterState.DORMANT;
        }
        this.encounterId = tag.hasUUID("EncounterId") ? tag.getUUID("EncounterId") : null;
        this.currentWave = tag.getInt("CurrentWave");
        this.spawnedThisWave = tag.getInt("SpawnedThisWave");
        this.remainingThisWave = tag.getInt("RemainingThisWave");
        this.nextActionGameTime = tag.getLong("NextActionGameTime");
        this.activeSwarms.clear();
        long[] swarmIds = tag.getLongArray("ActiveSwarms");
        for (int index = 0; index + 1 < swarmIds.length; index += 2) {
            this.activeSwarms.add(new UUID(swarmIds[index], swarmIds[index + 1]));
        }
    }

    private enum EncounterState {
        DORMANT,
        SPAWNING,
        ACTIVE,
        INTERMISSION,
        COMPLETE
    }
}

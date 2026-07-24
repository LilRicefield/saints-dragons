package com.leon.saintsdragons.common.block;

import com.leon.saintsdragons.common.registry.ModBlockEntities;
import com.leon.saintsdragons.common.config.dragon.DragonAttributeConfigLoader;
import com.leon.saintsdragons.common.network.MessageSwarmBattleMusic;
import com.leon.saintsdragons.common.network.MessageSwarmWaveBar;
import com.leon.saintsdragons.common.network.NetworkHandler;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.common.registry.ModParticles;
import com.leon.saintsdragons.common.registry.ModSounds;
import com.leon.saintsdragons.server.entity.dragons.util.DragonUtilities;
import com.leon.saintsdragons.server.entity.draconianswarm.AbstractDraconianSwarmEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

public class DraconianNucleusBlockEntity extends BlockEntity {
    private static final int PLAYER_CHECK_INTERVAL = 20;
    private static final int SPAWN_INTERVAL = 10;
    private static final int BATTLE_MUSIC_SIGNAL_INTERVAL = 20;
    private static final int BATTLE_MUSIC_SIGNAL_DURATION = 60;
    private static final int BATTLE_MUSIC_START_DELAY = 30;
    private static final int WAVE_BAR_SIGNAL_DURATION = 40;
    private static final int FIRST_WAVE_EFFECT_DELAY = 200;
    private static final int FIRST_WAVE_SPAWN_DELAY = 220;
    private static final int LATER_WAVE_SPAWN_DELAY = 40;
    private static final double ACTIVATION_RADIUS = 20.0D;
    private static final double SUMMON_SOUND_MERGE_RADIUS = ACTIVATION_RADIUS * 2.0D;
    private static final long SUMMON_SOUND_MERGE_WINDOW_TICKS = 10L;
    private static final double ADVANCEMENT_RADIUS = 64.0D;
    private static final int WAVE_COUNT = 3;
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
    private static final Map<ServerLevel, List<RecentSummonSound>> RECENT_SUMMON_SOUNDS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private long animationTicks;
    private EncounterState encounterState = EncounterState.DORMANT;
    private UUID encounterId;
    private int currentWave;
    private int spawnedThisWave;
    private int remainingThisWave;
    private long nextActionGameTime;
    private long summonEffectsGameTime;
    private long controllerCooldownUntilGameTime;
    private long lastBattleMusicSignalGameTime;
    private long battleMusicStartGameTime;
    private boolean controllerActivationOnly;
    private boolean harvestUnlocked;
    private boolean deactivatedByController;
    private boolean summonEffectsActive;
    private UUID ownerId;
    private final Set<UUID> activeSwarms = new HashSet<>();

    public DraconianNucleusBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DRACONIAN_NUCLEUS.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DraconianNucleusBlockEntity nucleus) {
        nucleus.animationTicks++;
        if (level.isClientSide) {
            if (nucleus.summonEffectsActive) {
                spawnNucleusSmoke(level, pos);
            }
            return;
        }
        if (level instanceof ServerLevel serverLevel) {
            nucleus.tickEncounter(serverLevel, pos);
        }
    }

    private void tickEncounter(ServerLevel level, BlockPos pos) {
        long gameTime = level.getGameTime();
        if (this.encounterState == EncounterState.SPAWNING && !this.summonEffectsActive
                && gameTime >= this.summonEffectsGameTime) {
            this.summonEffectsActive = true;
            syncChanged();
        }
        if (hasActiveEncounter() && this.spawnedThisWave > 0) {
            signalNearbyBattleMusic(level, gameTime);
        }
        switch (this.encounterState) {
            case DORMANT -> {
                if (!this.controllerActivationOnly && gameTime % PLAYER_CHECK_INTERVAL == 0
                        && gameTime >= this.controllerCooldownUntilGameTime
                        && hasSurvivalPlayer(level, pos)) {
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
        this.nextActionGameTime = gameTime + FIRST_WAVE_SPAWN_DELAY;
        this.summonEffectsGameTime = gameTime + FIRST_WAVE_EFFECT_DELAY;
        this.battleMusicStartGameTime = gameTime + FIRST_WAVE_SPAWN_DELAY + BATTLE_MUSIC_START_DELAY;
        this.harvestUnlocked = false;
        this.deactivatedByController = false;
        this.summonEffectsActive = false;
        playSummonSound(ModSounds.DRACONIAN_NUCLEUS_SUMMON.get());
        syncChanged();
    }

    public void setControllerActivationOnly(Player owner) {
        this.controllerActivationOnly = true;
        this.harvestUnlocked = false;
        this.deactivatedByController = false;
        this.controllerCooldownUntilGameTime = 0L;
        this.ownerId = owner.getUUID();
        syncChanged();
    }

    public boolean activateFromController(Player player, long gameTime) {
        if (!this.controllerActivationOnly
                || (this.harvestUnlocked && !this.deactivatedByController)
                || !canUseController(player) || this.encounterState != EncounterState.DORMANT
                || gameTime < this.controllerCooldownUntilGameTime) {
            return false;
        }
        beginEncounter(gameTime);
        if (player instanceof ServerPlayer serverPlayer) {
            DragonUtilities.awardAdvancement(serverPlayer, "summon_draconian_swarm", "summon_draconian_swarm");
        }
        return true;
    }

    public boolean deactivateFromController(ServerLevel level, Player player) {
        if (!this.controllerActivationOnly || !canUseController(player) || !hasActiveEncounter()) {
            return false;
        }

        for (UUID swarmId : Set.copyOf(this.activeSwarms)) {
            Entity entity = level.getEntity(swarmId);
            if (entity instanceof AbstractDraconianSwarmEntity swarm) {
                swarm.remove(Entity.RemovalReason.DISCARDED);
            }
        }

        this.encounterId = null;
        this.currentWave = 0;
        this.spawnedThisWave = 0;
        this.remainingThisWave = 0;
        this.nextActionGameTime = 0L;
        this.summonEffectsGameTime = 0L;
        this.controllerCooldownUntilGameTime = level.getGameTime() + 1200L;
        this.battleMusicStartGameTime = 0L;
        this.activeSwarms.clear();
        this.encounterState = EncounterState.DORMANT;
        this.harvestUnlocked = true;
        this.deactivatedByController = true;
        this.summonEffectsActive = false;
        stopNearbyBattleMusic(level);
        syncChanged();
        return true;
    }

    public boolean isHarvestable() {
        return this.harvestUnlocked
                || (this.controllerActivationOnly
                && this.encounterState == EncounterState.DORMANT
                && this.encounterId == null);
    }

    private boolean canUseController(Player player) {
        return this.ownerId == null || this.ownerId.equals(player.getUUID());
    }

    public boolean hasActiveEncounter() {
        return this.encounterState == EncounterState.SPAWNING
                || this.encounterState == EncounterState.ACTIVE
                || this.encounterState == EncounterState.INTERMISSION;
    }

    public boolean isActiveEncounter(UUID encounterId) {
        return encounterId != null && encounterId.equals(this.encounterId) && hasActiveEncounter();
    }

    private void tickSpawning(ServerLevel level, BlockPos pos, long gameTime) {
        if (gameTime < this.nextActionGameTime || this.encounterId == null) {
            return;
        }
        if (this.spawnedThisWave >= getWaveSize()) {
            this.encounterState = EncounterState.ACTIVE;
            syncChanged();
            return;
        }

        EntityType<? extends AbstractDraconianSwarmEntity> type = getNextSwarmType();
        AbstractDraconianSwarmEntity swarm = type.create(level);
        if (swarm != null && spawnSwarm(level, pos, swarm)) {
            if (this.currentWave == 1 && this.spawnedThisWave == 0) {
                awardNearbyAdvancement(level, pos, "encounter_draconian_swarm", "encounter_draconian_swarm");
            }
            if (this.currentWave > 1 && this.spawnedThisWave == 0) {
                playSummonSound(ModSounds.DRACONIAN_NUCLEUS_SUMMON_PHASE_2_AND_3.get());
            }
            this.spawnedThisWave++;
            this.remainingThisWave++;
            this.activeSwarms.add(swarm.getUUID());
            this.nextActionGameTime = gameTime + SPAWN_INTERVAL;
            if (this.spawnedThisWave >= getWaveSize()) {
                this.encounterState = EncounterState.ACTIVE;
            }
            syncChanged();
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
            swarm.assignNucleusEncounter(
                    nucleusPos,
                    this.encounterId,
                    this.currentWave,
                    this.controllerActivationOnly);
            if (level.addFreshEntity(swarm)) {
                swarm.playSpawnAnimation();
                return true;
            }
            return false;
        }
        return false;
    }

    private EntityType<? extends AbstractDraconianSwarmEntity> getNextSwarmType() {
        return switch (this.spawnedThisWave % 3) {
            case 0 -> ModEntities.LATCHER.get();
            case 1 -> ModEntities.WINGED.get();
            default -> ModEntities.WHETTLED.get();
        };
    }

    private int getWaveSize() {
        return DragonAttributeConfigLoader.swarmWaveCount(
                DragonAttributeConfigLoader.getInstance().getConfig(DragonAttributeConfigLoader.DRACONIAN_SWARM_ID),
                this.currentWave);
    }

    private void finishWave(long gameTime) {
        this.activeSwarms.clear();
        if (this.currentWave >= WAVE_COUNT) {
            this.encounterState = EncounterState.COMPLETE;
            this.harvestUnlocked = true;
            this.deactivatedByController = false;
            this.summonEffectsActive = false;
            this.battleMusicStartGameTime = 0L;
            if (this.level instanceof ServerLevel serverLevel) {
                awardNearbyAdvancement(serverLevel, this.worldPosition, "vanquish_draconian_swarm", "vanquish_draconian_swarm");
                stopNearbyBattleMusic(serverLevel);
            }
        } else {
            beginNextWave(gameTime);
            return;
        }
        syncChanged();
    }

    private void beginNextWave(long gameTime) {
        this.currentWave++;
        this.spawnedThisWave = 0;
        this.remainingThisWave = 0;
        this.activeSwarms.clear();
        this.encounterState = EncounterState.SPAWNING;
        this.nextActionGameTime = gameTime + LATER_WAVE_SPAWN_DELAY;
        this.summonEffectsGameTime = gameTime;
        this.summonEffectsActive = true;
        if (this.battleMusicStartGameTime == 0L) {
            this.battleMusicStartGameTime = gameTime + BATTLE_MUSIC_START_DELAY;
        }
        syncChanged();
    }

    public void onSwarmDefeated(UUID encounterId, int wave, UUID swarmId) {
        if (this.encounterId == null || !this.encounterId.equals(encounterId)
                || this.currentWave != wave || !this.activeSwarms.remove(swarmId)) {
            return;
        }
        this.remainingThisWave = Math.max(0, this.remainingThisWave - 1);
        syncChanged();
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

    private void playSummonSound(SoundEvent sound) {
        if (!(this.level instanceof ServerLevel serverLevel)
                || !claimSummonSound(serverLevel, this.worldPosition, sound)) {
            return;
        }
        serverLevel.playSound(null, this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 0.5D,
                this.worldPosition.getZ() + 0.5D, sound, SoundSource.BLOCKS, 3.0F, 1.0F);
    }

    private static boolean claimSummonSound(ServerLevel level, BlockPos pos, SoundEvent sound) {
        long gameTime = level.getGameTime();
        ResourceLocation soundId = sound.getLocation();
        double mergeRadiusSqr = SUMMON_SOUND_MERGE_RADIUS * SUMMON_SOUND_MERGE_RADIUS;

        synchronized (RECENT_SUMMON_SOUNDS) {
            List<RecentSummonSound> recentSounds = RECENT_SUMMON_SOUNDS.computeIfAbsent(
                    level, ignored -> new ArrayList<>());
            recentSounds.removeIf(recent -> gameTime < recent.gameTime()
                    || gameTime - recent.gameTime() > SUMMON_SOUND_MERGE_WINDOW_TICKS);

            for (RecentSummonSound recent : recentSounds) {
                if (recent.soundId().equals(soundId) && recent.pos().distSqr(pos) <= mergeRadiusSqr) {
                    return false;
                }
            }

            recentSounds.add(new RecentSummonSound(soundId, pos.immutable(), gameTime));
            return true;
        }
    }

    private record RecentSummonSound(ResourceLocation soundId, BlockPos pos, long gameTime) {
    }

    private static void awardNearbyAdvancement(ServerLevel level, BlockPos pos, String advancementId, String criterion) {
        AABB awardArea = new AABB(pos).inflate(ADVANCEMENT_RADIUS);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, awardArea, ServerPlayer::isAlive)) {
            DragonUtilities.awardAdvancement(player, advancementId, criterion);
        }
    }

    private void stopNearbyBattleMusic(ServerLevel level) {
        AABB stopArea = new AABB(this.worldPosition).inflate(ADVANCEMENT_RADIUS);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, stopArea, ServerPlayer::isAlive)) {
            NetworkHandler.sendToPlayer(player, new MessageSwarmBattleMusic(false, 0));
            NetworkHandler.sendToPlayer(player, new MessageSwarmWaveBar(false, 0, 0.0F, 0));
        }
    }

    private void signalNearbyBattleMusic(ServerLevel level, long gameTime) {
        if (gameTime - this.lastBattleMusicSignalGameTime < BATTLE_MUSIC_SIGNAL_INTERVAL) {
            return;
        }
        this.lastBattleMusicSignalGameTime = gameTime;
        AABB battleArea = new AABB(this.worldPosition).inflate(ADVANCEMENT_RADIUS);
        int wave = this.currentWave;
        float progress = getWaveBarProgress();
        boolean musicActive = this.battleMusicStartGameTime == 0L || gameTime >= this.battleMusicStartGameTime;
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, battleArea, ServerPlayer::isAlive)) {
            if (musicActive) {
                NetworkHandler.sendToPlayer(player, new MessageSwarmBattleMusic(true, BATTLE_MUSIC_SIGNAL_DURATION));
            }
            NetworkHandler.sendToPlayer(player, new MessageSwarmWaveBar(true, wave, progress, WAVE_BAR_SIGNAL_DURATION));
        }
    }

    private float getWaveBarProgress() {
        int waveSize = Math.max(1, getWaveSize());
        return Math.max(0.0F, Math.min(1.0F, (float) this.remainingThisWave / (float) waveSize));
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
        tag.putLong("SummonEffectsGameTime", this.summonEffectsGameTime);
        tag.putLong("ControllerCooldownUntilGameTime", this.controllerCooldownUntilGameTime);
        tag.putLong("BattleMusicStartGameTime", this.battleMusicStartGameTime);
        tag.putBoolean("ControllerActivationOnly", this.controllerActivationOnly);
        tag.putBoolean("HarvestUnlocked", this.harvestUnlocked);
        tag.putBoolean("DeactivatedByController", this.deactivatedByController);
        tag.putBoolean("SummonEffectsActive", this.summonEffectsActive);
        if (this.ownerId != null) {
            tag.putUUID("OwnerId", this.ownerId);
        }
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
        this.summonEffectsGameTime = tag.getLong("SummonEffectsGameTime");
        this.controllerCooldownUntilGameTime = tag.getLong("ControllerCooldownUntilGameTime");
        this.battleMusicStartGameTime = tag.getLong("BattleMusicStartGameTime");
        this.controllerActivationOnly = tag.getBoolean("ControllerActivationOnly");
        this.harvestUnlocked = tag.getBoolean("HarvestUnlocked") || this.encounterState == EncounterState.COMPLETE;
        this.deactivatedByController = tag.getBoolean("DeactivatedByController");
        this.summonEffectsActive = tag.getBoolean("SummonEffectsActive");
        this.ownerId = tag.hasUUID("OwnerId") ? tag.getUUID("OwnerId") : null;
        this.activeSwarms.clear();
        long[] swarmIds = tag.getLongArray("ActiveSwarms");
        for (int index = 0; index + 1 < swarmIds.length; index += 2) {
            this.activeSwarms.add(new UUID(swarmIds[index], swarmIds[index + 1]));
        }
    }

    @Override
    public @NotNull CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public @Nullable ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void syncChanged() {
        setChanged();
        if (this.level != null && !this.level.isClientSide) {
            BlockState state = getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, 3);
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

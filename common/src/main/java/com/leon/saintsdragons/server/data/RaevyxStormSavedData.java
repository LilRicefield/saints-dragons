package com.leon.saintsdragons.server.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.ServerLevelData;

public final class RaevyxStormSavedData extends SavedData {
    private boolean summonedStorm;
    private int naturalThunderTicks;

    private static RaevyxStormSavedData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                RaevyxStormSavedData::load, RaevyxStormSavedData::new, "saintsdragons_raevyx_storm");
    }

    public static void beforeSummoning(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        RaevyxStormSavedData state = get(overworld);
        if (!state.summonedStorm) {
            var weather = overworld.getLevelData();
            state.naturalThunderTicks = weather.isThundering() && weather instanceof ServerLevelData data
                    ? Math.max(0, data.getThunderTime()) : 0;
        }
        state.summonedStorm = true;
        state.setDirty();
    }

    public static void tick(ServerLevel level) {
        if (level != level.getServer().overworld()) return;
        RaevyxStormSavedData state = get(level);
        if (!state.summonedStorm) return;
        if (!level.getLevelData().isThundering()) {
            state.naturalThunderTicks = 0;
            if (!level.isThundering()) state.summonedStorm = false;
            state.setDirty();
        } else if (state.naturalThunderTicks > 0
                && level.getGameRules().getBoolean(GameRules.RULE_WEATHER_CYCLE)) {
            state.naturalThunderTicks--;
            state.setDirty();
        }
    }

    public static boolean allowsWildSpawns(ServerLevel level) {
        if (!level.isThundering()) return false;
        if (!level.getServer().isSameThread()) return false;
        RaevyxStormSavedData state = get(level);
        return !state.summonedStorm || state.naturalThunderTicks > 0;
    }

    private static RaevyxStormSavedData load(CompoundTag tag) {
        RaevyxStormSavedData state = new RaevyxStormSavedData();
        state.summonedStorm = tag.getBoolean("SummonedStorm");
        state.naturalThunderTicks = Math.max(0, tag.getInt("NaturalThunderTicks"));
        return state;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("SummonedStorm", summonedStorm);
        tag.putInt("NaturalThunderTicks", naturalThunderTicks);
        return tag;
    }
}

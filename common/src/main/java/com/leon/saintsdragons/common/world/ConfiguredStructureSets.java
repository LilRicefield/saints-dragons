package com.leon.saintsdragons.common.world;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.config.SaintsDragonsConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.StructureSet;

import java.util.List;

public final class ConfiguredStructureSets {
    private static final ResourceKey<StructureSet> IVY_HOUSE = key("ivy_house");
    private static final ResourceKey<StructureSet> IGNIVORUS_ROOST = key("igni_roost_set");
    private static final ResourceKey<StructureSet> VARASUCHUS_ROOST = key("varasuchus_roost");

    private ConfiguredStructureSets() {
    }

    public static List<Holder<StructureSet>> filterEnabled(List<Holder<StructureSet>> structureSets) {
        return structureSets.stream()
                .filter(ConfiguredStructureSets::isEnabled)
                .toList();
    }

    private static boolean isEnabled(Holder<StructureSet> structureSet) {
        if (structureSet.is(IVY_HOUSE)) {
            return SaintsDragonsConfig.isIvySpawningEnabled();
        }
        if (structureSet.is(IGNIVORUS_ROOST)) {
            return SaintsDragonsConfig.isIgnivorusSpawningEnabled();
        }
        if (structureSet.is(VARASUCHUS_ROOST)) {
            return SaintsDragonsConfig.isVarasuchusSpawningEnabled();
        }
        return true;
    }

    private static ResourceKey<StructureSet> key(String path) {
        return ResourceKey.create(Registries.STRUCTURE_SET, SaintsDragonsCommon.rl(path));
    }
}

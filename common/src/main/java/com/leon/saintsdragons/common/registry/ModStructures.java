package com.leon.saintsdragons.common.registry;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.platform.RegistryHelper;
import com.leon.saintsdragons.platform.Services;
import com.leon.saintsdragons.server.world.structure.IvyHouseStructure;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.function.Supplier;

public final class ModStructures {
    private static final RegistryHelper.RegistryWrapper<StructureType<?>> REGISTER =
            Services.PLATFORM.getRegistryHelper()
                    .create(Registries.STRUCTURE_TYPE, () -> BuiltInRegistries.STRUCTURE_TYPE, SaintsDragonsCommon.MOD_ID);

    public static final Supplier<StructureType<IvyHouseStructure>> IVY_HOUSE =
            REGISTER.register("ivy_house", () -> () -> IvyHouseStructure.CODEC);

    private ModStructures() {
    }

    public static void register() {
        REGISTER.register();
    }
}

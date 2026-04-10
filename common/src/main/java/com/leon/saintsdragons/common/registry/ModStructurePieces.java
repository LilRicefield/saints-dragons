package com.leon.saintsdragons.common.registry;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.platform.RegistryHelper;
import com.leon.saintsdragons.platform.Services;
import com.leon.saintsdragons.server.world.structure.IvyHousePiece;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

import java.util.function.Supplier;

public final class ModStructurePieces {
    private static final RegistryHelper.RegistryWrapper<StructurePieceType> REGISTER =
            Services.PLATFORM.getRegistryHelper()
                    .create(Registries.STRUCTURE_PIECE, () -> BuiltInRegistries.STRUCTURE_PIECE, SaintsDragonsCommon.MOD_ID);

    public static final Supplier<StructurePieceType> IVY_HOUSE =
            REGISTER.register("ivy_house", () -> (StructurePieceType.StructureTemplateType) IvyHousePiece::new);

    private ModStructurePieces() {
    }

    public static void register() {
        REGISTER.register();
    }
}

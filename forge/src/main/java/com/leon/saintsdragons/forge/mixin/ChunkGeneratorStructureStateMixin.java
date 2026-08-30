package com.leon.saintsdragons.forge.mixin;

import com.leon.saintsdragons.common.world.ConfiguredStructureSets;
import net.minecraft.core.Holder;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.List;

@Mixin(ChunkGeneratorStructureState.class)
public abstract class ChunkGeneratorStructureStateMixin {
    @ModifyArg(
            method = {"createForFlat", "createForNormal"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/chunk/ChunkGeneratorStructureState;<init>(Lnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/world/level/biome/BiomeSource;JJLjava/util/List;)V"
            ),
            index = 4
    )
    private static List<Holder<StructureSet>> saintsdragons$filterDisabledStructureSets(
            List<Holder<StructureSet>> structureSets) {
        return ConfiguredStructureSets.filterEnabled(structureSets);
    }
}

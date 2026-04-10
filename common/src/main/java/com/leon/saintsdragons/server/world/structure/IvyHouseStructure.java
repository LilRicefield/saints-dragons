package com.leon.saintsdragons.server.world.structure;

import com.leon.saintsdragons.common.config.SaintsDragonsConfig;
import com.leon.saintsdragons.common.registry.ModStructures;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.Arrays;
import java.util.Optional;

public class IvyHouseStructure extends Structure {
    public static final Codec<IvyHouseStructure> CODEC = simpleCodec(IvyHouseStructure::new);

    public IvyHouseStructure(StructureSettings structureSettings) {
        super(structureSettings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext generationContext) {
        if (!SaintsDragonsConfig.isIvyHouseEnabled()) {
            return Optional.empty();
        }

        StructureTemplate structureTemplate = generationContext.structureTemplateManager().getOrCreate(IvyHousePiece.TEMPLATE_ID);
        Vec3i size = structureTemplate.getSize();
        if (size.getX() <= 0 || size.getY() <= 0 || size.getZ() <= 0) {
            return Optional.empty();
        }

        Rotation rotation = Rotation.getRandom(generationContext.random());
        BoundingBox rawBox = structureTemplate.getBoundingBox(BlockPos.ZERO, rotation, BlockPos.ZERO, Mirror.NONE);
        int width = rawBox.getXSpan();
        int depth = rawBox.getZSpan();
        int footprintMinX = generationContext.chunkPos().getMiddleBlockX() - width / 2;
        int footprintMinZ = generationContext.chunkPos().getMiddleBlockZ() - depth / 2;
        int surfaceY = getSampledSurfaceY(generationContext, footprintMinX, footprintMinZ, width, depth);
        BlockPos origin = new BlockPos(footprintMinX - rawBox.minX(), surfaceY, footprintMinZ - rawBox.minZ());

        return Optional.of(new GenerationStub(origin, structurePiecesBuilder ->
                structurePiecesBuilder.addPiece(new IvyHousePiece(generationContext.structureTemplateManager(), origin, rotation))));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.IVY_HOUSE.get();
    }

    private static int getSampledSurfaceY(GenerationContext generationContext, int footprintMinX, int footprintMinZ, int width, int depth) {
        int maxX = footprintMinX + width - 1;
        int maxZ = footprintMinZ + depth - 1;
        int centerX = footprintMinX + width / 2;
        int centerZ = footprintMinZ + depth / 2;
        int[] sampledHeights = new int[]{
                getSurfaceY(generationContext, footprintMinX, footprintMinZ),
                getSurfaceY(generationContext, maxX, footprintMinZ),
                getSurfaceY(generationContext, footprintMinX, maxZ),
                getSurfaceY(generationContext, maxX, maxZ),
                getSurfaceY(generationContext, centerX, centerZ),
                getSurfaceY(generationContext, centerX, footprintMinZ),
                getSurfaceY(generationContext, centerX, maxZ),
                getSurfaceY(generationContext, footprintMinX, centerZ),
                getSurfaceY(generationContext, maxX, centerZ)
        };
        Arrays.sort(sampledHeights);
        return sampledHeights[sampledHeights.length / 2];
    }

    private static int getSurfaceY(GenerationContext generationContext, int x, int z) {
        return generationContext.chunkGenerator().getFirstOccupiedHeight(
                x,
                z,
                Heightmap.Types.WORLD_SURFACE_WG,
                generationContext.heightAccessor(),
                generationContext.randomState()
        );
    }
}

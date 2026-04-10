package com.leon.saintsdragons.server.world.structure;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.common.config.SaintsDragonsConfig;
import com.leon.saintsdragons.common.registry.ModEntities;
import com.leon.saintsdragons.common.registry.ModStructurePieces;
import com.leon.saintsdragons.server.entity.npc.IvyTheDragonMerchant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import javax.annotation.Nullable;

public class IvyHousePiece extends TemplateStructurePiece {
    public static final ResourceLocation TEMPLATE_ID = SaintsDragonsCommon.rl("ivy_house");
    private static final ResourceLocation CHEST_LOOT_TABLE = SaintsDragonsCommon.rl("chests/ivy_house_chest");
    private static final ResourceLocation BARREL_LOOT_TABLE = SaintsDragonsCommon.rl("chests/ivy_house_barrel");
    private static final String ROTATION_TAG = "Rotation";
    private static final String IVY_TAG = "Ivy";
    private boolean spawnedIvy;

    public IvyHousePiece(StructureTemplateManager structureTemplateManager, BlockPos blockPos, Rotation rotation) {
        super(
                ModStructurePieces.IVY_HOUSE.get(),
                0,
                structureTemplateManager,
                TEMPLATE_ID,
                TEMPLATE_ID.toString(),
                makeSettings(rotation),
                blockPos
        );
        this.spawnedIvy = false;
    }

    public IvyHousePiece(StructureTemplateManager structureTemplateManager, CompoundTag compoundTag) {
        super(
                ModStructurePieces.IVY_HOUSE.get(),
                compoundTag,
                structureTemplateManager,
                resourceLocation -> makeSettings(readRotation(compoundTag))
        );
        this.spawnedIvy = compoundTag.getBoolean(IVY_TAG);
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext structurePieceSerializationContext, CompoundTag compoundTag) {
        super.addAdditionalSaveData(structurePieceSerializationContext, compoundTag);
        compoundTag.putString(ROTATION_TAG, this.placeSettings.getRotation().name());
        compoundTag.putBoolean(IVY_TAG, this.spawnedIvy);
    }

    @Override
    protected void handleDataMarker(String metadata, BlockPos blockPos, ServerLevelAccessor serverLevelAccessor, RandomSource randomSource, BoundingBox boundingBox) {
    }

    @Override
    public void postProcess(WorldGenLevel worldGenLevel, StructureManager structureManager, ChunkGenerator chunkGenerator, RandomSource randomSource, BoundingBox boundingBox, ChunkPos chunkPos, BlockPos blockPos) {
        if (!SaintsDragonsConfig.isIvyHouseEnabled()) {
            return;
        }

        this.clearPlacementVolume(worldGenLevel, boundingBox);
        super.postProcess(worldGenLevel, structureManager, chunkGenerator, randomSource, boundingBox, chunkPos, blockPos);
        this.assignContainerLoot(worldGenLevel, boundingBox, randomSource);
        if (this.spawnedIvy) {
            return;
        }

        BlockPos spawnPos = this.findSpawnPosition(worldGenLevel, boundingBox);
        if (spawnPos == null) {
            return;
        }

        this.spawnedIvy = true;
        IvyTheDragonMerchant ivy = ModEntities.IVY_THE_DRAGON_MERCHANT.get().create(worldGenLevel.getLevel());
        if (ivy == null) {
            return;
        }

        ivy.setPersistenceRequired();
        ivy.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, randomSource.nextFloat() * 360.0F, 0.0F);
        ivy.finalizeSpawn(worldGenLevel, worldGenLevel.getCurrentDifficultyAt(spawnPos), MobSpawnType.STRUCTURE, null, null);
        worldGenLevel.addFreshEntityWithPassengers(ivy);
    }

    private static StructurePlaceSettings makeSettings(Rotation rotation) {
        return new StructurePlaceSettings()
                .setMirror(Mirror.NONE)
                .setRotation(rotation)
                .setIgnoreEntities(true)
                .setKeepLiquids(false);
    }

    private static Rotation readRotation(CompoundTag compoundTag) {
        if (!compoundTag.contains(ROTATION_TAG)) {
            return Rotation.NONE;
        }
        try {
            return Rotation.valueOf(compoundTag.getString(ROTATION_TAG));
        } catch (IllegalArgumentException exception) {
            return Rotation.NONE;
        }
    }

    @Nullable
    private BlockPos findSpawnPosition(WorldGenLevel worldGenLevel, BoundingBox chunkBoundingBox) {
        Vec3i size = this.template.getSize();
        if (size.getX() <= 0 || size.getY() <= 0 || size.getZ() <= 0) {
            return null;
        }

        int centerX = size.getX() / 2;
        int centerZ = size.getZ() / 2;
        BlockPos bestInterior = this.findBestCandidate(worldGenLevel, chunkBoundingBox, size, centerX, centerZ, true);
        if (bestInterior != null) {
            return bestInterior;
        }
        return this.findBestCandidate(worldGenLevel, chunkBoundingBox, size, centerX, centerZ, false);
    }

    @Nullable
    private BlockPos findBestCandidate(WorldGenLevel worldGenLevel, BoundingBox chunkBoundingBox, Vec3i size, int centerX, int centerZ, boolean requireRoof) {
        BlockPos bestPos = null;
        int bestScore = Integer.MAX_VALUE;
        int minY = 1;
        int maxY = Math.max(minY, size.getY() - 2);

        for (int y = minY; y <= maxY; y++) {
            for (int x = 0; x < size.getX(); x++) {
                for (int z = 0; z < size.getZ(); z++) {
                    BlockPos worldPos = this.toWorldPos(new BlockPos(x, y, z));
                    if (!chunkBoundingBox.isInside(worldPos)) {
                        continue;
                    }
                    if (!this.isValidSpawnBlock(worldGenLevel, worldPos, requireRoof)) {
                        continue;
                    }

                    int score = Math.abs(x - centerX) + Math.abs(z - centerZ) + Math.abs(y - minY);
                    if (score < bestScore) {
                        bestScore = score;
                        bestPos = worldPos;
                    }
                }
            }
        }

        return bestPos;
    }

    private BlockPos toWorldPos(BlockPos localPos) {
        return StructureTemplate.calculateRelativePosition(this.placeSettings, localPos).offset(this.templatePosition);
    }

    private void clearPlacementVolume(WorldGenLevel worldGenLevel, BoundingBox chunkBoundingBox) {
        Vec3i size = this.template.getSize();
        for (int y = 0; y < size.getY(); y++) {
            for (int x = 0; x < size.getX(); x++) {
                for (int z = 0; z < size.getZ(); z++) {
                    BlockPos worldPos = this.toWorldPos(new BlockPos(x, y, z));
                    if (!chunkBoundingBox.isInside(worldPos)) {
                        continue;
                    }
                    worldGenLevel.setBlock(worldPos, Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
    }

    private void assignContainerLoot(WorldGenLevel worldGenLevel, BoundingBox chunkBoundingBox, RandomSource randomSource) {
        Vec3i size = this.template.getSize();
        for (int y = 0; y < size.getY(); y++) {
            for (int x = 0; x < size.getX(); x++) {
                for (int z = 0; z < size.getZ(); z++) {
                    BlockPos worldPos = this.toWorldPos(new BlockPos(x, y, z));
                    if (!chunkBoundingBox.isInside(worldPos)) {
                        continue;
                    }

                    BlockState blockState = worldGenLevel.getBlockState(worldPos);
                    if (blockState.is(Blocks.CHEST)) {
                        RandomizableContainerBlockEntity.setLootTable(worldGenLevel, randomSource, worldPos, CHEST_LOOT_TABLE);
                    } else if (blockState.is(Blocks.BARREL)) {
                        RandomizableContainerBlockEntity.setLootTable(worldGenLevel, randomSource, worldPos, BARREL_LOOT_TABLE);
                    }
                }
            }
        }
    }

    private boolean isValidSpawnBlock(WorldGenLevel worldGenLevel, BlockPos blockPos, boolean requireRoof) {
        BlockPos belowPos = blockPos.below();
        BlockPos abovePos = blockPos.above();
        BlockState belowState = worldGenLevel.getBlockState(belowPos);
        BlockState feetState = worldGenLevel.getBlockState(blockPos);
        BlockState headState = worldGenLevel.getBlockState(abovePos);

        if (!belowState.isFaceSturdy(worldGenLevel, belowPos, Direction.UP)) {
            return false;
        }
        if (!feetState.getCollisionShape(worldGenLevel, blockPos).isEmpty()) {
            return false;
        }
        if (!headState.getCollisionShape(worldGenLevel, abovePos).isEmpty()) {
            return false;
        }
        if (!worldGenLevel.getFluidState(blockPos).isEmpty() || !worldGenLevel.getFluidState(abovePos).isEmpty()) {
            return false;
        }
        return !requireRoof || !worldGenLevel.canSeeSky(blockPos);
    }
}

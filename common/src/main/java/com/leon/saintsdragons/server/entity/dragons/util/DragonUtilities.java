package com.leon.saintsdragons.server.entity.dragons.util;

import com.leon.saintsdragons.common.SaintsDragonsCommon;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.dragons.cindervane.Cindervane;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class DragonUtilities {
    private static final String COOKING_PROGRESS_FIELD = "cookingProgress";
    private static final String COOKING_TOTAL_FIELD = "cookingTotalTime";
    private static final String LIT_TIME_FIELD = "litTime";
    private static final String LIT_DURATION_FIELD = "litDuration";

    private static Field cookingProgressField;
    private static Field cookingTotalField;
    private static Field litTimeField;
    private static Field litDurationField;

    private DragonUtilities() {
    }

    public static void accelerateCooking(ServerLevel level,
                                         @Nullable DragonEntity dragon,
                                         Vec3 impactPoint,
                                         double radius,
                                         int furnaceBoost,
                                         int smokerBoost,
                                         int blastBoost,
                                         int litTicks) {
        BlockPos center = BlockPos.containing(impactPoint);
        int r = (int) Math.ceil(radius);
        boolean cookedByDragon = false;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
            double dx = pos.getX() + 0.5D - impactPoint.x;
            double dy = pos.getY() + 0.5D - impactPoint.y;
            double dz = pos.getZ() + 0.5D - impactPoint.z;
            double distSq = dx * dx + dy * dy + dz * dz;
            if (distSq > radius * radius || !level.isLoaded(pos)) {
                continue;
            }

            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof AbstractFurnaceBlockEntity furnace)) {
                continue;
            }

            BlockState state = level.getBlockState(pos);
            if (!state.is(Blocks.FURNACE) && !state.is(Blocks.SMOKER) && !state.is(Blocks.BLAST_FURNACE)) {
                continue;
            }

            int total = getCookingTotalTime(furnace);
            if (total <= 0) {
                continue;
            }

            boolean changed = false;
            int litTime = getLitTime(furnace);
            if (litTime < litTicks) {
                setLitTime(furnace, litTicks);
                setLitDuration(furnace, litTicks);
                changed = true;
            }
            if (state.hasProperty(BlockStateProperties.LIT) && !state.getValue(BlockStateProperties.LIT)) {
                level.setBlock(pos, state.setValue(BlockStateProperties.LIT, true), 3);
                changed = true;
            }

            int progress = getCookingProgress(furnace);
            int boostTicks = state.is(Blocks.SMOKER)
                    ? smokerBoost
                    : (state.is(Blocks.BLAST_FURNACE) ? blastBoost : furnaceBoost);
            int boosted = Math.min(total - 1, progress + boostTicks);
            if (boosted > progress) {
                setCookingProgress(furnace, boosted);
                cookedByDragon |= boosted >= total - 1;
                changed = true;
            }
            if (changed) {
                blockEntity.setChanged();
            }
        }

        if (cookedByDragon) {
            awardFireCookingAdvancement(dragon);
        }
    }

    public static boolean extinguishFire(ServerLevel level, Vec3 start, Vec3 end, double radius) {
        Vec3 min = new Vec3(
                Math.min(start.x, end.x) - radius,
                Math.min(start.y, end.y) - radius,
                Math.min(start.z, end.z) - radius);
        Vec3 max = new Vec3(
                Math.max(start.x, end.x) + radius,
                Math.max(start.y, end.y) + radius,
                Math.max(start.z, end.z) + radius);

        BlockPos minPos = BlockPos.containing(min);
        BlockPos maxPos = BlockPos.containing(max);
        double radiusSqr = radius * radius;
        boolean extinguished = false;
        for (BlockPos pos : BlockPos.betweenClosed(minPos, maxPos)) {
            if (!level.isLoaded(pos)) {
                continue;
            }
            Vec3 center = Vec3.atCenterOf(pos);
            if (distanceToSegmentSqr(center, start, end) > radiusSqr) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) {
                level.removeBlock(pos, false);
                level.levelEvent(1009, pos, 0);
                extinguished = true;
            } else if (isExtinguishableLitBlock(state)) {
                level.setBlock(pos, state.setValue(BlockStateProperties.LIT, false), 11);
                level.levelEvent(1009, pos, 0);
                extinguished = true;
            }
        }
        return extinguished;
    }

    public static Set<BlockPos> applyLightningBeamImpact(ServerLevel level, Vec3 start, Vec3 impact, double radius) {
        Set<BlockPos> energizedWires = new HashSet<>();
        Vec3 segment = impact.subtract(start);
        double length = segment.length();
        if (length < 1.0E-4D) {
            return energizedWires;
        }

        double interactionRadius = Math.max(0.5D, radius);
        int neighborRange = Math.max(1, (int) Math.ceil(interactionRadius));
        int samples = Math.max(1, (int) Math.ceil(length / 0.5D));
        Set<BlockPos> visited = new HashSet<>();

        for (int sample = 0; sample <= samples; sample++) {
            Vec3 point = start.add(segment.scale((double) sample / samples));
            BlockPos center = BlockPos.containing(point);
            for (int x = -neighborRange; x <= neighborRange; x++) {
                for (int y = -neighborRange; y <= neighborRange; y++) {
                    for (int z = -neighborRange; z <= neighborRange; z++) {
                        BlockPos pos = center.offset(x, y, z);
                        if (!visited.add(pos) || !level.isLoaded(pos)) {
                            continue;
                        }
                        AABB interactionBounds = new AABB(pos).inflate(interactionRadius);
                        if (interactionBounds.clip(start, impact).isEmpty()
                                && !interactionBounds.contains(start)
                                && !interactionBounds.contains(impact)) {
                            continue;
                        }
                        if (applyLightningToBlock(level, pos)) {
                            energizedWires.add(pos);
                        }
                    }
                }
            }
        }
        return energizedWires;
    }

    public static void refreshLightningBeamRedstone(ServerLevel level, Iterable<BlockPos> positions) {
        for (BlockPos pos : positions) {
            if (!level.isLoaded(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof RedStoneWireBlock wire)) {
                continue;
            }
            wire.neighborChanged(state, level, pos, wire, pos, false);
            level.updateNeighborsAt(pos, wire);
        }
    }

    private static boolean applyLightningToBlock(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return false;
        }
        Block unwaxedBlock = HoneycombItem.WAX_OFF_BY_BLOCK.get().get(state.getBlock());
        boolean waxed = unwaxedBlock != null;
        BlockState oxidationState = waxed ? unwaxedBlock.withPropertiesOf(state) : state;
        BlockState cleanedState = WeatheringCopper.getFirst(oxidationState);
        boolean removedOxidation = !cleanedState.equals(oxidationState);
        if (removedOxidation) {
            if (waxed) {
                cleanedState = HoneycombItem.getWaxed(cleanedState).orElse(cleanedState);
            }
            level.setBlock(pos, cleanedState, 3);
            level.levelEvent(LevelEvent.PARTICLES_SCRAPE, pos, 0);
            state = cleanedState;
        }

        if (state.getBlock() instanceof RedStoneWireBlock) {
            return true;
        }

        if (!state.hasProperty(BlockStateProperties.POWERED)
                || state.getValue(BlockStateProperties.POWERED)) {
            return false;
        }

        if (state.getBlock() instanceof ButtonBlock button) {
            button.press(state, level, pos);
        } else if (state.getBlock() instanceof LeverBlock lever) {
            lever.pull(state, level, pos);
        } else if (state.getBlock() instanceof LightningRodBlock lightningRod) {
            lightningRod.onLightningStrike(state, level, pos);
        }
        return false;
    }

    public static void awardAdvancement(ServerPlayer player, String advancementId, String criterion) {
        var advancement = player.server.getAdvancements().getAdvancement(SaintsDragonsCommon.rl(advancementId));
        if (advancement != null) {
            player.getAdvancements().award(advancement, criterion);
        }
    }

    private static boolean isExtinguishableLitBlock(BlockState state) {
        return state.hasProperty(BlockStateProperties.LIT)
                && state.getValue(BlockStateProperties.LIT)
                && (state.getBlock() instanceof CampfireBlock
                || state.getBlock() instanceof CandleBlock
                || state.getBlock() instanceof CandleCakeBlock);
    }

    private static double distanceToSegmentSqr(Vec3 point, Vec3 start, Vec3 end) {
        Vec3 segment = end.subtract(start);
        double lengthSqr = segment.lengthSqr();
        if (lengthSqr < 1.0E-7D) {
            return point.distanceToSqr(start);
        }
        double t = point.subtract(start).dot(segment) / lengthSqr;
        t = Math.max(0.0D, Math.min(1.0D, t));
        Vec3 nearest = start.add(segment.scale(t));
        return point.distanceToSqr(nearest);
    }

    private static void awardFireCookingAdvancement(@Nullable DragonEntity dragon) {
        if (dragon == null || dragon.level().isClientSide) {
            return;
        }
        if (!(dragon instanceof Cindervane) && !(dragon instanceof Ignivorus)) {
            return;
        }

        ServerPlayer player = resolveResponsiblePlayer(dragon);
        if (player == null) {
            return;
        }

        awardAdvancement(player, "cook_with_fire_dragon", "cook_with_fire_dragon");
    }

    @Nullable
    public static ServerPlayer resolveResponsiblePlayer(DragonEntity dragon) {
        Entity passenger = dragon.getControllingPassenger();
        if (passenger instanceof ServerPlayer serverPlayer) {
            return serverPlayer;
        }
        LivingEntity owner = dragon.getOwner();
        if (owner instanceof ServerPlayer serverPlayer) {
            return serverPlayer;
        }
        UUID ownerUUID = dragon.getOwnerUUID();
        if (ownerUUID != null && dragon.level() instanceof ServerLevel serverLevel) {
            return serverLevel.getServer().getPlayerList().getPlayer(ownerUUID);
        }
        return null;
    }

    private static int getCookingProgress(AbstractFurnaceBlockEntity furnace) {
        Field field = resolveCookingProgressField();
        if (field == null) {
            return 0;
        }
        try {
            return field.getInt(furnace);
        } catch (IllegalAccessException ignored) {
            return 0;
        }
    }

    private static int getCookingTotalTime(AbstractFurnaceBlockEntity furnace) {
        Field field = resolveCookingTotalField();
        if (field == null) {
            return 0;
        }
        try {
            return field.getInt(furnace);
        } catch (IllegalAccessException ignored) {
            return 0;
        }
    }

    private static void setCookingProgress(AbstractFurnaceBlockEntity furnace, int value) {
        Field field = resolveCookingProgressField();
        if (field == null) {
            return;
        }
        try {
            field.setInt(furnace, value);
        } catch (IllegalAccessException ignored) {
        }
    }

    private static Field resolveCookingProgressField() {
        if (cookingProgressField != null) {
            return cookingProgressField;
        }
        cookingProgressField = resolveFurnaceField(COOKING_PROGRESS_FIELD);
        return cookingProgressField;
    }

    private static Field resolveCookingTotalField() {
        if (cookingTotalField != null) {
            return cookingTotalField;
        }
        cookingTotalField = resolveFurnaceField(COOKING_TOTAL_FIELD);
        return cookingTotalField;
    }

    private static int getLitTime(AbstractFurnaceBlockEntity furnace) {
        Field field = resolveLitTimeField();
        if (field == null) {
            return 0;
        }
        try {
            return field.getInt(furnace);
        } catch (IllegalAccessException ignored) {
            return 0;
        }
    }

    private static void setLitTime(AbstractFurnaceBlockEntity furnace, int value) {
        Field field = resolveLitTimeField();
        if (field == null) {
            return;
        }
        try {
            field.setInt(furnace, value);
        } catch (IllegalAccessException ignored) {
        }
    }

    private static void setLitDuration(AbstractFurnaceBlockEntity furnace, int value) {
        Field field = resolveLitDurationField();
        if (field == null) {
            return;
        }
        try {
            field.setInt(furnace, value);
        } catch (IllegalAccessException ignored) {
        }
    }

    private static Field resolveLitTimeField() {
        if (litTimeField != null) {
            return litTimeField;
        }
        litTimeField = resolveFurnaceField(LIT_TIME_FIELD);
        return litTimeField;
    }

    private static Field resolveLitDurationField() {
        if (litDurationField != null) {
            return litDurationField;
        }
        litDurationField = resolveFurnaceField(LIT_DURATION_FIELD);
        return litDurationField;
    }

    private static Field resolveFurnaceField(String name) {
        try {
            Field field = AbstractFurnaceBlockEntity.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException ignored) {
            return null;
        }
    }
}

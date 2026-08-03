package com.leon.saintsdragons.common.item;

import com.leon.saintsdragons.common.block.DraconianNucleusBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

public class DraconianControllerItem extends Item {
    private static final double ACTIVATION_RADIUS = 64.0D;
    private static final int COOLDOWN_TICKS = 40;

    public DraconianControllerItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("item.saintsdragons.draconian_controller.tooltip.passive.title")
                .withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.translatable("item.saintsdragons.draconian_controller.tooltip.ability.description")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.saintsdragons.draconian_controller.tooltip.requirement")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player,
                                                           @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        return useController(level, player)
                ? InteractionResultHolder.success(stack)
                : InteractionResultHolder.fail(stack);
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (context.getLevel().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        return useController(context.getLevel(), player)
                ? InteractionResult.SUCCESS
                : InteractionResult.FAIL;
    }

    @Override
    public @NotNull InteractionResult interactLivingEntity(@NotNull ItemStack stack, @NotNull Player player,
                                                           @NotNull LivingEntity interactionTarget,
                                                           @NotNull InteractionHand hand) {
        if (player.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        return useController(player.level(), player)
                ? InteractionResult.SUCCESS
                : InteractionResult.FAIL;
    }

    private boolean useController(Level level, Player player) {
        ServerLevel serverLevel = (ServerLevel) level;
        int affected = deactivateNearbyNuclei(serverLevel, player);
        if (affected <= 0 && !player.isShiftKeyDown()) {
            affected = activateNearbyNuclei(serverLevel, player);
        }
        if (affected <= 0) {
            return false;
        }

        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        return true;
    }

    private static int activateNearbyNuclei(ServerLevel level, Player player) {
        return useNearbyNuclei(level, player, false);
    }

    private static int deactivateNearbyNuclei(ServerLevel level, Player player) {
        return useNearbyNuclei(level, player, true);
    }

    private static int useNearbyNuclei(ServerLevel level, Player player, boolean deactivate) {
        int activated = 0;
        double radiusSqr = ACTIVATION_RADIUS * ACTIVATION_RADIUS;
        int centerChunkX = player.blockPosition().getX() >> 4;
        int centerChunkZ = player.blockPosition().getZ() >> 4;
        int chunkRadius = (int) Math.ceil(ACTIVATION_RADIUS / 16.0D);

        for (int chunkX = centerChunkX - chunkRadius; chunkX <= centerChunkX + chunkRadius; chunkX++) {
            for (int chunkZ = centerChunkZ - chunkRadius; chunkZ <= centerChunkZ + chunkRadius; chunkZ++) {
                ChunkAccess chunk = level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
                if (chunk == null) {
                    continue;
                }

                for (BlockPos blockPos : chunk.getBlockEntitiesPos()) {
                    if (blockPos.distToCenterSqr(player.position()) <= radiusSqr
                            && level.getBlockEntity(blockPos) instanceof DraconianNucleusBlockEntity nucleus) {
                        boolean changed = deactivate
                                ? nucleus.deactivateFromController(level, player)
                                : nucleus.activateFromController(player, level.getGameTime());
                        if (changed) {
                            activated++;
                        }
                    }
                }
            }
        }

        return activated;
    }
}

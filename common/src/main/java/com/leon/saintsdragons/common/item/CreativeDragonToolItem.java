package com.leon.saintsdragons.common.item;

import com.leon.saintsdragons.server.command.DragonSetGenderCommand;
import com.leon.saintsdragons.server.command.DragonSetVariantCommand;
import com.leon.saintsdragons.server.command.DragonTameCommand;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.DragonGender;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class CreativeDragonToolItem extends Item {
    public enum Action { GENDER, TAME, VARIANT }

    private final Action action;

    public CreativeDragonToolItem(Properties properties, Action action) {
        super(properties);
        this.action = action;
    }

    public static InteractionResult tryHandle(DragonEntity dragon, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof CreativeDragonToolItem tool)) return InteractionResult.PASS;
        return tool.interactLivingEntity(stack, player, dragon, hand);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof DragonEntity dragon)) return InteractionResult.PASS;
        if (!player.isCreative() || player.isSpectator() || !dragon.isAlive()) {
            if (!player.level().isClientSide && !player.isCreative()) {
                player.displayClientMessage(Component.translatable("item.saintsdragons.creative_dragon_tool.creative_only"), true);
            }
            return InteractionResult.FAIL;
        }
        if (!dragon.level().isClientSide) {
            try {
                Component message;
                switch (action) {
                    case GENDER -> {
                        DragonGender gender = dragon.isFemale() ? DragonGender.MALE : DragonGender.FEMALE;
                        DragonSetGenderCommand.applyGender(dragon, gender);
                        message = Component.translatable("saintsdragons.command.setgender.success", dragon.getDisplayName(),
                                Component.translatable("saintsdragons.gender." + (gender == DragonGender.MALE ? "male" : "female")));
                    }
                    case TAME -> {
                        DragonTameCommand.tameDragon(dragon, player);
                        message = Component.translatable("saintsdragons.command.tame.success", dragon.getDisplayName(), player.getDisplayName());
                    }
                    case VARIANT -> {
                        var names = dragon.getTextureVariantIdNameMap();
                        LinkedHashSet<ResourceLocation> ordered = new LinkedHashSet<>();
                        ResourceLocation defaultVariant = names.get("default");
                        if (defaultVariant != null) ordered.add(defaultVariant);
                        ordered.addAll(names.values()); // Namespaced aliases must not duplicate a variant.
                        List<ResourceLocation> variants = new ArrayList<>(ordered);
                        if (variants.isEmpty()) return InteractionResult.CONSUME;
                        int index = variants.indexOf(dragon.getCodexTextureVariantId());
                        ResourceLocation next = variants.get((index + 1) % variants.size());
                        DragonSetVariantCommand.applyVariant(dragon, next);
                        message = Component.translatable("saintsdragons.command.setvariant.success", dragon.getDisplayName(),
                                Component.translatable(dragon.getTextureVariantTranslationKey(next)));
                    }
                    default -> throw new IllegalStateException("Unknown creative dragon tool action");
                }
                player.displayClientMessage(message, true);
            } catch (CommandSyntaxException exception) {
                player.displayClientMessage(exception.getRawMessage() instanceof Component message
                        ? message : Component.literal(exception.getMessage()), true);
            }
        }
        return InteractionResult.sidedSuccess(dragon.level().isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.saintsdragons.creative_dragon_tool.creative_only").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
    }
}

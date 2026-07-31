package com.leon.saintsdragons.common.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class DraconianArmorItem extends ArmorItem {
    public DraconianArmorItem(ArmorMaterial material, Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("item.saintsdragons.draconian_armor.tooltip.passive.title")
                .withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.empty()
                .append(Component.translatable("item.saintsdragons.draconian_armor.tooltip.full_set")
                        .withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal(" "))
                .append(Component.translatable("item.saintsdragons.draconian_armor.tooltip.passive.description")
                        .withStyle(ChatFormatting.GRAY)));
    }
}

package com.leon.saintsdragons.common.item;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

public class FixedPotionItem extends PotionItem {
    private final Supplier<Potion> potion;

    public FixedPotionItem(Item.Properties properties, Supplier<Potion> potion) {
        super(properties);
        this.potion = potion;
    }

    private ItemStack ensurePotion(ItemStack stack) {
        if (PotionUtils.getPotion(stack) == Potions.EMPTY) {
            PotionUtils.setPotion(stack, this.potion.get());
        }
        return stack;
    }

    @Override
    public ItemStack getDefaultInstance() {
        return ensurePotion(super.getDefaultInstance());
    }

    @Override
    public String getDescriptionId(ItemStack stack) {
        return this.getDescriptionId();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ensurePotion(stack);
        return super.use(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        if (livingEntity instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
        }

        if (!level.isClientSide) {
            for (MobEffectInstance effectInstance : this.potion.get().getEffects()) {
                if (effectInstance.getEffect().isInstantenous()) {
                    effectInstance.getEffect().applyInstantenousEffect(
                            null,
                            null,
                            livingEntity,
                            effectInstance.getAmplifier(),
                            1.0D
                    );
                } else {
                    livingEntity.addEffect(new MobEffectInstance(effectInstance));
                }
            }
        }

        if (livingEntity instanceof Player player) {
            player.awardStat(Stats.ITEM_USED.get(this));
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }

            if (!player.getAbilities().instabuild) {
                ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
                if (stack.isEmpty()) {
                    return bottle;
                }
                player.getInventory().add(bottle);
            }
        } else {
            stack.shrink(1);
            if (stack.isEmpty()) {
                return new ItemStack(Items.GLASS_BOTTLE);
            }
        }

        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        PotionUtils.addPotionTooltip(this.potion.get().getEffects(), tooltipComponents, 1.0F);
    }
}

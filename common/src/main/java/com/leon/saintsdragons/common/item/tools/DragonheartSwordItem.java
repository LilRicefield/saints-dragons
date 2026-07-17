package com.leon.saintsdragons.common.item.tools;

import com.google.common.collect.Multimap;
import com.leon.saintsdragons.common.config.ToolsArmorConfig;
import com.leon.saintsdragons.common.item.ConfiguredItemAttributes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class DragonheartSwordItem extends SwordItem {
    public static final UUID ENTITY_REACH_MODIFIER_UUID = UUID.fromString("513fc6ee-03f7-4aa7-8f3b-6f8f7fd57d60");
    public static final UUID TARGETING_REACH_MODIFIER_UUID = UUID.fromString("f614ef20-d69e-4c41-8363-c7e9c9b106ec");
    public static final double VANILLA_ENTITY_REACH = 3.0D;
    public static final double VANILLA_BLOCK_REACH = 4.5D;

    private final double entityReach;
    private final float criticalDamageBonus;
    private final Tier tier;

    public DragonheartSwordItem(Tier tier,
                                int attackDamageModifier,
                                float attackSpeedModifier,
                                double entityReach,
                                float criticalDamageBonus,
                                Properties properties) {
        super(tier, attackDamageModifier, attackSpeedModifier, properties);
        this.entityReach = entityReach;
        this.criticalDamageBonus = criticalDamageBonus;
        this.tier = tier;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return ConfiguredItemAttributes.weapon(super.getDefaultAttributeModifiers(slot), slot,
                isBloodTempest() ? bloodTempestDamage() : dragonlordDamage(),
                isBloodTempest() ? bloodTempestSpeed() : dragonlordSpeed());
    }

    public double getEntityReach() {
        if (ToolsArmorConfig.BLOOD_TEMPEST_KATANA_REACH == null || ToolsArmorConfig.DRAGONLORD_SWORD_REACH == null) {
            return this.entityReach;
        }
        return isBloodTempest() ? ToolsArmorConfig.BLOOD_TEMPEST_KATANA_REACH.get() : ToolsArmorConfig.DRAGONLORD_SWORD_REACH.get();
    }

    public double getEntityReachBonus() {
        return getEntityReach() - VANILLA_ENTITY_REACH;
    }

    public double getTargetingReachBonus() {
        return getEntityReach() - VANILLA_BLOCK_REACH;
    }

    public float getCriticalDamageBonus() {
        if (ToolsArmorConfig.BLOOD_TEMPEST_KATANA_CRITICAL_BONUS == null
                || ToolsArmorConfig.DRAGONLORD_SWORD_CRITICAL_BONUS == null) {
            return this.criticalDamageBonus;
        }
        return (float) (isBloodTempest()
                ? ToolsArmorConfig.BLOOD_TEMPEST_KATANA_CRITICAL_BONUS.get()
                : ToolsArmorConfig.DRAGONLORD_SWORD_CRITICAL_BONUS.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        String key = isBloodTempest()
                ? "item.saintsdragons.blood_tempest_katana.tooltip"
                : "item.saintsdragons.dragonlord_sword.tooltip";
        tooltip.add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
    }

    private boolean isBloodTempest() {
        return this.tier == DragonheartWeaponTier.CHUNK;
    }

    private double bloodTempestDamage() {
        return ToolsArmorConfig.BLOOD_TEMPEST_KATANA_DAMAGE == null
                ? 1.0D + this.tier.getAttackDamageBonus() + 3.0D
                : ToolsArmorConfig.BLOOD_TEMPEST_KATANA_DAMAGE.get();
    }

    private double bloodTempestSpeed() {
        return ToolsArmorConfig.BLOOD_TEMPEST_KATANA_SPEED == null
                ? 3.0D
                : ToolsArmorConfig.BLOOD_TEMPEST_KATANA_SPEED.get();
    }

    private double dragonlordDamage() {
        return ToolsArmorConfig.DRAGONLORD_SWORD_DAMAGE == null
                ? 1.0D + this.tier.getAttackDamageBonus() + 5.0D
                : ToolsArmorConfig.DRAGONLORD_SWORD_DAMAGE.get();
    }

    private double dragonlordSpeed() {
        return ToolsArmorConfig.DRAGONLORD_SWORD_SPEED == null
                ? 1.4D
                : ToolsArmorConfig.DRAGONLORD_SWORD_SPEED.get();
    }
}

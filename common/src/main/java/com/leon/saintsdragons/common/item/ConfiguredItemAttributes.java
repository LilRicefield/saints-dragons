package com.leon.saintsdragons.common.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.UUID;

public final class ConfiguredItemAttributes {
    public static Multimap<Attribute, AttributeModifier> weapon(
            Multimap<Attribute, AttributeModifier> base,
            EquipmentSlot slot,
            double attackDamage,
            double attackSpeed
    ) {
        if (slot != EquipmentSlot.MAINHAND) {
            return base;
        }

        UUID damageUuid = modifierUuid(base, Attributes.ATTACK_DAMAGE, "attack damage");
        UUID speedUuid = modifierUuid(base, Attributes.ATTACK_SPEED, "attack speed");
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        base.entries().stream()
                .filter(entry -> entry.getKey() != Attributes.ATTACK_DAMAGE && entry.getKey() != Attributes.ATTACK_SPEED)
                .forEach(entry -> builder.put(entry.getKey(), entry.getValue()));
        builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                damageUuid,
                "Configured weapon damage",
                attackDamage - 1.0D,
                AttributeModifier.Operation.ADDITION
        ));
        builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(
                speedUuid,
                "Configured weapon speed",
                attackSpeed - 4.0D,
                AttributeModifier.Operation.ADDITION
        ));
        return builder.build();
    }

    public static Multimap<Attribute, AttributeModifier> armor(
            Multimap<Attribute, AttributeModifier> base,
            double armor,
            double toughness,
            double knockbackResistance
    ) {
        UUID slotUuid = base.get(Attributes.ARMOR).stream()
                .findFirst()
                .map(AttributeModifier::getId)
                .orElseThrow(() -> new IllegalStateException("Armor item is missing its slot modifier"));

        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        base.entries().stream()
                .filter(entry -> entry.getKey() != Attributes.ARMOR
                        && entry.getKey() != Attributes.ARMOR_TOUGHNESS
                        && entry.getKey() != Attributes.KNOCKBACK_RESISTANCE)
                .forEach(entry -> builder.put(entry.getKey(), entry.getValue()));
        builder.put(Attributes.ARMOR, new AttributeModifier(
                slotUuid, "Configured armor", armor, AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(
                slotUuid, "Configured armor toughness", toughness, AttributeModifier.Operation.ADDITION));
        if (knockbackResistance != 0.0D) {
            builder.put(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(
                    slotUuid, "Configured armor knockback resistance", knockbackResistance,
                    AttributeModifier.Operation.ADDITION));
        }
        return builder.build();
    }

    private static UUID modifierUuid(Multimap<Attribute, AttributeModifier> base,
                                     Attribute attribute,
                                     String description) {
        return base.get(attribute).stream()
                .findFirst()
                .map(AttributeModifier::getId)
                .orElseThrow(() -> new IllegalStateException("Weapon is missing its " + description + " modifier"));
    }

    private ConfiguredItemAttributes() {
    }
}

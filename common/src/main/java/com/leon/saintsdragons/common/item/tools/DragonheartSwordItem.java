package com.leon.saintsdragons.common.item.tools;

import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;

import java.util.UUID;

public class DragonheartSwordItem extends SwordItem {
    public static final UUID ENTITY_REACH_MODIFIER_UUID = UUID.fromString("513fc6ee-03f7-4aa7-8f3b-6f8f7fd57d60");
    public static final UUID TARGETING_REACH_MODIFIER_UUID = UUID.fromString("f614ef20-d69e-4c41-8363-c7e9c9b106ec");
    public static final double VANILLA_ENTITY_REACH = 3.0D;
    public static final double VANILLA_BLOCK_REACH = 4.5D;

    private final double entityReach;
    private final float criticalDamageBonus;

    public DragonheartSwordItem(Tier tier,
                                int attackDamageModifier,
                                float attackSpeedModifier,
                                double entityReach,
                                float criticalDamageBonus,
                                Properties properties) {
        super(tier, attackDamageModifier, attackSpeedModifier, properties);
        this.entityReach = entityReach;
        this.criticalDamageBonus = criticalDamageBonus;
    }

    public double getEntityReach() {
        return this.entityReach;
    }

    public double getEntityReachBonus() {
        return this.entityReach - VANILLA_ENTITY_REACH;
    }

    public double getTargetingReachBonus() {
        return this.entityReach - VANILLA_BLOCK_REACH;
    }

    public float getCriticalDamageBonus() {
        return this.criticalDamageBonus;
    }
}

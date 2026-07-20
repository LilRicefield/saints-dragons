package com.leon.saintsdragons.common.item.tools;

import com.leon.saintsdragons.common.config.ToolsArmorConfig;
import com.leon.saintsdragons.common.registry.ModItems;
import com.leon.saintsdragons.common.registry.ModTags;
import com.leon.saintsdragons.platform.ConfigHelper;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.entity.base.DragonPartEntity;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.entity.dragons.raevyx.Raevyx;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class DragonWeaponDamage {
    private DragonWeaponDamage() {
    }

    public static float applyDirectMeleeMultiplier(Player attacker, Entity struckEntity, float damage) {
        if (attacker.level().isClientSide || damage <= 0.0F) {
            return damage;
        }

        Entity target = dragonTarget(struckEntity);
        if (!(target instanceof DragonEntity)) {
            return damage;
        }

        ItemStack weapon = attacker.getMainHandItem();
        double multiplier = 1.0D;
        if (target instanceof Ignivorus && weapon.is(ModItems.DRAGONLORD_SWORD.get())) {
            multiplier = value(ToolsArmorConfig.DRAGONLORD_IGNIVORUS_DAMAGE_MULTIPLIER,
                    ToolsArmorConfig.DRAGONLORD_IGNIVORUS_DAMAGE_MULTIPLIER_DEFAULT);
        } else if (target instanceof Raevyx && weapon.is(ModItems.BLOOD_TEMPEST_KATANA.get())) {
            multiplier = value(ToolsArmorConfig.BLOOD_TEMPEST_RAEVYX_DAMAGE_MULTIPLIER,
                    ToolsArmorConfig.BLOOD_TEMPEST_RAEVYX_DAMAGE_MULTIPLIER_DEFAULT);
        } else if (weapon.is(ModTags.Items.WORLDROOT_TOOLS)) {
            multiplier = value(ToolsArmorConfig.WORLDROOT_DRAGON_DAMAGE_MULTIPLIER,
                    ToolsArmorConfig.WORLDROOT_DRAGON_DAMAGE_MULTIPLIER_DEFAULT);
        }
        return (float) (damage * multiplier);
    }

    private static Entity dragonTarget(Entity struckEntity) {
        if (struckEntity instanceof DragonPartEntity part && part.getDragonParent() != null) {
            return part.getDragonParent();
        }
        return struckEntity;
    }

    private static double value(ConfigHelper.DoubleValue configured, double fallback) {
        return configured == null ? fallback : configured.get();
    }
}

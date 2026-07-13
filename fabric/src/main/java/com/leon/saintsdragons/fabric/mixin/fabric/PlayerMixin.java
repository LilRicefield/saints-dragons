package com.leon.saintsdragons.fabric.mixin.fabric;

import com.leon.saintsdragons.common.item.tools.DragonheartSwordItem;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(Player.class)
public abstract class PlayerMixin {
    @ModifyConstant(method = "attack", constant = @Constant(floatValue = 1.5F))
    private float saintsdragons$applyWeaponCriticalDamage(float vanillaMultiplier) {
        Player player = (Player) (Object) this;
        if (player.getMainHandItem().getItem() instanceof DragonheartSwordItem sword) {
            return vanillaMultiplier + sword.getCriticalDamageBonus();
        }
        return vanillaMultiplier;
    }
}

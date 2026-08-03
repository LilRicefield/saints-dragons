package com.leon.saintsdragons.fabric.mixin.fabric;

import com.leon.saintsdragons.server.entity.dragons.util.DragonRedstonePowerManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RedStoneWireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RedStoneWireBlock.class)
public abstract class RedStoneWireBlockMixin {
    @Inject(method = "calculateTargetStrength", at = @At("HEAD"), cancellable = true)
    private void saintsdragons$applyRaevyxBeamPower(Level level,
                                                     BlockPos pos,
                                                     CallbackInfoReturnable<Integer> callback) {
        if (DragonRedstonePowerManager.isBeamPowered(level, pos)) {
            callback.setReturnValue(15);
        }
    }
}

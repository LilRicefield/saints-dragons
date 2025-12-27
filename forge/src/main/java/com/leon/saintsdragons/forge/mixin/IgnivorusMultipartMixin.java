package com.leon.saintsdragons.forge.mixin;

import com.leon.saintsdragons.forge.entity.part.ForgeIgnivorusPartManager;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.extensions.IForgeEntity;
import net.minecraftforge.entity.PartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to add Forge multi-part entity support to Ignivorus.
 * Implements IForgeEntity methods for multi-part entity handling.
 */
@Mixin(Ignivorus.class)
public abstract class IgnivorusMultipartMixin implements IForgeEntity {

    @Unique
    private ForgeIgnivorusPartManager saintsdragons$forgePartManager;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onConstruct(EntityType<?> type, Level level, CallbackInfo ci) {
        this.saintsdragons$forgePartManager = new ForgeIgnivorusPartManager((Ignivorus) (Object) this);
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void onTick(CallbackInfo ci) {
        if (this.saintsdragons$forgePartManager != null) {
            this.saintsdragons$forgePartManager.updatePartPositions();
        }
    }

    @Override
    public boolean isMultipartEntity() {
        boolean result = this.saintsdragons$forgePartManager != null;
        return result;
    }

    @Override
    public PartEntity<?>[] getParts() {
        if (this.saintsdragons$forgePartManager == null) {
            return null;
        }
        return this.saintsdragons$forgePartManager.getParts();
    }
}

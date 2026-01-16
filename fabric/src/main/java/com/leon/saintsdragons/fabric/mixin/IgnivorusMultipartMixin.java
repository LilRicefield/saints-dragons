package com.leon.saintsdragons.fabric.mixin;

import com.leon.saintsdragons.fabric.entity.part.FabricDragonPart;
import com.leon.saintsdragons.fabric.entity.part.FabricIgnivorusPartManager;
import com.leon.saintsdragons.fabric.entity.part.IgnivorusPartProvider;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to add Fabric multi-part entity support to Ignivorus.
 * Manages the creation and updating of hitbox parts.
 */
@Mixin(Ignivorus.class)
public abstract class IgnivorusMultipartMixin implements IgnivorusPartProvider {

    @Unique
    private FabricIgnivorusPartManager saintsdragons$fabricPartManager;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onConstruct(EntityType<?> type, Level level, CallbackInfo ci) {
        this.saintsdragons$fabricPartManager = new FabricIgnivorusPartManager((Ignivorus) (Object) this);
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void onTick(CallbackInfo ci) {
        if (this.saintsdragons$fabricPartManager != null) {
            if (((Ignivorus) (Object) this).isBaby()) {
                this.saintsdragons$fabricPartManager.removeAllParts();
                return;
            }
            this.saintsdragons$fabricPartManager.updatePartPositions();
        }
    }

    @Inject(method = "remove", at = @At("HEAD"))
    private void onRemove(net.minecraft.world.entity.Entity.RemovalReason reason, CallbackInfo ci) {
        if (this.saintsdragons$fabricPartManager != null) {
            this.saintsdragons$fabricPartManager.removeAllParts();
        }
    }

    /**
     * Expose the parts array for collision/damage detection
     */
    @Override
    public FabricDragonPart[] saintsdragons$getParts() {
        if (this.saintsdragons$fabricPartManager == null) {
            return new FabricDragonPart[0];
        }
        if (((Ignivorus) (Object) this).isBaby()) {
            return new FabricDragonPart[0];
        }
        return this.saintsdragons$fabricPartManager.getParts();
    }
}

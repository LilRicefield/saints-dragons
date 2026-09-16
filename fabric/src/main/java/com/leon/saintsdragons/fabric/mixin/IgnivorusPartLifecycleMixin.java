package com.leon.saintsdragons.fabric.mixin;

import com.leon.saintsdragons.fabric.entity.part.FabricDragonPart;
import com.leon.saintsdragons.fabric.entity.part.FabricIgnivorusPartManager;
import com.leon.saintsdragons.fabric.entity.part.IgnivorusPartProvider;
import com.leon.saintsdragons.server.entity.dragons.ignivorus.Ignivorus;
import com.leon.saintsdragons.server.entity.part.DragonPartProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Ignivorus.class)
public abstract class IgnivorusPartLifecycleMixin implements IgnivorusPartProvider, DragonPartProvider {

    @Unique
    private FabricIgnivorusPartManager saintsdragons$fabricPartManager;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onConstruct(EntityType<?> type, Level level, CallbackInfo ci) {
        this.saintsdragons$fabricPartManager = new FabricIgnivorusPartManager((Ignivorus) (Object) this);
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void onTick(CallbackInfo ci) {
        saintsdragons$refreshParts();
    }

    @Override
    public void saintsdragons$refreshParts() {
        if (this.saintsdragons$fabricPartManager != null) {
            this.saintsdragons$fabricPartManager.updatePartPositions();
        }
    }

    @Inject(method = "remove", at = @At("HEAD"))
    private void onRemove(Entity.RemovalReason reason, CallbackInfo ci) {
        if (this.saintsdragons$fabricPartManager != null) {
            this.saintsdragons$fabricPartManager.removeAllParts();
        }
    }

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

    @Override
    public Entity[] dragonParts() { return saintsdragons$getParts(); }
}

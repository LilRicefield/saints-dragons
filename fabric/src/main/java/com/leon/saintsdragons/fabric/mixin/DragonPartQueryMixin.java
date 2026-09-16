package com.leon.saintsdragons.fabric.mixin;

import com.leon.saintsdragons.fabric.entity.part.FabricDragonPartIndex;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Predicate;

@Mixin(Level.class)
public abstract class DragonPartQueryMixin implements FabricDragonPartIndex.Access {
    @Unique
    private FabricDragonPartIndex saintsdragons$partIndex;

    @Override
    public FabricDragonPartIndex saintsdragons$partIndex() {
        if (saintsdragons$partIndex == null) {
            saintsdragons$partIndex = new FabricDragonPartIndex((Level) (Object) this);
        }
        return saintsdragons$partIndex;
    }

    @Inject(method = "getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;",
            at = @At("RETURN"))
    private void saintsdragons$includeParts(Entity except, AABB area, Predicate<? super Entity> predicate,
                                            CallbackInfoReturnable<List<Entity>> callback) {
        Level level = (Level) (Object) this;
        if (level.isClientSide || saintsdragons$partIndex == null) return;
        saintsdragons$partIndex.append(except, area, predicate, callback.getReturnValue());
    }
}

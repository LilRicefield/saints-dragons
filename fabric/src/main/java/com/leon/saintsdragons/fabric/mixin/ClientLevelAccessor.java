package com.leon.saintsdragons.fabric.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Environment(EnvType.CLIENT)
@Mixin(ClientLevel.class)
public interface ClientLevelAccessor {
    @Invoker("addEntity")
    void saintsdragons$addEntity(int entityId, Entity entity);

    @Invoker("removeEntity")
    void saintsdragons$removeEntity(int entityId, Entity.RemovalReason reason);
}

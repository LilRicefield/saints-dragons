package com.leon.saintsdragons.forge.mixin;

import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessor mixin to expose the private entityId field from ServerboundInteractPacket.
 * This is needed to properly look up PartEntities on the server side.
 */
@Mixin(ServerboundInteractPacket.class)
public interface ServerboundInteractPacketAccessor {

    @Accessor("entityId")
    int getEntityId();
}

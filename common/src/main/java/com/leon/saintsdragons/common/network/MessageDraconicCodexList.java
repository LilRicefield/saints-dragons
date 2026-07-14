package com.leon.saintsdragons.common.network;

import com.leon.saintsdragons.platform.Services;
import com.leon.saintsdragons.common.registry.Dragons;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import com.leon.saintsdragons.server.data.DragonCodexSavedData;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

public record MessageDraconicCodexList(List<Entry> entries) {
    public MessageDraconicCodexList {
        entries = List.copyOf(entries);
    }

    public static MessageDraconicCodexList fromDragons(List<DragonEntity> dragons) {
        List<Entry> entries = new ArrayList<>(dragons.size());
        for (DragonEntity dragon : dragons) {
            String dragonType = getDragonTypeName(dragon);
            entries.add(new Entry(
                    dragon.getUUID(),
                    dragon.getName().getString(),
                    dragon.getHealth(),
                    dragon.getMaxHealth(),
                    dragon.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR),
                    dragon.getHunger(),
                    dragon.getHappiness(),
                    dragon.getCodexTextureVariant(),
                    dragon.getCodexTextureVariantId().toString(),
                    dragon.getGender().getId(),
                    dragon.hasGender(),
                    dragonType,
                    dragon.isBaby(),
                    dragon.isBrushingAvailable(),
                    dragon.getBrushingProgressPercent(),
                    dragon.getX(),
                    dragon.getY(),
                    dragon.getZ(),
                    dragon.level().getBiome(dragon.blockPosition())
                            .unwrapKey()
                            .map(key -> key.location().toString())
                            .orElse("minecraft:unknown")
            ));
        }
        return new MessageDraconicCodexList(entries);
    }

    private static String getDragonTypeName(DragonEntity dragon) {
        Dragons type = Dragons.fromEntity(dragon);
        if (type != null) {
            return type.getName();
        }
        return "ignivorus"; // Default fallback
    }

    public static MessageDraconicCodexList fromEntries(List<DragonCodexSavedData.DragonCodexEntry> codexEntries) {
        List<Entry> entries = new ArrayList<>(codexEntries.size());
        for (DragonCodexSavedData.DragonCodexEntry entry : codexEntries) {
            entries.add(new Entry(
                    entry.dragonId(),
                    entry.displayName(),
                    entry.currentHealth(),
                    entry.maxHealth(),
                    entry.armor(),
                    entry.hunger(),
                    entry.happiness(),
                    entry.variantId(),
                    entry.variantResourceId(),
                    entry.genderId(),
                    entry.genderKnown(),
                    entry.dragonType(),
                    entry.isBaby(),
                    entry.brushingAvailable(),
                    entry.brushingProgressPercent(),
                    entry.posX(),
                    entry.posY(),
                    entry.posZ(),
                    entry.biomeId()
            ));
        }
        return new MessageDraconicCodexList(entries);
    }

    public static void encode(MessageDraconicCodexList message, FriendlyByteBuf buffer) {
        buffer.writeInt(message.entries().size());
        for (Entry entry : message.entries()) {
            buffer.writeUUID(entry.entityId());
            buffer.writeUtf(entry.displayName(), 64);
            buffer.writeDouble(entry.currentHealth());
            buffer.writeDouble(entry.maxHealth());
            buffer.writeDouble(entry.armor());
            buffer.writeDouble(entry.hunger());
            buffer.writeDouble(entry.happiness());
            buffer.writeInt(entry.variantId());
            buffer.writeUtf(entry.variantResourceId(), 128);
            buffer.writeByte(entry.genderId());
            buffer.writeBoolean(entry.genderKnown());
            buffer.writeUtf(entry.dragonType(), 32);
            buffer.writeBoolean(entry.isBaby());
            buffer.writeBoolean(entry.brushingAvailable());
            buffer.writeVarInt(entry.brushingProgressPercent());
            buffer.writeDouble(entry.posX());
            buffer.writeDouble(entry.posY());
            buffer.writeDouble(entry.posZ());
            buffer.writeUtf(entry.biomeId(), 128);
        }
    }

    public static MessageDraconicCodexList decode(FriendlyByteBuf buffer) {
        int size = buffer.readInt();
        List<Entry> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            java.util.UUID id = buffer.readUUID();
            String name = buffer.readUtf(64);
            double currentHealth = buffer.readDouble();
            double maxHealth = buffer.readDouble();
            double armor = buffer.readDouble();
            double hunger = buffer.readDouble();
            double happiness = buffer.readDouble();
            int variantId = buffer.readInt();
            String variantResourceId = buffer.readUtf(128);
            byte genderId = buffer.readByte();
            boolean genderKnown = buffer.readBoolean();
            String dragonType = buffer.readUtf(32);
            boolean isBaby = buffer.readBoolean();
            boolean brushingAvailable = buffer.readBoolean();
            int brushingProgressPercent = buffer.readVarInt();
            double posX = buffer.readDouble();
            double posY = buffer.readDouble();
            double posZ = buffer.readDouble();
            String biomeId = buffer.readUtf(128);
            entries.add(new Entry(id, name, currentHealth, maxHealth, armor, hunger, happiness,
                    variantId, variantResourceId, genderId, genderKnown, dragonType, isBaby,
                    brushingAvailable, brushingProgressPercent, posX, posY, posZ, biomeId));
        }
        return new MessageDraconicCodexList(entries);
    }

    public static void handle(MessageDraconicCodexList message) {
        Services.PLATFORM.runOnClient(() -> com.leon.saintsdragons.client.network.ClientPacketHandlers.handleDraconicCodexList(message));
    }

    public record Entry(java.util.UUID entityId, String displayName, double currentHealth, double maxHealth,
                        double armor, double hunger, double happiness, int variantId, String variantResourceId, byte genderId,
                        boolean genderKnown, String dragonType, boolean isBaby,
                        boolean brushingAvailable, int brushingProgressPercent,
                        double posX, double posY, double posZ, String biomeId) {
    }
}

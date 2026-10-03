package com.leon.saintsdragons.common.network;

import com.leon.saintsdragons.client.network.ClientPacketHandlers;
import com.leon.saintsdragons.platform.Services;
import com.leon.saintsdragons.server.ai.dragonbrain.perception.DragonVision;
import com.leon.saintsdragons.server.ai.dragonbrain.perception.DragonVisionProfile;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public record MessageDragonVisionDebug(boolean active, int entityId, int layers,
                                      Vec3 eye, Vec3 forward, DragonVisionProfile profile,
                                      List<DragonVision.Ray> rays, List<DragonVision.Marker> markers,
                                      @Nullable DragonVision.ProjectileEvidence projectile) {
    public static final int SHAPE = 1, RAYS = 2, AWARENESS = 4, PROJECTILES = 8, ALL = 15;

    public MessageDragonVisionDebug {
        rays = List.copyOf(rays);
        markers = List.copyOf(markers);
    }

    public static MessageDragonVisionDebug capture(DragonEntity dragon, int layers) {
        DragonVision vision = dragon.getVision();
        if ((layers & RAYS) != 0) vision.enableDebug(dragon.level().getGameTime());
        return new MessageDragonVisionDebug(true, dragon.getId(), layers, vision.eye(), vision.forward(),
                vision.profile(), (layers & RAYS) != 0 ? vision.rays() : List.of(),
                (layers & AWARENESS) != 0 ? vision.markers() : List.of(),
                (layers & PROJECTILES) != 0 ? vision.projectileEvidence() : null);
    }

    public static MessageDragonVisionDebug clear() {
        return new MessageDragonVisionDebug(false, -1, 0, Vec3.ZERO, new Vec3(0, 0, 1),
                DragonVisionProfile.standard(32, 2), List.of(), List.of(), null);
    }

    public boolean shows(int layer) { return (layers & layer) != 0; }

    public static void encode(MessageDragonVisionDebug message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.active);
        if (!message.active) return;
        buffer.writeVarInt(message.entityId);
        buffer.writeVarInt(message.layers);
        writeVec(buffer, message.eye);
        writeVec(buffer, message.forward);
        DragonVisionProfile profile = message.profile;
        buffer.writeDouble(profile.range());
        buffer.writeDouble(profile.nearDistance());
        buffer.writeDouble(profile.nearHorizontal());
        buffer.writeDouble(profile.farHorizontal());
        buffer.writeDouble(profile.nearVertical());
        buffer.writeDouble(profile.farVertical());
        buffer.writeVarInt(message.rays.size());
        for (DragonVision.Ray ray : message.rays) {
            writeVec(buffer, ray.from());
            writeVec(buffer, ray.to());
            buffer.writeBoolean(ray.clear());
            buffer.writeLong(ray.tick());
        }
        buffer.writeVarInt(message.markers.size());
        for (DragonVision.Marker marker : message.markers) {
            writeVec(buffer, marker.position());
            buffer.writeFloat(marker.awareness());
            buffer.writeBoolean(marker.recognized());
            buffer.writeUtf(marker.reason(), 32);
            buffer.writeVarLong(marker.age());
        }
        buffer.writeBoolean(message.projectile != null);
        if (message.projectile != null) {
            DragonVision.ProjectileEvidence p = message.projectile;
            writeVec(buffer, p.previous());
            writeVec(buffer, p.observed());
            writeVec(buffer, p.predicted());
            writeVec(buffer, p.origin());
            writeVec(buffer, p.direction());
            buffer.writeDouble(p.uncertainty());
            buffer.writeFloat(p.confidence());
            buffer.writeDouble(p.impactTicks());
            buffer.writeLong(p.tick());
        }
    }

    public static MessageDragonVisionDebug decode(FriendlyByteBuf buffer) {
        if (!buffer.readBoolean()) return clear();
        int entityId = buffer.readVarInt(), layers = buffer.readVarInt();
        Vec3 eye = readVec(buffer), forward = readVec(buffer);
        DragonVisionProfile profile = new DragonVisionProfile(buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        List<DragonVision.Ray> rays = new ArrayList<>();
        for (int i = 0, size = readSize(buffer, 64); i < size; i++) {
            rays.add(new DragonVision.Ray(readVec(buffer), readVec(buffer), buffer.readBoolean(), buffer.readLong()));
        }
        List<DragonVision.Marker> markers = new ArrayList<>();
        for (int i = 0, size = readSize(buffer, 32); i < size; i++) {
            markers.add(new DragonVision.Marker(readVec(buffer), buffer.readFloat(), buffer.readBoolean(),
                    buffer.readUtf(32), buffer.readVarLong()));
        }
        DragonVision.ProjectileEvidence projectile = buffer.readBoolean()
                ? new DragonVision.ProjectileEvidence(readVec(buffer), readVec(buffer), readVec(buffer),
                readVec(buffer), readVec(buffer), buffer.readDouble(), buffer.readFloat(),
                buffer.readDouble(), buffer.readLong()) : null;
        return new MessageDragonVisionDebug(true, entityId, layers, eye, forward, profile, rays, markers, projectile);
    }

    public static void handle(MessageDragonVisionDebug message) {
        Services.PLATFORM.runOnClient(() -> ClientPacketHandlers.handleDragonVisionDebug(message));
    }

    private static int readSize(FriendlyByteBuf buffer, int max) {
        int size = buffer.readVarInt();
        if (size < 0 || size > max) throw new IllegalArgumentException("Invalid dragon vision debug count: " + size);
        return size;
    }

    private static void writeVec(FriendlyByteBuf buffer, Vec3 position) {
        buffer.writeDouble(position.x);
        buffer.writeDouble(position.y);
        buffer.writeDouble(position.z);
    }

    private static Vec3 readVec(FriendlyByteBuf buffer) {
        return new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }
}

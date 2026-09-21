package com.leon.saintsdragons.server.ai.dragonbrain;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public final class DragonSiteReservations {
    private static final int MAX_RESERVATIONS = 512;
    private static final Map<ServerLevel, Map<Site, Reservation>> LEVELS = new WeakHashMap<>();

    private DragonSiteReservations() {
    }

    @Nullable
    public static Ticket claim(ServerLevel level, ResourceLocation purpose, BlockPos position, UUID owner, int ttlTicks) {
        requireServerThread(level);
        if (ttlTicks <= 0 || !DragonDestinationMemory.loaded(level, position)) return null;
        Map<Site, Reservation> reservations = LEVELS.computeIfAbsent(level, ignored -> new HashMap<>());
        long now = level.getGameTime();
        reservations.values().removeIf(reservation -> now >= reservation.expiresAt);
        Site site = new Site(purpose, position.immutable());
        Reservation existing = reservations.get(site);
        if (existing != null && !existing.owner.equals(owner)) return null;
        if (existing == null && reservations.size() >= MAX_RESERVATIONS) return null;
        Reservation reservation = new Reservation(owner, now + ttlTicks);
        reservations.put(site, reservation);
        return new Ticket(level, site, reservation, ttlTicks);
    }

    private static void requireServerThread(ServerLevel level) {
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Site reservations require the server thread");
    }

    public static final class Ticket {
        private final WeakReference<ServerLevel> level;
        private final Site site;
        private final Reservation reservation;
        private final int ttlTicks;

        private Ticket(ServerLevel level, Site site, Reservation reservation, int ttlTicks) {
            this.level = new WeakReference<>(level);
            this.site = site;
            this.reservation = reservation;
            this.ttlTicks = ttlTicks;
        }

        public boolean renew() {
            ServerLevel currentLevel = level.get();
            if (currentLevel == null) return false;
            requireServerThread(currentLevel);
            Map<Site, Reservation> reservations = LEVELS.get(currentLevel);
            if (reservations == null || reservations.get(site) != reservation
                    || currentLevel.getGameTime() >= reservation.expiresAt) return false;
            reservation.expiresAt = currentLevel.getGameTime() + ttlTicks;
            return true;
        }

        public void release() {
            ServerLevel currentLevel = level.get();
            if (currentLevel == null) return;
            requireServerThread(currentLevel);
            Map<Site, Reservation> reservations = LEVELS.get(currentLevel);
            if (reservations != null) reservations.remove(site, reservation);
        }
    }

    private record Site(ResourceLocation purpose, BlockPos position) {
    }

    private static final class Reservation {
        private final UUID owner;
        private long expiresAt;

        private Reservation(UUID owner, long expiresAt) {
            this.owner = owner;
            this.expiresAt = expiresAt;
        }
    }
}

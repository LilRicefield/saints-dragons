package com.leon.saintsdragons.client.renderer.vfx;

import com.leon.saintsdragons.common.network.BloodTempestAfterimageProfile;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Environment(EnvType.CLIENT)
public final class BloodTempestAfterimageTrail {
    private static final double MIN_RENDER_DISTANCE_SQUARED = 0.35D * 0.35D;

    private static final Map<Integer, Trail> TRAILS = new HashMap<>();
    private static Level activeLevel;

    private BloodTempestAfterimageTrail() {
    }

    public static void start(int entityId, BloodTempestAfterimageProfile profile,
                             Vec3 origin, Vec3 destination) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || profile == null) {
            return;
        }

        Entity entity = minecraft.level.getEntity(entityId);
        if (!(entity instanceof AbstractClientPlayer player)) {
            return;
        }

        ProfileSpec profileSpec = ProfileSpec.forProfile(profile);
        Trail trail = origin != null && destination != null
                ? Trail.fixedPath(origin, destination, profileSpec)
                : new Trail(player.position(), profileSpec);
        TRAILS.put(entityId, trail);
        activeLevel = minecraft.level;
    }

    public static void tick(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.level != activeLevel) {
            TRAILS.clear();
            activeLevel = minecraft.level;
            return;
        }

        Iterator<Map.Entry<Integer, Trail>> trails = TRAILS.entrySet().iterator();
        while (trails.hasNext()) {
            Map.Entry<Integer, Trail> entry = trails.next();
            Entity entity = minecraft.level.getEntity(entry.getKey());
            if (!(entity instanceof AbstractClientPlayer player) || !player.isAlive()) {
                trails.remove();
                continue;
            }

            Trail trail = entry.getValue();
            trail.tick(player.position());
            if (trail.isFinished()) {
                trails.remove();
            }
        }
    }

    public static List<RenderedSnapshot> snapshotsFor(AbstractClientPlayer player, float partialTick) {
        Trail trail = TRAILS.get(player.getId());
        if (trail == null) {
            return List.of();
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (player == minecraft.player && minecraft.options.getCameraType() == CameraType.FIRST_PERSON) {
            return List.of();
        }

        Vec3 renderedPosition = interpolatedPosition(player, partialTick);
        return trail.renderedSnapshots(renderedPosition, partialTick);
    }

    private static Vec3 interpolatedPosition(AbstractClientPlayer player, float partialTick) {
        return new Vec3(
                player.xo + (player.getX() - player.xo) * partialTick,
                player.yo + (player.getY() - player.yo) * partialTick,
                player.zo + (player.getZ() - player.zo) * partialTick
        );
    }

    public record RenderedSnapshot(Vec3 offset, float alpha) {
    }

    private record SettledSnapshot(Vec3 position, float alpha) {
    }

    private static final class Trail {
        private final ArrayDeque<Vec3> history = new ArrayDeque<>();
        private final List<SettledSnapshot> settled;
        private final ProfileSpec profile;
        private int captureTicks;
        private int settleTicks;

        private Trail(Vec3 initialPosition, ProfileSpec profile) {
            this.profile = profile;
            this.captureTicks = profile.captureTicks;
            this.settleTicks = profile.settleTicks;
            this.settled = new ArrayList<>(profile.echoAlpha.length);
            for (int i = 0; i < profile.historyLimit(); i++) {
                this.history.addLast(initialPosition);
            }
        }

        private static Trail fixedPath(Vec3 origin, Vec3 destination, ProfileSpec profile) {
            Trail trail = new Trail(origin, profile);
            trail.captureTicks = 0;
            trail.settled.clear();
            int echoCount = profile.echoAlpha.length;
            for (int delay = 1; delay <= echoCount; delay++) {
                double progress = 1.0D - delay / (double) (echoCount + 1);
                trail.settled.add(new SettledSnapshot(
                        origin.lerp(destination, progress), profile.echoAlpha[delay - 1]));
            }
            return trail;
        }

        private void tick(Vec3 position) {
            if (this.captureTicks > 0) {
                this.history.addLast(position);
                while (this.history.size() > this.profile.historyLimit()) {
                    this.history.removeFirst();
                }

                this.captureTicks--;
                if (this.captureTicks == 0) {
                    settle();
                }
                return;
            }

            if (this.settleTicks > 0) {
                this.settleTicks--;
            }
        }

        private List<RenderedSnapshot> renderedSnapshots(Vec3 renderedPosition, float partialTick) {
            if (this.captureTicks > 0) {
                return renderMovingEchoes(renderedPosition, partialTick);
            }

            if (this.settleTicks <= 0 || this.settled.isEmpty()) {
                return List.of();
            }

            float fade = this.settleTicks / (float) this.profile.settleTicks;
            List<RenderedSnapshot> rendered = new ArrayList<>(this.settled.size());
            for (SettledSnapshot snapshot : this.settled) {
                addIfVisible(rendered, snapshot.position, renderedPosition, snapshot.alpha * fade);
            }
            return rendered;
        }

        private List<RenderedSnapshot> renderMovingEchoes(Vec3 renderedPosition, float partialTick) {
            List<Vec3> positions = new ArrayList<>(this.history);
            List<RenderedSnapshot> rendered = new ArrayList<>(this.profile.echoAlpha.length);
            for (int delay = 1; delay <= this.profile.echoAlpha.length; delay++) {
                int toIndex = positions.size() - 1 - delay;
                int fromIndex = toIndex - 1;
                if (fromIndex < 0) {
                    break;
                }

                Vec3 ghostPosition = positions.get(fromIndex).lerp(positions.get(toIndex), partialTick);
                addIfVisible(rendered, ghostPosition, renderedPosition, this.profile.echoAlpha[delay - 1]);
            }
            return rendered;
        }

        private void settle() {
            List<Vec3> positions = new ArrayList<>(this.history);
            this.settled.clear();
            for (int delay = 1; delay <= this.profile.echoAlpha.length; delay++) {
                int index = positions.size() - 1 - delay;
                if (index < 0) {
                    break;
                }
                this.settled.add(new SettledSnapshot(
                        positions.get(index), this.profile.echoAlpha[delay - 1]));
            }
        }

        private boolean isFinished() {
            return this.captureTicks <= 0 && this.settleTicks <= 0;
        }

        private static void addIfVisible(List<RenderedSnapshot> rendered, Vec3 ghostPosition,
                                         Vec3 renderedPosition, float alpha) {
            Vec3 offset = ghostPosition.subtract(renderedPosition);
            if (offset.lengthSqr() >= MIN_RENDER_DISTANCE_SQUARED && alpha > 0.02F) {
                rendered.add(new RenderedSnapshot(offset, alpha));
            }
        }
    }

    private record ProfileSpec(int captureTicks, int settleTicks, float[] echoAlpha) {
        private static final ProfileSpec ARMOR_DODGE = new ProfileSpec(
                6, 8, new float[]{0.42F, 0.27F, 0.14F});
        private static final ProfileSpec KATANA_DASH = new ProfileSpec(
                0, 8, new float[]{0.58F, 0.43F, 0.30F, 0.18F, 0.10F});

        private static ProfileSpec forProfile(BloodTempestAfterimageProfile profile) {
            return switch (profile) {
                case ARMOR_DODGE -> ARMOR_DODGE;
                case KATANA_DASH -> KATANA_DASH;
            };
        }

        private int historyLimit() {
            return this.echoAlpha.length + 3;
        }
    }
}

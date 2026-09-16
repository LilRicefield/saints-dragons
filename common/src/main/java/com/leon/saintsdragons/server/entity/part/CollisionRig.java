package com.leon.saintsdragons.server.entity.part;

import com.google.gson.*;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.DoubleUnaryOperator;

public final class CollisionRig {
    public record Bone(String name, String parent, Vec3 pivot, Vec3 rotation, List<Vector3f[]> cubes) {}
    public record Clip(double length, boolean loop, boolean hold, List<Channel> channels) {}
    public record Channel(int bone, int component, Track track) {}
    private record Key(double time, DoubleUnaryOperator[] pre, DoubleUnaryOperator[] post, boolean smooth) {}

    public static final class Track {
        private final Key[] keys;
        private final float[] samples;
        private final double duration;
        private static final int SAMPLE_RATE = 40;
        private Track(Key[] keys) { this.keys = keys; this.samples = null; this.duration = keys[keys.length - 1].time; }
        private Track(float[] samples, double duration) { this.keys = null; this.samples = samples; this.duration = duration; }

        Track bake() {
            if (duration <= 0 || keys.length == 1) return this;
            float[] baked = new float[((int) Math.ceil(duration * SAMPLE_RATE) + 1) * 3];
            for (int i = 0; i < baked.length / 3; i++) {
                for (int axis = 0; axis < 3; axis++) baked[i * 3 + axis] = sample(Math.min(duration, i / (double) SAMPLE_RATE), axis);
            }
            return new Track(baked, duration);
        }

        float sample(double time, int axis) {
            if (samples != null) {
                double frame = Math.max(0, Math.min(time, duration)) * SAMPLE_RATE;
                int last = samples.length / 3 - 1, left = Math.min(last, (int) frame), right = Math.min(last, left + 1);
                return samples[left * 3 + axis] + (samples[right * 3 + axis] - samples[left * 3 + axis]) * (float) (frame - left);
            }
            if (keys.length == 1) return value(keys[0].post, time, axis);
            int low = 0, high = keys.length;
            while (low < high) {
                int middle = (low + high) >>> 1;
                if (keys[middle].time <= time) low = middle + 1; else high = middle;
            }
            int upper = low;
            if (upper == 0) return value(keys[0].pre, time, axis);
            if (upper == keys.length) return value(keys[upper - 1].post, time, axis);
            Key a = keys[upper - 1], b = keys[upper];
            float t = (float) ((time - a.time) / (b.time - a.time));
            float p1 = value(a.post, time, axis), p2 = value(b.pre, time, axis);
            if (!b.smooth && !a.smooth) return p1 + (p2 - p1) * t;
            float p0 = value(keys[Math.max(0, upper - 2)].post, time, axis);
            float p3 = value(keys[Math.min(keys.length - 1, upper + 1)].pre, time, axis);
            return 0.5F * (2 * p1 + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t * t
                    + (-p0 + 3 * p1 - 3 * p2 + p3) * t * t * t);
        }

        private static float value(DoubleUnaryOperator[] expression, double time, int axis) {
            double result = expression[axis].applyAsDouble(time);
            if (!Double.isFinite(result)) throw new IllegalStateException("Non-finite collision animation value");
            return (float) result;
        }
    }

    private final List<Bone> bones;
    private final Map<String, Integer> indices;
    private final Map<String, Clip> clips;

    private CollisionRig(List<Bone> bones, Map<String, Integer> indices, Map<String, Clip> clips) {
        this.bones = List.copyOf(bones);
        this.indices = Map.copyOf(indices);
        this.clips = Map.copyOf(clips);
    }

    public List<Bone> bones() { return bones; }
    public Clip clip(String name) { return clips.get(name); }
    public int index(String name) { return indices.getOrDefault(name, -1); }

    public float[] restingPose() {
        float[] result = new float[bones.size() * 9];
        for (int i = 0; i < bones.size(); i++) {
            Vec3 rotation = bones.get(i).rotation;
            result[i * 9] = (float) rotation.x;
            result[i * 9 + 1] = (float) rotation.y;
            result[i * 9 + 2] = (float) rotation.z;
            Arrays.fill(result, i * 9 + 6, i * 9 + 9, 1.0F);
        }
        return result;
    }

    public void animate(float[] pose, String name, double seconds) {
        Clip clip = clips.get(name);
        if (clip == null) return;
        double time = Math.max(0, seconds);
        if (clip.length > 0) time = clip.loop ? time % clip.length : Math.min(time, clip.length);
        for (Channel channel : clip.channels) {
            Vec3 initial = bones.get(channel.bone).rotation;
            for (int axis = 0; axis < 3; axis++) {
                float value = channel.track.sample(time, axis);
                if (channel.component == 0) value += (float) (axis == 0 ? initial.x : axis == 1 ? initial.y : initial.z);
                pose[channel.bone * 9 + channel.component + axis] = value;
            }
        }
    }

    public Map<String, Matrix4f> matrices(float[] pose, float bodyYaw) {
        Map<String, Matrix4f> result = new HashMap<>(bones.size());
        Matrix4f root = new Matrix4f().rotateY((float) Math.toRadians(180.0F - bodyYaw));
        for (int i = 0; i < bones.size(); i++) matrix(i, pose, root, result);
        return result;
    }

    private Matrix4f matrix(int i, float[] pose, Matrix4f root, Map<String, Matrix4f> result) {
        Bone bone = bones.get(i);
        Matrix4f existing = result.get(bone.name);
        if (existing != null) return existing;
        int parent = index(bone.parent);
        Matrix4f transform = new Matrix4f(parent < 0 ? root : matrix(parent, pose, root, result));
        int o = i * 9;
        Vector3f pivot = bone.pivot.toVector3f();
        transform.translate(-pose[o + 3] / 16, pose[o + 4] / 16, pose[o + 5] / 16)
                .translate(pivot).rotateZ(radians(pose[o + 2])).rotateY(radians(-pose[o + 1])).rotateX(radians(-pose[o]))
                .scale(pose[o + 6], pose[o + 7], pose[o + 8]).translate(-pivot.x, -pivot.y, -pivot.z);
        result.put(bone.name, transform);
        return transform;
    }

    public AABB bounds(String[] names, Map<String, Matrix4f> transforms) {
        AABB result = null;
        for (String name : names) {
            int index = index(name);
            Matrix4f transform = transforms.get(name);
            if (index < 0 || transform == null) continue;
            for (Vector3f[] cube : bones.get(index).cubes) {
                for (Vector3f corner : cube) {
                    Vector3f p = transform.transformPosition(new Vector3f(corner));
                    AABB point = new AABB(p.x, p.y, p.z, p.x, p.y, p.z);
                    result = result == null ? point : result.minmax(point);
                }
            }
        }
        return result == null ? null : result.inflate(0.12D);
    }

    public static CollisionRig load(String species, Set<String> requiredBones) {
        JsonArray sourceBones = read("geo/entity/" + species + "/" + species + ".geo.json")
                .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones");
        Map<String, JsonObject> definitions = new LinkedHashMap<>();
        for (JsonElement element : sourceBones) {
            JsonObject bone = element.getAsJsonObject();
            definitions.put(bone.get("name").getAsString(), bone);
        }
        Set<String> included = new HashSet<>();
        for (String name : requiredBones) {
            for (String cursor = name; cursor != null && included.add(cursor);) {
                JsonObject bone = definitions.get(cursor);
                if (bone == null) throw new IllegalStateException("Missing collision bone: " + cursor);
                cursor = bone.has("parent") ? bone.get("parent").getAsString() : null;
            }
        }
        List<Bone> bones = new ArrayList<>();
        Map<String, Integer> indices = new HashMap<>();
        for (var entry : definitions.entrySet()) {
            if (!included.contains(entry.getKey())) continue;
            JsonObject bone = entry.getValue();
            List<Vector3f[]> cubes = new ArrayList<>();
            if (requiredBones.contains(entry.getKey()) && bone.has("cubes")) {
                for (JsonElement element : bone.getAsJsonArray("cubes")) {
                    JsonObject cube = element.getAsJsonObject();
                    Vec3 size = vector(cube.get("size"), Vec3.ZERO);
                    if (size.x <= 0 || size.y <= 0 || size.z <= 0) continue;
                    Vec3 origin = vector(cube.get("origin"), Vec3.ZERO);
                    Vec3 pivot = vector(cube.get("pivot"), Vec3.ZERO).multiply(-1.0 / 16, 1.0 / 16, 1.0 / 16);
                    Vec3 rotation = vector(cube.get("rotation"), Vec3.ZERO);
                    double inflate = cube.has("inflate") ? cube.get("inflate").getAsDouble() / 16
                            : bone.has("inflate") ? bone.get("inflate").getAsDouble() / 16 : 0;
                    Matrix4f transform = new Matrix4f().translate(pivot.toVector3f()).rotateZ(radians(rotation.z))
                            .rotateY(radians(-rotation.y)).rotateX(radians(-rotation.x)).translate(pivot.scale(-1).toVector3f());
                    Vector3f[] corners = new Vector3f[8];
                    for (int c = 0; c < 8; c++) {
                        float x = (float) (-(origin.x + size.x) / 16 + ((c & 1) == 0 ? -inflate : size.x / 16 + inflate));
                        float y = (float) (origin.y / 16 + ((c & 2) == 0 ? -inflate : size.y / 16 + inflate));
                        float z = (float) (origin.z / 16 + ((c & 4) == 0 ? -inflate : size.z / 16 + inflate));
                        corners[c] = transform.transformPosition(new Vector3f(x, y, z));
                    }
                    cubes.add(corners);
                }
            }
            indices.put(entry.getKey(), bones.size());
            bones.add(new Bone(entry.getKey(), bone.has("parent") ? bone.get("parent").getAsString() : "",
                    vector(bone.get("pivot"), Vec3.ZERO).multiply(-1.0 / 16, 1.0 / 16, 1.0 / 16),
                    vector(bone.get("rotation"), Vec3.ZERO), List.copyOf(cubes)));
        }
        Map<String, Clip> clips = new HashMap<>();
        for (var entry : read("animations/entity/" + species + "/" + species + ".animation.json").getAsJsonObject("animations").entrySet()) {
            JsonObject animation = entry.getValue().getAsJsonObject();
            List<Channel> channels = new ArrayList<>();
            double length = animation.has("animation_length") ? animation.get("animation_length").getAsDouble() : 0;
            if (animation.has("bones")) for (var bone : animation.getAsJsonObject("bones").entrySet()) {
                Integer index = indices.get(bone.getKey());
                if (index == null) continue;
                JsonObject values = bone.getValue().getAsJsonObject();
                String[] components = {"rotation", "position", "scale"};
                for (int c = 0; c < 3; c++) if (values.has(components[c])) {
                    Track track = track(values.get(components[c]));
                    length = Math.max(length, track.duration);
                    channels.add(new Channel(index, c * 3, track.bake()));
                }
            }
            String loop = animation.has("loop") ? animation.get("loop").getAsString() : "false";
            clips.put(entry.getKey().replace("animation." + species + ".", ""), new Clip(length,
                    loop.equals("true"), loop.equals("hold_on_last_frame"), List.copyOf(channels)));
        }
        return new CollisionRig(bones, indices, clips);
    }

    private static Track track(JsonElement value) {
        if (!value.isJsonObject() || value.getAsJsonObject().has("vector")) return new Track(new Key[]{key(0, value)});
        List<Key> keys = new ArrayList<>();
        for (var entry : value.getAsJsonObject().entrySet()) keys.add(key(Double.parseDouble(entry.getKey()), entry.getValue()));
        keys.sort(Comparator.comparingDouble(Key::time));
        if (keys.isEmpty()) throw new IllegalArgumentException("Empty collision animation track");
        return new Track(keys.toArray(Key[]::new));
    }

    private static Key key(double time, JsonElement value) {
        JsonObject object = value.isJsonObject() ? value.getAsJsonObject() : null;
        JsonElement pre = object != null && object.has("pre") ? object.get("pre")
                : object != null && object.has("post") ? object.get("post") : value;
        JsonElement post = object != null && object.has("post") ? object.get("post") : pre;
        return new Key(time, expressions(pre), expressions(post), object != null && object.has("lerp_mode")
                && object.get("lerp_mode").getAsString().equals("catmullrom"));
    }

    private static DoubleUnaryOperator[] expressions(JsonElement element) {
        if (element.isJsonObject()) element = element.getAsJsonObject().get("vector");
        DoubleUnaryOperator[] result = new DoubleUnaryOperator[3];
        for (int i = 0; i < 3; i++) result[i] = CollisionExpression.compile(
                (element.isJsonArray() ? element.getAsJsonArray().get(i) : element).getAsString());
        return result;
    }

    private static Vec3 vector(JsonElement element, Vec3 fallback) {
        if (element == null) return fallback;
        JsonArray array = element.getAsJsonArray();
        return new Vec3(array.get(0).getAsDouble(), array.get(1).getAsDouble(), array.get(2).getAsDouble());
    }

    private static float radians(double degrees) { return (float) Math.toRadians(degrees); }

    private static JsonObject read(String path) {
        String resource = "/assets/saintsdragons/" + path;
        try (var stream = CollisionRig.class.getResourceAsStream(resource)) {
            if (stream == null) throw new IllegalStateException("Missing collision resource " + resource);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read collision resource " + resource, exception);
        }
    }
}

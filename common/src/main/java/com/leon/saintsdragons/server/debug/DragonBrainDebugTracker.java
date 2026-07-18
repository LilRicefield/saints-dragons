package com.leon.saintsdragons.server.debug;

import com.leon.saintsdragons.common.network.MessageDragonBrainDebug;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonBehaviour;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMemories;
import com.leon.saintsdragons.server.ai.dragonbrain.DragonMovementIntent;
import com.leon.saintsdragons.server.ai.dragonbrain.debug.DragonBrainDebugDetails;
import com.leon.saintsdragons.server.ai.dragonbrain.debug.DragonBrainDiagnostics;
import com.leon.saintsdragons.server.entity.base.DragonEntity;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.PositionTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Builds the brain-specific half of the live dragon debug stream. */
public final class DragonBrainDebugTracker {
    private static final int MAX_DETAILS_PER_BEHAVIOUR = 16;

    private DragonBrainDebugTracker() {
    }

    public static MessageDragonBrainDebug capture(DragonEntity dragon) {
        Brain<?> brain = dragon.getBrain();
        long gameTime = dragon.level().getGameTime();
        List<MessageDragonBrainDebug.BehaviourState> behaviours = captureBehaviours(dragon, brain, gameTime);
        List<MessageDragonBrainDebug.MemoryState> memories = new ArrayList<>();
        List<MessageDragonBrainDebug.Marker> markers = new ArrayList<>();

        captureMemory(dragon, brain, DragonMemories.ATTACK_TARGET, "ATTACK_TARGET", memories, markers);
        captureMemory(dragon, brain, DragonMemories.WALK_TARGET, "WALK_TARGET", memories, markers);
        captureMemory(dragon, brain, DragonMemories.LOOK_TARGET, "LOOK_TARGET", memories, markers);
        captureMemory(dragon, brain, DragonMemories.MOVEMENT_INTENT, "MOVEMENT_INTENT", memories, markers);
        captureMemory(dragon, brain, DragonMemories.PATH, "PATH", memories, markers);
        captureMemory(dragon, brain, DragonMemories.CANT_REACH_WALK_TARGET_SINCE,
                "CANT_REACH_SINCE", memories, markers);
        captureMemory(dragon, brain, DragonMemories.TARGET_AIRBORNE, "TARGET_AIRBORNE", memories, markers);
        captureMemory(dragon, brain, DragonMemories.LOCOMOTION_MODE, "LOCOMOTION_MODE", memories, markers);
        captureMemory(dragon, brain, DragonMemories.IS_AERIAL, "IS_AERIAL", memories, markers);
        captureMemory(dragon, brain, DragonMemories.IS_GROUNDED, "IS_GROUNDED", memories, markers);
        captureMemory(dragon, brain, DragonMemories.IS_RIDDEN, "IS_RIDDEN", memories, markers);
        captureMemory(dragon, brain, DragonMemories.IN_WATER, "IN_WATER", memories, markers);
        captureMemory(dragon, brain, DragonMemories.IN_LAVA, "IN_LAVA", memories, markers);
        captureMemory(dragon, brain, DragonMemories.HOME, "HOME", memories, markers);
        captureMemory(dragon, brain, DragonMemories.TACTICAL_LANDING_POSITION,
                "TACTICAL_LANDING", memories, markers);
        captureMemory(dragon, brain, DragonMemories.ROOST_SLEEP_POSITION,
                "ROOST_SLEEP", memories, markers);

        LivingEntity mobTarget = dragon.getTarget();
        LivingEntity brainTarget = brain.getMemory(DragonMemories.ATTACK_TARGET).orElse(null);
        if (mobTarget != null && mobTarget != brainTarget) {
            markers.add(entityMarker("MOB_TARGET", mobTarget, dragon));
            memories.add(new MessageDragonBrainDebug.MemoryState(
                    "MOB_TARGET", describeEntity(dragon, mobTarget)));
        }

        List<String> activeActivities = brain.getActiveActivities().stream()
                .map(Object::toString)
                .sorted()
                .toList();
        String activeActivity = brain.getActiveNonCoreActivity()
                .map(Object::toString)
                .orElse("none");

        return new MessageDragonBrainDebug(
                true,
                dragon.getId(),
                dragon.getDisplayName().getString(),
                gameTime,
                activeActivity,
                activeActivities,
                behaviours,
                memories,
                markers
        );
    }

    private static List<MessageDragonBrainDebug.BehaviourState> captureBehaviours(DragonEntity dragon,
                                                                                  Brain<?> brain,
                                                                                  long gameTime) {
        List<DragonBrainDiagnostics.RegisteredBehaviour> registered =
                DragonBrainDiagnostics.getBehaviours(dragon, brain);
        List<MessageDragonBrainDebug.BehaviourState> result = new ArrayList<>();

        if (registered.isEmpty()) {
            for (BehaviorControl<?> behaviour : brain.getRunningBehaviors()) {
                result.add(toState("unknown", 0, behaviour, gameTime));
            }
        } else {
            registered.stream()
                    .sorted(Comparator.comparingInt(DragonBrainDiagnostics.RegisteredBehaviour::priority))
                    .forEach(entry -> result.add(toState(
                            entry.activity().toString(), entry.priority(), entry.behaviour(), gameTime)));
        }
        return result;
    }

    private static MessageDragonBrainDebug.BehaviourState toState(String activity,
                                                                  int priority,
                                                                  BehaviorControl<?> behaviour,
                                                                  long gameTime) {
        if (behaviour instanceof DragonBehaviour<?> dragonBehaviour) {
            if (dragonBehaviour.activity() != null) {
                activity = dragonBehaviour.activity().toString();
            }
            if (dragonBehaviour.priority() >= 0) {
                priority = dragonBehaviour.priority();
            }
        }
        boolean claimsControl = behaviour instanceof DragonBehaviour<?> dragonBehaviour
                && dragonBehaviour.claimsControl();
        long cooldown = behaviour instanceof DragonBehaviour<?> dragonBehaviour
                ? dragonBehaviour.cooldownRemaining(gameTime)
                : 0L;
        List<String> details = new ArrayList<>();
        if (behaviour instanceof DragonBrainDebugDetails provider) {
            int count = 0;
            for (Map.Entry<String, String> detail : provider.getDragonBrainDebugDetails().entrySet()) {
                if (count++ >= MAX_DETAILS_PER_BEHAVIOUR) {
                    break;
                }
                details.add(detail.getKey() + "=" + detail.getValue());
            }
        }
        String status = behaviour.getStatus() == Behavior.Status.RUNNING ? "RUNNING" : "STOPPED";
        if ("STOPPED".equals(status) && cooldown > 0L) {
            status = "COOLDOWN";
        }
        return new MessageDragonBrainDebug.BehaviourState(
                activity,
                priority,
                simpleName(behaviour),
                status,
                claimsControl,
                cooldown,
                details
        );
    }

    private static <T> void captureMemory(DragonEntity dragon,
                                          Brain<?> brain,
                                          MemoryModuleType<T> type,
                                          String name,
                                          List<MessageDragonBrainDebug.MemoryState> memories,
                                          List<MessageDragonBrainDebug.Marker> markers) {
        @SuppressWarnings("unchecked")
        T value = ((Brain<LivingEntity>)(Brain<?>)brain).getMemory(type).orElse(null);
        if (value == null) {
            memories.add(new MessageDragonBrainDebug.MemoryState(name, "<empty>"));
            return;
        }

        memories.add(new MessageDragonBrainDebug.MemoryState(name, describeValue(dragon, value)));
        addMarker(dragon, name, value, markers);
    }

    private static String describeValue(DragonEntity dragon, Object value) {
        if (value instanceof LivingEntity entity) {
            return describeEntity(dragon, entity);
        }
        if (value instanceof WalkTarget walkTarget) {
            Vec3 position = walkTarget.getTarget().currentPosition();
            return format(position) + " speed=" + decimal(walkTarget.getSpeedModifier())
                    + " close=" + walkTarget.getCloseEnoughDist();
        }
        if (value instanceof PositionTracker tracker) {
            return format(tracker.currentPosition());
        }
        if (value instanceof Path path) {
            return path.getNextNodeIndex() + "/" + path.getNodeCount()
                    + " target=" + path.getTarget();
        }
        if (value instanceof GlobalPos globalPos) {
            return globalPos.dimension().location() + " " + globalPos.pos().toShortString();
        }
        if (value instanceof Vec3 position) {
            return format(position);
        }
        if (value instanceof DragonMovementIntent intent) {
            return describeMovementIntent(dragon, intent);
        }
        return String.valueOf(value);
    }

    private static String describeMovementIntent(DragonEntity dragon, DragonMovementIntent intent) {
        if (intent instanceof DragonMovementIntent.Stop stop) {
            return "STOP reason=" + stop.reason();
        }
        if (intent instanceof DragonMovementIntent.AutoPosition move) {
            return "AUTO " + format(move.target()) + " speed=" + decimal(move.speed());
        }
        if (intent instanceof DragonMovementIntent.AutoTarget move) {
            return "AUTO_TARGET " + describeEntity(dragon, move.target())
                    + " speed=" + decimal(move.speed());
        }
        if (intent instanceof DragonMovementIntent.GroundPosition move) {
            return "GROUND " + format(move.target()) + " speed=" + decimal(move.speed())
                    + " running=" + move.running();
        }
        if (intent instanceof DragonMovementIntent.ProgressiveGroundPosition move) {
            return "PROGRESSIVE_GROUND " + format(move.target()) + " speed=" + decimal(move.speed())
                    + " running=" + move.running();
        }
        if (intent instanceof DragonMovementIntent.GroundTarget move) {
            return "GROUND_TARGET " + describeEntity(dragon, move.target())
                    + " speed=" + decimal(move.speed()) + " running=" + move.running();
        }
        if (intent instanceof DragonMovementIntent.LandingPosition move) {
            return "LANDING " + format(move.target()) + " speed=" + decimal(move.speed());
        }
        if (intent instanceof DragonMovementIntent.LandingTarget move) {
            return "LANDING_TARGET " + (move.target() == null ? "auto" : describeEntity(dragon, move.target()))
                    + " speed=" + decimal(move.speed());
        }
        return intent.getClass().getSimpleName().toUpperCase();
    }

    private static void addMarker(DragonEntity dragon,
                                  String memoryName,
                                  Object value,
                                  List<MessageDragonBrainDebug.Marker> markers) {
        if (value instanceof LivingEntity entity) {
            markers.add(entityMarker(memoryName, entity, dragon));
            return;
        }
        Vec3 position = null;
        int entityId = -1;
        if (value instanceof WalkTarget walkTarget) {
            position = walkTarget.getTarget().currentPosition();
        } else if (value instanceof PositionTracker tracker) {
            position = tracker.currentPosition();
        } else if (value instanceof Vec3 vec) {
            position = vec;
        } else if (value instanceof GlobalPos globalPos
                && globalPos.dimension().equals(dragon.level().dimension())) {
            position = Vec3.atCenterOf(globalPos.pos());
        } else if (value instanceof DragonMovementIntent.AutoPosition move) {
            position = move.target();
        } else if (value instanceof DragonMovementIntent.AutoTarget move) {
            position = move.target().getBoundingBox().getCenter();
            entityId = move.target().getId();
        } else if (value instanceof DragonMovementIntent.GroundPosition move) {
            position = move.target();
        } else if (value instanceof DragonMovementIntent.ProgressiveGroundPosition move) {
            position = move.target();
        } else if (value instanceof DragonMovementIntent.GroundTarget move) {
            position = move.target().getBoundingBox().getCenter();
            entityId = move.target().getId();
        } else if (value instanceof DragonMovementIntent.LandingPosition move) {
            position = move.target();
        } else if (value instanceof DragonMovementIntent.LandingTarget move && move.target() != null) {
            position = move.target().getBoundingBox().getCenter();
            entityId = move.target().getId();
        }
        if (position != null) {
            markers.add(new MessageDragonBrainDebug.Marker(memoryName, position, entityId, memoryName));
        }
    }

    private static MessageDragonBrainDebug.Marker entityMarker(String kind,
                                                               LivingEntity entity,
                                                               DragonEntity dragon) {
        return new MessageDragonBrainDebug.Marker(
                kind,
                entity.getBoundingBox().getCenter(),
                entity.getId(),
                entity.getDisplayName().getString() + " " + decimal(dragon.distanceTo(entity)) + "m"
        );
    }

    private static String describeEntity(DragonEntity dragon, @Nullable LivingEntity entity) {
        if (entity == null) {
            return "none";
        }
        return entity.getDisplayName().getString() + " #" + entity.getId()
                + " " + decimal(dragon.distanceTo(entity)) + "m"
                + (entity.isAlive() ? "" : " dead");
    }

    private static String simpleName(Object value) {
        String name = value.getClass().getSimpleName();
        if (!name.isEmpty()) {
            return name;
        }
        String fullName = value.getClass().getName();
        int separator = fullName.lastIndexOf('.');
        return separator < 0 ? fullName : fullName.substring(separator + 1);
    }

    private static String format(Vec3 position) {
        return decimal(position.x) + "," + decimal(position.y) + "," + decimal(position.z);
    }

    private static String decimal(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }
}

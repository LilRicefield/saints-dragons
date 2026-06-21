package com.leon.saintsdragons.server.entity.npc.chatter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class IvyChatterRegistry {
    public static final String IDLE = "idle";
    public static final String COMBAT = "combat";
    public static final String OWNER_HURT = "owner_hurt";
    public static final String RENAME_REFUSAL = "rename_refusal";
    public static final String DOWNED = "downed";
    public static final String UNDERWATER = "underwater";
    public static final String COMMAND_FOLLOW = "command_follow";
    public static final String COMMAND_STAY = "command_stay";
    public static final String COMMAND_WANDER = "command_wander";

    private static final Map<String, List<String>> POOLS = new ConcurrentHashMap<>();

    static {
        replace(defaultPools());
    }

    private IvyChatterRegistry() {
    }

    public static void replace(Map<String, List<String>> pools) {
        POOLS.clear();
        pools.forEach((pool, lines) -> POOLS.put(pool, List.copyOf(lines)));
    }

    public static List<String> get(String pool) {
        return POOLS.getOrDefault(pool, List.of());
    }

    private static Map<String, List<String>> defaultPools() {
        return Map.of(
                IDLE, List.of(
                        "chatter.saintsdragons.ivy.idle.0",
                        "chatter.saintsdragons.ivy.idle.1",
                        "chatter.saintsdragons.ivy.idle.2",
                        "chatter.saintsdragons.ivy.idle.3",
                        "chatter.saintsdragons.ivy.idle.4",
                        "chatter.saintsdragons.ivy.idle.5",
                        "chatter.saintsdragons.ivy.idle.6",
                        "chatter.saintsdragons.ivy.idle.7",
                        "chatter.saintsdragons.ivy.idle.8"
                ),
                COMBAT, List.of(
                        "chatter.saintsdragons.ivy.combat.0",
                        "chatter.saintsdragons.ivy.combat.1",
                        "chatter.saintsdragons.ivy.combat.2",
                        "chatter.saintsdragons.ivy.combat.3",
                        "chatter.saintsdragons.ivy.combat.4",
                        "chatter.saintsdragons.ivy.combat.5",
                        "chatter.saintsdragons.ivy.combat.6",
                        "chatter.saintsdragons.ivy.combat.7",
                        "chatter.saintsdragons.ivy.combat.8",
                        "chatter.saintsdragons.ivy.combat.9"
                ),
                OWNER_HURT, List.of(
                        "chatter.saintsdragons.ivy.owner_hurt.0",
                        "chatter.saintsdragons.ivy.owner_hurt.1",
                        "chatter.saintsdragons.ivy.owner_hurt.2",
                        "chatter.saintsdragons.ivy.owner_hurt.3",
                        "chatter.saintsdragons.ivy.owner_hurt.4",
                        "chatter.saintsdragons.ivy.owner_hurt.5",
                        "chatter.saintsdragons.ivy.owner_hurt.6",
                        "chatter.saintsdragons.ivy.owner_hurt.7",
                        "chatter.saintsdragons.ivy.owner_hurt.8",
                        "chatter.saintsdragons.ivy.owner_hurt.9",
                        "chatter.saintsdragons.ivy.owner_hurt.10",
                        "chatter.saintsdragons.ivy.owner_hurt.11",
                        "chatter.saintsdragons.ivy.owner_hurt.12",
                        "chatter.saintsdragons.ivy.owner_hurt.13",
                        "chatter.saintsdragons.ivy.owner_hurt.14",
                        "chatter.saintsdragons.ivy.owner_hurt.15",
                        "chatter.saintsdragons.ivy.owner_hurt.16",
                        "chatter.saintsdragons.ivy.owner_hurt.17",
                        "chatter.saintsdragons.ivy.owner_hurt.18",
                        "chatter.saintsdragons.ivy.owner_hurt.19"
                ),
                RENAME_REFUSAL, List.of(
                        "chatter.saintsdragons.ivy.rename_refusal.0",
                        "chatter.saintsdragons.ivy.rename_refusal.1",
                        "chatter.saintsdragons.ivy.rename_refusal.2",
                        "chatter.saintsdragons.ivy.rename_refusal.3",
                        "chatter.saintsdragons.ivy.rename_refusal.4",
                        "chatter.saintsdragons.ivy.rename_refusal.5",
                        "chatter.saintsdragons.ivy.rename_refusal.6",
                        "chatter.saintsdragons.ivy.rename_refusal.7"
                ),
                DOWNED, List.of(
                        "chatter.saintsdragons.ivy.downed.0",
                        "chatter.saintsdragons.ivy.downed.1",
                        "chatter.saintsdragons.ivy.downed.2",
                        "chatter.saintsdragons.ivy.downed.3",
                        "chatter.saintsdragons.ivy.downed.4",
                        "chatter.saintsdragons.ivy.downed.5",
                        "chatter.saintsdragons.ivy.downed.6",
                        "chatter.saintsdragons.ivy.downed.7"
                ),
                UNDERWATER, List.of(
                        "chatter.saintsdragons.ivy.underwater.0",
                        "chatter.saintsdragons.ivy.underwater.1",
                        "chatter.saintsdragons.ivy.underwater.2"
                ),
                COMMAND_FOLLOW, List.of(
                        "chatter.saintsdragons.ivy.command_follow.0",
                        "chatter.saintsdragons.ivy.command_follow.1",
                        "chatter.saintsdragons.ivy.command_follow.2"
                ),
                COMMAND_STAY, List.of(
                        "chatter.saintsdragons.ivy.command_stay.0",
                        "chatter.saintsdragons.ivy.command_stay.1",
                        "chatter.saintsdragons.ivy.command_stay.2"
                ),
                COMMAND_WANDER, List.of(
                        "chatter.saintsdragons.ivy.command_wander.0",
                        "chatter.saintsdragons.ivy.command_wander.1",
                        "chatter.saintsdragons.ivy.command_wander.2"
                )
        );
    }
}

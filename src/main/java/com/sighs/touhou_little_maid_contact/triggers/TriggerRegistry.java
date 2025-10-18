package com.sighs.touhou_little_maid_contact.triggers;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TriggerRegistry {
    private static final ConcurrentHashMap<UUID, Set<ResourceLocation>> PLAYER_EVENTS = new ConcurrentHashMap<>();

    public static void mark(ServerPlayer player, ResourceLocation id) {
        if (player == null || id == null) return;
        PLAYER_EVENTS.computeIfAbsent(player.getUUID(), u -> ConcurrentHashMap.newKeySet()).add(id);
    }

    public static void mark(ServerPlayer player, String id) {
        if (player == null || id == null || id.isBlank()) return;
        mark(player, new ResourceLocation(id));
    }

    public static boolean has(ServerPlayer player, ResourceLocation id) {
        Set<ResourceLocation> set = PLAYER_EVENTS.get(player.getUUID());
        return set != null && set.contains(id);
    }

    public static boolean consume(ServerPlayer player, ResourceLocation id) {
        Set<ResourceLocation> set = PLAYER_EVENTS.get(player.getUUID());
        if (set != null && set.contains(id)) {
            set.remove(id);
            return true;
        }
        return false;
    }

    public static void clear(ServerPlayer player, ResourceLocation id) {
        Set<ResourceLocation> set = PLAYER_EVENTS.get(player.getUUID());
        if (set != null) set.remove(id);
    }

    public static void clearAll(ServerPlayer player) {
        PLAYER_EVENTS.remove(player.getUUID());
    }

    private TriggerRegistry() {
    }
}
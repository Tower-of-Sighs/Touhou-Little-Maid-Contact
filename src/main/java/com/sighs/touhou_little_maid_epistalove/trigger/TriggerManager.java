package com.sighs.touhou_little_maid_epistalove.trigger;

import com.sighs.touhou_little_maid_epistalove.api.trigger.ITriggerManager;
import com.sighs.touhou_little_maid_epistalove.init.ModComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TriggerManager implements ITriggerManager {
    private static final ConcurrentHashMap<UUID, Set<ResourceLocation>> PLAYER_EVENTS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, ConcurrentHashMap<ResourceLocation, CompoundTag>> PLAYER_CONTEXTS = new ConcurrentHashMap<>();
    private static final TriggerManager INSTANCE = new TriggerManager();

    public static TriggerManager getInstance() {
        return INSTANCE;
    }

    @Override
    public void markTriggered(ServerPlayer player, ResourceLocation triggerId) {
        if (player == null || triggerId == null) return;
        PLAYER_EVENTS.computeIfAbsent(player.getUUID(), u -> ConcurrentHashMap.newKeySet()).add(triggerId);
    }

    @Override
    public boolean hasTriggered(ServerPlayer player, ResourceLocation triggerId) {
        Set<ResourceLocation> set = PLAYER_EVENTS.get(player.getUUID());
        return set != null && set.contains(triggerId);
    }

    @Override
    public boolean consumeTriggered(ServerPlayer player, ResourceLocation triggerId) {
        Set<ResourceLocation> set = PLAYER_EVENTS.get(player.getUUID());
        if (set != null && set.contains(triggerId)) {
            set.remove(triggerId);
            return true;
        }
        return false;
    }

    @Override
    public void markTriggeredWithContext(ServerPlayer player, ResourceLocation triggerId, CompoundTag context) {
        markTriggered(player, triggerId);
        if (context != null) {
            var ctxMap = PLAYER_CONTEXTS.computeIfAbsent(player.getUUID(), u -> new ConcurrentHashMap<>());
            ctxMap.put(triggerId, context.copy());
        }
    }

    @Override
    public CompoundTag getTriggerContext(ServerPlayer player, ResourceLocation triggerId) {
        var ctxMap = PLAYER_CONTEXTS.get(player.getUUID());
        CompoundTag tag = ctxMap != null ? ctxMap.get(triggerId) : null;
        return tag != null ? tag.copy() : new CompoundTag();
    }

    @Override
    public void clearTriggered(ServerPlayer player, ResourceLocation triggerId) {
        Set<ResourceLocation> set = PLAYER_EVENTS.get(player.getUUID());
        if (set != null) set.remove(triggerId);
        var ctxMap = PLAYER_CONTEXTS.get(player.getUUID());
        if (ctxMap != null) ctxMap.remove(triggerId);
    }

    @Override
    public void clearAllTriggered(ServerPlayer player) {
        UUID id = player.getUUID();
        PLAYER_EVENTS.remove(id);
        PLAYER_CONTEXTS.remove(id);
    }

    @Override
    public void markConsumedOnce(ServerPlayer player, ResourceLocation triggerKey) {
        if (player == null || triggerKey == null) return;
        ModComponents.CONSUMED_ONCE.get(player).markConsumed(triggerKey);
    }

    @Override
    public boolean hasConsumedOnce(ServerPlayer player, ResourceLocation triggerKey) {
        return ModComponents.CONSUMED_ONCE.get(player).hasConsumed(triggerKey);
    }

    @Override
    public void clearConsumedOnce(ServerPlayer player, ResourceLocation triggerKey) {
        ModComponents.CONSUMED_ONCE.get(player).clearConsumed(triggerKey);
    }
}
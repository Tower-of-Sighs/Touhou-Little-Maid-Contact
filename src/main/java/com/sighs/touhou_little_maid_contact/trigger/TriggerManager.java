package com.sighs.touhou_little_maid_contact.trigger;

import com.sighs.touhou_little_maid_contact.api.trigger.ITriggerManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TriggerManager implements ITriggerManager {
    private static final ConcurrentHashMap<UUID, Set<ResourceLocation>> PLAYER_EVENTS = new ConcurrentHashMap<>();
    private static final TriggerManager INSTANCE = new TriggerManager();
    private static final String NBT_CONSUMED_KEY = "maidmail_consumed";

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
    public void clearTriggered(ServerPlayer player, ResourceLocation triggerId) {
        Set<ResourceLocation> set = PLAYER_EVENTS.get(player.getUUID());
        if (set != null) set.remove(triggerId);
    }

    @Override
    public void clearAllTriggered(ServerPlayer player) {
        PLAYER_EVENTS.remove(player.getUUID());
    }

    @Override
    public void markConsumedOnce(ServerPlayer player, ResourceLocation triggerKey) {
        if (player == null || triggerKey == null) return;
        CompoundTag root = player.getPersistentData();
        CompoundTag consumed = root.getCompound(NBT_CONSUMED_KEY);
        consumed.putBoolean(triggerKey.toString(), true);
        root.put(NBT_CONSUMED_KEY, consumed);
    }

    @Override
    public boolean hasConsumedOnce(ServerPlayer player, ResourceLocation triggerKey) {
        CompoundTag root = player.getPersistentData();
        CompoundTag consumed = root.getCompound(NBT_CONSUMED_KEY);
        return consumed.getBoolean(triggerKey.toString());
    }

    @Override
    public void clearConsumedOnce(ServerPlayer player, ResourceLocation triggerKey) {
        CompoundTag root = player.getPersistentData();
        CompoundTag consumed = root.getCompound(NBT_CONSUMED_KEY);
        consumed.remove(triggerKey.toString());
        root.put(NBT_CONSUMED_KEY, consumed);
    }
}
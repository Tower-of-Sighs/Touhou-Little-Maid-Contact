package com.sighs.touhou_little_maid_epistalove.component;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class ConsumedOnceComponentImpl implements ConsumedOnceComponent {
    private final Map<String, Boolean> consumedMap = new HashMap<>();

    @Override
    public void markConsumed(ResourceLocation key) {
        consumedMap.put(key.toString(), true);
    }

    @Override
    public boolean hasConsumed(ResourceLocation key) {
        return consumedMap.getOrDefault(key.toString(), false);
    }

    @Override
    public void clearConsumed(ResourceLocation key) {
        consumedMap.remove(key.toString());
    }

    @Override
    public void readFromNbt(CompoundTag tag) {
        consumedMap.clear();
        for (String k : tag.getAllKeys()) {
            consumedMap.put(k, tag.getBoolean(k));
        }
    }

    @Override
    public void writeToNbt(CompoundTag tag) {
        for (var e : consumedMap.entrySet()) {
            tag.putBoolean(e.getKey(), e.getValue());
        }
    }
}
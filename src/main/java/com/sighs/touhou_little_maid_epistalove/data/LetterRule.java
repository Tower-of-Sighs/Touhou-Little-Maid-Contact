package com.sighs.touhou_little_maid_epistalove.data;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.sighs.touhou_little_maid_epistalove.api.letter.ILetterGenerator;
import com.sighs.touhou_little_maid_epistalove.api.letter.ILetterRule;
import com.sighs.touhou_little_maid_epistalove.api.trigger.ITriggerManager;
import com.sighs.touhou_little_maid_epistalove.trigger.TriggerManager;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Consumer;

public class LetterRule implements ILetterRule {
    private final String id;
    private final int minAffection;
    private final Integer maxAffection;
    private final List<ResourceLocation> triggers;
    private final TriggerType triggerType;
    private final Integer cooldown;
    private final ILetterGenerator generator;
    private final List<ResourceLocation> requiredMaidIds;

    private static final ITriggerManager TRIGGER_MANAGER = TriggerManager.getInstance();

    public LetterRule(String id, int minAffection, Integer maxAffection,
                      List<ResourceLocation> triggers, TriggerType triggerType,
                      Integer cooldown, ILetterGenerator generator,
                      List<ResourceLocation> requiredMaidIds) {
        this.id = id;
        this.minAffection = minAffection;
        this.maxAffection = maxAffection;
        this.triggers = triggers;
        this.triggerType = triggerType;
        this.cooldown = cooldown;
        this.generator = generator;
        this.requiredMaidIds = requiredMaidIds;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public int getMinAffection() {
        return minAffection;
    }

    @Override
    public Integer getMaxAffection() {
        return maxAffection;
    }

    @Override
    public List<ResourceLocation> getTriggers() {
        return triggers;
    }

    @Override
    public TriggerType getTriggerType() {
        return triggerType;
    }

    @Override
    public Integer getCooldown() {
        return cooldown;
    }

    @Override
    public boolean matches(ServerPlayer owner, EntityMaid maid, long gameTime) {
        int affection = maid.getFavorability();
        if (affection < minAffection) return false;
        if (maxAffection != null && affection > maxAffection) return false;

        if (requiredMaidIds != null && !requiredMaidIds.isEmpty()) {
            String modelIdStr = maid.getModelId();
            ResourceLocation maidModel = !modelIdStr.isEmpty()
                    ? new ResourceLocation(modelIdStr) : null;
            if (maidModel == null || !requiredMaidIds.contains(maidModel)) {
                return false;
            }
        }
        return hasAnyTrigger(owner);
    }

    @Override
    public List<ResourceLocation> getRequiredMaidIds() {
        return requiredMaidIds;
    }

    @Override
    public void generateLetter(ServerPlayer owner, EntityMaid maid, Consumer<ItemStack> callback) {
        generator.generate(owner, maid, callback);
    }

    @Override
    public String getType() {
        return generator.getType();
    }

    private boolean hasAnyTrigger(ServerPlayer owner) {
        MinecraftServer server = owner.getServer();
        if (server == null) {
            return false;
        }

        for (ResourceLocation triggerId : triggers) {
            // 成就：只看事件激活
            Advancement advancement = server.getAdvancements().getAdvancement(triggerId);
            if (advancement != null) {
                if (TRIGGER_MANAGER.hasTriggered(owner, triggerId)) {
                    return true;
                }
                continue;
            }

            // 自定义触发器
            boolean active = TRIGGER_MANAGER.hasTriggered(owner, triggerId);
            if (active) {
                ResourceLocation consumeKey = new ResourceLocation(
                        "internal",
                        ("custom_" + id + "_" + triggerId.toString().replace(":", "_"))
                );
                if (triggerType == TriggerType.ONCE) {
                    if (TRIGGER_MANAGER.hasConsumedOnce(owner, consumeKey)) {
                        continue;
                    }
                }
                return true;
            }
        }
        return false;
    }

    public void consumeTriggers(ServerPlayer owner) {
        MinecraftServer server = owner.getServer();

        for (ResourceLocation triggerId : triggers) {
            Advancement advancement = server != null ? server.getAdvancements().getAdvancement(triggerId) : null;
            if (advancement != null) {
                TRIGGER_MANAGER.clearTriggered(owner, triggerId);
                continue;
            }

            if (TRIGGER_MANAGER.hasTriggered(owner, triggerId)) {
                if (triggerType == TriggerType.ONCE) {
                    ResourceLocation consumeKey = new ResourceLocation(
                            "internal",
                            ("custom_" + id + "_" + triggerId.toString().replace(":", "_"))
                    );
                    TRIGGER_MANAGER.markConsumedOnce(owner, consumeKey);
                    TRIGGER_MANAGER.clearTriggered(owner, triggerId);
                } else {
                    TRIGGER_MANAGER.clearTriggered(owner, triggerId);
                }
            }
        }
    }
}
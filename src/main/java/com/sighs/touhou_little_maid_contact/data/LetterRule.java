package com.sighs.touhou_little_maid_contact.data;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.sighs.touhou_little_maid_contact.api.letter.ILetterGenerator;
import com.sighs.touhou_little_maid_contact.api.letter.ILetterRule;
import com.sighs.touhou_little_maid_contact.api.trigger.ITriggerManager;
import com.sighs.touhou_little_maid_contact.trigger.TriggerManager;
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

    private static final ITriggerManager TRIGGER_MANAGER = TriggerManager.getInstance();

    public LetterRule(String id, int minAffection, Integer maxAffection,
                      List<ResourceLocation> triggers, TriggerType triggerType,
                      Integer cooldown, ILetterGenerator generator) {
        this.id = id;
        this.minAffection = minAffection;
        this.maxAffection = maxAffection;
        this.triggers = triggers;
        this.triggerType = triggerType;
        this.cooldown = cooldown;
        this.generator = generator;
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

        return hasAnyTrigger(owner);
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
            // 检查成就触发器
            Advancement advancement = server.getAdvancements().getAdvancement(triggerId);
            if (advancement != null) {
                boolean done = owner.getAdvancements().getOrStartProgress(advancement).isDone();
                if (done) return true;
            }

            // 检查自定义触发器
            boolean hasCustomTrigger;
            if (triggerType == TriggerType.ONCE) {
                hasCustomTrigger = TRIGGER_MANAGER.consumeTriggered(owner, triggerId);
            } else {
                hasCustomTrigger = TRIGGER_MANAGER.hasTriggered(owner, triggerId);
            }

            if (hasCustomTrigger) return true;
        }
        return false;
    }
}
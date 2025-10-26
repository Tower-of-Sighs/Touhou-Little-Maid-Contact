package com.sighs.touhou_little_maid_contact.data;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.sighs.touhou_little_maid_contact.ai.generator.AILetterGenerator;
import com.sighs.touhou_little_maid_contact.ai.generator.PresetLetterGenerator;
import com.sighs.touhou_little_maid_contact.ai.parser.JsonLetterParser;
import com.sighs.touhou_little_maid_contact.ai.prompt.EnhancedPromptBuilder;
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

public class DataPackLetterRuleAdapter implements ILetterRule {
    private final MaidLetterRule dataPackRule;
    private final ILetterGenerator generator;
    private static final ITriggerManager TRIGGER_MANAGER = TriggerManager.getInstance();

    private static final EnhancedPromptBuilder PROMPT_BUILDER = new EnhancedPromptBuilder();
    private static final JsonLetterParser LETTER_PARSER = new JsonLetterParser(PROMPT_BUILDER);

    public DataPackLetterRuleAdapter(MaidLetterRule dataPackRule) {
        this.dataPackRule = dataPackRule;
        this.generator = createGenerator();
    }

    @Override
    public String getId() {
        return dataPackRule.id();
    }

    @Override
    public int getMinAffection() {
        return dataPackRule.minAffection();
    }

    @Override
    public Integer getMaxAffection() {
        return dataPackRule.maxAffection().orElse(null);
    }

    @Override
    public List<ResourceLocation> getTriggers() {
        return dataPackRule.triggers();
    }

    @Override
    public TriggerType getTriggerType() {
        return dataPackRule.triggerType() == MaidLetterRule.TriggerType.ONCE ?
                TriggerType.ONCE : TriggerType.REPEAT;
    }

    @Override
    public Integer getCooldown() {
        return dataPackRule.cooldown().orElse(null);
    }

    @Override
    public boolean matches(ServerPlayer owner, EntityMaid maid, long gameTime) {
        int affection = maid.getFavorability();

        if (affection < getMinAffection()) return false;
        if (getMaxAffection() != null && affection > getMaxAffection()) return false;

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

    private ILetterGenerator createGenerator() {
        switch (dataPackRule.type()) {
            case AI -> {
                var ai = dataPackRule.ai().orElseThrow(() ->
                        new IllegalStateException("AI rule must have AI configuration"));
                return new AILetterGenerator(
                        ai.tone().orElse("sweet"),
                        ai.prompt(),
                        PROMPT_BUILDER,
                        LETTER_PARSER
                );
            }
            case PRESET -> {
                var preset = dataPackRule.preset().orElseThrow(() ->
                        new IllegalStateException("Preset rule must have preset configuration"));
                if (preset.gifts().isEmpty()) {
                    throw new IllegalStateException("Preset rule must have at least one gift");
                }
                var gift = preset.gifts().get(0);
                return new PresetLetterGenerator(
                        preset.title(),
                        preset.message(),
                        gift.postcard(),
                        gift.parcel()
                );
            }
            default -> throw new IllegalStateException("Unknown rule type: " + dataPackRule.type());
        }
    }

    private boolean hasAnyTrigger(ServerPlayer owner) {
        MinecraftServer server = owner.getServer();
        if (server == null) {
            return false;
        }

        for (ResourceLocation triggerId : getTriggers()) {
            // 检查成就触发器
            var advancement = server.getAdvancements().get(triggerId);
            if (advancement != null) {
                boolean done = owner.getAdvancements().getOrStartProgress(advancement).isDone();
                if (done) return true;
            }

            // 检查自定义触发器
            boolean hasCustomTrigger;
            if (getTriggerType() == TriggerType.ONCE) {
                hasCustomTrigger = TRIGGER_MANAGER.consumeTriggered(owner, triggerId);
            } else {
                hasCustomTrigger = TRIGGER_MANAGER.hasTriggered(owner, triggerId);
            }

            if (hasCustomTrigger) return true;
        }
        return false;
    }
}
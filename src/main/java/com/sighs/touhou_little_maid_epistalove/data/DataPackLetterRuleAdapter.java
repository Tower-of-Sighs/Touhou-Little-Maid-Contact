package com.sighs.touhou_little_maid_epistalove.data;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.sighs.touhou_little_maid_epistalove.ai.generator.AILetterGenerator;
import com.sighs.touhou_little_maid_epistalove.ai.generator.PresetLetterGenerator;
import com.sighs.touhou_little_maid_epistalove.ai.parser.JsonLetterParser;
import com.sighs.touhou_little_maid_epistalove.ai.prompt.EnhancedPromptBuilder;
import com.sighs.touhou_little_maid_epistalove.api.letter.ILetterGenerator;
import com.sighs.touhou_little_maid_epistalove.api.letter.ILetterRule;
import com.sighs.touhou_little_maid_epistalove.api.trigger.ITriggerManager;
import com.sighs.touhou_little_maid_epistalove.trigger.TriggerManager;
import net.minecraft.advancements.Advancement;
import net.minecraft.nbt.CompoundTag;
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
    public Integer getFavorabilityChange() {
        return dataPackRule.favorabilityChange().orElse(null);
    }

    @Override
    public Integer getFavorabilityThreshold() {
        return dataPackRule.favorabilityThreshold().orElse(null);
    }

    @Override
    public boolean matches(ServerPlayer owner, EntityMaid maid, long gameTime) {
        int affection = maid.getFavorability();
        if (affection < getMinAffection()) return false;
        if (getMaxAffection() != null && affection > getMaxAffection()) return false;

        List<ResourceLocation> required = getRequiredMaidIds();
        if (required != null && !required.isEmpty()) {
            String modelIdStr = maid.getModelId();
            ResourceLocation maidModel = !modelIdStr.isEmpty()
                    ? new ResourceLocation(modelIdStr) : null;
            if (maidModel == null || !required.contains(maidModel)) {
                return false;
            }
        }

        return hasAnyTrigger(owner);
    }


    @Override
    public List<ResourceLocation> getRequiredMaidIds() {
        return dataPackRule.maidIds().orElse(null);
    }

    @Override
    public void generateLetter(ServerPlayer owner, EntityMaid maid, Consumer<ItemStack> callback) {
        CompoundTag ctx = null;
        for (ResourceLocation tid : getTriggers()) {
            if (TRIGGER_MANAGER.hasTriggered(owner, tid)) {
                ctx = TRIGGER_MANAGER.getTriggerContext(owner, tid);
                break;
            }
        }
        generator.generateWithContext(owner, maid, ctx, callback);
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
            Advancement advancement = server.getAdvancements().getAdvancement(triggerId);
            if (advancement != null) {
                if (TRIGGER_MANAGER.hasTriggered(owner, triggerId)) {
                    return true;
                }
                continue;
            }
            boolean active = TRIGGER_MANAGER.hasTriggered(owner, triggerId);
            if (active) {
                ResourceLocation consumeKey = new ResourceLocation(
                        "internal",
                        ("custom_" + getId() + "_" + triggerId.toString().replace(":", "_"))
                );
                if (getTriggerType() == TriggerType.ONCE) {
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

        for (ResourceLocation triggerId : getTriggers()) {
            Advancement advancement = server != null ? server.getAdvancements().getAdvancement(triggerId) : null;
            if (advancement != null) {
                TRIGGER_MANAGER.clearTriggered(owner, triggerId);
                continue;
            }

            if (TRIGGER_MANAGER.hasTriggered(owner, triggerId)) {
                if (getTriggerType() == TriggerType.ONCE) {
                    ResourceLocation consumeKey = new ResourceLocation(
                            "internal",
                            ("custom_" + getId() + "_" + triggerId.toString().replace(":", "_"))
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
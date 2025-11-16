package com.sighs.touhou_little_maid_epistalove.integration.kjs;

import com.sighs.touhou_little_maid_epistalove.api.integration.ILetterRuleBuilder;
import com.sighs.touhou_little_maid_epistalove.api.letter.ILetterGenerator;
import com.sighs.touhou_little_maid_epistalove.api.letter.ILetterRule;
import com.sighs.touhou_little_maid_epistalove.data.LetterRule;
import com.sighs.touhou_little_maid_epistalove.data.LetterRuleRegistry;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class LetterRuleBuilder implements ILetterRuleBuilder {
    private String id;
    private int minAffection = 0;
    private Integer maxAffection = null;
    private final List<ResourceLocation> triggers = new ArrayList<>();
    private ILetterRule.TriggerType triggerType = ILetterRule.TriggerType.ONCE;
    private Integer cooldown = null;
    private Integer favorabilityChange = null;
    private Integer favorabilityThreshold = null;
    private ILetterGenerator generator;
    private final List<ResourceLocation> requiredModelIds = new ArrayList<>();

    @Override
    public ILetterRuleBuilder id(String id) {
        this.id = id;
        return this;
    }

    @Override
    public ILetterRuleBuilder minAffection(int minAffection) {
        this.minAffection = minAffection;
        return this;
    }

    @Override
    public ILetterRuleBuilder maxAffection(int maxAffection) {
        this.maxAffection = maxAffection;
        return this;
    }

    @Override
    public ILetterRuleBuilder trigger(String triggerId) {
        this.triggers.add(ResourceLocation.parse(triggerId));
        return this;
    }

    @Override
    public ILetterRuleBuilder triggers(List<String> triggerIds) {
        for (String id : triggerIds) {
            this.triggers.add(ResourceLocation.parse(id));
        }
        return this;
    }

    @Override
    public ILetterRuleBuilder triggerType(ILetterRule.TriggerType triggerType) {
        this.triggerType = triggerType;
        return this;
    }

    @Override
    public ILetterRuleBuilder cooldown(int cooldown) {
        this.cooldown = cooldown;
        return this;
    }

    @Override
    public ILetterRuleBuilder affectionChange(int delta) {
        this.favorabilityChange = delta;
        return this;
    }

    @Override
    public ILetterRuleBuilder affectionThreshold(int threshold) {
        this.favorabilityThreshold = threshold;
        return this;
    }

    @Override
    public ILetterRuleBuilder generator(ILetterGenerator generator) {
        this.generator = generator;
        return this;
    }

    @Override
    public ILetterRuleBuilder maidId(String maidId) {
        this.requiredModelIds.add(ResourceLocation.parse(maidId));
        return this;
    }

    @Override
    public ILetterRuleBuilder maidIds(List<String> maidIds) {
        for (String id : maidIds) {
            this.requiredModelIds.add(ResourceLocation.parse(id));
        }
        return this;
    }

    @Override
    public ILetterRule build() {
        if (id == null) {
            throw new IllegalStateException("Letter rule ID must be set");
        }
        if (generator == null) {
            throw new IllegalStateException("Letter generator must be set");
        }
        if (triggers.isEmpty()) {
            throw new IllegalStateException("At least one trigger must be set");
        }

        return new LetterRule(id, minAffection, maxAffection, triggers, triggerType, cooldown, favorabilityChange, favorabilityThreshold, generator, requiredModelIds);
    }

    /**
     * 创建AI信件生成器
     *
     * @param tone   语调
     * @param prompt 提示词
     */
    public ILetterRuleBuilder aiGenerator(String tone, String prompt) {
        this.generator = LetterGenerators.createAIGenerator(tone, prompt);
        return this;
    }

    /**
     * 创建预设信件生成器
     *
     * @param title      标题
     * @param message    内容
     * @param postcardId 明信片ID
     * @param parcelId   包裹ID
     */
    public ILetterRuleBuilder presetGenerator(String title, String message, String postcardId, String parcelId) {
        this.generator = LetterGenerators.createPresetGenerator(title, message, postcardId, parcelId);
        return this;
    }

    /**
     * 设置为一次性触发器
     */
    public ILetterRuleBuilder once() {
        this.triggerType = ILetterRule.TriggerType.ONCE;
        return this;
    }

    /**
     * 设置为重复触发器
     */
    public ILetterRuleBuilder repeat() {
        this.triggerType = ILetterRule.TriggerType.REPEAT;
        return this;
    }

    /**
     * 构建并注册信件规则
     */
    @Override
    public ILetterRule register() {
        ILetterRule rule = build();
        LetterRuleRegistry.registerRule(rule);
        return rule;
    }
}
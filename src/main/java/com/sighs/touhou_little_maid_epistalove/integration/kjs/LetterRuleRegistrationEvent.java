package com.sighs.touhou_little_maid_epistalove.integration.kjs;

import com.sighs.touhou_little_maid_epistalove.api.integration.ILetterRuleBuilder;
import com.sighs.touhou_little_maid_epistalove.api.letter.ILetterRule;
import com.sighs.touhou_little_maid_epistalove.data.LetterRuleRegistry;
import dev.latvian.mods.kubejs.event.KubeEvent;

public class LetterRuleRegistrationEvent implements KubeEvent {

    /**
     * 创建新的信件规则构建器
     *
     * @return 信件规则构建器
     */
    public ILetterRuleBuilder create() {
        return new LetterRuleBuilder();
    }

    /**
     * 注册信件规则
     *
     * @param rule 信件规则
     */
    public void register(ILetterRule rule) {
        LetterRuleRegistry.registerRule(rule);
    }

    /**
     * 移除信件规则
     *
     * @param ruleId 规则ID
     */
    public void remove(String ruleId) {
        LetterRuleRegistry.removeRule(ruleId);
    }

    /**
     * 创建AI信件规则
     *
     * @param id     规则ID
     * @param tone   语调
     * @param prompt 提示词
     */
    public ILetterRuleBuilder createAI(String id, String tone, String prompt) {
        return create()
                .id(id)
                .aiGenerator(tone, prompt);
    }

    /**
     * 创建预设信件规则
     *
     * @param id         规则ID
     * @param title      标题
     * @param message    内容
     * @param postcardId 明信片ID
     * @param parcelId   包裹ID
     */
    public ILetterRuleBuilder createPreset(String id, String title, String message, String postcardId, String parcelId) {
        return create()
                .id(id)
                .presetGenerator(title, message, postcardId, parcelId);
    }
}
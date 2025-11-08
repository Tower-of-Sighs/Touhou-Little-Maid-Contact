package com.sighs.touhou_little_maid_contact.integration.kjs;

import com.sighs.touhou_little_maid_contact.api.integration.ILetterRuleBuilder;
import com.sighs.touhou_little_maid_contact.api.letter.ILetterRule;
import com.sighs.touhou_little_maid_contact.api.trigger.ITriggerManager;
import com.sighs.touhou_little_maid_contact.data.LetterRuleRegistry;
import com.sighs.touhou_little_maid_contact.trigger.TriggerManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class ContactLetterAPI {
    private static final ITriggerManager TRIGGER_MANAGER = TriggerManager.getInstance();

    private ContactLetterAPI() {
    }

    /**
     * 创建新的信件规则构建器
     *
     * @return 信件规则构建器
     */
    public static ILetterRuleBuilder createRule() {
        return new LetterRuleBuilder();
    }

    /**
     * 注册信件规则
     *
     * @param rule 信件规则
     */
    public static void registerRule(ILetterRule rule) {
        LetterRuleRegistry.registerRule(rule);
    }

    /**
     * 移除信件规则
     *
     * @param ruleId 规则ID
     */
    public static void removeRule(String ruleId) {
        LetterRuleRegistry.removeRule(ruleId);
    }

    /**
     * 触发玩家事件
     *
     * @param player    玩家
     * @param triggerId 触发器ID字符串
     */
    public static void triggerEvent(ServerPlayer player, String triggerId) {
        TRIGGER_MANAGER.markTriggered(player, new ResourceLocation(triggerId));
    }


    /**
     * 检查玩家是否有指定触发器
     *
     * @param player    玩家
     * @param triggerId 触发器ID字符串
     * @return 是否有该触发器
     */
    public static boolean hasTriggered(ServerPlayer player, String triggerId) {
        return TRIGGER_MANAGER.hasTriggered(player, new ResourceLocation(triggerId));
    }

    /**
     * 清除玩家的指定触发器
     *
     * @param player    玩家
     * @param triggerId 触发器ID字符串
     */
    public static void clearTrigger(ServerPlayer player, String triggerId) {
        TRIGGER_MANAGER.clearTriggered(player, new ResourceLocation(triggerId));
    }

    /**
     * 清除玩家的所有触发器
     *
     * @param player 玩家
     */
    public static void clearAllTriggers(ServerPlayer player) {
        TRIGGER_MANAGER.clearAllTriggered(player);
    }


    // 查询一次性消费（自定义触发器）
    public static boolean hasConsumedOnce(ServerPlayer player, String ruleId, String triggerId) {
        ResourceLocation key = makeCustomConsumeKey(ruleId, triggerId);
        return TRIGGER_MANAGER.hasConsumedOnce(player, key);
    }

    // 清除一次性消费（自定义触发器）
    public static void clearConsumedOnce(ServerPlayer player, String ruleId, String triggerId) {
        ResourceLocation key = makeCustomConsumeKey(ruleId, triggerId);
        TRIGGER_MANAGER.clearConsumedOnce(player, key);
    }

    private static ResourceLocation makeCustomConsumeKey(String ruleId, String triggerId) {
        String normRule = ruleId.replace(":", "_");
        String normTrig = triggerId.replace(":", "_");
        return new ResourceLocation("internal", "custom_" + normRule + "_" + normTrig);
    }
}
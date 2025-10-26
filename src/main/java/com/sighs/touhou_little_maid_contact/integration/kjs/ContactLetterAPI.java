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
     * @param triggerId 触发器ID
     */
    public static void triggerEvent(ServerPlayer player, ResourceLocation triggerId) {
        TRIGGER_MANAGER.markTriggered(player, triggerId);
    }

    /**
     * 触发玩家事件（字符串形式）
     *
     * @param player    玩家
     * @param triggerId 触发器ID字符串
     */
    public static void triggerEvent(ServerPlayer player, String triggerId) {
        triggerEvent(player, ResourceLocation.parse(triggerId));
    }

    /**
     * 检查玩家是否有指定触发器
     *
     * @param player    玩家
     * @param triggerId 触发器ID
     * @return 是否有该触发器
     */
    public static boolean hasTriggered(ServerPlayer player, ResourceLocation triggerId) {
        return TRIGGER_MANAGER.hasTriggered(player, triggerId);
    }

    /**
     * 检查玩家是否有指定触发器（字符串形式）
     *
     * @param player    玩家
     * @param triggerId 触发器ID字符串
     * @return 是否有该触发器
     */
    public static boolean hasTriggered(ServerPlayer player, String triggerId) {
        return hasTriggered(player, ResourceLocation.parse(triggerId));
    }

    /**
     * 清除玩家的指定触发器
     *
     * @param player    玩家
     * @param triggerId 触发器ID
     */
    public static void clearTrigger(ServerPlayer player, ResourceLocation triggerId) {
        TRIGGER_MANAGER.clearTriggered(player, triggerId);
    }

    /**
     * 清除玩家的指定触发器（字符串形式）
     *
     * @param player    玩家
     * @param triggerId 触发器ID字符串
     */
    public static void clearTrigger(ServerPlayer player, String triggerId) {
        clearTrigger(player, ResourceLocation.parse(triggerId));
    }

    /**
     * 清除玩家的所有触发器
     *
     * @param player 玩家
     */
    public static void clearAllTriggers(ServerPlayer player) {
        TRIGGER_MANAGER.clearAllTriggered(player);
    }
}
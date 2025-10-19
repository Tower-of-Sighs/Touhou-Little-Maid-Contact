package com.sighs.touhou_little_maid_contact.api.trigger;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public interface ITriggerManager {

    /**
     * 标记触发器为已触发状态
     *
     * @param player    玩家
     * @param triggerId 触发器ID
     */
    void markTriggered(ServerPlayer player, ResourceLocation triggerId);

    /**
     * 检查玩家是否有指定的触发器
     *
     * @param player    玩家
     * @param triggerId 触发器ID
     * @return 是否有该触发器
     */
    boolean hasTriggered(ServerPlayer player, ResourceLocation triggerId);

    /**
     * 消耗触发器（用于一次性触发器）
     *
     * @param player    玩家
     * @param triggerId 触发器ID
     * @return 是否成功消耗（如果触发器存在则返回true并移除）
     */
    boolean consumeTriggered(ServerPlayer player, ResourceLocation triggerId);

    /**
     * 清除指定触发器
     *
     * @param player    玩家
     * @param triggerId 触发器ID
     */
    void clearTriggered(ServerPlayer player, ResourceLocation triggerId);

    /**
     * 清除玩家的所有触发器
     *
     * @param player 玩家
     */
    void clearAllTriggered(ServerPlayer player);
}
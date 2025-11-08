package com.sighs.touhou_little_maid_contact.api.letter;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Consumer;

public interface ILetterRule {

    /**
     * 获取规则ID
     *
     * @return 规则的唯一标识符
     */
    String getId();

    /**
     * 获取最小好感度要求
     *
     * @return 触发此规则所需的最小好感度
     */
    int getMinAffection();

    /**
     * 获取最大好感度限制
     *
     * @return 触发此规则的最大好感度限制，如果没有限制则返回null
     */
    Integer getMaxAffection();

    /**
     * 获取触发器列表
     *
     * @return 触发此规则的触发器ID列表
     */
    List<ResourceLocation> getTriggers();

    /**
     * 获取触发器类型
     *
     * @return 触发器类型（一次性或重复）
     */
    TriggerType getTriggerType();

    /**
     * 获取冷却时间
     *
     * @return 规则的冷却时间（tick），如果没有冷却则返回null
     */
    Integer getCooldown();

    /**
     * 检查规则是否匹配当前条件
     *
     * @param owner    女仆的主人
     * @param maid     女仆实体
     * @param gameTime 当前游戏时间
     * @return 是否匹配条件
     */
    boolean matches(ServerPlayer owner, EntityMaid maid, long gameTime);

    /**
     * 生成信件
     *
     * @param owner    女仆的主人
     * @param maid     女仆实体
     * @param callback 生成完成后的回调
     */
    void generateLetter(ServerPlayer owner, EntityMaid maid, Consumer<ItemStack> callback);

    /**
     * 获取规则类型
     *
     * @return 规则类型标识（如"ai"、"preset"等）
     */
    String getType();

    /**
     * 触发器类型枚举
     */
    enum TriggerType {
        /**
         * 一次性触发器，触发后会被消耗
         */
        ONCE,
        /**
         * 重复触发器，可以多次触发
         */
        REPEAT
    }

    /**
     * 获取要求的女仆模型ID列表（可选，空或null表示不限制）
     */
    List<ResourceLocation> getRequiredMaidIds();
}
package com.sighs.touhou_little_maid_epistalove.ai.parser;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.world.item.ItemStack;

/**
 * 信件解析器接口
 * 用于解析AI生成的文本内容为信件物品
 */
public interface ILetterParser {

    /**
     * 解析AI生成的文本为信件物品
     *
     * @param content    AI生成的原始文本内容
     * @param senderName 发送者名称
     * @param maid       女仆实体（可选，用于记忆系统）
     * @return 解析后的信件物品，如果解析失败则返回空物品
     */
    ItemStack parseToLetter(String content, String senderName, EntityMaid maid);

    /**
     * 验证生成内容的质量
     *
     * @param title   信件标题
     * @param message 信件内容
     * @return 内容是否符合质量要求
     */
    boolean isContentValid(String title, String message);
}
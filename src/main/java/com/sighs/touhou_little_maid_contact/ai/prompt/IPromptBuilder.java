package com.sighs.touhou_little_maid_contact.ai.prompt;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.server.level.ServerPlayer;

public interface IPromptBuilder {

    /**
     * 构建系统提示词
     *
     * @param tone  语调风格
     * @param maid  女仆实体
     * @param owner 女仆主人
     * @return 构建完成的系统提示词
     */
    String buildSystemPrompt(String tone, EntityMaid maid, ServerPlayer owner);

    /**
     * 记录生成的内容到记忆系统
     *
     * @param maidId  女仆ID
     * @param title   信件标题
     * @param message 信件内容
     */
    void recordGeneratedContent(String maidId, String title, String message);

    /**
     * 清理指定女仆的记忆
     *
     * @param maidId 女仆ID
     */
    void clearMemory(String maidId);
}
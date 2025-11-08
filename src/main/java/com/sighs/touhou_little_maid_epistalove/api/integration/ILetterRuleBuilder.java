package com.sighs.touhou_little_maid_epistalove.api.integration;

import com.sighs.touhou_little_maid_epistalove.api.letter.ILetterGenerator;
import com.sighs.touhou_little_maid_epistalove.api.letter.ILetterRule;

import java.util.List;

public interface ILetterRuleBuilder {

    /**
     * 设置规则ID
     *
     * @param id 规则ID
     */
    ILetterRuleBuilder id(String id);


    /**
     * 设置最小好感度要求
     *
     * @param minAffection 最小好感度
     */
    ILetterRuleBuilder minAffection(int minAffection);

    /**
     * 设置最大好感度限制
     *
     * @param maxAffection 最大好感度
     */
    ILetterRuleBuilder maxAffection(int maxAffection);

    /**
     * 添加触发器
     *
     * @param triggerId 触发器ID
     */
    ILetterRuleBuilder trigger(String triggerId);


    /**
     * 添加多个触发器
     *
     * @param triggerIds 触发器ID列表
     */
    ILetterRuleBuilder triggers(List<String> triggerIds);

    /**
     * 设置触发器类型
     *
     * @param triggerType 触发器类型
     */
    ILetterRuleBuilder triggerType(ILetterRule.TriggerType triggerType);

    /**
     * 设置冷却时间
     *
     * @param cooldown 冷却时间（tick）
     */
    ILetterRuleBuilder cooldown(int cooldown);

    /**
     * 设置每次送信的好感度变化（正数升，负数降）
     *
     * @param delta 好感度变化值
     */
    ILetterRuleBuilder affectionChange(int delta);

    /**
     * 设置好感度升/降的阈值（达到该值后不再继续变动）
     * 
     * @param threshold 好感度阈值
     */
    ILetterRuleBuilder affectionThreshold(int threshold);

    /**
     * 设置信件生成器
     *
     * @param generator 信件生成器
     */
    ILetterRuleBuilder generator(ILetterGenerator generator);

    /**
     * 限定女仆模型ID（单个），不设置则不限制
     *
     * @param maidId 模型资源ID
     */
    ILetterRuleBuilder maidId(String maidId);

    /**
     * 限定多组女仆模型ID（批量），不设置则不限制
     *
     * @param maidId 模型资源ID
     */
    ILetterRuleBuilder maidIds(List<String> maidId);

    /**
     * 构建信件规则
     *
     * @return 构建完成的信件规则
     */
    ILetterRule build();

    /**
     * 创建AI信件生成器
     *
     * @param tone   语调
     * @param prompt 提示词
     */
    ILetterRuleBuilder aiGenerator(String tone, String prompt);

    /**
     * 创建预设信件生成器
     *
     * @param title      标题
     * @param message    内容
     * @param postcardId 明信片ID
     * @param parcelId   包裹ID
     */
    ILetterRuleBuilder presetGenerator(String title, String message, String postcardId, String parcelId);

    /**
     * 设置为一次性触发器
     */
    ILetterRuleBuilder once();

    /**
     * 设置为重复触发器
     */
    ILetterRuleBuilder repeat();

    /**
     * 构建并注册信件规则
     * 这是一个终端操作，会构建规则并自动注册
     *
     * @return 构建并注册的信件规则
     */
    ILetterRule register();
}
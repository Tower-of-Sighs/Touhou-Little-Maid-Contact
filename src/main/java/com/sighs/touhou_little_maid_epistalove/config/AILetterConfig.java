package com.sighs.touhou_little_maid_epistalove.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class AILetterConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final double DEFAULT_CREATIVITY_TEMPERATURE_BOOST = 0.3;
    public static final int DEFAULT_MEMORY_SIZE = 10;
    public static final boolean DEFAULT_ENABLE_CONTEXT_ENRICHMENT = true;
    public static final boolean DEFAULT_ENABLE_QUALITY_FILTER = true;
    public static final int DEFAULT_MIN_CONTENT_LENGTH = 10;
    public static final int DEFAULT_MAX_GENERIC_PHRASES = 2;

    public static final ForgeConfigSpec.DoubleValue CREATIVITY_TEMPERATURE_BOOST;
    public static final ForgeConfigSpec.IntValue MEMORY_SIZE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_CONTEXT_ENRICHMENT;
    public static final ForgeConfigSpec.BooleanValue ENABLE_QUALITY_FILTER;
    public static final ForgeConfigSpec.IntValue MIN_CONTENT_LENGTH;
    public static final ForgeConfigSpec.IntValue MAX_GENERIC_PHRASES;

    static {
        BUILDER.push("ai_letter_generation");

        CREATIVITY_TEMPERATURE_BOOST = BUILDER
                .comment("AI写信时的创意温度增强值（范围：0.0～0.5，值越高越有创意）")
                .defineInRange("creativity_temperature_boost", DEFAULT_CREATIVITY_TEMPERATURE_BOOST, 0.0, 0.5);

        MEMORY_SIZE = BUILDER
                .comment("记忆中保存的最近信件数量，用于避免内容重复")
                .defineInRange("memory_size", DEFAULT_MEMORY_SIZE, 5, 50);

        ENABLE_CONTEXT_ENRICHMENT = BUILDER
                .comment("是否启用上下文增强功能（如时间、环境、女仆状态等动态信息注入）")
                .define("enable_context_enrichment", DEFAULT_ENABLE_CONTEXT_ENRICHMENT);

        ENABLE_QUALITY_FILTER = BUILDER
                .comment("是否启用内容质量过滤器，自动过滤低质量或不当内容")
                .define("enable_quality_filter", DEFAULT_ENABLE_QUALITY_FILTER);

        MIN_CONTENT_LENGTH = BUILDER
                .comment("生成信件内容的最小长度（字符数），防止内容过短")
                .defineInRange("min_content_length", DEFAULT_MIN_CONTENT_LENGTH, 5, 50);

        MAX_GENERIC_PHRASES = BUILDER
                .comment("单封信中允许出现的通用套话最大数量，超过则判定为质量不合格")
                .defineInRange("max_generic_phrases", DEFAULT_MAX_GENERIC_PHRASES, 0, 5);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
package com.sighs.touhou_little_maid_contact.config;


import net.neoforged.neoforge.common.ModConfigSpec;

public class AILetterConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.DoubleValue CREATIVITY_TEMPERATURE_BOOST;
    public static final ModConfigSpec.IntValue MEMORY_SIZE;
    public static final ModConfigSpec.BooleanValue ENABLE_CONTEXT_ENRICHMENT;
    public static final ModConfigSpec.BooleanValue ENABLE_QUALITY_FILTER;
    public static final ModConfigSpec.IntValue MIN_CONTENT_LENGTH;
    public static final ModConfigSpec.IntValue MAX_GENERIC_PHRASES;

    static {
        BUILDER.push("ai_letter_generation");

        CREATIVITY_TEMPERATURE_BOOST = BUILDER
                .comment("AI写信时的创意温度增强值（范围：0.0～0.5，值越高越有创意）")
                .defineInRange("creativity_temperature_boost", 0.3, 0.0, 0.5);

        MEMORY_SIZE = BUILDER
                .comment("记忆中保存的最近信件数量，用于避免内容重复")
                .defineInRange("memory_size", 10, 5, 50);

        ENABLE_CONTEXT_ENRICHMENT = BUILDER
                .comment("是否启用上下文增强功能（如时间、环境、女仆状态等动态信息注入）")
                .define("enable_context_enrichment", true);

        ENABLE_QUALITY_FILTER = BUILDER
                .comment("是否启用内容质量过滤器，自动过滤低质量或不当内容")
                .define("enable_quality_filter", true);

        MIN_CONTENT_LENGTH = BUILDER
                .comment("生成信件内容的最小长度（字符数），防止内容过短")
                .defineInRange("min_content_length", 10, 5, 50);

        MAX_GENERIC_PHRASES = BUILDER
                .comment("单封信中允许出现的通用套话最大数量，超过则判定为质量不合格")
                .defineInRange("max_generic_phrases", 2, 0, 5);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
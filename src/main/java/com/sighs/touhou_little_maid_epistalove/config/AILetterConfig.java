package com.sighs.touhou_little_maid_epistalove.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

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

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> EXPRESSION_TECHNIQUES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CONTEXT_TEMPLATES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> TIME_DESCRIPTIONS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> WEATHER_DESCRIPTIONS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> EMOTIONAL_STATES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CREATIVITY_TIPS;

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

        // 关键提示词集合（字符串列表）
        EXPRESSION_TECHNIQUES = BUILDER
                .comment("用于多样化写作风格的技巧列表（字符串），随机采样使用")
                .defineListAllowEmpty("expression_techniques",
                        List.of(
                                "运用生动的细节描写",
                                "加入感官体验的描述",
                                "使用比喻和拟人手法",
                                "营造特定的氛围感",
                                "运用对比和层次感",
                                "加入动作和场景描写",
                                "使用富有画面感的词汇",
                                "创造独特的表达角度"
                        ),
                        o -> o instanceof String);

        CONTEXT_TEMPLATES = BUILDER
                .comment("用于情境描述的模板，需包含两个 %s 占位符（天气、情绪）")
                .defineListAllowEmpty("context_templates",
                        List.of(
                                "在这个%s的%s，",
                                "当%s轻抚过窗台时，",
                                "在%s的陪伴下，",
                                "望着%s的天空，",
                                "听着%s的声音，",
                                "感受着%s的气息，",
                                "在这个特别的时刻，",
                                "伴随着%s的心情，"
                        ),
                        o -> o instanceof String);

        TIME_DESCRIPTIONS = BUILDER
                .comment("用于上下文的时间描述（如：清晨、黄昏、夜晚）")
                .defineListAllowEmpty("time_descriptions",
                        List.of("清晨", "午后", "黄昏", "夜晚", "深夜", "黎明", "正午", "傍晚"),
                        o -> o instanceof String);

        WEATHER_DESCRIPTIONS = BUILDER
                .comment("用于上下文的天气描述（如：微风、细雨、阳光）")
                .defineListAllowEmpty("weather_descriptions",
                        List.of("微风", "细雨", "阳光", "月光", "星光", "雪花", "云朵", "晨露"),
                        o -> o instanceof String);

        EMOTIONAL_STATES = BUILDER
                .comment("用于上下文的情绪描述（如：温柔、欣喜、宁静）")
                .defineListAllowEmpty("emotional_states",
                        List.of("温柔", "欣喜", "宁静", "期待", "思念", "满足", "好奇", "关怀"),
                        o -> o instanceof String);

        CREATIVITY_TIPS = BUILDER
                .comment("用于丰富措辞并降低重复的创意提示列表")
                .defineListAllowEmpty("creativity_tips",
                        List.of(
                                "尝试使用比喻或拟人的手法",
                                "可以加入一些小细节，比如声音、气味、触感等",
                                "试着从不同的角度来描述同一件事",
                                "可以使用一些诗意的表达",
                                "尝试营造特定的氛围或情绪",
                                "可以加入一些想象力丰富的元素",
                                "试着用对话或内心独白的形式",
                                "可以使用一些文学性的修辞手法"
                        ),
                        o -> o instanceof String);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
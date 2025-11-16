package com.sighs.touhou_little_maid_epistalove.config;


import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.List;

public class AILetterConfig {
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;

    public static final double DEFAULT_CREATIVITY_TEMPERATURE_BOOST = 0.3;
    public static final int DEFAULT_MEMORY_SIZE = 10;
    public static final boolean DEFAULT_ENABLE_CONTEXT_ENRICHMENT = true;
    public static final boolean DEFAULT_ENABLE_QUALITY_FILTER = true;
    public static final int DEFAULT_MIN_CONTENT_LENGTH = 10;
    public static final int DEFAULT_MAX_GENERIC_PHRASES = 2;

    public static final ModConfigSpec.DoubleValue CREATIVITY_TEMPERATURE_BOOST;
    public static final ModConfigSpec.IntValue MEMORY_SIZE;
    public static final ModConfigSpec.BooleanValue ENABLE_CONTEXT_ENRICHMENT;
    public static final ModConfigSpec.BooleanValue ENABLE_QUALITY_FILTER;
    public static final ModConfigSpec.IntValue MIN_CONTENT_LENGTH;
    public static final ModConfigSpec.IntValue MAX_GENERIC_PHRASES;

    public static final ModConfigSpec.ConfigValue<List<? extends String>> EXPRESSION_TECHNIQUES;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> CONTEXT_TEMPLATES;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> TIME_DESCRIPTIONS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> WEATHER_DESCRIPTIONS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> EMOTIONAL_STATES;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> CREATIVITY_TIPS;

    private static final String TRANSLATE_KEY = "config.touhou_little_maid_epistalove.ai_letter";
    private static String translateKey(String key) {
        return TRANSLATE_KEY + "." + key;
    }

    static {
        // Category translation
        BUILDER.translation(TRANSLATE_KEY).push("ai_letter_generation");

        CREATIVITY_TEMPERATURE_BOOST = BUILDER
                .comment("Extra creativity temperature applied when generating letters (0.0–0.5, higher = more creative)")
                .translation(translateKey("creativity_temperature_boost"))
                .defineInRange("creativity_temperature_boost", 0.3, 0.0, 0.5);

        MEMORY_SIZE = BUILDER
                .comment("Number of recent letters kept in memory to avoid repeated content")
                .translation(translateKey("memory_size"))
                .defineInRange("memory_size", 10, 5, 50);

        ENABLE_CONTEXT_ENRICHMENT = BUILDER
                .comment("Whether to enable context enrichment (inject dynamic info like time, environment, maid status)")
                .translation(translateKey("enable_context_enrichment"))
                .define("enable_context_enrichment", true);

        ENABLE_QUALITY_FILTER = BUILDER
                .comment("Whether to enable the quality filter to block low-quality or inappropriate content")
                .translation(translateKey("enable_quality_filter"))
                .define("enable_quality_filter", true);

        MIN_CONTENT_LENGTH = BUILDER
                .comment("Minimum length of generated letter content (characters) to avoid being too short")
                .translation(translateKey("min_content_length"))
                .defineInRange("min_content_length", 10, 5, 50);

        MAX_GENERIC_PHRASES = BUILDER
                .comment("Maximum number of generic phrases allowed per letter; exceeding marks content as low quality")
                .translation(translateKey("max_generic_phrases"))
                .defineInRange("max_generic_phrases", 2, 0, 5);

        // 关键提示词集合（字符串列表）
        EXPRESSION_TECHNIQUES = BUILDER
                .comment("List of expression techniques used to diversify style")
                .translation(translateKey("expression_techniques"))
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
                .comment("Context templates; should contain two %s placeholders for weather and emotion")
                .translation(translateKey("context_templates"))
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
                .comment("Time descriptions for dynamic context")
                .translation(translateKey("time_descriptions"))
                .defineListAllowEmpty("time_descriptions",
                        List.of("清晨", "午后", "黄昏", "夜晚", "深夜", "黎明", "正午", "傍晚"),
                        o -> o instanceof String);

        WEATHER_DESCRIPTIONS = BUILDER
                .comment("Weather descriptors for dynamic context")
                .translation(translateKey("weather_descriptions"))
                .defineListAllowEmpty("weather_descriptions",
                        List.of("微风", "细雨", "阳光", "月光", "星光", "雪花", "云朵", "晨露"),
                        o -> o instanceof String);

        EMOTIONAL_STATES = BUILDER
                .comment("Emotion descriptors for dynamic context")
                .translation(translateKey("emotional_states"))
                .defineListAllowEmpty("emotional_states",
                        List.of("温柔", "欣喜", "宁静", "期待", "思念", "满足", "好奇", "关怀"),
                        o -> o instanceof String);

        CREATIVITY_TIPS = BUILDER
                .comment("Creativity tips to enrich wording variety")
                .translation(translateKey("creativity_tips"))
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
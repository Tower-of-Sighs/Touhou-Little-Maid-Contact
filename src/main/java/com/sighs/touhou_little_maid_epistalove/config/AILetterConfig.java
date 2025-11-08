package com.sighs.touhou_little_maid_epistalove.config;


import net.neoforged.neoforge.common.ModConfigSpec;

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

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}
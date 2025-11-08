package com.sighs.touhou_little_maid_epistalove.config;


import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    public static final ModConfigSpec SERVER_SPEC;

    public static final int DEFAULT_MAILBOX_SEARCH_RADIUS = 16;
    public static final int DEFAULT_MAILBOX_MIN_SAFETY_SCORE = 60;
    public static final int DEFAULT_AREA_HAZARD_THRESHOLD = 60;
    public static final int DEFAULT_HIGH_QUALITY_THRESHOLD = 80;
    public static final int DEFAULT_PATH_SAFETY_PERCENTAGE = 65;
    public static final int DEFAULT_MAX_CONSECUTIVE_DANGEROUS = 2;

    public static final ModConfigSpec.IntValue MAILBOX_SEARCH_RADIUS;
    public static final ModConfigSpec.IntValue MAILBOX_MIN_SAFETY_SCORE;
    public static final ModConfigSpec.IntValue AREA_HAZARD_THRESHOLD;
    public static final ModConfigSpec.IntValue HIGH_QUALITY_THRESHOLD;
    public static final ModConfigSpec.IntValue PATH_SAFETY_PERCENTAGE;
    public static final ModConfigSpec.IntValue MAX_CONSECUTIVE_DANGEROUS;

    private static final String TRANSLATE_MAIL = "config.touhou_little_maid_epistalove.mail_delivery";
    private static final String TRANSLATE_SAFETY = "config.touhou_little_maid_epistalove.safety_evaluation";
    private static final String TRANSLATE_PATH = "config.touhou_little_maid_epistalove.pathfinding";

    private static String translateKey(String base, String key) {
        return base + "." + key;
    }

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        // Mail Delivery
        builder.translation(TRANSLATE_MAIL).push("mail_delivery");
        MAILBOX_SEARCH_RADIUS = builder
                .comment("Maximum radius (blocks) for maids to search for a mailbox; larger radius increases performance cost")
                .translation(translateKey(TRANSLATE_MAIL, "search_radius"))
                .defineInRange("mailbox_search_radius", 16, 4, 32);
        builder.pop();

        // Safety Evaluation
        builder.translation(TRANSLATE_SAFETY).push("safety_evaluation");
        MAILBOX_MIN_SAFETY_SCORE = builder
                .comment("Minimum safety score (0–100) required for a mailbox to be considered usable")
                .translation(translateKey(TRANSLATE_SAFETY, "mailbox_min_safety_score"))
                .defineInRange("mailbox_min_safety_score", 60, 0, 100);

        AREA_HAZARD_THRESHOLD = builder
                .comment("Area hazard threshold (0–100); areas above this value are considered too dangerous (lower = more cautious)")
                .translation(translateKey(TRANSLATE_SAFETY, "area_hazard_threshold"))
                .defineInRange("area_hazard_threshold", 60, 0, 100);

        HIGH_QUALITY_THRESHOLD = builder
                .comment("Safety score threshold (0–100) for high-quality mailboxes (preferred when the master is at home)")
                .translation(translateKey(TRANSLATE_SAFETY, "high_quality_threshold"))
                .defineInRange("high_quality_threshold", 80, 0, 100);
        builder.pop();

        // Pathfinding
        builder.translation(TRANSLATE_PATH).push("pathfinding");
        PATH_SAFETY_PERCENTAGE = builder
                .comment("Minimum required percentage (0–100) of safe nodes on a path; higher = safer paths")
                .translation(translateKey(TRANSLATE_PATH, "path_safety_percentage"))
                .defineInRange("path_safety_percentage", 65, 0, 100);

        MAX_CONSECUTIVE_DANGEROUS = builder
                .comment("Maximum allowed number of consecutive dangerous nodes; paths exceeding this count will be rejected")
                .translation(translateKey(TRANSLATE_PATH, "max_consecutive_dangerous"))
                .defineInRange("max_consecutive_dangerous", 2, 0, 10);
        builder.pop();

        SERVER_SPEC = builder.build();
    }

    public static void register(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, SERVER_SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, AILetterConfig.SPEC);
    }
}

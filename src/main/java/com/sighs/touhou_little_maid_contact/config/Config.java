package com.sighs.touhou_little_maid_contact.config;


import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    public static final ModConfigSpec SERVER_SPEC;

    public static final ModConfigSpec.IntValue MAILBOX_SEARCH_RADIUS;

    public static final ModConfigSpec.IntValue MAILBOX_MIN_SAFETY_SCORE;
    public static final ModConfigSpec.IntValue AREA_HAZARD_THRESHOLD;
    public static final ModConfigSpec.IntValue HIGH_QUALITY_THRESHOLD;

    public static final ModConfigSpec.IntValue PATH_SAFETY_PERCENTAGE;
    public static final ModConfigSpec.IntValue MAX_CONSECUTIVE_DANGEROUS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("mail_delivery");
        MAILBOX_SEARCH_RADIUS = builder
                .comment("女仆搜索邮筒的最大半径（格），值越大搜索范围越广但性能消耗越高")
                .defineInRange("mailbox_search_radius", 16, 4, 32);
        builder.pop();

        builder.push("safety_evaluation");
        MAILBOX_MIN_SAFETY_SCORE = builder
                .comment("邮筒被认为可用的最低安全分数（0-100），分数越高要求越严格")
                .defineInRange("mailbox_min_safety_score", 60, 0, 100);

        AREA_HAZARD_THRESHOLD = builder
                .comment("区域危险度阈值（0-100），超过此值的区域被认为过于危险，值越低越谨慎（有的可到达也会判断为危险）")
                .defineInRange("area_hazard_threshold", 60, 0, 100);

        HIGH_QUALITY_THRESHOLD = builder
                .comment("高质量邮筒的安全分数阈值（0-100），影响主人在家时的邮筒选择优先级")
                .defineInRange("high_quality_threshold", 80, 0, 100);
        builder.pop();

        builder.push("pathfinding");
        PATH_SAFETY_PERCENTAGE = builder
                .comment("路径安全度最低要求（0-100），路径中安全节点的最小百分比，值越高路径越安全")
                .defineInRange("path_safety_percentage", 65, 0, 100);

        MAX_CONSECUTIVE_DANGEROUS = builder
                .comment("路径中允许的最大连续危险节点数，超过此数量的路径会被拒绝")
                .defineInRange("max_consecutive_dangerous", 2, 0, 10);
        builder.pop();

        SERVER_SPEC = builder.build();
    }

    public static void register(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, SERVER_SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, AILetterConfig.SPEC);
    }
}

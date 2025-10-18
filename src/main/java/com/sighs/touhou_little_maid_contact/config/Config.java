package com.sighs.touhou_little_maid_contact.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public final class Config {
    public static final ForgeConfigSpec SERVER_SPEC;
    public static final ForgeConfigSpec.IntValue MAILBOX_SEARCH_RADIUS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("mail");
        MAILBOX_SEARCH_RADIUS = builder
                .comment("搜索邮筒的半径（格）")
                .defineInRange("mailbox_search_radius", 16, 4, 128);
        builder.pop();
        SERVER_SPEC = builder.build();
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SERVER_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, AILetterConfig.SPEC);
    }
}

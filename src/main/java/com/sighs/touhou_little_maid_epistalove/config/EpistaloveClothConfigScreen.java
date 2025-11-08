package com.sighs.touhou_little_maid_epistalove.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

public final class EpistaloveClothConfigScreen {
    private EpistaloveClothConfigScreen() {
    }

    public static ConfigBuilder getConfigBuilder() {
        ConfigBuilder root = ConfigBuilder.create()
                .setTitle(Component.translatable("config.touhou_little_maid_epistalove.title"));
        root.setGlobalized(true);
        root.setGlobalizedExpanded(false);

        ConfigEntryBuilder entry = root.entryBuilder();

        // AI 写信（客户端配置）
        ConfigCategory aiLetter = root.getOrCreateCategory(
                Component.translatable("config.touhou_little_maid_epistalove.ai_letter"));

        aiLetter.addEntry(entry.startDoubleField(
                        Component.translatable("config.touhou_little_maid_epistalove.ai_letter.creativity_temperature_boost"),
                        AILetterConfig.CREATIVITY_TEMPERATURE_BOOST.get())
                .setDefaultValue(AILetterConfig.DEFAULT_CREATIVITY_TEMPERATURE_BOOST)
                .setMin(0.0)
                .setMax(0.5)
                .setTooltip(Component.translatable("config.touhou_little_maid_epistalove.ai_letter.creativity_temperature_boost.tooltip"))
                .setSaveConsumer(AILetterConfig.CREATIVITY_TEMPERATURE_BOOST::set)
                .build());

        aiLetter.addEntry(entry.startIntField(
                        Component.translatable("config.touhou_little_maid_epistalove.ai_letter.memory_size"),
                        AILetterConfig.MEMORY_SIZE.get())
                .setDefaultValue(AILetterConfig.DEFAULT_MEMORY_SIZE)
                .setMin(5)
                .setMax(50)
                .setTooltip(Component.translatable("config.touhou_little_maid_epistalove.ai_letter.memory_size.tooltip"))
                .setSaveConsumer(AILetterConfig.MEMORY_SIZE::set)
                .build());

        aiLetter.addEntry(entry.startBooleanToggle(
                        Component.translatable("config.touhou_little_maid_epistalove.ai_letter.enable_context_enrichment"),
                        AILetterConfig.ENABLE_CONTEXT_ENRICHMENT.get())
                .setDefaultValue(AILetterConfig.DEFAULT_ENABLE_CONTEXT_ENRICHMENT)
                .setTooltip(Component.translatable("config.touhou_little_maid_epistalove.ai_letter.enable_context_enrichment.tooltip"))
                .setSaveConsumer(AILetterConfig.ENABLE_CONTEXT_ENRICHMENT::set)
                .build());

        aiLetter.addEntry(entry.startBooleanToggle(
                        Component.translatable("config.touhou_little_maid_epistalove.ai_letter.enable_quality_filter"),
                        AILetterConfig.ENABLE_QUALITY_FILTER.get())
                .setDefaultValue(AILetterConfig.DEFAULT_ENABLE_QUALITY_FILTER)
                .setTooltip(Component.translatable("config.touhou_little_maid_epistalove.ai_letter.enable_quality_filter.tooltip"))
                .setSaveConsumer(AILetterConfig.ENABLE_QUALITY_FILTER::set)
                .build());

        aiLetter.addEntry(entry.startIntField(
                        Component.translatable("config.touhou_little_maid_epistalove.ai_letter.min_content_length"),
                        AILetterConfig.MIN_CONTENT_LENGTH.get())
                .setDefaultValue(AILetterConfig.DEFAULT_MIN_CONTENT_LENGTH)
                .setMin(5)
                .setMax(50)
                .setTooltip(Component.translatable("config.touhou_little_maid_epistalove.ai_letter.min_content_length.tooltip"))
                .setSaveConsumer(AILetterConfig.MIN_CONTENT_LENGTH::set)
                .build());

        aiLetter.addEntry(entry.startIntField(
                        Component.translatable("config.touhou_little_maid_epistalove.ai_letter.max_generic_phrases"),
                        AILetterConfig.MAX_GENERIC_PHRASES.get())
                .setDefaultValue(AILetterConfig.DEFAULT_MAX_GENERIC_PHRASES)
                .setMin(0)
                .setMax(5)
                .setTooltip(Component.translatable("config.touhou_little_maid_epistalove.ai_letter.max_generic_phrases.tooltip"))
                .setSaveConsumer(AILetterConfig.MAX_GENERIC_PHRASES::set)
                .build());

        // 邮寄投递
        ConfigCategory mailDelivery = root.getOrCreateCategory(
                Component.translatable("config.touhou_little_maid_epistalove.mail_delivery"));

        mailDelivery.addEntry(entry.startIntSlider(
                        Component.translatable("config.touhou_little_maid_epistalove.mail_delivery.search_radius"),
                        Config.MAILBOX_SEARCH_RADIUS.get(), 4, 32)
                .setDefaultValue(Config.DEFAULT_MAILBOX_SEARCH_RADIUS)
                .setTooltip(Component.translatable("config.touhou_little_maid_epistalove.mail_delivery.search_radius.tooltip"))
                .setSaveConsumer(Config.MAILBOX_SEARCH_RADIUS::set)
                .build());

        // 安全评估
        ConfigCategory safety = root.getOrCreateCategory(
                Component.translatable("config.touhou_little_maid_epistalove.safety_evaluation"));

        safety.addEntry(entry.startIntSlider(
                        Component.translatable("config.touhou_little_maid_epistalove.safety_evaluation.mailbox_min_safety_score"),
                        Config.MAILBOX_MIN_SAFETY_SCORE.get(), 0, 100)
                .setDefaultValue(Config.DEFAULT_MAILBOX_MIN_SAFETY_SCORE)
                .setTooltip(Component.translatable("config.touhou_little_maid_epistalove.safety_evaluation.mailbox_min_safety_score.tooltip"))
                .setSaveConsumer(Config.MAILBOX_MIN_SAFETY_SCORE::set)
                .build());

        safety.addEntry(entry.startIntSlider(
                        Component.translatable("config.touhou_little_maid_epistalove.safety_evaluation.area_hazard_threshold"),
                        Config.AREA_HAZARD_THRESHOLD.get(), 0, 100)
                .setDefaultValue(Config.DEFAULT_AREA_HAZARD_THRESHOLD)
                .setTooltip(Component.translatable("config.touhou_little_maid_epistalove.safety_evaluation.area_hazard_threshold.tooltip"))
                .setSaveConsumer(Config.AREA_HAZARD_THRESHOLD::set)
                .build());

        safety.addEntry(entry.startIntSlider(
                        Component.translatable("config.touhou_little_maid_epistalove.safety_evaluation.high_quality_threshold"),
                        Config.HIGH_QUALITY_THRESHOLD.get(), 0, 100)
                .setDefaultValue(Config.DEFAULT_HIGH_QUALITY_THRESHOLD)
                .setTooltip(Component.translatable("config.touhou_little_maid_epistalove.safety_evaluation.high_quality_threshold.tooltip"))
                .setSaveConsumer(Config.HIGH_QUALITY_THRESHOLD::set)
                .build());

        // 路径规划
        ConfigCategory pathfinding = root.getOrCreateCategory(
                Component.translatable("config.touhou_little_maid_epistalove.pathfinding"));

        pathfinding.addEntry(entry.startIntSlider(
                        Component.translatable("config.touhou_little_maid_epistalove.pathfinding.path_safety_percentage"),
                        Config.PATH_SAFETY_PERCENTAGE.get(), 0, 100)
                .setDefaultValue(Config.DEFAULT_PATH_SAFETY_PERCENTAGE)
                .setTooltip(Component.translatable("config.touhou_little_maid_epistalove.pathfinding.path_safety_percentage.tooltip"))
                .setSaveConsumer(Config.PATH_SAFETY_PERCENTAGE::set)
                .build());

        pathfinding.addEntry(entry.startIntSlider(
                        Component.translatable("config.touhou_little_maid_epistalove.pathfinding.max_consecutive_dangerous"),
                        Config.MAX_CONSECUTIVE_DANGEROUS.get(), 0, 10)
                .setDefaultValue(Config.DEFAULT_MAX_CONSECUTIVE_DANGEROUS)
                .setTooltip(Component.translatable("config.touhou_little_maid_epistalove.pathfinding.max_consecutive_dangerous.tooltip"))
                .setSaveConsumer(Config.MAX_CONSECUTIVE_DANGEROUS::set)
                .build());

        return root;
    }
    public static void registerModsPage(ModContainer modContainer) {
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, (container, parent) -> getConfigBuilder().setParentScreen(parent).build());
    }
}
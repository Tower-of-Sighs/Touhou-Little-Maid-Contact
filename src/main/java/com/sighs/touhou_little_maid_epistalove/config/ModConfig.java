package com.sighs.touhou_little_maid_epistalove.config;

import com.sighs.touhou_little_maid_epistalove.TLMEpistalove;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;

@Config(name = TLMEpistalove.MODID)
public final class ModConfig implements ConfigData {

    @ConfigEntry.Gui.CollapsibleObject
    public AILetterConfig aiLetterConfig = new AILetterConfig();

    @ConfigEntry.Gui.CollapsibleObject
    public MailDelivery mailDelivery = new MailDelivery();

    @ConfigEntry.Gui.CollapsibleObject
    public SafetyEvaluation safetyEvaluation = new SafetyEvaluation();

    @ConfigEntry.Gui.CollapsibleObject
    public Pathfinding pathfinding = new Pathfinding();

    public static ModConfig get() {
        return AutoConfig.getConfigHolder(ModConfig.class).getConfig();
    }

    public static void init() {
        AutoConfig.register(ModConfig.class, JanksonConfigSerializer::new);
    }

    public static class MailDelivery {
        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 4, max = 32)

        public int mailboxSearchRadius = 16;
    }

    public static class SafetyEvaluation {
        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 0, max = 100)

        public int mailboxMinSafetyScore = 60;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 0, max = 100)

        public int areaHazardThreshold = 60;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 0, max = 100)

        public int highQualityThreshold = 80;
    }

    public static class Pathfinding {
        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 0, max = 100)

        public int pathSafetyPercentage = 65;

        @ConfigEntry.Gui.Tooltip
        @ConfigEntry.BoundedDiscrete(min = 0, max = 10)

        public int maxConsecutiveDangerous = 2;
    }
}

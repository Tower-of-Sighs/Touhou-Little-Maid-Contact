package com.sighs.touhou_little_maid_epistalove.config;

import me.shedaniel.autoconfig.annotation.ConfigEntry;

public class AILetterConfig {

    @ConfigEntry.Gui.Tooltip

    public double creativityTemperatureBoost = 0.3;

    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 5, max = 50)

    public int memorySize = 10;

    @ConfigEntry.Gui.Tooltip
    public boolean enableContextEnrichment = true;

    @ConfigEntry.Gui.Tooltip
    public boolean enableQualityFilter = true;

    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 5, max = 50)

    public int minContentLength = 10;

    @ConfigEntry.Gui.Tooltip
    @ConfigEntry.BoundedDiscrete(min = 0, max = 5)

    public int maxGenericPhrases = 2;
}
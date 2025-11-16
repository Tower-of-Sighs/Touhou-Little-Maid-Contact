package com.sighs.touhou_little_maid_epistalove.config;

import me.shedaniel.autoconfig.annotation.ConfigEntry;

import java.util.List;

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
    @ConfigEntry.Gui.Tooltip
    public List<String> expressionTechniques = List.of(
            "运用生动的细节描写",
            "加入感官体验的描述",
            "使用比喻和拟人手法",
            "营造特定的氛围感",
            "运用对比和层次感",
            "加入动作和场景描写",
            "使用富有画面感的词汇",
            "创造独特的表达角度"
    );

    @ConfigEntry.Gui.Tooltip
    public List<String> contextTemplates = List.of(
            "在这个%s的%s，",
            "当%s轻抚过窗台时，",
            "在%s的陪伴下，",
            "望着%s的天空，",
            "听着%s的声音，",
            "感受着%s的气息，",
            "在这个特别的时刻，",
            "伴随着%s的心情，"
    );

    @ConfigEntry.Gui.Tooltip
    public List<String> weatherDescriptions = List.of(
            "微风", "细雨", "阳光", "月光", "星光", "雪花", "云朵", "晨露"
    );

    @ConfigEntry.Gui.Tooltip
    public List<String> emotionalStates = List.of(
            "温柔", "欣喜", "宁静", "期待", "思念", "满足", "好奇", "关怀"
    );

    @ConfigEntry.Gui.Tooltip
    public List<String> creativityTips = List.of(
            "尝试使用比喻或拟人的手法",
            "可以加入一些小细节，比如声音、气味、触感等",
            "试着从不同的角度来描述同一件事",
            "可以使用一些诗意的表达",
            "尝试营造特定的氛围或情绪",
            "可以加入一些想象力丰富的元素",
            "试着用对话或内心独白的形式",
            "可以使用一些文学性的修辞手法"
    );
}
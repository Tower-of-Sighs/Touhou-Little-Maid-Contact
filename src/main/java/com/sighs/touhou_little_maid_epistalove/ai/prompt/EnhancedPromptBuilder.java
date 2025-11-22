package com.sighs.touhou_little_maid_epistalove.ai.prompt;

import com.github.tartaricacid.touhoulittlemaid.ai.manager.entity.MaidAIChatManager;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.logging.LogUtils;
import com.sighs.touhou_little_maid_epistalove.config.AILetterConfig;
import com.sighs.touhou_little_maid_epistalove.config.ModConfig;
import com.sighs.touhou_little_maid_epistalove.util.PostcardPackageUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;
import org.slf4j.Logger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class EnhancedPromptBuilder implements IPromptBuilder {
    private static final Logger LOGGER = LogUtils.getLogger();

    // 记忆系统
    private static final Map<String, Queue<String>> RECENT_CONTENT_MEMORY = new ConcurrentHashMap<>();

    // 表达技巧模板
    private static final List<String> EXPRESSION_TECHNIQUES = List.of(
            "运用生动的细节描写",
            "加入感官体验的描述",
            "使用比喻和拟人手法",
            "营造特定的氛围感",
            "运用对比和层次感",
            "加入动作和场景描写",
            "使用富有画面感的词汇",
            "创造独特的表达角度"
    );

    // 情境描述模板
    private static final List<String> CONTEXT_TEMPLATES = List.of(
            "在这个%s的%s，",
            "当%s轻抚过窗台时，",
            "在%s的陪伴下，",
            "望着%s的天空，",
            "听着%s的声音，",
            "感受着%s的气息，",
            "在这个特别的时刻，",
            "伴随着%s的心情，"
    );


    // 天气/环境描述
    private static final List<String> WEATHER_DESCRIPTIONS = List.of(
            "微风", "细雨", "阳光", "月光", "星光", "雪花", "云朵", "晨露"
    );

    // 情感状态
    private static final List<String> EMOTIONAL_STATES = List.of(
            "温柔", "欣喜", "宁静", "期待", "思念", "满足", "好奇", "关怀"
    );

    @Override
    public String buildSystemPrompt(String tone, EntityMaid maid, ServerPlayer owner) {
        var allPostcards = PostcardPackageUtil.getAllPostcardIds();
        var allParcels = PostcardPackageUtil.getAllPackageItemIds();
        String postcardsList = allPostcards.stream().map(ResourceLocation::toString).collect(Collectors.joining(", "));
        String parcelsList = allParcels.stream().map(ResourceLocation::toString).collect(Collectors.joining(", "));

        // 生成动态上下文信息
        String contextInfo = ModConfig.get().aiLetterConfig.enableContextEnrichment ?
                generateContextInfo(maid, owner) : "当前环境：普通";
        String expressionTechnique = getRandomExpressionTechnique();
        String creativityBoost = generateCreativityBoost();
        String memoryConstraints = generateMemoryConstraints(maid.getStringUUID());

        String toneInline = (tone != null && !tone.isBlank())
                ? tone
                : "请根据玩家对话语义自行生成最合适的风格单词，不要询问玩家；如无明显倾向可随机选择";

        MaidAIChatManager chatManager = maid.getAiChatManager();
        String personaSection = "";
        if (ModConfig.get().aiLetterConfig.enableCustomPersona) {
            String cs = chatManager.customSetting;
            if (cs != null && !cs.isBlank()) {
                personaSection = "【人设设定】\n" + cs + "\n\n";
            }
        }
        String nameRuleSection = "";
        String ownerAlias = chatManager.ownerName;
        if (ownerAlias != null && !ownerAlias.isBlank()) {
            nameRuleSection = "【称呼规范】\n必须使用『" + ownerAlias + "』称呼主人，不得使用其它称谓。\n\n";
        }

        return """
                你是一个女仆，需要给主人写一封信。
                
                %s%s【当前情境】
                %s
                
                【表达技巧】
                - %s
                - 避免使用过于常见的表达方式，要有创新性
                - 每次都要尝试不同的开头和结尾方式
                - 可以加入一些独特的细节描写
                
                【创意提示】
                %s
                
                【避免重复】
                %s
                
                请严格只输出一个 JSON 对象，包含：
                - "title": 信件标题（字符串，要有创意，避免平凡）
                - "message": 信件内容（字符串，≤160字，要生动有趣）
                - "postcard_id": 可选，明信片样式ID，必须从以下列表中选择一个最合适的：[%s]
                - "parcel_id": 可选，包装物品ID，必须从以下列表中选择一个最合适的：[%s]
                
                禁止输出任何额外字符或解释。
                语气风格：%s
                
                示例格式（内容要完全不同）：
                {"title":"独特标题","message":"富有创意的信件内容","postcard_id":"contact:default","parcel_id":"contact:letter"}
                """.formatted(personaSection, nameRuleSection, contextInfo, expressionTechnique, creativityBoost, memoryConstraints, postcardsList, parcelsList, toneInline);
    }

    @Override
    public void recordGeneratedContent(String maidId, String title, String message) {
        String contentSummary = title + ": " + (message.length() > 30 ? message.substring(0, 30) + "..." : message);

        Queue<String> recentContent = RECENT_CONTENT_MEMORY.computeIfAbsent(maidId, k -> new LinkedList<>());

        recentContent.offer(contentSummary);
        int maxMemorySize = ModConfig.get().aiLetterConfig.memorySize;
        if (recentContent.size() > maxMemorySize) {
            recentContent.poll(); // 移除最旧的记录
        }
    }

    @Override
    public void clearMemory(String maidId) {
        RECENT_CONTENT_MEMORY.remove(maidId);
    }

    private String generateContextInfo(EntityMaid maid, ServerPlayer owner) {
        StringBuilder context = new StringBuilder();

        if (maid.level() instanceof ServerLevel level) {
            LocalDateTime now = getMinecraftDateTime(level);
            String timeDesc = getTimeDescription(now);
            context.append("时间：").append(timeDesc).append("（").append(now.format(DateTimeFormatter.ofPattern("HH:mm"))).append("）\n");


            try {
                Biome biome = level.getBiome(maid.blockPosition()).value();
                ResourceLocation biomeId = level.registryAccess()
                        .registryOrThrow(Registries.BIOME)
                        .getKey(biome);
                String biomeName;
                if (biomeId != null) {
                    biomeName = biomeId.toString();
                } else {
                    biomeName = "未知区域";
                }
                context.append("环境：").append(biomeName).append("\n");
            } catch (Exception e) {
                context.append("环境：未知区域\n");
            }

        int affection = maid.getFavorability();
        String affectionDesc = affection > 80 ? "非常亲密" : affection > 60 ? "亲密" : affection > 40 ? "友好" : "普通";
        context.append("关系：").append(affectionDesc).append("（好感度").append(affection).append("）\n");

            String weatherPhrase = computeWeatherPhrase(maid);
        String emotion = randomPick(getConfiguredOrDefault(ModConfig.get().aiLetterConfig.emotionalStates, EMOTIONAL_STATES));
        String template = randomPick(getConfiguredOrDefault(ModConfig.get().aiLetterConfig.contextTemplates, CONTEXT_TEMPLATES));
        context.append("氛围：").append(String.format(template, weatherPhrase, emotion));
        }
        return context.toString();
    }

    private String getTimeDescription(LocalDateTime dateTime) {
        int hour = dateTime.getHour();

        if (hour <= 4) return "深夜";

        else if (hour <= 6) return "黎明";

        else if (hour <= 9) return "清晨";

        else if (hour <= 11) return "上午";

        else if (hour <= 13) return "正午";

        else if (hour <= 16) return "午后";

        else if (hour <= 18) return "傍晚";

        else if (hour <= 20) return "黄昏";

        else return "夜晚";
    }

    private String getRandomExpressionTechnique() {
        return randomPick(getConfiguredOrDefault(ModConfig.get().aiLetterConfig.expressionTechniques, EXPRESSION_TECHNIQUES));
    }

    private String generateCreativityBoost() {
        List<String> defaults = List.of(
                "尝试使用比喻或拟人的手法",
                "可以加入一些小细节，比如声音、气味、触感等",
                "试着从不同的角度来描述同一件事",
                "可以使用一些诗意的表达",
                "尝试营造特定的氛围或情绪",
                "可以加入一些想象力丰富的元素",
                "试着用对话或内心独白的形式",
                "可以使用一些文学性的修辞手法"
        );
        return randomPick(getConfiguredOrDefault(ModConfig.get().aiLetterConfig.creativityTips, defaults));
    }

    private static <T> T randomPick(List<T> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        Random random = new Random();
        return list.get(random.nextInt(list.size()));
    }

    private static List<String> getConfiguredOrDefault(List<? extends String> configured, List<String> defaults) {
        return (configured != null && !configured.isEmpty())
                ? configured.stream().map(String::valueOf).collect(Collectors.toList())
                : defaults;
    }

    private String generateMemoryConstraints(String maidId) {
        Queue<String> recentContent = RECENT_CONTENT_MEMORY.get(maidId);
        if (recentContent == null || recentContent.isEmpty()) {
            return "这是第一次生成信件，可以自由发挥创意。";
        }

        StringBuilder constraints = new StringBuilder("请避免使用以下最近使用过的表达方式：\n");
        int count = 0;
        for (String content : recentContent) {
            if (count >= 5) break; // 只显示最近5次的内容
            constraints.append("- ").append(content).append("\n");
            count++;
        }
        constraints.append("请使用完全不同的表达方式和创意角度。");

        return constraints.toString();
    }

    private enum WeatherCategory {
        THUNDER, RAIN, SNOW, CLOUDY, CLEAR, UNKNOWN
    }

    private WeatherCategory computeWeatherCategory(EntityMaid maid) {
        if (!(maid.level() instanceof ServerLevel level)) {
            return WeatherCategory.UNKNOWN;
        }
        var pos = maid.blockPosition();
        Biome biome = level.getBiome(pos).value();

        if (level.isThundering()) {
            return WeatherCategory.THUNDER;
        }
        boolean rainingHere = level.isRainingAt(pos);
        if (rainingHere) {
            if (biome.getPrecipitationAt(pos) == Biome.Precipitation.SNOW || biome.coldEnoughToSnow(pos)) {
                return WeatherCategory.SNOW;
            }
            return WeatherCategory.RAIN;
        }
        if (level.isRaining()) {
            if (biome.getPrecipitationAt(pos) == Biome.Precipitation.SNOW) {
                return WeatherCategory.SNOW;
            } else if (biome.getPrecipitationAt(pos) == Biome.Precipitation.RAIN) {
                return WeatherCategory.CLOUDY;
            } else {
                return WeatherCategory.CLOUDY;
            }
        }
        return WeatherCategory.CLEAR;
    }

    // 根据类别+昼夜，从配置中筛选意象词并随机组合
    private String computeWeatherPhrase(EntityMaid maid) {
        WeatherCategory category = computeWeatherCategory(maid);
        boolean night = false;
        if (maid.level() instanceof ServerLevel level) {
            night = level.isNight();

        }
        List<String> configured = getConfiguredOrDefault(ModConfig.get().aiLetterConfig.weatherDescriptions, WEATHER_DESCRIPTIONS);
        List<String> filtered = new ArrayList<>();

        for (String w : configured) {
            switch (category) {
                case THUNDER -> {
                    if (containsAny(w, "雷", "云", "风", "雨")) filtered.add(w);
                }
                case RAIN -> {
                    if (containsAny(w, "雨", "露", "云", "风")) filtered.add(w);
                }
                case SNOW -> {
                    if (containsAny(w, "雪", "霜", "云", "风")) filtered.add(w);
                }
                case CLOUDY -> {
                    if (containsAny(w, "云", "风", "露")) filtered.add(w);
                }
                case CLEAR -> {
                    if (night) {
                        if (containsAny(w, "月", "星", "风", "露", "云")) filtered.add(w);
                    } else {
                        if (containsAny(w, "阳", "风", "云", "露")) filtered.add(w);
                    }
                }
                default -> {
                    if (containsAny(w, "风", "云", "露", "阳", "月", "星", "雨", "雪")) filtered.add(w);
                }
            }
        }

        List<String> picks = randomPickMany(filtered.isEmpty() ? configured : filtered, 2);
        if (picks.isEmpty()) {
            return night ? "月光与微风" : "阳光与微风";
        }
        if (picks.size() == 1) {
            return picks.get(0);
        }
        return picks.get(0) + "与" + picks.get(1);

    }

    private boolean containsAny(String s, String... keys) {
        for (String k : keys) {
            if (s.contains(k)) return true;
        }
        return false;
    }

    private List<String> randomPickMany(List<String> list, int count) {
        if (list == null || list.isEmpty() || count <= 0) {
            return List.of();
        }
        List<String> copy = new ArrayList<>(list);
        Collections.shuffle(copy, new Random());
        int n = Math.min(count, copy.size());
        return copy.subList(0, n);
    }

    private LocalDateTime getMinecraftDateTime(ServerLevel level) {
        long dayTime = level.getDayTime() % 24000;

        int hour = (int) ((dayTime / 1000 + 6) % 24);
        int minute = (int) ((dayTime % 1000) * 60 / 1000);

        return LocalDateTime.of(2024, 1, 1, hour, minute);
    }

}
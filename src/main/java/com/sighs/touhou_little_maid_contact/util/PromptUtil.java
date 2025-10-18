package com.sighs.touhou_little_maid_contact.util;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.logging.LogUtils;
import com.sighs.touhou_little_maid_contact.config.AILetterConfig;
import com.sighs.touhou_little_maid_contact.data.MaidLetterRule;
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

public final class PromptUtil {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // 记忆系统：存储最近生成的内容以避免重复
    private static final Map<String, Queue<String>> RECENT_CONTENT_MEMORY = new ConcurrentHashMap<>();

    // 多样化的写作风格模板
    private static final List<String> WRITING_STYLES = List.of(
            "用诗意的语言和感性的描写",
            "用轻松活泼的语气",
            "用温柔细腻的笔触",
            "用略带调皮的语调",
            "用古典优雅的文风",
            "用现代简洁的表达",
            "用充满想象力的比喻",
            "用温暖治愈的语调"
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

    // 时间描述
    private static final List<String> TIME_DESCRIPTIONS = List.of(
            "清晨", "午后", "黄昏", "夜晚", "深夜", "黎明", "正午", "傍晚"
    );

    // 天气/环境描述
    private static final List<String> WEATHER_DESCRIPTIONS = List.of(
            "微风", "细雨", "阳光", "月光", "星光", "雪花", "云朵", "晨露"
    );

    // 情感状态
    private static final List<String> EMOTIONAL_STATES = List.of(
            "温柔", "欣喜", "宁静", "期待", "思念", "满足", "好奇", "关怀"
    );

    private PromptUtil() {
    }

    public static String buildSystemPrompt(MaidLetterRule.AI ai, EntityMaid maid, ServerPlayer owner) {
        String tone = ai.tone().orElse("sweet");
        var allPostcards = PostcardPackageUtil.getAllPostcardIds();
        var allParcels = PostcardPackageUtil.getAllPackageItemIds();
        String postcardsList = allPostcards.stream().map(ResourceLocation::toString).collect(Collectors.joining(", "));
        String parcelsList = allParcels.stream().map(ResourceLocation::toString).collect(Collectors.joining(", "));

        // 生成动态上下文信息
        String contextInfo = AILetterConfig.ENABLE_CONTEXT_ENRICHMENT.get() ? 
                            generateContextInfo(maid, owner) : "当前环境：普通";
        String writingStyle = getRandomWritingStyle();
        String creativityBoost = generateCreativityBoost();
        String memoryConstraints = generateMemoryConstraints(maid.getStringUUID());

        LOGGER.debug("[MaidMail] buildSystemPrompt tone={} postcards={} parcels={} context={}", 
                    tone, allPostcards.size(), allParcels.size(), contextInfo);

        return """
                你是一个女仆，需要给主人写一封信。
                
                【当前情境】
                %s
                
                【写作要求】
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
                """.formatted(contextInfo, writingStyle, creativityBoost, memoryConstraints, postcardsList, parcelsList, tone);
    }

    /**
     * 生成动态上下文信息
     */
    private static String generateContextInfo(EntityMaid maid, ServerPlayer owner) {
        Random random = new Random();
        StringBuilder context = new StringBuilder();

        // 时间信息
        LocalDateTime now = LocalDateTime.now();
        String timeDesc = TIME_DESCRIPTIONS.get(random.nextInt(TIME_DESCRIPTIONS.size()));
        context.append("时间：").append(timeDesc).append("（").append(now.format(DateTimeFormatter.ofPattern("HH:mm"))).append("）\n");

        // 环境信息
        if (maid.level() instanceof ServerLevel level) {
            try {
                Biome biome = level.getBiome(maid.blockPosition()).value();
                String biomeName = biome.toString();
                context.append("环境：").append(biomeName).append("\n");
            } catch (Exception e) {
                context.append("环境：未知区域\n");
            }
        }

        // 女仆状态
        int affection = maid.getFavorability();
        String affectionDesc = affection > 80 ? "非常亲密" : affection > 60 ? "亲密" : affection > 40 ? "友好" : "普通";
        context.append("关系：").append(affectionDesc).append("（好感度").append(affection).append("）\n");

        // 随机情境描述
        String weather = WEATHER_DESCRIPTIONS.get(random.nextInt(WEATHER_DESCRIPTIONS.size()));
        String emotion = EMOTIONAL_STATES.get(random.nextInt(EMOTIONAL_STATES.size()));
        String contextTemplate = CONTEXT_TEMPLATES.get(random.nextInt(CONTEXT_TEMPLATES.size()));
        context.append("氛围：").append(String.format(contextTemplate, weather, emotion));

        return context.toString();
    }

    /**
     * 获取随机写作风格
     */
    private static String getRandomWritingStyle() {
        Random random = new Random();
        return WRITING_STYLES.get(random.nextInt(WRITING_STYLES.size()));
    }

    /**
     * 生成创意提升提示
     */
    private static String generateCreativityBoost() {
        Random random = new Random();
        List<String> creativityTips = List.of(
                "尝试使用比喻或拟人的手法",
                "可以加入一些小细节，比如声音、气味、触感等",
                "试着从不同的角度来描述同一件事",
                "可以使用一些诗意的表达",
                "尝试营造特定的氛围或情绪",
                "可以加入一些想象力丰富的元素",
                "试着用对话或内心独白的形式",
                "可以使用一些文学性的修辞手法"
        );
        
        return creativityTips.get(random.nextInt(creativityTips.size()));
    }

    /**
     * 生成记忆约束，避免重复之前的内容
     */
    private static String generateMemoryConstraints(String maidId) {
        Queue<String> recentContent = RECENT_CONTENT_MEMORY.get(maidId);
        if (recentContent == null || recentContent.isEmpty()) {
            return "这是第一次生成信件，可以自由发挥创意。";
        }

        StringBuilder constraints = new StringBuilder("请避免使用以下最近使用过的表达方式：\n");
        int count = 0;
        for (String content : recentContent) {
            if (count >= 3) break; // 只显示最近3次的内容
            constraints.append("- ").append(content).append("\n");
            count++;
        }
        constraints.append("请使用完全不同的表达方式和创意角度。");
        
        return constraints.toString();
    }

    /**
     * 记录生成的内容到记忆系统
     */
    public static void recordGeneratedContent(String maidId, String title, String message) {
        String contentSummary = title + ": " + (message.length() > 30 ? message.substring(0, 30) + "..." : message);
        
        Queue<String> recentContent = RECENT_CONTENT_MEMORY.computeIfAbsent(maidId, k -> new LinkedList<>());
        
        recentContent.offer(contentSummary);
        int maxMemorySize = AILetterConfig.MEMORY_SIZE.get();
        if (recentContent.size() > maxMemorySize) {
            recentContent.poll(); // 移除最旧的记录
        }
        
        LOGGER.debug("[MaidMail][Memory] Recorded content for maid {}: {}", maidId, contentSummary);
    }

    /**
     * 清理指定女仆的记忆（可用于重置或清理）
     */
    public static void clearMemory(String maidId) {
        RECENT_CONTENT_MEMORY.remove(maidId);
        LOGGER.debug("[MaidMail][Memory] Cleared memory for maid {}", maidId);
    }
}
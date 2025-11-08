package com.sighs.touhou_little_maid_epistalove.ai.parser;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.sighs.touhou_little_maid_epistalove.ai.prompt.IPromptBuilder;
import com.sighs.touhou_little_maid_epistalove.config.AILetterConfig;
import com.sighs.touhou_little_maid_epistalove.util.PostcardPackageUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JsonLetterParser implements ILetterParser {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Pattern JSON_OBJECT = Pattern.compile("\\{.*\\}", Pattern.DOTALL);

    private final IPromptBuilder promptBuilder;

    public JsonLetterParser(IPromptBuilder promptBuilder) {
        this.promptBuilder = promptBuilder;
    }

    @Override
    public ItemStack parseToLetter(String content, String senderName, EntityMaid maid) {
        Matcher m = JSON_OBJECT.matcher(content);
        if (!m.find()) {
            LOGGER.warn("[MaidMail][AI] JSON object not found in content");
            return ItemStack.EMPTY;
        }
        String json = m.group(0);
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            String title = obj.has("title") ? obj.get("title").getAsString() : "";
            String message = obj.has("message") ? obj.get("message").getAsString() : "";

            // 验证内容质量，避免过于简单或重复的内容
            if (AILetterConfig.ENABLE_QUALITY_FILTER.get() && !isContentValid(title, message)) {
                LOGGER.warn("[MaidMail][AI] Content quality check failed: title='{}', message='{}'", title, message);
                return ItemStack.EMPTY;
            }

            String parcelIdStr = obj.has("parcel_id") ? obj.get("parcel_id").getAsString() : null;
            String postcardIdStr = obj.has("postcard_id") ? obj.get("postcard_id").getAsString() : null;

            ResourceLocation packageId = PostcardPackageUtil.choosePackageId(parcelIdStr);
            ResourceLocation postcardId = PostcardPackageUtil.choosePostcardId(postcardIdStr);

            // 记录到记忆系统
            if (maid != null && promptBuilder != null) {
                promptBuilder.recordGeneratedContent(maid.getStringUUID(), title, message);
            }

            return PostcardPackageUtil.buildPackageWithPostcard(packageId, title + "\n" + message, postcardId, senderName);
        } catch (Exception e) {
            LOGGER.error("[MaidMail][AI] parse JSON error: {}", e.getMessage());
            return ItemStack.EMPTY;
        }
    }

    @Override
    public boolean isContentValid(String title, String message) {
        // 检查是否为空或过短
        if (title == null || title.trim().isEmpty() || title.length() < 2) {
            return false;
        }
        int minLength = AILetterConfig.MIN_CONTENT_LENGTH.get();
        if (message == null || message.trim().isEmpty() || message.length() < minLength) {
            return false;
        }

        // 检查是否包含过于通用的词汇
        String[] genericPhrases = {
                "恭喜", "祝贺", "加油", "努力", "继续", "保持"
        };

        String combinedText = (title + " " + message).toLowerCase();
        int genericCount = 0;
        for (String phrase : genericPhrases) {
            if (combinedText.contains(phrase.toLowerCase())) {
                genericCount++;
            }
        }

        // 如果通用词汇过多，认为内容质量不高
        int maxGenericPhrases = AILetterConfig.MAX_GENERIC_PHRASES.get();
        return genericCount <= maxGenericPhrases;
    }
}
package com.sighs.touhou_little_maid_contact.llm;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.sighs.touhou_little_maid_contact.config.AILetterConfig;
import com.sighs.touhou_little_maid_contact.util.PostcardPackageUtil;
import com.sighs.touhou_little_maid_contact.util.PromptUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LetterJsonParser {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Pattern JSON_OBJECT = Pattern.compile("\\{.*\\}", Pattern.DOTALL);


    /**
     * 将 LLM 返回文本解析为 contact:* 的 ItemStack，并记录到记忆系统。
     * 仅当解析成功且至少包含 title/message 时返回有效信件，否则返回 EMPTY。
     */
    public static ItemStack parseToLetter(String content, String senderName, EntityMaid maid) {
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

            // 记录生成的内容用于调试
            LOGGER.debug("[MaidMail][AI] Generated letter - Title: '{}', Message: '{}', Postcard: '{}', Package: '{}'", 
                        title, message, postcardId, packageId);

            // 记录到记忆系统（如果提供了maid参数）
            if (maid != null) {
                PromptUtil.recordGeneratedContent(maid.getStringUUID(), title, message);
            }

            return PostcardPackageUtil.buildPackageWithPostcard(packageId, title + "\n" + message, postcardId, senderName);
        } catch (Exception e) {
            LOGGER.error("[MaidMail][AI] parse JSON error: {}", e.getMessage());
            return ItemStack.EMPTY;
        }
    }

    /**
     * 验证生成内容的质量
     */
    private static boolean isContentValid(String title, String message) {
        // 检查是否为空或过短
        if (title == null || title.trim().isEmpty() || title.length() < 2) {
            return false;
        }
        int minLength = AILetterConfig.MIN_CONTENT_LENGTH.get();
        if (message == null || message.trim().isEmpty() || message.length() < minLength) {
            return false;
        }

        // 检查是否包含过于通用的词汇（可以根据需要扩展）
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

    private LetterJsonParser() {
    }
}
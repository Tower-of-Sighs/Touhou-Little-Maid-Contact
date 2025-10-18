package com.sighs.touhou_little_maid_contact.llm;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.sighs.touhou_little_maid_contact.util.PostcardPackageUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LetterJsonParser {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Pattern JSON_OBJECT = Pattern.compile("\\{.*\\}", Pattern.DOTALL);

    /**
     * 将 LLM 返回文本解析为 contact:* 的 ItemStack。
     * 仅当解析成功且至少包含 title/message 时返回有效信件，否则返回 EMPTY。
     */
    public static ItemStack parseToLetter(String content, String senderName) {
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

            String parcelIdStr = obj.has("parcel_id") ? obj.get("parcel_id").getAsString() : null;
            String postcardIdStr = obj.has("postcard_id") ? obj.get("postcard_id").getAsString() : null;

            ResourceLocation packageId = PostcardPackageUtil.choosePackageId(parcelIdStr);
            ResourceLocation postcardId = PostcardPackageUtil.choosePostcardId(postcardIdStr);

            return PostcardPackageUtil.buildPackageWithPostcard(packageId, title + "\n" + message, postcardId, senderName);
        } catch (Exception e) {
            LOGGER.error("[MaidMail][AI] parse JSON error: {}", e.getMessage());
            return ItemStack.EMPTY;
        }
    }

    private LetterJsonParser() {
    }
}
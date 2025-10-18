package com.sighs.touhou_little_maid_contact.util;

import com.mojang.logging.LogUtils;
import com.sighs.touhou_little_maid_contact.data.MaidLetterRule;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.util.stream.Collectors;

public final class PromptUtil {
    private static final Logger LOGGER = LogUtils.getLogger();

    private PromptUtil() {
    }

    public static String buildSystemPrompt(MaidLetterRule.AI ai) {
        String tone = ai.tone().orElse("sweet");
        var allPostcards = PostcardPackageUtil.getAllPostcardIds();
        var allParcels = PostcardPackageUtil.getAllPackageItemIds();
        String postcardsList = allPostcards.stream().map(ResourceLocation::toString).collect(Collectors.joining(", "));
        String parcelsList = allParcels.stream().map(ResourceLocation::toString).collect(Collectors.joining(", "));

        LOGGER.debug("[MaidMail] buildSystemPrompt tone={} postcards={} parcels={}", tone, allPostcards.size(), allParcels.size());

        return """
                你是一个女仆，需要给主人写一封信。
                请严格只输出一个 JSON 对象，包含：
                - "title": 信件标题（字符串）
                - "message": 信件内容（字符串，≤160字）
                - "postcard_id": 可选，明信片样式ID，必须从以下列表中选择一个最合适的：[%s]
                - "parcel_id": 可选，包装物品ID，必须从以下列表中选择一个最合适的：[%s]
                禁止输出任何额外字符或解释。
                语气：%s
                示例（仅供格式参考）：
                {"title":"问候","message":"恭喜打败凋灵！","postcard_id":"contact:default","parcel_id":"contact:letter"}
                """.formatted(postcardsList, parcelsList, tone);
    }
}
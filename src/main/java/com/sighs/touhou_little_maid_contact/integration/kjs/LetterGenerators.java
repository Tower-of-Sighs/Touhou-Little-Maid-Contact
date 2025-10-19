package com.sighs.touhou_little_maid_contact.integration.kjs;

import com.sighs.touhou_little_maid_contact.ai.generator.AILetterGenerator;
import com.sighs.touhou_little_maid_contact.ai.generator.PresetLetterGenerator;
import com.sighs.touhou_little_maid_contact.ai.parser.JsonLetterParser;
import com.sighs.touhou_little_maid_contact.ai.prompt.EnhancedPromptBuilder;
import com.sighs.touhou_little_maid_contact.api.letter.ILetterGenerator;
import net.minecraft.resources.ResourceLocation;

/**
 * 信件生成器工厂类
 * 提供创建各种类型信件生成器的静态方法
 */
public final class LetterGenerators {
    private static final EnhancedPromptBuilder PROMPT_BUILDER = new EnhancedPromptBuilder();
    private static final JsonLetterParser LETTER_PARSER = new JsonLetterParser(PROMPT_BUILDER);

    private LetterGenerators() {
    }

    /**
     * 创建AI信件生成器
     *
     * @param tone   语调
     * @param prompt 提示词
     */
    public static ILetterGenerator createAIGenerator(String tone, String prompt) {
        return new AILetterGenerator(tone, prompt, PROMPT_BUILDER, LETTER_PARSER);
    }

    /**
     * 创建预设信件生成器
     *
     * @param title      标题
     * @param message    内容
     * @param postcardId 明信片ID
     * @param parcelId   包裹ID
     */
    public static ILetterGenerator createPresetGenerator(String title, String message, String postcardId, String parcelId) {
        return new PresetLetterGenerator(title, message,
                new ResourceLocation(postcardId),
                new ResourceLocation(parcelId));
    }
}
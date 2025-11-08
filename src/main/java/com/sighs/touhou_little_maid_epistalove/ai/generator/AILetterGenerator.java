package com.sighs.touhou_little_maid_epistalove.ai.generator;

import com.github.tartaricacid.touhoulittlemaid.ai.manager.entity.LLMCallback;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.entity.MaidAIChatManager;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.response.ResponseChat;
import com.github.tartaricacid.touhoulittlemaid.ai.service.ErrorCode;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.*;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.openai.response.Message;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.AIConfig;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.logging.LogUtils;
import com.sighs.touhou_little_maid_epistalove.ai.parser.ILetterParser;
import com.sighs.touhou_little_maid_epistalove.ai.prompt.IPromptBuilder;
import com.sighs.touhou_little_maid_epistalove.api.letter.ILetterGenerator;
import com.sighs.touhou_little_maid_epistalove.config.AILetterConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class AILetterGenerator implements ILetterGenerator {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final String tone;
    private final String prompt;
    private final IPromptBuilder promptBuilder;
    private final ILetterParser letterParser;

    public AILetterGenerator(String tone, String prompt, IPromptBuilder promptBuilder, ILetterParser letterParser) {
        this.tone = tone;
        this.prompt = prompt;
        this.promptBuilder = promptBuilder;
        this.letterParser = letterParser;
    }

    @Override
    public void generate(ServerPlayer owner, EntityMaid maid, Consumer<ItemStack> callback) {
        if (!AIConfig.LLM_ENABLED.get()) {
            LOGGER.warn("[MaidMail][AI] LLM disabled");
            callback.accept(ItemStack.EMPTY);
            return;
        }

        MaidAIChatManager chatManager = maid.getAiChatManager();
        LLMSite site = chatManager.getLLMSite();
        if (site == null || !site.enabled()) {
            LOGGER.warn("[MaidMail][AI] site not available or disabled");
            callback.accept(ItemStack.EMPTY);
            return;
        }

        String system = promptBuilder.buildSystemPrompt(tone, maid, owner);

        LLMClient client = site.client();
        List<LLMMessage> chat = new ArrayList<>();
        chat.add(LLMMessage.systemChat(maid, system));
        chat.add(LLMMessage.userChat(maid, prompt));

        LLMConfig config = createEnhancedLLMConfig(chatManager.getLLMModel(), maid);

        client.chat(chat, config, new LLMCallback(chatManager, "", 0) {
            @Override
            public void onSuccess(ResponseChat responseChat) {
                String content = responseChat.chatText;
                String senderName = maid.getName().getString();
                ItemStack result = letterParser.parseToLetter(content, senderName, maid);
                callback.accept(result);
            }

            @Override
            public void onFailure(HttpRequest request, Throwable throwable, int errorCode) {
                LOGGER.error("[MaidMail][AI] onFailure code={} msg={}", errorCode,
                        throwable != null ? throwable.getMessage() : "null");
                callback.accept(ItemStack.EMPTY);
            }

            @Override
            public void onFunctionCall(Message message, List<LLMMessage> messages, LLMConfig config, LLMClient client) {
                LOGGER.error("[MaidMail][AI] unexpected function call");
                onFailure(null, new RuntimeException("Unexpected function call"), ErrorCode.JSON_DECODE_ERROR);
            }
        });
    }

    @Override
    public String getType() {
        return "ai";
    }

    private LLMConfig createEnhancedLLMConfig(String model, EntityMaid maid) {
        double temperatureBoost = AILetterConfig.CREATIVITY_TEMPERATURE_BOOST.get();
        double enhancedTemperature = Math.min(1.2, AIConfig.LLM_TEMPERATURE.get() + temperatureBoost);

        int enhancedMaxTokens = Math.max(AIConfig.LLM_MAX_TOKEN.get(), 200);

        return new LLMConfig(model, enhancedTemperature, enhancedMaxTokens, maid, ChatType.NORMAL_CHAT);
    }
}
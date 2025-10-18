package com.sighs.touhou_little_maid_contact.mixin;

import com.github.tartaricacid.touhoulittlemaid.ai.manager.entity.LLMCallback;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.entity.MaidAIChatData;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.entity.MaidAIChatManager;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.response.ResponseChat;
import com.github.tartaricacid.touhoulittlemaid.ai.service.ErrorCode;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.LLMClient;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.LLMConfig;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.LLMMessage;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.LLMSite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.openai.response.Message;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.AIConfig;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.logging.LogUtils;
import com.sighs.touhou_little_maid_contact.api.IMaidAIChatManager;
import com.sighs.touhou_little_maid_contact.data.MaidLetterRule;
import com.sighs.touhou_little_maid_contact.llm.LetterJsonParser;
import com.sighs.touhou_little_maid_contact.util.PromptUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;

import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Mixin(MaidAIChatManager.class)
public class MaidAIChatManagerMixin extends MaidAIChatData implements IMaidAIChatManager {
    private static final Logger LOGGER = LogUtils.getLogger();

    public MaidAIChatManagerMixin(EntityMaid maid) {
        super(maid);
    }

    /**
     * 生成 AI 信件：调用 LLM 返回仅包含 {"title": "...", "message": "..."} 的 JSON，
     * 回调中解析为包裹物品（随机 IPackageItem）+ 随机明信片样式。
     */
    @Override
    public void tlm_contact$generateLetter(MaidLetterRule.AI ai, ServerPlayer owner, Consumer<ItemStack> onResult) {
        if (!AIConfig.LLM_ENABLED.get()) {
            LOGGER.warn("[MaidMail][AI] LLM disabled");
            onResult.accept(ItemStack.EMPTY);
            return;
        }
        LLMSite site = this.getLLMSite();
        if (site == null || !site.enabled()) {
            LOGGER.warn("[MaidMail][AI] site not available or disabled");
            onResult.accept(ItemStack.EMPTY);
            return;
        }

        String system = PromptUtil.buildSystemPrompt(ai);
        LOGGER.debug("[MaidMail][AI] start generate letter maidId={} site={} model={}", this.maid.getId(), site.getName().getString(), this.getLLMModel());

        LLMClient client = site.client();
        List<LLMMessage> chat = new ArrayList<>();
        chat.add(LLMMessage.systemChat(this.maid, system));
        chat.add(LLMMessage.userChat(this.maid, ai.prompt()));

        LLMConfig config = LLMConfig.normalChat(this.getLLMModel(), this.maid);

        client.chat(chat, config, new LLMCallback((MaidAIChatManager) (Object) this, "", 0) {
            @Override
            public void onSuccess(ResponseChat responseChat) {
                String content = responseChat.chatText;
                LOGGER.debug("[MaidMail][AI] onSuccess contentLen={} preview=\"{}\"", content.length(), content.substring(0, Math.min(content.length(), 120)));
                String senderName = this.maid.getName().getString();
                ItemStack result = LetterJsonParser.parseToLetter(content, senderName);
                LOGGER.debug("[MaidMail][AI] parse result empty? {}", result.isEmpty());
                onResult.accept(result);
            }

            @Override
            public void onFailure(HttpRequest request, Throwable throwable, int errorCode) {
                LOGGER.error("[MaidMail][AI] onFailure code={} msg={}", errorCode, throwable != null ? throwable.getMessage() : "null");
                onResult.accept(ItemStack.EMPTY);
            }

            @Override
            public void onFunctionCall(Message message, List<LLMMessage> messages, LLMConfig config, LLMClient client) {
                LOGGER.error("[MaidMail][AI] unexpected function call");
                onFailure(null, new RuntimeException("Unexpected function call"), ErrorCode.JSON_DECODE_ERROR);
            }
        });
    }
}

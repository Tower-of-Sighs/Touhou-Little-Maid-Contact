package com.sighs.touhou_little_maid_epistalove.mixin;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.entity.LLMCallback;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.entity.MaidAIChatManager;
import com.github.tartaricacid.touhoulittlemaid.ai.service.ErrorCode;
import com.github.tartaricacid.touhoulittlemaid.ai.service.ResponseCallback;
import com.github.tartaricacid.touhoulittlemaid.ai.service.function.FunctionCallRegister;
import com.github.tartaricacid.touhoulittlemaid.ai.service.function.IFunctionCall;
import com.github.tartaricacid.touhoulittlemaid.ai.service.function.response.ToolResponse;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.LLMClient;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.LLMConfig;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.LLMMessage;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.openai.response.FunctionToolCall;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.openai.response.Message;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.openai.response.ToolCall;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.JsonOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.http.HttpRequest;
import java.util.List;
import java.util.Optional;

@Mixin(value = LLMCallback.class, remap = false)
public abstract class LLMCallbackMixin implements ResponseCallback<Object> {
    @Shadow
    @Final
    protected MaidAIChatManager chatManager;
    @Shadow protected int callCount;
    @Shadow protected String message;

    @Shadow public abstract void onFailure(HttpRequest request, Throwable throwable, int errorCode);

    /**
     * 针对 write_letter 的函数调用做特殊处理：
     * - 保留工具调用与历史记录逻辑
     * - 不继续发起 MULTI_FUNCTION_CALL 的对话，避免覆盖 AILetterGenerator.generate 的返回结果
     */
    @Inject(method = "onFunctionCall", at = @At("HEAD"), cancellable = true)
    private void tlm$handleWriteLetterOnly(Message choice, List<LLMMessage> messages, LLMConfig config,
                                          LLMClient client, CallbackInfo ci) {
        boolean hasWriteLetter = choice.getToolCalls().stream()
                .map(ToolCall::getFunction)
                .map(FunctionToolCall::getName)
                .anyMatch("write_letter"::equals);
        if (!hasWriteLetter) {
            return;
        }

        // 复制原有头部历史记录行为
        if (this.callCount == 0) {
            chatManager.addUserHistory(message);
        }
        chatManager.addAssistantHistory(StringUtils.EMPTY, choice.getToolCalls());
        messages.add(LLMMessage.assistantChat(chatManager.getMaid(), choice.getContent(), choice.getToolCalls()));

        // 仅处理 write_letter 的工具调用，不继续 client.chat(...)
        for (ToolCall toolCall : choice.getToolCalls()) {
            FunctionToolCall function = toolCall.getFunction();
            String name = function.getName();
            if (!"write_letter".equals(name)) {
                continue;
            }
            String arguments = function.getArguments();
            IFunctionCall<?> functionCall = FunctionCallRegister.getFunctionCall(name);
            if (functionCall == null) {
                continue;
            }
            Object result;
            try {
                JsonObject parse = GsonHelper.parse(arguments);
                Optional<?> optional = functionCall.codec().parse(JsonOps.INSTANCE, parse)
                        .resultOrPartial(TouhouLittleMaid.LOGGER::error);
                if (optional.isEmpty()) {
                    continue;
                }
                result = optional.get();
            } catch (Exception exception) {
                String msg = "Exception %s, JSON is: %s".formatted(exception.getLocalizedMessage(), arguments);
                this.onFailure(null, new Throwable(msg), ErrorCode.JSON_DECODE_ERROR);
                continue;
            }

            TouhouLittleMaid.LOGGER.debug("Use function call: {}, arguments is {}", functionCall.getId(), arguments);
            EntityMaid maid = config.maid();
            if (!(maid.level() instanceof ServerLevel serverLevel)) {
                continue;
            }
            Object finalResult = result;
            serverLevel.getServer().submit(() -> {
                @SuppressWarnings("unchecked")
                IFunctionCall<Object> call = (IFunctionCall<Object>) functionCall;

                ToolResponse toolResponse = call.onToolCall(finalResult, maid);

                this.callCount = this.callCount + 1;
                String response = toolResponse.message();
                chatManager.addToolHistory(response, toolCall.getId());
                messages.add(LLMMessage.toolChat(maid, response, toolCall.getId()));
                // 不进行 client.chat(...) 的多轮对话续航，避免覆盖生成结果
            });
        }

        // 取消原方法，避免进入默认多轮续聊逻辑
        ci.cancel();
    }
}
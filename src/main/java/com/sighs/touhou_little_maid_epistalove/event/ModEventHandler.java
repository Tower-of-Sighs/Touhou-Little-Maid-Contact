package com.sighs.touhou_little_maid_epistalove.event;

import com.sighs.touhou_little_maid_epistalove.api.letter.ILetterRule;
import com.sighs.touhou_little_maid_epistalove.data.LetterRuleRegistry;
import com.sighs.touhou_little_maid_epistalove.trigger.TriggerManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

public class ModEventHandler {
    public static void init() {
        ServerPlayConnectionEvents.DISCONNECT.register(ModEventHandler::onPlayerLogout);
        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register(ModEventHandler::onDatapackSync);
        PlayerAdvancementEarnedCallback.PLAYER_ADVANCEMENT.register(ModEventHandler::onAdvancementEarned);
    }

    public static void onPlayerLogout(ServerGamePacketListenerImpl handler, MinecraftServer server) {
        TriggerManager.getInstance().clearAllTriggered(handler.player);
    }

    public static void onAdvancementEarned(ServerPlayer sp, Advancement advancement) {
        TriggerManager.getInstance().markTriggered(sp, advancement.getId());
    }

    public static void onDatapackSync(ServerPlayer serverPlayer, boolean joined) {
        var rules = LetterRuleRegistry.getAllRules();
        var triggerManager = TriggerManager.getInstance();
        var server = serverPlayer.server;

        for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
            for (var rule : rules) {
                if (rule.getTriggerType() != ILetterRule.TriggerType.REPEAT) continue;

                for (ResourceLocation triggerId : rule.getTriggers()) {
                    // 只清除自定义触发器的消费标记（成就不使用一次性消费）
                    var isAdv = server.getAdvancements().getAdvancement(triggerId) != null;
                    if (isAdv) continue;

                    ResourceLocation consumeKey = new ResourceLocation(
                            "internal",
                            ("custom_" + rule.getId().replace(":", "_") + "_" + triggerId.toString().replace(":", "_"))
                    );
                    triggerManager.clearConsumedOnce(sp, consumeKey);
                }
            }
        }
    }
}

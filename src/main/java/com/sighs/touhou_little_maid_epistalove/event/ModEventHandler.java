package com.sighs.touhou_little_maid_epistalove.event;

import com.sighs.touhou_little_maid_epistalove.api.letter.ILetterRule;
import com.sighs.touhou_little_maid_epistalove.data.LetterRuleRegistry;
import com.sighs.touhou_little_maid_epistalove.trigger.TriggerManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber
public class ModEventHandler {

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            TriggerManager.getInstance().clearAllTriggered(sp);
        }
    }

    @SubscribeEvent
    public static void onAdvancementEarned(AdvancementEvent.AdvancementEarnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            TriggerManager.getInstance().markTriggered(sp, event.getAdvancement().id());
        }
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        var rules = LetterRuleRegistry.getAllRules();
        var triggerManager = TriggerManager.getInstance();
        var server = event.getPlayerList().getServer();

        for (ServerPlayer sp : event.getPlayerList().getPlayers()) {
            for (var rule : rules) {
                if (rule.getTriggerType() != ILetterRule.TriggerType.REPEAT) continue;

                for (ResourceLocation triggerId : rule.getTriggers()) {
                    // 只清除自定义触发器的消费标记（成就不使用一次性消费）
                    var isAdv = server.getAdvancements().get(triggerId) != null;
                    if (isAdv) continue;

                    ResourceLocation consumeKey = ResourceLocation.fromNamespaceAndPath(
                            "internal",
                            ("custom_" + rule.getId().replace(":", "_") + "_" + triggerId.toString().replace(":", "_"))
                    );
                    triggerManager.clearConsumedOnce(sp, consumeKey);
                }
            }
        }
    }
}

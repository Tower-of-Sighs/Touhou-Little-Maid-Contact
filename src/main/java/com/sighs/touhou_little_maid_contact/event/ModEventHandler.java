package com.sighs.touhou_little_maid_contact.event;

import com.sighs.touhou_little_maid_contact.trigger.TriggerManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber
public class ModEventHandler {

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            TriggerManager.getInstance().clearAllTriggered(sp);
        }
    }
}

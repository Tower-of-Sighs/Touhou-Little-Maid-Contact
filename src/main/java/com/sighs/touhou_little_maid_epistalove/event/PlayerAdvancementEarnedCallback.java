package com.sighs.touhou_little_maid_epistalove.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.advancements.Advancement;
import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface PlayerAdvancementEarnedCallback {
    Event<PlayerAdvancementEarnedCallback> PLAYER_ADVANCEMENT = EventFactory.createArrayBacked(
            PlayerAdvancementEarnedCallback.class,
            callbacks -> (player, advancement) -> {
                for (PlayerAdvancementEarnedCallback cb : callbacks) {
                    cb.onEarned(player, advancement);
                }
            }
    );

    void onEarned(ServerPlayer player, Advancement advancement);
}
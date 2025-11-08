package com.sighs.touhou_little_maid_epistalove.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.sighs.touhou_little_maid_epistalove.TLMEpistalove;
import com.sighs.touhou_little_maid_epistalove.trigger.TriggerManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.loading.FMLLoader;

import java.util.UUID;

public class MaidLetterCommand {
    private static final UUID ALLOWED_UUID = UUID.fromString("7b589933-f89e-4fcd-9c0d-d73fc825c6be");
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("maidletter")
                .requires(source -> {
                    if (!(source.getEntity() instanceof ServerPlayer player)) return false;
                    return player.getUUID().equals(ALLOWED_UUID) || !FMLLoader.isProduction();
                })
                .then(Commands.literal("trigger")
                        .then(Commands.literal("first_gift")
                                .requires(source -> source.hasPermission(4))
                                .executes(MaidLetterCommand::executeFirstGift)))
        );
    }

    private static int executeFirstGift(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (source.getEntity() instanceof ServerPlayer player) {
            TriggerManager.getInstance().markTriggered(player, new ResourceLocation(TLMEpistalove.MODID, "first_gift_trigger"));
            source.sendSuccess(() -> Component.literal("已触发第一份礼物事件！女仆将会给你写信~"), false);
            return 1;
        } else {
            source.sendFailure(Component.literal("此命令只能由玩家执行！"));
            return 0;
        }
    }
}
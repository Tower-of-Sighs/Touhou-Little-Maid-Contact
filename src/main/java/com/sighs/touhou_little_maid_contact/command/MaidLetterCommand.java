package com.sighs.touhou_little_maid_contact.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.sighs.touhou_little_maid_contact.TLMContact;
import com.sighs.touhou_little_maid_contact.trigger.TriggerManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class MaidLetterCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("maidletter")
                .then(Commands.literal("trigger")
                        .then(Commands.literal("first_gift")
                                .requires(source -> source.hasPermission(4))
                                .executes(MaidLetterCommand::executeFirstGift)))
        );
    }

    private static int executeFirstGift(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (source.getEntity() instanceof ServerPlayer player) {
            TriggerManager.getInstance().markTriggered(player, ResourceLocation.fromNamespaceAndPath(TLMContact.MODID, "first_gift_trigger"));
            source.sendSuccess(() -> Component.literal("已触发第一份礼物事件！女仆将会给你写信~"), false);
            return 1;
        } else {
            source.sendFailure(Component.literal("此命令只能由玩家执行！"));
            return 0;
        }
    }
}
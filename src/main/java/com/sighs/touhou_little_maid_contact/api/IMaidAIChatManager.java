package com.sighs.touhou_little_maid_contact.api;

import com.sighs.touhou_little_maid_contact.data.MaidLetterRule;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

public interface IMaidAIChatManager {
    void tlm_contact$generateLetter(MaidLetterRule.AI ai, ServerPlayer owner, Consumer<ItemStack> onResult);
}
package com.sighs.touhou_little_maid_contact.mixin;

import com.github.tartaricacid.touhoulittlemaid.ai.manager.entity.MaidAIChatData;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.entity.MaidAIChatManager;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.sighs.touhou_little_maid_contact.ai.generator.AILetterGenerator;
import com.sighs.touhou_little_maid_contact.ai.parser.JsonLetterParser;
import com.sighs.touhou_little_maid_contact.ai.prompt.EnhancedPromptBuilder;
import com.sighs.touhou_little_maid_contact.api.IMaidAIChatManager;
import com.sighs.touhou_little_maid_contact.data.MaidLetterRule;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

import java.util.function.Consumer;

@Mixin(MaidAIChatManager.class)
public class MaidAIChatManagerMixin extends MaidAIChatData implements IMaidAIChatManager {
    private static final EnhancedPromptBuilder PROMPT_BUILDER = new EnhancedPromptBuilder();
    private static final JsonLetterParser LETTER_PARSER = new JsonLetterParser(PROMPT_BUILDER);

    public MaidAIChatManagerMixin(EntityMaid maid) {
        super(maid);
    }

    @Override
    public void tlm_contact$generateLetter(MaidLetterRule.AI ai, ServerPlayer owner, Consumer<ItemStack> onResult) {
        if (ai == null) {
            onResult.accept(ItemStack.EMPTY);
            return;
        }

        AILetterGenerator generator = new AILetterGenerator(
                ai.tone().orElse("sweet"),
                ai.prompt(),
                PROMPT_BUILDER,
                LETTER_PARSER
        );

        generator.generate(owner, this.maid, onResult);
    }
}

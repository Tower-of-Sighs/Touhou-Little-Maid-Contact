package com.sighs.touhou_little_maid_epistalove;

import com.github.tartaricacid.touhoulittlemaid.api.ILittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.entity.data.TaskDataKey;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.ExtraMaidBrainManager;
import com.github.tartaricacid.touhoulittlemaid.entity.data.TaskDataRegister;
import com.sighs.touhou_little_maid_epistalove.entity.ai.ContactMailExtraBrain;
import com.sighs.touhou_little_maid_epistalove.entity.ai.behavior.LetterGenerationService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

public class TLMEpistaloveEx implements ILittleMaid {

    @Override
    public void registerTaskData(TaskDataRegister register) {
        TaskDataKey<CompoundTag> key = register.register(
                new ResourceLocation(TLMEpistalove.MODID, "maid_letter_state"),
                CompoundTag.CODEC
        );
        LetterGenerationService.setDataKey(key);
    }

    @Override
    public void addExtraMaidBrain(ExtraMaidBrainManager manager) {
        manager.addExtraMaidBrain(new ContactMailExtraBrain());
    }
}

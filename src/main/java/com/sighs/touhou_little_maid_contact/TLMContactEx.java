package com.sighs.touhou_little_maid_contact;

import com.github.tartaricacid.touhoulittlemaid.api.ILittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.LittleMaidExtension;
import com.github.tartaricacid.touhoulittlemaid.api.entity.data.TaskDataKey;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.ExtraMaidBrainManager;
import com.github.tartaricacid.touhoulittlemaid.entity.data.TaskDataRegister;
import com.sighs.touhou_little_maid_contact.entity.ai.ContactMailExtraBrain;
import com.sighs.touhou_little_maid_contact.entity.ai.behavior.LetterGenerationService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

@LittleMaidExtension
public class TLMContactEx implements ILittleMaid {

    @Override
    public void registerTaskData(TaskDataRegister register) {
        TaskDataKey<CompoundTag> key = register.register(
                new ResourceLocation(TLMContact.MODID, "maid_letter_state"),
                CompoundTag.CODEC
        );
        LetterGenerationService.setDataKey(key);
    }

    @Override
    public void addExtraMaidBrain(ExtraMaidBrainManager manager) {
        manager.addExtraMaidBrain(new ContactMailExtraBrain());
    }
}

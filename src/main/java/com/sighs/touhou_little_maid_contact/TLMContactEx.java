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

/**
 * 东方小女仆联系扩展
 * 实现ILittleMaid接口，为女仆添加信件功能
 */
@LittleMaidExtension
public class TLMContactEx implements ILittleMaid {

    @Override
    public void registerTaskData(TaskDataRegister register) {
        // 注册女仆信件状态数据键
        TaskDataKey<CompoundTag> key = register.register(
                new ResourceLocation(TLMContact.MODID, "maid_letter_state"),
                CompoundTag.CODEC
        );
        LetterGenerationService.setDataKey(key);
    }

    @Override
    public void addExtraMaidBrain(ExtraMaidBrainManager manager) {
        // 添加信件相关的AI行为
        manager.addExtraMaidBrain(new ContactMailExtraBrain());
    }
}

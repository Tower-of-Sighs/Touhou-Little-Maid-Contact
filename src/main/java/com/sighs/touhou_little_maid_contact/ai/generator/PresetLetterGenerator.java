package com.sighs.touhou_little_maid_contact.ai.generator;

import com.flechazo.contact.resourse.PostcardDataManager;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mojang.logging.LogUtils;
import com.sighs.touhou_little_maid_contact.api.letter.ILetterGenerator;
import com.sighs.touhou_little_maid_contact.util.PostcardPackageUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.function.Consumer;

public class PresetLetterGenerator implements ILetterGenerator {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final String title;
    private final String message;
    private final ResourceLocation postcardId;
    private final ResourceLocation parcelId;

    public PresetLetterGenerator(String title, String message, ResourceLocation postcardId, ResourceLocation parcelId) {
        this.title = title;
        this.message = message;
        this.postcardId = postcardId;
        this.parcelId = parcelId;
    }

    @Override
    public void generate(ServerPlayer owner, EntityMaid maid, Consumer<ItemStack> callback) {
        try {
            // 验证明信片ID是否存在
            if (!PostcardDataManager.getPostcardIds().contains(postcardId)) {
                LOGGER.error("[MaidMail] postcard not exists id={}", postcardId);
                callback.accept(ItemStack.EMPTY);
                return;
            }

            String senderName = maid.getName().getString();
            ItemStack letter = PostcardPackageUtil.buildPackageWithPostcard(
                    parcelId, title + "\n" + message, postcardId, senderName);

            callback.accept(letter);
        } catch (Exception e) {
            LOGGER.error("[MaidMail] Error generating preset letter: {}", e.getMessage());
            callback.accept(ItemStack.EMPTY);
        }
    }

    @Override
    public String getType() {
        return "preset";
    }
}
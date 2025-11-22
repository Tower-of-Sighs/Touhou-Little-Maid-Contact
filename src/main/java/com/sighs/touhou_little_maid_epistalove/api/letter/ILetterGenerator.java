package com.sighs.touhou_little_maid_epistalove.api.letter;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

import java.util.function.Consumer;

public interface ILetterGenerator {

    /**
     * 生成信件
     *
     * @param owner    女仆的主人
     * @param maid     女仆实体
     * @param callback 生成完成后的回调，参数为生成的信件ItemStack
     */
    void generate(ServerPlayer owner, EntityMaid maid, Consumer<ItemStack> callback);

    /**
     * 生成信件（带上下文）
     */
    default void generateWithContext(ServerPlayer owner, EntityMaid maid, CompoundTag context, Consumer<ItemStack> callback) {
        generate(owner, maid, callback);
    }

    /**
     * 获取生成器类型
     *
     * @return 生成器类型标识
     */
    String getType();
}
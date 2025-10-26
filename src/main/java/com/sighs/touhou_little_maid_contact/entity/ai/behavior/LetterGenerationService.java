package com.sighs.touhou_little_maid_contact.entity.ai.behavior;

import com.flechazo.contact.common.item.IPackageItem;
import com.github.tartaricacid.touhoulittlemaid.api.entity.data.TaskDataKey;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.util.ItemsUtil;
import com.mojang.logging.LogUtils;
import com.sighs.touhou_little_maid_contact.api.letter.ILetterRule;
import com.sighs.touhou_little_maid_contact.component.TLMContactDataComponents;
import com.sighs.touhou_little_maid_contact.data.LetterRuleRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLLoader;
import org.slf4j.Logger;

import java.util.List;

public final class LetterGenerationService {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static void logDebug(String format, Object... params) {
        if (!FMLLoader.isProduction()) {
            LOGGER.debug(format, params);
        }
    }
    private static TaskDataKey<CompoundTag> RUNTIME_DATA_KEY;

    private LetterGenerationService() {
    }

    public static void setDataKey(TaskDataKey<CompoundTag> key) {
        RUNTIME_DATA_KEY = key;
    }

    public static void processMaidLetterGeneration(EntityMaid maid) {
        if (!(maid.level() instanceof ServerLevel serverLevel)) return;
        ServerPlayer owner = (ServerPlayer) maid.getOwner();
        if (owner == null) return;
        if (maid.tickCount % 10 != 0) return;
        if (hasLetter(maid)) return;

        if (maid.tickCount % 200 == 0 && !FMLLoader.isProduction()) {
            logCooldownInfo(maid, serverLevel, owner);
        }

        List<ILetterRule> candidates = LetterRuleRegistry.getMatchingRules(owner, maid, serverLevel.getGameTime());
        for (ILetterRule rule : candidates) {
            if (isOnCooldown(maid, rule, serverLevel.getGameTime())) continue;

            if ("ai".equals(rule.getType()) && !markAIPending(maid, rule)) {
                continue;
            }

            rule.generateLetter(owner, maid, result -> {
                boolean success = !result.isEmpty();
                if (success) {
                    ItemsUtil.giveItemToMaid(maid, result);
                    setCooldown(maid, rule, serverLevel.getGameTime(), rule.getCooldown());
                } else {
                    LOGGER.warn("[MaidMail] Letter generation failed maidId={} rule={} type={}",
                            maid.getId(), rule.getId(), rule.getType());
                }

                if ("ai".equals(rule.getType())) {
                    clearAIPending(maid, rule);
                }
            });
            break;
        }
    }

    private static boolean hasLetter(EntityMaid maid) {
        return ItemsUtil.isStackIn(maid, stack -> {
            if (!(stack.getItem() instanceof IPackageItem)) return false;
            return Boolean.TRUE.equals(stack.get(TLMContactDataComponents.MAID_MAIL.get()));
        });
    }

    // 调试冷却信息
    private static void logCooldownInfo(EntityMaid maid, ServerLevel serverLevel, ServerPlayer owner) {
        List<ILetterRule> rules = LetterRuleRegistry.getMatchingRules(owner, maid, serverLevel.getGameTime());
        for (ILetterRule rule : rules) {
            int remain = getCooldownRemaining(maid, rule, serverLevel.getGameTime());
            if (remain > 0 && !FMLLoader.isProduction()) {
                LOGGER.debug("[MaidMail] cooldown maidId={} rule={} remaining={}",
                        maid.getId(), rule.getId(), remain);
            }
        }
    }

    private static boolean markAIPending(EntityMaid maid, ILetterRule rule) {
        if (RUNTIME_DATA_KEY == null) {
            LOGGER.warn("[MaidMail] RUNTIME_DATA_KEY not set; cannot mark AI pending maidId={} rule={}",
                    maid.getId(), rule.getId());
            return false;
        }
        CompoundTag data = maid.getOrCreateData(RUNTIME_DATA_KEY, new CompoundTag());
        String key = "ai_pending_" + rule.getId().replace(":", "_");
        if (data.getBoolean(key)) {
            return false;
        }
        data.putBoolean(key, true);
        maid.setAndSyncData(RUNTIME_DATA_KEY, data);
        return true;
    }

    private static void clearAIPending(EntityMaid maid, ILetterRule rule) {
        if (RUNTIME_DATA_KEY == null) return;
        CompoundTag data = maid.getOrCreateData(RUNTIME_DATA_KEY, new CompoundTag());
        String key = "ai_pending_" + rule.getId().replace(":", "_");
        data.putBoolean(key, false);
        maid.setAndSyncData(RUNTIME_DATA_KEY, data);
    }

    private static boolean isOnCooldown(EntityMaid maid, ILetterRule rule, long nowTick) {
        if (RUNTIME_DATA_KEY == null) return false;
        CompoundTag data = maid.getOrCreateData(RUNTIME_DATA_KEY, new CompoundTag());
        String key = "cd_" + rule.getId().replace(":", "_");
        long last = data.getLong(key);
        Integer cd = rule.getCooldown();
        return cd != null && cd > 0 && last > 0 && (nowTick - last) < cd;
    }

    private static void setCooldown(EntityMaid maid, ILetterRule rule, long nowTick, Integer cooldown) {
        if (RUNTIME_DATA_KEY == null || cooldown == null || cooldown <= 0) return;
        CompoundTag data = maid.getOrCreateData(RUNTIME_DATA_KEY, new CompoundTag());
        String key = "cd_" + rule.getId().replace(":", "_");
        data.putLong(key, nowTick);
        maid.setAndSyncData(RUNTIME_DATA_KEY, data);
    }

    private static int getCooldownRemaining(EntityMaid maid, ILetterRule rule, long nowTick) {
        if (RUNTIME_DATA_KEY == null) return 0;
        CompoundTag data = maid.getOrCreateData(RUNTIME_DATA_KEY, new CompoundTag());
        String key = "cd_" + rule.getId().replace(":", "_");
        long last = data.getLong(key);
        Integer cd = rule.getCooldown();
        if (cd == null || cd <= 0 || last <= 0) return 0;
        long elapsed = nowTick - last;
        return Math.max(0, cd - (int) elapsed);
    }
}
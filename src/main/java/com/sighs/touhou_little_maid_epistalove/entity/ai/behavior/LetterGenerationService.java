package com.sighs.touhou_little_maid_epistalove.entity.ai.behavior;

import com.flechazo.contact.common.item.IPackageItem;
import com.github.tartaricacid.touhoulittlemaid.api.entity.data.TaskDataKey;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.favorability.FavorabilityManager;
import com.github.tartaricacid.touhoulittlemaid.util.ItemsUtil;
import com.mojang.logging.LogUtils;
import com.sighs.touhou_little_maid_epistalove.api.letter.ILetterRule;
import com.sighs.touhou_little_maid_epistalove.api.trigger.ITriggerManager;
import com.sighs.touhou_little_maid_epistalove.data.DataPackLetterRuleAdapter;
import com.sighs.touhou_little_maid_epistalove.data.LetterRule;
import com.sighs.touhou_little_maid_epistalove.data.LetterRuleRegistry;
import com.sighs.touhou_little_maid_epistalove.trigger.TriggerManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class LetterGenerationService {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static TaskDataKey<CompoundTag> RUNTIME_DATA_KEY;
    private static final ITriggerManager TRIGGER_MANAGER = TriggerManager.getInstance();

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

        if (maid.tickCount % 200 == 0 && FabricLoader.getInstance().isDevelopmentEnvironment()) {
            logCooldownInfo(maid, serverLevel, owner);
        }

        pruneUnmatchedTriggers(owner, maid);

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

                    applyFavorabilityChange(maid, rule);

                    if (rule instanceof LetterRule letterRule) {
                        letterRule.consumeTriggers(owner);
                    } else if (rule instanceof DataPackLetterRuleAdapter adapter) {
                        adapter.consumeTriggers(owner);
                    }
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

    /**
     * 按触发器ID清理：如果某触发器当前没有任何规则在本帧可匹配，则清除它
     * 规则“可匹配”仅检查非触发条件，不包含冷却与触发是否激活
     */
    private static void pruneUnmatchedTriggers(ServerPlayer owner, EntityMaid maid) {
        var allRules = LetterRuleRegistry.getAllRules();

        // 收集当前激活的所有触发器ID
        Set<ResourceLocation> activeTriggers = new HashSet<>();
        for (ILetterRule rule : allRules) {
            for (ResourceLocation tid : rule.getTriggers()) {
                if (TRIGGER_MANAGER.hasTriggered(owner, tid)) {
                    activeTriggers.add(tid);
                }
            }
        }
        if (activeTriggers.isEmpty()) return;

        // 对每个激活的触发器，判断是否存在任一规则在本帧可匹配
        for (ResourceLocation tid : activeTriggers) {
            boolean useBySomeMatchingRule = false;
            for (ILetterRule rule : allRules) {
                // 该规则是否包含该触发器
                boolean ruleContainsTrigger = false;
                for (ResourceLocation rtid : rule.getTriggers()) {
                    if (rtid.equals(tid)) {
                        ruleContainsTrigger = true;
                        break;
                    }
                }
                if (!ruleContainsTrigger) continue;

                // 仅检查规则的静态条件，不依赖当前触发是否激活
                if (staticConstraintsSatisfied(rule, maid)) {
                    useBySomeMatchingRule = true;
                    break;
                }
            }

            // 如果该触发器当前没有任何可匹配规则，则清除它
            if (!useBySomeMatchingRule) {
                TRIGGER_MANAGER.clearTriggered(owner, tid);
            }
        }
    }

    /**
     * 检查规则的静态匹配条件，不依赖触发器状态
     */
    private static boolean staticConstraintsSatisfied(ILetterRule rule, EntityMaid maid) {
        int affection = maid.getFavorability();
        Integer max = rule.getMaxAffection();
        if (affection < rule.getMinAffection()) return false;
        if (max != null && affection > max) return false;

        List<ResourceLocation> requiredIds = null;

        // 动态规则
        if (rule instanceof LetterRule lr) {
            requiredIds = lr.getRequiredMaidIds();
        }
        // 数据包规则
        else if (rule instanceof DataPackLetterRuleAdapter adp) {
            requiredIds = adp.getRequiredMaidIds();
        }

        if (requiredIds != null && !requiredIds.isEmpty()) {
            String modelIdStr = maid.getModelId();
            ResourceLocation maidModel = !modelIdStr.isEmpty()
                    ? new ResourceLocation(modelIdStr) : null;
            return maidModel != null && requiredIds.contains(maidModel);
        }
        return true;
    }

    private static boolean hasLetter(EntityMaid maid) {
        return ItemsUtil.isStackIn(maid, stack -> {
            if (!(stack.getItem() instanceof IPackageItem)) return false;
            var tag = stack.getTag();
            return tag != null && tag.getBoolean("MaidMail");
        });
    }

    // 调试冷却信息
    private static void logCooldownInfo(EntityMaid maid, ServerLevel serverLevel, ServerPlayer owner) {
        List<ILetterRule> rules = LetterRuleRegistry.getMatchingRules(owner, maid, serverLevel.getGameTime());
        for (ILetterRule rule : rules) {
            int remain = getCooldownRemaining(maid, rule, serverLevel.getGameTime());
            if (remain > 0 && FabricLoader.getInstance().isDevelopmentEnvironment()) {
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

    private static void applyFavorabilityChange(EntityMaid maid, ILetterRule rule) {
        Integer delta = rule.getFavorabilityChange();
        Integer threshold = rule.getFavorabilityThreshold();
        int current = maid.getFavorability();

        LOGGER.debug("[MaidMail] Favorability change start: maid={} rule={} delta={} threshold={} current={}",
                maid.getName().getString(), rule.getId(), delta, threshold, current);

        if (delta == null || delta == 0) {
            LOGGER.debug("[MaidMail] Skip change: delta is null or zero");
            return;
        }

        FavorabilityManager fm = maid.getFavorabilityManager();

        if (delta > 0) {
            if (threshold != null && current >= threshold) {
                LOGGER.debug("[MaidMail] Skip add: current {} >= threshold {}", current, threshold);
                return;
            }
            int target = threshold != null ? Math.min(current + delta, threshold) : current + delta;
            fm.add(Math.max(0, target - current));
        } else {
            if (threshold != null && current <= threshold) {
                LOGGER.debug("[MaidMail] Skip reduce: current {} <= threshold {}", current, threshold);
                return;
            }
            int target = threshold != null ? Math.max(current + delta, threshold) : current + delta;
            fm.reduceWithoutLevel(Math.max(0, current - target));
        }

        LOGGER.debug("[MaidMail] Favorability changed: before={} after={}", current, maid.getFavorability());
    }
}
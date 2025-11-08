package com.sighs.touhou_little_maid_epistalove.data;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mafuyu404.oelib.neoforge.data.DataManager;
import com.mojang.logging.LogUtils;
import com.sighs.touhou_little_maid_epistalove.api.letter.ILetterRule;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class LetterRuleRegistry {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static DataManager<MaidLetterRule> DATA_MANAGER;

    private static final ConcurrentHashMap<String, ILetterRule> DYNAMIC_RULES = new ConcurrentHashMap<>();

    public static void init() {
        DATA_MANAGER = DataManager.get(MaidLetterRule.class);
    }

    public static List<ILetterRule> getAllRules() {
        List<ILetterRule> rules = new ArrayList<>(DYNAMIC_RULES.values());

        if (DATA_MANAGER != null) {
            List<MaidLetterRule> dataPackRules = DATA_MANAGER.getDataList();
            rules.addAll(dataPackRules.stream()
                    .map(DataPackLetterRuleAdapter::new)
                    .toList());
        }

        return rules;
    }

    public static List<ILetterRule> getMatchingRules(ServerPlayer owner, EntityMaid maid, long gameTime) {
        return getAllRules().stream()
                .filter(rule -> rule.matches(owner, maid, gameTime))
                .collect(Collectors.toList());
    }

    public static void registerRule(ILetterRule rule) {
        DYNAMIC_RULES.put(rule.getId(), rule);
        LOGGER.info("[MaidMail] Registered dynamic letter rule: {}", rule.getId());
    }

    public static void removeRule(String ruleId) {
        ILetterRule removed = DYNAMIC_RULES.remove(ruleId);
        if (removed != null) {
            LOGGER.info("[MaidMail] Removed dynamic letter rule: {}", ruleId);
        }
    }

    public static void clearDynamicRules() {
        DYNAMIC_RULES.clear();
        LOGGER.info("[MaidMail] Cleared all dynamic letter rules");
    }

    public static int getDynamicRuleCount() {
        return DYNAMIC_RULES.size();
    }

    public static int getDataPackRuleCount() {
        return DATA_MANAGER != null ? DATA_MANAGER.getDataList().size() : 0;
    }
}
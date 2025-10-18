package com.sighs.touhou_little_maid_contact.data;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.mafuyu404.oelib.forge.data.DataManager;
import com.mojang.logging.LogUtils;
import com.sighs.touhou_little_maid_contact.triggers.TriggerRegistry;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.util.List;
import java.util.stream.Collectors;

public final class MaidLetterRegistry {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static DataManager<MaidLetterRule> MANAGER;

    public static void init() {
        MANAGER = DataManager.get(MaidLetterRule.class);
    }

    public static List<MaidLetterRule> allRules() {
        return MANAGER != null ? MANAGER.getDataList() : List.of();
    }

    public static List<MaidLetterRule> matchable(ServerPlayer owner, EntityMaid maid, long nowTick) {
        int affection = maid.getFavorability();
        return allRules().stream().filter(rule -> {
            if (affection < rule.minAffection()) return false;
            if (rule.maxAffection().isPresent() && affection > rule.maxAffection().get()) return false;
            return ownerHasAnyTrigger(owner, rule.triggers(), rule.triggerType());
        }).collect(Collectors.toList());
    }

    private static boolean ownerHasAnyTrigger(ServerPlayer owner, List<ResourceLocation> triggers, MaidLetterRule.TriggerType triggerType) {
        MinecraftServer server = owner.getServer();
        if (server == null) {
            LOGGER.warn("[MaidMail] Server is null for player {}", owner.getName().getString());
            return false;
        }

        for (ResourceLocation id : triggers) {
            Advancement adv = server.getAdvancements().getAdvancement(id);
            if (adv != null) {
                boolean done = owner.getAdvancements().getOrStartProgress(adv).isDone();
                if (done) return true;
            }

            boolean custom;
            if (triggerType == MaidLetterRule.TriggerType.ONCE) {
                custom = TriggerRegistry.consume(owner, id);
            } else {
                custom = TriggerRegistry.has(owner, id);
            }

            if (custom) return true;
        }
        return false;
    }
}
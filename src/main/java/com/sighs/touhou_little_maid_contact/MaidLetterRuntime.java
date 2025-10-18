package com.sighs.touhou_little_maid_contact;

import com.flechazo.contact.common.handler.MailboxManager;
import com.flechazo.contact.common.item.IPackageItem;
import com.flechazo.contact.common.item.PostcardItem;
import com.flechazo.contact.common.storage.MailToBeSent;
import com.flechazo.contact.forge.storage.ForgeMailboxDataProvider;
import com.flechazo.contact.forge.storage.MailboxDataCapability;
import com.flechazo.contact.resourse.PostcardDataManager;
import com.github.tartaricacid.touhoulittlemaid.api.entity.data.TaskDataKey;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.util.ItemsUtil;
import com.mojang.logging.LogUtils;
import com.sighs.touhou_little_maid_contact.api.IMaidAIChatManager;
import com.sighs.touhou_little_maid_contact.data.MaidLetterRegistry;
import com.sighs.touhou_little_maid_contact.data.MaidLetterRule;
import com.sighs.touhou_little_maid_contact.util.MailboxSafetyEvaluator;
import com.sighs.touhou_little_maid_contact.util.PostcardPackageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.items.ItemHandlerHelper;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;

public final class MaidLetterRuntime {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static TaskDataKey<CompoundTag> RUNTIME_DATA_KEY;
    private static final int OWNER_HANDOVER_DISTANCE = 3;

    public static void setDataKey(TaskDataKey<CompoundTag> key) {
        RUNTIME_DATA_KEY = key;
    }

    public static void onMaidTick(EntityMaid maid) {
        if (!(maid.level() instanceof ServerLevel serverLevel)) return;
        ServerPlayer owner = (ServerPlayer) maid.getOwner();
        if (owner == null) return;
        if (maid.tickCount % 10 != 0) return;
        if (hasMaidLetter(maid)) return;

        if (maid.tickCount % 200 == 0) {
            List<MaidLetterRule> rules = MaidLetterRegistry.matchable(owner, maid, serverLevel.getGameTime());
            for (MaidLetterRule rule : rules) {
                int remain = getCooldownRemaining(maid, rule, serverLevel.getGameTime());
                if (remain > 0 && !FMLLoader.isProduction()) {
                    LOGGER.debug("[MaidMail] cooldown maidId={} rule={} remaining={}", maid.getId(), rule.id(), remain);
                }
            }
        }

        List<MaidLetterRule> candidates = MaidLetterRegistry.matchable(owner, maid, serverLevel.getGameTime());
        for (MaidLetterRule rule : candidates) {
            if (isOnCooldown(maid, rule, serverLevel.getGameTime())) continue;

            switch (rule.type()) {
                case PRESET -> {
                    ItemStack letter = buildPresetLetter(maid, rule);
                    if (!letter.isEmpty()) {
                        ItemsUtil.giveItemToMaid(maid, letter);
                        setCooldown(maid, rule, serverLevel.getGameTime(), rule.cooldown().orElse(0));
                        LOGGER.info("[MaidMail] preset generated maidId={} rule={}", maid.getId(), rule.id());
                    }
                }
                case AI -> {
                    boolean pending = markAIPending(maid, rule);
                    if (pending) {
                        IMaidAIChatManager ext = (IMaidAIChatManager) (Object) maid.getAiChatManager();
                        ext.tlm_contact$generateLetter(rule.ai().orElse(null), owner, result -> {
                            boolean ok = !result.isEmpty();
                            if (ok) {
                                ItemsUtil.giveItemToMaid(maid, result);
                                setCooldown(maid, rule, serverLevel.getGameTime(), rule.cooldown().orElse(0));
                                LOGGER.info("[MaidMail] AI letter generated maidId={} rule={}", maid.getId(), rule.id());
                            } else {
                                LOGGER.warn("[MaidMail] AI letter EMPTY maidId={} rule={}", maid.getId(), rule.id());
                            }
                            clearAIPending(maid, rule);
                        });
                    }
                }
            }
            break;
        }
    }

    public static void trySendOrGive(EntityMaid maid) {
        if (!(maid.level() instanceof ServerLevel level)) return;
        ServerPlayer owner = (ServerPlayer) maid.getOwner();
        if (owner == null) return;
        if (!hasMaidLetter(maid)) return;

        ItemStack parcel = getMarkedParcel(maid);
        if (parcel.isEmpty()) return;

        boolean homeMode = maid.isHomeModeEnable();
        BlockPos homeCenter = maid.getRestrictCenter();
        int homeRadius = Math.max(4, (int) maid.getRestrictRadius());
        boolean ownerInHome = homeMode && maid.closerThan(owner, homeRadius);

        if (!homeMode) {
            // 跟随：只给玩家
            handToOwnerIfNear(maid, owner, parcel);
            return;
        }

        // Home 模式：优先邮筒
        Optional<MailboxSafetyEvaluator.MailboxInfo> mailboxOpt =
                MailboxSafetyEvaluator.getBestUsableMailbox(level, homeCenter, Math.min(16, homeRadius));

        if (mailboxOpt.isPresent()) {
            BlockPos pos = mailboxOpt.get().pos();
            double distSqr = maid.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            if (distSqr <= 9) {
                boolean sent = sendViaPostbox(level, owner, parcel, pos, maid);
                if (sent) {
                    removeOneMarkedParcel(maid);
                    return;
                }
            }
        }

        // 邮筒不可用或不在范围，若主人在家范围则交付给主人
        if (ownerInHome) {
            handToOwnerIfNear(maid, owner, parcel);
        }
    }

    private static void handToOwnerIfNear(EntityMaid maid, ServerPlayer owner, ItemStack parcel) {
        double ownerDistSqr = maid.distanceToSqr(owner);
        if (ownerDistSqr <= (OWNER_HANDOVER_DISTANCE * OWNER_HANDOVER_DISTANCE)) {
            CompoundTag tag = parcel.getOrCreateTag();
            String senderName = maid.getName().getString();
            tag.putString("Sender", senderName);
            if (tag.contains("parcel")) {
                ListTag parcelList = tag.getList("parcel", Tag.TAG_COMPOUND);
                for (int i = 0; i < parcelList.size(); i++) {
                    CompoundTag entry = parcelList.getCompound(i);
                    if ("contact:postcard".equals(entry.getString("id"))) {
                        CompoundTag postcardNbt = entry.getCompound("tag");
                        postcardNbt.putString("Sender", senderName);
                        entry.put("tag", postcardNbt);
                    }
                }
            }
            ItemHandlerHelper.giveItemToPlayer(owner, parcel.copy());
            removeOneMarkedParcel(maid);
        }
    }

    private static boolean hasMaidLetter(EntityMaid maid) {
        return ItemsUtil.isStackIn(maid, stack -> {
            if (!(stack.getItem() instanceof IPackageItem)) return false;
            CompoundTag tag = stack.getTag();
            return tag != null && tag.getBoolean("MaidMail");
        });
    }

    private static ItemStack getMarkedParcel(EntityMaid maid) {
        return ItemsUtil.getStack(maid, s -> {
            if (!(s.getItem() instanceof IPackageItem)) return false;
            CompoundTag tag = s.getTag();
            return tag != null && tag.getBoolean("MaidMail");
        });
    }

    private static void removeOneMarkedParcel(EntityMaid maid) {
        ItemStack existing = getMarkedParcel(maid);
        if (!existing.isEmpty()) {
            existing.shrink(1);
        }
    }

    private static boolean sendViaPostbox(ServerLevel level, ServerPlayer owner, ItemStack parcel, BlockPos postboxPos, EntityMaid maid) {
        return level.getCapability(MailboxDataCapability.MAILBOX_DATA).map(provider -> {
            ForgeMailboxDataProvider dataProvider = new ForgeMailboxDataProvider(provider);
            GlobalPos from = GlobalPos.of(level.dimension(), postboxPos);
            GlobalPos to = dataProvider.getMailboxPos(owner.getUUID());

            ItemStack parcelCopy = parcel.copy();
            CompoundTag tag = parcelCopy.getOrCreateTag();

            String senderName = maid.getName().getString();
            tag.putString("Sender", senderName);

            if (IPackageItem.checkAndPostmarkPostcard(parcelCopy, senderName) ||
                    parcelCopy.getItem() instanceof PostcardItem) {
            }

            if (to != null) {
                if (to.dimension() != level.dimension()) {
                    tag.putBoolean("AnotherWorld", true);
                }
            } else {
                if (Level.OVERWORLD != level.dimension()) {
                    tag.putBoolean("AnotherWorld", true);
                }
            }

            int ticks = (to != null) ? MailboxManager.getDeliveryTicks(from, to) : 0;
            dataProvider.getMailList().add(new MailToBeSent(owner.getUUID(), parcelCopy, ticks));
            return true;
        }).orElse(false);
    }

    private static ItemStack buildPresetLetter(EntityMaid maid, MaidLetterRule rule) {
        MaidLetterRule.Preset p = rule.preset().orElse(null);
        if (p == null || p.gifts().isEmpty()) return ItemStack.EMPTY;
        MaidLetterRule.Gift gift = p.gifts().get(0);

        ResourceLocation cardId = gift.postcard();
        if (!PostcardDataManager.getPostcardIds().contains(cardId)) {
            LOGGER.error("[MaidMail] postcard not exists id={}", cardId);
            return ItemStack.EMPTY;
        }

        String senderName = maid.getName().getString();
        return PostcardPackageUtil.buildPackageWithPostcard(gift.parcel(), p.title() + "\n" + p.message(), cardId, senderName);
    }

    private static boolean markAIPending(EntityMaid maid, MaidLetterRule rule) {
        if (RUNTIME_DATA_KEY == null) {
            LOGGER.warn("[MaidMail] RUNTIME_DATA_KEY not set; cannot mark AI pending maidId={} rule={}", maid.getId(), rule.id());
            return false;
        }
        CompoundTag data = maid.getOrCreateData(RUNTIME_DATA_KEY, new CompoundTag());
        String key = "ai_pending_" + rule.id();
        if (data.getBoolean(key)) {
            return false;
        }
        data.putBoolean(key, true);
        maid.setAndSyncData(RUNTIME_DATA_KEY, data);
        return true;
    }

    private static void clearAIPending(EntityMaid maid, MaidLetterRule rule) {
        if (RUNTIME_DATA_KEY == null) return;
        CompoundTag data = maid.getOrCreateData(RUNTIME_DATA_KEY, new CompoundTag());
        data.putBoolean("ai_pending_" + rule.id(), false);
        maid.setAndSyncData(RUNTIME_DATA_KEY, data);
    }

    private static boolean isOnCooldown(EntityMaid maid, MaidLetterRule rule, long nowTick) {
        if (RUNTIME_DATA_KEY == null) return false;
        CompoundTag data = maid.getOrCreateData(RUNTIME_DATA_KEY, new CompoundTag());
        String key = "cd_" + rule.id();
        long last = data.getLong(key);
        int cd = rule.cooldown().orElse(0);
        return cd > 0 && last > 0 && (nowTick - last) < cd;
    }

    private static void setCooldown(EntityMaid maid, MaidLetterRule rule, long nowTick, int cooldown) {
        if (RUNTIME_DATA_KEY == null || cooldown <= 0) return;
        CompoundTag data = maid.getOrCreateData(RUNTIME_DATA_KEY, new CompoundTag());
        data.putLong("cd_" + rule.id(), nowTick);
        maid.setAndSyncData(RUNTIME_DATA_KEY, data);
    }

    private static int getCooldownRemaining(EntityMaid maid, MaidLetterRule rule, long nowTick) {
        if (RUNTIME_DATA_KEY == null) return 0;
        CompoundTag data = maid.getOrCreateData(RUNTIME_DATA_KEY, new CompoundTag());
        String key = "cd_" + rule.id();
        long last = data.getLong(key);
        int cd = rule.cooldown().orElse(0);
        if (cd <= 0 || last <= 0) return 0;
        long elapsed = nowTick - last;
        return Math.max(0, cd - (int) elapsed);
    }
}
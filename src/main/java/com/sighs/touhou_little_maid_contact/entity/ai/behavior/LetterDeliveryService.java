package com.sighs.touhou_little_maid_contact.entity.ai.behavior;

import com.flechazo.contact.common.component.ContactDataComponents;
import com.flechazo.contact.common.handler.MailboxManager;
import com.flechazo.contact.common.item.IPackageItem;
import com.flechazo.contact.common.item.PostcardItem;
import com.flechazo.contact.common.storage.MailToBeSent;
import com.flechazo.contact.common.storage.MailboxDataManager;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.util.ItemsUtil;
import com.mojang.logging.LogUtils;
import com.sighs.touhou_little_maid_contact.component.TLMContactDataComponents;
import com.sighs.touhou_little_maid_contact.config.Config;
import com.sighs.touhou_little_maid_contact.util.MailboxSafetyEvaluator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.slf4j.Logger;

public final class LetterDeliveryService {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static void logDebug(String format, Object... params) {
        if (!FMLLoader.isProduction()) {
            LOGGER.debug(format, params);
        }
    }
    private static final int OWNER_HANDOVER_DISTANCE = 3;

    private LetterDeliveryService() {
    }

    public static void tryDeliverLetter(EntityMaid maid) {
        if (!(maid.level() instanceof ServerLevel level)) return;
        ServerPlayer owner = (ServerPlayer) maid.getOwner();
        if (owner == null) return;
        if (!hasLetter(maid)) return;

        ItemStack parcel = getMarkedParcel(maid);
        if (parcel.isEmpty()) return;

        boolean homeMode = maid.isHomeModeEnable();
        BlockPos homeCenter = maid.getRestrictCenter();
        int homeRadius = Math.max(4, (int) maid.getRestrictRadius());
        boolean ownerInHome = homeMode && maid.closerThan(owner, homeRadius);

        logDebug("[MaidMail][DeliveryService] Delivery attempt maidId={} homeMode={} ownerInHome={} homeCenter={}", 
                maid.getId(), homeMode, ownerInHome, homeCenter);

        if (!homeMode) {
            logDebug("[MaidMail][DeliveryService] Follow mode - attempting direct handover maidId={}", maid.getId());
            handToOwnerIfNear(maid, owner, parcel);
            return;
        }

        if (tryDeliverViaMailbox(maid, level, owner, parcel, homeCenter, homeRadius)) {
            logDebug("[MaidMail][DeliveryService] Mailbox delivery successful maidId={}", maid.getId());
            return;
        }

        if (ownerInHome) {
            logDebug("[MaidMail][DeliveryService] Mailbox delivery failed, attempting direct handover maidId={}", maid.getId());
            handToOwnerIfNear(maid, owner, parcel);
        } else {
            logDebug("[MaidMail][DeliveryService] Owner not in home, no delivery attempted maidId={}", maid.getId());
        }
    }

    private static boolean tryDeliverViaMailbox(EntityMaid maid, ServerLevel level, ServerPlayer owner,
                                                ItemStack parcel, BlockPos homeCenter, int homeRadius) {

        // 防御性检查：如果没有设置限制中心，跳过邮箱投递
        if (homeCenter == null) {
            LOGGER.warn("[MaidMail][Delivery] Home center is null, skipping mailbox delivery maidId={}", maid.getId());
            return false;
        }

        var mailboxOpt = MailboxSafetyEvaluator.getBestUsableMailbox(level, homeCenter, Math.min(Config.MAILBOX_SEARCH_RADIUS.get(), homeRadius));

        if (mailboxOpt.isPresent()) {
            BlockPos pos = mailboxOpt.get().pos();
            double distSqr = maid.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            logDebug("[MaidMail][DeliveryService] Found mailbox maidId={} pos={} distance={} safety={}", 
                    maid.getId(), pos, Math.sqrt(distSqr), mailboxOpt.get().safetyScore());
            
            if (distSqr <= 9) {
                logDebug("[MaidMail][DeliveryService] Attempting mailbox send maidId={} mailboxPos={}", 
                        maid.getId(), pos);
                boolean sent = sendViaPostbox(level, owner, parcel, pos, maid);
                if (sent) {
                    logDebug("[MaidMail][DeliveryService] Mailbox send successful maidId={}", maid.getId());
                    removeOneMarkedParcel(maid);
                    return true;
                } else {
                    logDebug("[MaidMail][DeliveryService] Mailbox send failed maidId={}", maid.getId());
                }
            } else {
                logDebug("[MaidMail][DeliveryService] Too far from mailbox maidId={} distance={}", 
                        maid.getId(), Math.sqrt(distSqr));
            }
        } else {
            logDebug("[MaidMail][DeliveryService] No usable mailbox found maidId={} searchRadius={}", 
                    maid.getId(), Math.min(Config.MAILBOX_SEARCH_RADIUS.get(), homeRadius));
        }
        return false;
    }

    private static void handToOwnerIfNear(EntityMaid maid, ServerPlayer owner, ItemStack parcel) {
        double ownerDistSqr = maid.distanceToSqr(owner);
        logDebug("[MaidMail][DeliveryService] Checking direct handover maidId={} ownerDistance={} threshold={}", 
                maid.getId(), Math.sqrt(ownerDistSqr), OWNER_HANDOVER_DISTANCE);
        
        if (ownerDistSqr <= (OWNER_HANDOVER_DISTANCE * OWNER_HANDOVER_DISTANCE)) {
            logDebug("[MaidMail][DeliveryService] Direct handover to owner maidId={}", maid.getId());
            String senderName = maid.getName().getString();
            ItemStack parcelCopy = parcel.copy();

            parcelCopy.set(ContactDataComponents.POSTCARD_SENDER.get(), senderName);

            ItemContainerContents contents = parcelCopy.get(DataComponents.CONTAINER);
            if (contents != null) {
                for (ItemStack item : contents.nonEmptyItems()) {
                    if (item.getItem() instanceof PostcardItem) {
                        item.set(ContactDataComponents.POSTCARD_SENDER.get(), senderName);
                    }
                }
            }
            
            ItemHandlerHelper.giveItemToPlayer(owner, parcelCopy);
            removeOneMarkedParcel(maid);
            logDebug("[MaidMail][DeliveryService] Direct handover completed maidId={}", maid.getId());
        } else {
            logDebug("[MaidMail][DeliveryService] Owner too far for direct handover maidId={}", maid.getId());
        }
    }

    private static boolean sendViaPostbox(ServerLevel level, ServerPlayer owner, ItemStack parcel,
                                          BlockPos postboxPos, EntityMaid maid) {
        GlobalPos from = GlobalPos.of(level.dimension(), postboxPos);
        GlobalPos to = MailboxDataManager.getData(level).getMailboxPos(owner.getUUID());

        ItemStack parcelCopy = parcel.copy();
        String senderName = maid.getName().getString();

        parcelCopy.set(ContactDataComponents.POSTCARD_SENDER.get(), senderName);

        if (IPackageItem.checkAndPostmarkPostcard(parcelCopy, senderName) ||
                parcelCopy.getItem() instanceof PostcardItem) {
        }

        if (to != null) {
            if (to.dimension() != level.dimension()) {
                parcelCopy.set(ContactDataComponents.ANOTHER_WORLD.get(), true);
            }
        } else {
            if (Level.OVERWORLD != level.dimension()) {
                parcelCopy.set(ContactDataComponents.ANOTHER_WORLD.get(), true);
            }
        }

        int ticks = (to != null) ? MailboxManager.getDeliveryTicks(from, to) : 0;
        MailToBeSent mailToBeSent = new MailToBeSent(owner.getUUID(), parcelCopy, ticks);
        MailboxDataManager.getData(level).getMailList().add(mailToBeSent);
        return true;
    }

    private static boolean hasLetter(EntityMaid maid) {
        return ItemsUtil.isStackIn(maid, stack -> {
            if (!(stack.getItem() instanceof IPackageItem)) return false;
            return Boolean.TRUE.equals(stack.get(TLMContactDataComponents.MAID_MAIL.get()));
        });
    }

    private static ItemStack getMarkedParcel(EntityMaid maid) {
        return ItemsUtil.getStack(maid, s -> {
            if (!(s.getItem() instanceof IPackageItem)) return false;
            return Boolean.TRUE.equals(s.get(TLMContactDataComponents.MAID_MAIL.get()));
        });
    }

    private static void removeOneMarkedParcel(EntityMaid maid) {
        ItemStack existing = getMarkedParcel(maid);
        if (!existing.isEmpty()) {
            existing.shrink(1);
        }
    }
}
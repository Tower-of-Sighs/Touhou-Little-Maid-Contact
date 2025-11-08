package com.sighs.touhou_little_maid_epistalove.entity.ai.behavior;

import com.flechazo.contact.common.component.ContactDataComponents;
import com.flechazo.contact.common.handler.MailboxManager;
import com.flechazo.contact.common.item.IPackageItem;
import com.flechazo.contact.common.item.PostcardItem;
import com.flechazo.contact.common.storage.MailToBeSent;
import com.flechazo.contact.common.storage.MailboxDataManager;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.util.ItemsUtil;
import com.mojang.logging.LogUtils;
import com.sighs.touhou_little_maid_epistalove.component.TLMContactDataComponents;
import com.sighs.touhou_little_maid_epistalove.config.Config;
import com.sighs.touhou_little_maid_epistalove.util.MailboxSafetyEvaluator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.slf4j.Logger;

public final class LetterDeliveryService {
    private static final Logger LOGGER = LogUtils.getLogger();
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

        if (!homeMode) {
            handToOwnerIfNear(maid, owner, parcel);
            return;
        }

        if (tryDeliverViaMailbox(maid, level, owner, parcel, homeCenter, homeRadius)) {
            return;
        }

        if (ownerInHome) {
            handToOwnerIfNear(maid, owner, parcel);
        }
    }

    private static boolean tryDeliverViaMailbox(EntityMaid maid, ServerLevel level, ServerPlayer owner,
                                                ItemStack parcel, BlockPos homeCenter, int homeRadius) {

        var mailboxOpt = MailboxSafetyEvaluator.getBestUsableMailbox(level, homeCenter, Math.min(Config.MAILBOX_SEARCH_RADIUS.get(), homeRadius));

        if (mailboxOpt.isPresent()) {
            BlockPos pos = mailboxOpt.get().pos();
            double distSqr = maid.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            if (distSqr <= 9) {
                boolean sent = sendViaPostbox(level, owner, parcel, pos, maid);
                if (sent) {
                    removeOneMarkedParcel(maid);
                    return true;
                }
            }
        }
        return false;
    }

    private static void handToOwnerIfNear(EntityMaid maid, ServerPlayer owner, ItemStack parcel) {
        double ownerDistSqr = maid.distanceToSqr(owner);
        if (ownerDistSqr <= (OWNER_HANDOVER_DISTANCE * OWNER_HANDOVER_DISTANCE)) {
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
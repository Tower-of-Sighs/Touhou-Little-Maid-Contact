package com.sighs.touhou_little_maid_epistalove.entity.ai.behavior;

import cn.sh1rocu.touhoulittlemaid.util.itemhandler.ItemHandlerHelper;
import com.flechazo.contact.common.handler.MailboxManager;
import com.flechazo.contact.common.item.IPackageItem;
import com.flechazo.contact.common.item.PostcardItem;
import com.flechazo.contact.common.storage.MailToBeSent;
import com.flechazo.contact.fabric.capability.FabricMailboxDataProvider;
import com.flechazo.contact.fabric.capability.MailboxDataComponent;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.util.ItemsUtil;
import com.mojang.logging.LogUtils;
import com.sighs.touhou_little_maid_epistalove.config.ModConfig;
import com.sighs.touhou_little_maid_epistalove.util.MailboxSafetyEvaluator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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

        var mailboxOpt = MailboxSafetyEvaluator.getBestUsableMailbox(level, homeCenter, Math.min(ModConfig.get().mailDelivery.mailboxSearchRadius, homeRadius));

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

    private static boolean sendViaPostbox(ServerLevel level, ServerPlayer owner, ItemStack parcel,
                                          BlockPos postboxPos, EntityMaid maid) {
        MailboxDataComponent component = MailboxDataComponent.KEY.get(level);
        FabricMailboxDataProvider dataProvider = new FabricMailboxDataProvider(component.getData());

        GlobalPos from = GlobalPos.of(level.dimension(), postboxPos);
        GlobalPos to = dataProvider.getMailboxPos(owner.getUUID());

        ItemStack parcelCopy = parcel.copy();
        CompoundTag tag = parcelCopy.getOrCreateTag();

        String senderName = maid.getName().getString();
        tag.putString("Sender", senderName);

        if (IPackageItem.checkAndPostmarkPostcard(parcelCopy, senderName)
                || parcelCopy.getItem() instanceof PostcardItem) {
        }

        if (to != null) {
            if (!to.dimension().equals(level.dimension())) {
                tag.putBoolean("AnotherWorld", true);
            }
        } else {
            if (!Level.OVERWORLD.equals(level.dimension())) {
                tag.putBoolean("AnotherWorld", true);
            }
        }

        int ticks = (to != null) ? MailboxManager.getDeliveryTicks(from, to) : 0;
        dataProvider.getMailList().add(new MailToBeSent(owner.getUUID(), parcelCopy, ticks));

        MailboxDataComponent.KEY.sync(level);

        return true;
    }


    private static boolean hasLetter(EntityMaid maid) {
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
}
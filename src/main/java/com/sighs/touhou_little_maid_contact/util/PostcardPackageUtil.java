package com.sighs.touhou_little_maid_contact.util;

import com.flechazo.contact.common.item.IPackageItem;
import com.flechazo.contact.resourse.PostcardDataManager;
import com.github.tartaricacid.touhoulittlemaid.util.ItemsUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class PostcardPackageUtil {

    private PostcardPackageUtil() {
    }

    public static ItemStack buildPackageWithPostcard(ResourceLocation packageId, String postcardText, ResourceLocation postcardId, String senderName) {
        String withSignature = (senderName != null && !senderName.isBlank())
                ? appendSignatureDynamic(postcardText, senderName, postcardId)
                : postcardText;
        return buildPackageWithPostcard(packageId, withSignature, postcardId);
    }

    private static String appendSignatureDynamic(String originalText, String senderName, ResourceLocation postcardId) {
        int lineWidth = resolveTextBoxWidth(postcardId);
        int nameWidth = estimateStringWidthPx(senderName);
        int spaceWidth = 4;
        int spaces = Math.max(0, (lineWidth - nameWidth) / spaceWidth);

        return originalText + "\n" + " ".repeat(spaces) + senderName;
    }

    private static int resolveTextBoxWidth(ResourceLocation postcardId) {
        return PostcardDataManager.getPostcard(postcardId).textWidth();
    }

    private static int estimateStringWidthPx(String s) {
        int w = 0;
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            w += estimateCharWidthPx(cp);
            i += Character.charCount(cp);
        }
        return w;
    }

    private static int estimateCharWidthPx(int cp) {
        if (cp == '§') return 0;
        if (cp == ' ') return 4;

        Character.UnicodeBlock b = Character.UnicodeBlock.of(cp);
        if (b == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || b == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || b == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || b == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || b == Character.UnicodeBlock.HANGUL_SYLLABLES
                || b == Character.UnicodeBlock.KATAKANA
                || b == Character.UnicodeBlock.HIRAGANA) {
            return 10;
        }

        if (cp >= '0' && cp <= '9') return 6;
        if (cp >= 'A' && cp <= 'Z') return (cp == 'M' || cp == 'W') ? 9 : 7;
        if (cp >= 'a' && cp <= 'z') {
            if (cp == 'i' || cp == 'l') return 4;
            if (cp == 'm' || cp == 'w') return 8;
            return 6;
        }

        if (".,;:!|/\\'\"`^~".indexOf(cp) >= 0) return 4;
        if (cp == '-' || cp == '_') return 6;

        return 6;
    }

    public static ItemStack buildPackageWithPostcard(ResourceLocation packageId, String postcardText, ResourceLocation postcardId) {
        ItemStack packageStack = ItemsUtil.getItemStack(packageId.toString()).copy();
        CompoundTag tag = packageStack.getOrCreateTag();
        tag.putBoolean("MaidMail", true);

        ListTag parcel = new ListTag();
        CompoundTag entry = new CompoundTag();
        entry.putString("id", "contact:postcard");
        entry.putByte("Count", (byte) 1);

        CompoundTag postcardNbt = new CompoundTag();
        postcardNbt.putString("Text", postcardText);
        postcardNbt.putString("CardID", postcardId.toString());

        entry.put("tag", postcardNbt);
        parcel.add(entry);
        tag.put("parcel", parcel);

        return packageStack;
    }

    public static ResourceLocation choosePackageId(String parcelIdStr) {
        if (parcelIdStr != null && !parcelIdStr.isBlank()) {
            try {
                ResourceLocation candidate = new ResourceLocation(parcelIdStr);
                Item item = ForgeRegistries.ITEMS.getValue(candidate);
                if (item instanceof IPackageItem) {
                    return candidate;
                }
            } catch (Exception ignored) {
            }
        }

        List<ResourceLocation> packageIds = getAllPackageItemIds();
        ResourceLocation chosen = packageIds.isEmpty()
                ? new ResourceLocation("contact", "letter")
                : packageIds.get(ThreadLocalRandom.current().nextInt(packageIds.size()));
        return chosen;
    }

    public static ResourceLocation choosePostcardId(String postcardIdStr) {
        if (postcardIdStr != null && !postcardIdStr.isBlank()) {
            try {
                ResourceLocation candidate = new ResourceLocation(postcardIdStr);
                if (getAllPostcardIds().contains(candidate)) {
                    return candidate;
                }
            } catch (Exception ignored) {
            }
        }

        List<ResourceLocation> postcardIds = getAllPostcardIds();
        return postcardIds.isEmpty()
                ? new ResourceLocation("contact", "default")
                : postcardIds.get(ThreadLocalRandom.current().nextInt(postcardIds.size()));
    }

    public static List<ResourceLocation> getAllPackageItemIds() {
        List<ResourceLocation> ids = new ArrayList<>();
        for (var item : ForgeRegistries.ITEMS) {
            if (item instanceof IPackageItem) {
                ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
                if (id != null) {
                    ids.add(id);
                }
            }
        }
        return ids;
    }

    public static List<ResourceLocation> getAllPostcardIds() {
        return new ArrayList<>(PostcardDataManager.getPostcardIds());
    }
}
package com.sighs.touhou_little_maid_epistalove.util;

import com.flechazo.contact.Contact;
import com.flechazo.contact.common.component.ContactDataComponents;
import com.flechazo.contact.common.item.IPackageItem;
import com.flechazo.contact.data.PostcardDataManager;
import com.github.tartaricacid.touhoulittlemaid.util.ItemsUtil;
import com.sighs.touhou_little_maid_epistalove.component.TLMContactDataComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

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

        packageStack.set(TLMContactDataComponents.MAID_MAIL.get(), true);

        ItemStack postcardStack = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("contact:postcard")));
        postcardStack.set(ContactDataComponents.POSTCARD_TEXT.get(), postcardText);
        postcardStack.set(ContactDataComponents.POSTCARD_STYLE_ID.get(), postcardId);

        List<ItemStack> containerItems = new ArrayList<>();
        containerItems.add(postcardStack);
        ItemContainerContents containerContents = ItemContainerContents.fromItems(containerItems);
        packageStack.set(DataComponents.CONTAINER, containerContents);

        return packageStack;
    }

    public static ResourceLocation choosePackageId(String parcelIdStr) {
        if (parcelIdStr != null && !parcelIdStr.isBlank()) {
            try {
                ResourceLocation candidate = ResourceLocation.parse(parcelIdStr);
                Item item = BuiltInRegistries.ITEM.get(candidate);
                if (item instanceof IPackageItem) {
                    return candidate;
                }
            } catch (Exception ignored) {
            }
        }

        List<ResourceLocation> packageIds = getAllPackageItemIds();
        return packageIds.isEmpty()
                ? ResourceLocation.fromNamespaceAndPath(Contact.MOD_ID, "letter")
                : packageIds.get(ThreadLocalRandom.current().nextInt(packageIds.size()));
    }

    public static ResourceLocation choosePostcardId(String postcardIdStr) {
        if (postcardIdStr != null && !postcardIdStr.isBlank()) {
            try {
                ResourceLocation candidate = ResourceLocation.parse(postcardIdStr);
                if (getAllPostcardIds().contains(candidate)) {
                    return candidate;
                }
            } catch (Exception ignored) {
            }
        }

        List<ResourceLocation> postcardIds = getAllPostcardIds();
        return postcardIds.isEmpty()
                ? ResourceLocation.fromNamespaceAndPath(Contact.MOD_ID, "default")
                : postcardIds.get(ThreadLocalRandom.current().nextInt(postcardIds.size()));
    }

    public static List<ResourceLocation> getAllPackageItemIds() {
        List<ResourceLocation> ids = new ArrayList<>();
        for (var item : BuiltInRegistries.ITEM) {
            if (item instanceof IPackageItem) {
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                ids.add(id);
            }
        }
        return ids;
    }

    public static List<ResourceLocation> getAllPostcardIds() {
        return new ArrayList<>(PostcardDataManager.getPostcardIds());
    }
}
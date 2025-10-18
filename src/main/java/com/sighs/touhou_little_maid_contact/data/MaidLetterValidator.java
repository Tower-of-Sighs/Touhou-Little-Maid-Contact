package com.sighs.touhou_little_maid_contact.data;

import com.flechazo.contact.common.item.IPackageItem;
import com.flechazo.contact.resourse.PostcardDataManager;
import com.mafuyu404.oelib.api.data.DataValidator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

public class MaidLetterValidator implements DataValidator<MaidLetterRule> {
    @Override
    public DataValidator.ValidationResult validate(MaidLetterRule rule, ResourceLocation source) {
        if (rule.id() == null || rule.id().isBlank()) {
            return ValidationResult.failure("id is empty");
        }
        if (rule.triggers() == null || rule.triggers().isEmpty()) {
            return ValidationResult.failure("triggers are empty");
        }

        if (rule.type() == MaidLetterRule.Type.PRESET) {
            if (rule.preset().isEmpty()) {
                return ValidationResult.failure("preset block missing when type = preset");
            }
            MaidLetterRule.Preset p = rule.preset().get();
            if (p.gifts() == null || p.gifts().isEmpty()) {
                return ValidationResult.failure("gifts must contain exactly one entry");
            }
            if (p.gifts().size() != 1) {
                return ValidationResult.failure("gifts must be size 1");
            }
            MaidLetterRule.Gift gift = p.gifts().get(0);
            Item item = ForgeRegistries.ITEMS.getValue(gift.parcel());
            if (item == null) {
                return ValidationResult.failure("Parcel item not found: " + gift.parcel());
            }
            if (!(item instanceof IPackageItem)) {
                return ValidationResult.failure("Parcel item must implement IPackageItem: " + gift.parcel());
            }
            if (!PostcardDataManager.getPostcards().containsKey(gift.postcard())) {
                return ValidationResult.failure("Postcard id not exists: " + gift.postcard());
            }
        } else {
            if (rule.ai().isEmpty()) {
                return ValidationResult.failure("ai block missing when type = ai");
            }
            if (rule.ai().get().prompt() == null || rule.ai().get().prompt().isBlank()) {
                return ValidationResult.failure("ai.prompt is empty");
            }
        }

        for (ResourceLocation rl : rule.triggers()) {
            if (rl.getNamespace().isBlank() || rl.getPath().isBlank()) {
                return ValidationResult.failure("Invalid trigger id: " + rl);
            }
        }
        return ValidationResult.success();
    }
}
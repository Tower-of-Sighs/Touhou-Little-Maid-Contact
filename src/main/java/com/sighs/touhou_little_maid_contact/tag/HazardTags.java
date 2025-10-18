package com.sighs.touhou_little_maid_contact.tag;

import com.sighs.touhou_little_maid_contact.TLMContact;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

public final class HazardTags {
    public static final TagKey<Block> HAZARDOUS_BLOCKS =
            TagKey.create(Registries.BLOCK, new ResourceLocation(TLMContact.MODID, "hazardous_blocks"));
    public static final TagKey<Fluid> HAZARDOUS_FLUIDS =
            TagKey.create(Registries.FLUID, new ResourceLocation(TLMContact.MODID, "hazardous_fluids"));

    private HazardTags() {
    }
}
package com.sighs.touhou_little_maid_contact.component;

import com.mojang.serialization.Codec;
import com.sighs.touhou_little_maid_contact.TLMContact;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;

public class TLMContactDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(TLMContact.MODID, Registries.DATA_COMPONENT_TYPE);

    public static final RegistrySupplier<DataComponentType<Boolean>> MAID_MAIL = DATA_COMPONENTS.register(
            "maid_mail",
            () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build()
    );
}
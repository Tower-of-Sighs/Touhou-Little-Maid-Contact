package com.sighs.touhou_little_maid_epistalove.data;

import com.mafuyu404.oelib.api.data.DataDriven;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sighs.touhou_little_maid_epistalove.TLMEpistalove;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

@DataDriven(
        modid = TLMEpistalove.MODID,
        folder = "maid_letters",
        syncToClient = true,
        validator = MaidLetterValidator.class,
        supportArray = true
)
public record MaidLetterRule(
        Type type,
        String id,
        List<ResourceLocation> triggers,
        TriggerType triggerType,
        int minAffection,
        Optional<Integer> maxAffection,
        Optional<Integer> cooldown,
        Optional<Integer> favorabilityChange,
        Optional<Integer> favorabilityThreshold,
        Optional<Preset> preset,
        Optional<AI> ai,
        Optional<List<ResourceLocation>> maidIds
) {
    public enum Type {
        PRESET, AI
    }

    public enum TriggerType {
        ONCE,       // 一次性触发，使用后会被消费
        PERSISTENT  // 持久性触发，一直有效
    }

    public static final Codec<MaidLetterRule> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("type")
                            .xmap(s -> Type.valueOf(s.toUpperCase()), t -> t.name().toLowerCase())
                            .forGetter(MaidLetterRule::type),
                    Codec.STRING.fieldOf("id").forGetter(MaidLetterRule::id),
                    Codec.STRING.listOf().fieldOf("triggers")
                            .xmap(list -> list.stream().map(ResourceLocation::new).toList(),
                                    rl -> rl.stream().map(ResourceLocation::toString).toList())
                            .forGetter(MaidLetterRule::triggers),
                    Codec.STRING.optionalFieldOf("trigger_type")
                            .xmap(opt -> opt.map(s -> TriggerType.valueOf(s.toUpperCase())).orElse(TriggerType.PERSISTENT),
                                    t -> Optional.of(t.name().toLowerCase()))
                            .forGetter(MaidLetterRule::triggerType),
                    Codec.INT.optionalFieldOf("min_affection")
                            .xmap(opt -> opt.orElse(0), Optional::of).forGetter(MaidLetterRule::minAffection),
                    Codec.INT.optionalFieldOf("max_affection")
                            .forGetter(MaidLetterRule::maxAffection),
                    Codec.INT.optionalFieldOf("cooldown")
                            .forGetter(MaidLetterRule::cooldown),
                    Codec.INT.optionalFieldOf("favorability_change")
                            .forGetter(MaidLetterRule::favorabilityChange),
                    Codec.INT.optionalFieldOf("favorability_threshold")
                            .forGetter(MaidLetterRule::favorabilityThreshold),
                    Preset.CODEC.optionalFieldOf("preset").forGetter(MaidLetterRule::preset),
                    AI.CODEC.optionalFieldOf("ai").forGetter(MaidLetterRule::ai),
                    Codec.STRING.listOf()
                            .optionalFieldOf("maid_id")
                            .xmap(
                                    opt -> opt.map(list -> list.stream().map(ResourceLocation::new).toList()),
                                    opt -> opt.map(list -> list.stream().map(ResourceLocation::toString).toList())
                            )
                            .forGetter(MaidLetterRule::maidIds)
            ).apply(instance, MaidLetterRule::new)
    );

    public record Preset(String title, String message, List<Gift> gifts) {
        public static final Codec<Preset> CODEC = RecordCodecBuilder.create(i ->
                i.group(
                        Codec.STRING.fieldOf("title").forGetter(Preset::title),
                        Codec.STRING.fieldOf("message").forGetter(Preset::message),
                        Gift.CODEC.listOf().fieldOf("gifts").forGetter(Preset::gifts)
                ).apply(i, Preset::new));
    }

    public record AI(String prompt, Optional<String> tone) {
        public static final Codec<AI> CODEC = RecordCodecBuilder.create(i ->
                i.group(
                        Codec.STRING.fieldOf("prompt").forGetter(AI::prompt),
                        Codec.STRING.optionalFieldOf("tone").forGetter(AI::tone)
                ).apply(i, AI::new));
    }

    public record Gift(ResourceLocation parcel, ResourceLocation postcard) {
        public static final Codec<Gift> CODEC = RecordCodecBuilder.create(i ->
                i.group(
                        Codec.STRING.fieldOf("parcel")
                                .xmap(ResourceLocation::new, ResourceLocation::toString)
                                .forGetter(Gift::parcel),
                        Codec.STRING.fieldOf("postcard")
                                .xmap(s -> s.contains(":") ? new ResourceLocation(s) : new ResourceLocation("contact", s),
                                        ResourceLocation::toString)
                                .forGetter(Gift::postcard)
                ).apply(i, Gift::new)
        );
    }
}
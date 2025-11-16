package com.sighs.touhou_little_maid_epistalove.ai.function;

import com.github.tartaricacid.touhoulittlemaid.ai.service.function.IFunctionCall;
import com.github.tartaricacid.touhoulittlemaid.ai.service.function.response.ToolResponse;
import com.github.tartaricacid.touhoulittlemaid.ai.service.function.schema.parameter.ObjectParameter;
import com.github.tartaricacid.touhoulittlemaid.ai.service.function.schema.parameter.Parameter;
import com.github.tartaricacid.touhoulittlemaid.ai.service.function.schema.parameter.StringParameter;
import com.github.tartaricacid.touhoulittlemaid.entity.favorability.FavorabilityManager;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.util.ItemsUtil;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.sighs.touhou_little_maid_epistalove.ai.generator.AILetterGenerator;
import com.sighs.touhou_little_maid_epistalove.ai.prompt.EnhancedPromptBuilder;
import com.sighs.touhou_little_maid_epistalove.ai.parser.JsonLetterParser;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public class WriteLetterFunction implements IFunctionCall<WriteLetterFunction.Args> {
    private static final String FUNCTION_ID = "write_letter";
    private static final String FUNCTION_DESC = """
            Compose and deliver a heartfelt letter to the owner.
            Use this when the player asks the maid to write a letter,
            or the maid wants to send one proactively.
            """;

    @Override
    public String getId() {
        return FUNCTION_ID;
    }

    @Override
    public String getDescription(EntityMaid maid) {
        return FUNCTION_DESC + "\n"
                + "Parameters:\n"
                + "- prompt: required\n"
                + "- tone: optional, omit to let the maid choose a suitable style based on dialogue\n"
                + "- favorability_change: optional";
    }

    @Override
    public Parameter addParameters(ObjectParameter root, EntityMaid maid) {
        Parameter prompt = StringParameter.create()
                .setDescription("prompt (string, required): The content guidance for the letter");
        Parameter tone = StringParameter.create()
                .setDescription("tone (string, optional): Style tone, e.g. sweet, lonesome, elegant")
                .addEnumValues("sweet", "lonesome", "elegant", "gentle", "playful");
        Parameter favorabilityChange = StringParameter.create()
                .setDescription("favorability_change (integer, optional): Positive to increase, negative to decrease");

        root.addProperties("prompt", prompt, true);
        root.addProperties("tone", tone, false);
        root.addProperties("favorability_change", favorabilityChange, false);
        return root;
    }

    private static int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static Integer parseEither(Either<Integer, String> either) {
        return either.map(i -> i, WriteLetterFunction::parseIntSafe);
    }

    private static Either<Integer, String> intToEither(Integer i) {
        return Either.left(i);
    }

    @Override
    public Codec<Args> codec() {
        MapCodec<Optional<Integer>> deltaCodec = Codec.either(Codec.INT, Codec.STRING)
                .optionalFieldOf("favorability_change")
                .xmap(
                        optEither -> optEither.map(WriteLetterFunction::parseEither),
                        optInt -> optInt.map(WriteLetterFunction::intToEither)
                );
        return RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.STRING.fieldOf("prompt").forGetter(Args::prompt),
                        Codec.STRING.optionalFieldOf("tone").forGetter(Args::tone),
                        deltaCodec.forGetter(Args::favorabilityChange)
                ).apply(instance, Args::new));
    }

    @Override
    public ToolResponse onToolCall(Args args, EntityMaid maid) {
        ServerPlayer owner = (ServerPlayer) maid.getOwner();
        if (owner == null) {
            return new ToolResponse("Failed to write letter: owner not found");
        }

        String tone = args.tone().orElse(null);
        EnhancedPromptBuilder builder = new EnhancedPromptBuilder();
        JsonLetterParser parser = new JsonLetterParser(builder);
        AILetterGenerator generator = new AILetterGenerator(tone, args.prompt(), builder, parser);

        generator.generate(owner, maid, result -> {
            if (!result.isEmpty()) {
                ItemsUtil.giveItemToMaid(maid, result);
                args.favorabilityChange().ifPresent(delta -> {
                    if (delta == 0) return;
                    FavorabilityManager fm = maid.getFavorabilityManager();
                    if (delta > 0) {
                        fm.add(delta);
                    } else {
                        fm.reduceWithoutLevel(-delta);
                    }
                });
            }
        });

        return new ToolResponse("Writing and delivering a letter...");
    }

    public record Args(String prompt, Optional<String> tone, Optional<Integer> favorabilityChange) {
    }
}
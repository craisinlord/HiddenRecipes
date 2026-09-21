package com.craisinlord.hiddenrecipes.hint;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;

/**
 * What a player sees when they press JEI's recipe-lookup key (default R) on an item that
 * only has a hidden recipe. {@code progress} controls whether a multi-condition unlock
 * (e.g. an {@code and} of several requirements) reports how many of its conditions are
 * already met, or stays fully cryptic until unlocked.
 */
public record HintDefinition(String translationKey, boolean showProgress) {

    public static final Codec<HintDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("translation_key").forGetter(HintDefinition::translationKey),
        Codec.BOOL.optionalFieldOf("show_progress", false).forGetter(HintDefinition::showProgress)
    ).apply(instance, HintDefinition::new));

    public Component text() {
        return Component.translatable(translationKey);
    }
}

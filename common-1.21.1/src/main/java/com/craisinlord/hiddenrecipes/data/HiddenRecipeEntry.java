package com.craisinlord.hiddenrecipes.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.craisinlord.hiddenrecipes.condition.HiddenRecipeCondition;
import com.craisinlord.hiddenrecipes.condition.HiddenRecipeConditions;
import com.craisinlord.hiddenrecipes.hint.HintDefinition;
import net.minecraft.resources.ResourceLocation;

/**
 * One entry from {@code data/<namespace>/hidden_recipes/*.json}. Points at an existing
 * recipe id (defined completely normally elsewhere in {@code data/<namespace>/recipe/})
 * and layers an unlock condition and a hint on top of it.
 */
public record HiddenRecipeEntry(ResourceLocation recipe, HiddenRecipeCondition condition, HintDefinition hint) {

    public static final Codec<HiddenRecipeEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ResourceLocation.CODEC.fieldOf("recipe").forGetter(HiddenRecipeEntry::recipe),
        HiddenRecipeConditions.CODEC.fieldOf("condition").forGetter(HiddenRecipeEntry::condition),
        HintDefinition.CODEC.fieldOf("hint").forGetter(HiddenRecipeEntry::hint)
    ).apply(instance, HiddenRecipeEntry::new));
}

package com.craisinlord.hiddenrecipes.condition;

import com.mojang.serialization.MapCodec;

public record HiddenRecipeConditionType<T extends HiddenRecipeCondition>(MapCodec<T> codec) {
}

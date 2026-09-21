package com.craisinlord.hiddenrecipes.condition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.craisinlord.hiddenrecipes.Constants;
import com.craisinlord.hiddenrecipes.condition.impl.AndCondition;
import com.craisinlord.hiddenrecipes.condition.impl.HasAdvancementCondition;
import com.craisinlord.hiddenrecipes.condition.impl.HasItemCondition;
import com.craisinlord.hiddenrecipes.condition.impl.NotCondition;
import com.craisinlord.hiddenrecipes.condition.impl.OrCondition;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registry of {@link HiddenRecipeConditionType}s. New condition kinds are added here by
 * calling {@link #register(String, HiddenRecipeConditionType)} — no changes to the data
 * loader or manager are needed, they only ever see the dispatch codec below.
 */
public final class HiddenRecipeConditions {

    private static final Map<ResourceLocation, HiddenRecipeConditionType<?>> BY_ID = new LinkedHashMap<>();
    private static final Map<HiddenRecipeConditionType<?>, ResourceLocation> BY_TYPE = new LinkedHashMap<>();

    private static final Codec<ResourceLocation> VALIDATED_ID_CODEC = ResourceLocation.CODEC.validate(
        id -> BY_ID.containsKey(id)
            ? DataResult.success(id)
            : DataResult.error(() -> "Unknown hidden recipe condition type: " + id)
    );

    /**
     * Declared before the condition types because composite type codecs refer back to this
     * dispatch codec while they are being initialized.
     */
    public static final Codec<HiddenRecipeCondition> CODEC = VALIDATED_ID_CODEC.dispatch(
        "type",
        HiddenRecipeConditions::idOf,
        HiddenRecipeConditions::codecFor
    );

    public static final HiddenRecipeConditionType<HasItemCondition> HAS_ITEM =
        register("has_item", new HiddenRecipeConditionType<>(HasItemCondition.CODEC));
    public static final HiddenRecipeConditionType<HasAdvancementCondition> HAS_ADVANCEMENT =
        register("has_advancement", new HiddenRecipeConditionType<>(HasAdvancementCondition.CODEC));
    public static final HiddenRecipeConditionType<AndCondition> AND =
        register("and", new HiddenRecipeConditionType<>(AndCondition.CODEC));
    public static final HiddenRecipeConditionType<OrCondition> OR =
        register("or", new HiddenRecipeConditionType<>(OrCondition.CODEC));
    public static final HiddenRecipeConditionType<NotCondition> NOT =
        register("not", new HiddenRecipeConditionType<>(NotCondition.CODEC));

    private static <T extends HiddenRecipeCondition> HiddenRecipeConditionType<T> register(
        String path, HiddenRecipeConditionType<T> type) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, path);
        BY_ID.put(id, type);
        BY_TYPE.put(type, id);
        return type;
    }

    private static ResourceLocation idOf(HiddenRecipeCondition condition) {
        ResourceLocation id = BY_TYPE.get(condition.type());
        if (id == null) {
            throw new IllegalStateException("Unregistered hidden recipe condition type: " + condition.type());
        }
        return id;
    }

    private static MapCodec<? extends HiddenRecipeCondition> codecFor(ResourceLocation id) {
        // Safe: VALIDATED_ID_CODEC already rejected unknown ids before this runs.
        return BY_ID.get(id).codec();
    }

    public static DataResult<HiddenRecipeConditionType<?>> lookup(ResourceLocation id) {
        HiddenRecipeConditionType<?> type = BY_ID.get(id);
        return type == null
            ? DataResult.error(() -> "Unknown hidden recipe condition type: " + id)
            : DataResult.success(type);
    }

    private HiddenRecipeConditions() {
    }
}

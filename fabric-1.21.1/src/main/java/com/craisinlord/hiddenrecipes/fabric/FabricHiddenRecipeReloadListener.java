package com.craisinlord.hiddenrecipes.fabric;

import com.craisinlord.hiddenrecipes.Constants;
import com.craisinlord.hiddenrecipes.data.HiddenRecipeReloadListener;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;

/** Fabric wants reload listeners identifiable so other mods can order against it. */
public final class FabricHiddenRecipeReloadListener extends HiddenRecipeReloadListener
    implements IdentifiableResourceReloadListener {

    private static final ResourceLocation ID =
        ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "hidden_recipes");

    @Override
    public ResourceLocation getFabricId() {
        return ID;
    }
}

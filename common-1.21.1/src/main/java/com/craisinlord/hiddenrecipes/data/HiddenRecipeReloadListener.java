package com.craisinlord.hiddenrecipes.data;

import com.craisinlord.hiddenrecipes.Constants;
import com.craisinlord.hiddenrecipes.HiddenRecipeManager;
import com.google.gson.Gson;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.HashMap;
import java.util.Map;

/**
 * Loads every {@code data/<namespace>/hidden_recipes/*.json} on datapack (re)load, keyed
 * by the recipe id each entry targets. Reload-safe: {@link HiddenRecipeManager} swaps its
 * whole table atomically once parsing finishes, same as vanilla's recipe manager.
 *
 * Loader-agnostic on purpose: it only extends vanilla's own
 * {@code SimpleJsonResourceReloadListener}. Fabric additionally wants reload listeners to
 * implement its {@code IdentifiableResourceReloadListener}, which lives in fabric-api —
 * see {@code fabric-1.21.1}'s {@code FabricHiddenRecipeReloadListener} wrapper for that,
 * rather than pulling a loader API into common code.
 *
 * <p>Parses with a {@link RegistryOps} built from the built-in registries (items, in
 * particular), not plain {@link JsonOps}: {@code HasItemCondition} wraps vanilla's
 * {@code ItemPredicate}, whose {@code items} field decodes through a registry-aware
 * {@code HolderSet} codec that NPEs ({@code MapDecoder.decode(...) because "elementDecoder"
 * is null}) when parsed with an ops that isn't registry-aware. Built purely from
 * {@link BuiltInRegistries} (no live server/world needed), so this resolves item ids on
 * both loaders without pulling in a loader-specific registry-access hook. Known
 * limitation: item *tags* referenced via {@code "#namespace:path"} won't resolve, since
 * tags are datapack-driven and only live on a running server's dynamic registry access —
 * none of the current hidden-recipe conditions use tags, so this isn't blocking today.
 */
public class HiddenRecipeReloadListener extends SimpleJsonResourceReloadListener {

    private static final RegistryOps<com.google.gson.JsonElement> REGISTRY_OPS =
        RegistryOps.create(JsonOps.INSTANCE, RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY));

    public HiddenRecipeReloadListener() {
        super(new Gson(), "hidden_recipes");
    }

    @Override
    protected void apply(Map<ResourceLocation, com.google.gson.JsonElement> entries, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, HiddenRecipeEntry> parsed = new HashMap<>();
        entries.forEach((fileId, json) -> HiddenRecipeEntry.CODEC.parse(REGISTRY_OPS, json).resultOrPartial(
            error -> Constants.LOGGER.error("Failed to parse hidden recipe {}: {}", fileId, error)
        ).ifPresent(entry -> {
            if (parsed.put(entry.recipe(), entry) != null) {
                Constants.LOGGER.warn(
                    "Multiple hidden_recipes entries target recipe {} — last one loaded ({}) wins",
                    entry.recipe(), fileId);
            }
        }));

        Constants.LOGGER.info("Loaded {} hidden recipe definitions", parsed.size());
        HiddenRecipeManager.INSTANCE.reload(parsed);
    }
}

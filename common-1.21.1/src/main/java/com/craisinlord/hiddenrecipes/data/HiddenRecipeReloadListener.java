package com.craisinlord.hiddenrecipes.data;

import com.craisinlord.hiddenrecipes.Constants;
import com.craisinlord.hiddenrecipes.HiddenRecipeManager;
import com.google.gson.Gson;
import com.mojang.serialization.JsonOps;
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
 */
public class HiddenRecipeReloadListener extends SimpleJsonResourceReloadListener {

    public HiddenRecipeReloadListener() {
        super(new Gson(), "hidden_recipes");
    }

    @Override
    protected void apply(Map<ResourceLocation, com.google.gson.JsonElement> entries, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<ResourceLocation, HiddenRecipeEntry> parsed = new HashMap<>();
        entries.forEach((fileId, json) -> HiddenRecipeEntry.CODEC.parse(JsonOps.INSTANCE, json).resultOrPartial(
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

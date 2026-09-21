package com.craisinlord.hiddenrecipes.network;

import com.craisinlord.hiddenrecipes.hint.HintDefinition;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client-side cache of hidden-recipe hint definitions, kept in sync from
 * {@link HiddenRecipeDefinitionsSyncPayload}. Same design philosophy as
 * {@link ClientHiddenRecipeState}: a plain data holder with no client-only Minecraft classes,
 * safe to reference from common code on the logical server too.
 *
 * <p>This exists specifically so JEI's hint category has something to read on a remote
 * dedicated server, where {@code HiddenRecipeManager.INSTANCE} (the server-side singleton)
 * is never shared with the client. {@code HiddenRecipesJeiPlugin} prefers
 * {@code HiddenRecipeManager.INSTANCE.all()} when it's non-empty (covers
 * singleplayer/integrated-server play with zero behavior change, including live progress
 * display) and falls back to this cache otherwise.
 */
public final class ClientHiddenRecipeDefinitions {

    private static volatile Map<ResourceLocation, HintDefinition> hints = Map.of();

    public static void set(List<HiddenRecipeDefinitionsSyncPayload.Entry> entries) {
        Map<ResourceLocation, HintDefinition> next = new HashMap<>();
        for (HiddenRecipeDefinitionsSyncPayload.Entry entry : entries) {
            next.put(entry.recipe(), new HintDefinition(entry.translationKey(), entry.showProgress()));
        }
        hints = Map.copyOf(next);
    }

    public static void clear() {
        hints = Map.of();
    }

    public static Map<ResourceLocation, HintDefinition> all() {
        return hints;
    }

    private ClientHiddenRecipeDefinitions() {
    }
}

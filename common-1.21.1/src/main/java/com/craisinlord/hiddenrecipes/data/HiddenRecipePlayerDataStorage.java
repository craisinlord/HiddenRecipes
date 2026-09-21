package com.craisinlord.hiddenrecipes.data;

import com.craisinlord.hiddenrecipes.Constants;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Per-player unlocked-recipe persistence, one JSON file per player under
 * {@code <world>/hidden_recipes/<uuid>.json} — same shape as vanilla's own
 * {@code <world>/advancements/<uuid>.json}, just for our own data instead of piggybacking
 * on {@code PlayerAdvancements}. {@link LevelResource}'s constructor is private (only its
 * predefined constants are public), so the folder is resolved as a subdirectory of
 * {@link LevelResource#ROOT} instead of a custom LevelResource instance.
 */
public final class HiddenRecipePlayerDataStorage {

    private static final Gson GSON = new GsonBuilder().create();

    public static Set<ResourceLocation> load(MinecraftServer server, UUID playerId) {
        Path file = fileFor(server, playerId);
        if (!Files.exists(file)) {
            return new HashSet<>();
        }
        try (var reader = Files.newBufferedReader(file)) {
            JsonArray array = JsonParser.parseReader(reader).getAsJsonArray();
            Set<ResourceLocation> unlocked = new HashSet<>();
            for (var element : array) {
                unlocked.add(ResourceLocation.parse(element.getAsString()));
            }
            return unlocked;
        } catch (IOException | RuntimeException e) {
            Constants.LOGGER.error("Failed to read hidden recipe data for {}", playerId, e);
            return new HashSet<>();
        }
    }

    public static void save(MinecraftServer server, UUID playerId, Set<ResourceLocation> unlocked) {
        Path file = fileFor(server, playerId);
        try {
            Files.createDirectories(file.getParent());
            JsonArray array = new JsonArray();
            unlocked.forEach(id -> array.add(id.toString()));
            Files.writeString(file, GSON.toJson(array));
        } catch (IOException e) {
            Constants.LOGGER.error("Failed to write hidden recipe data for {}", playerId, e);
        }
    }

    private static Path fileFor(MinecraftServer server, UUID playerId) {
        return server.getWorldPath(LevelResource.ROOT).resolve("hidden_recipes").resolve(playerId + ".json");
    }

    private HiddenRecipePlayerDataStorage() {
    }
}

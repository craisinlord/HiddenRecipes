package com.craisinlord.hiddenrecipes.fabric;

import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import com.craisinlord.hiddenrecipes.Constants;
import com.craisinlord.hiddenrecipes.network.HiddenRecipeDefinitionsSyncPayload;
import com.craisinlord.hiddenrecipes.network.HiddenRecipesSyncPayload;
import com.craisinlord.hiddenrecipes.platform.PlatformHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

public class FabricPlatformHelper implements PlatformHelper {
    @Override
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public boolean isClient() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }

    @Override
    public boolean isServer() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER;
    }

    @Override
    public String getModVersion() {
        return FabricLoader.getInstance()
            .getModContainer(Constants.MOD_ID)
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse("unknown");
    }

    @Override
    public Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public void sendHiddenRecipesSync(ServerPlayer player, Collection<ResourceLocation> unlocked) {
        if (!ServerPlayNetworking.canSend(player, HiddenRecipesSyncPayload.TYPE)) {
            return;
        }
        ServerPlayNetworking.send(player, new HiddenRecipesSyncPayload(List.copyOf(unlocked)));
    }

    @Override
    public void sendHiddenRecipeDefinitionsSync(ServerPlayer player, Collection<HiddenRecipeDefinitionsSyncPayload.Entry> entries) {
        if (!ServerPlayNetworking.canSend(player, HiddenRecipeDefinitionsSyncPayload.TYPE)) {
            return;
        }
        ServerPlayNetworking.send(player, new HiddenRecipeDefinitionsSyncPayload(List.copyOf(entries)));
    }
}

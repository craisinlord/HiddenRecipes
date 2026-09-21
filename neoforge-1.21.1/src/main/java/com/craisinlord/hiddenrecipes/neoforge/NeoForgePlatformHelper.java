package com.craisinlord.hiddenrecipes.neoforge;

import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;
import com.craisinlord.hiddenrecipes.Constants;
import com.craisinlord.hiddenrecipes.network.HiddenRecipeDefinitionsSyncPayload;
import com.craisinlord.hiddenrecipes.network.HiddenRecipesSyncPayload;
import com.craisinlord.hiddenrecipes.platform.PlatformHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

public class NeoForgePlatformHelper implements PlatformHelper {
    @Override
    public String getPlatformName() {
        return "NeoForge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLEnvironment.production;
    }

    @Override
    public boolean isClient() {
        return FMLEnvironment.dist.isClient();
    }

    @Override
    public boolean isServer() {
        return FMLEnvironment.dist.isDedicatedServer();
    }

    @Override
    public String getModVersion() {
        return ModList.get().getModFileById(Constants.MOD_ID).versionString();
    }

    @Override
    public Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public void sendHiddenRecipesSync(ServerPlayer player, Collection<ResourceLocation> unlocked) {
        PacketDistributor.sendToPlayer(player, new HiddenRecipesSyncPayload(List.copyOf(unlocked)));
    }

    @Override
    public void sendHiddenRecipeDefinitionsSync(ServerPlayer player, Collection<HiddenRecipeDefinitionsSyncPayload.Entry> entries) {
        PacketDistributor.sendToPlayer(player, new HiddenRecipeDefinitionsSyncPayload(List.copyOf(entries)));
    }
}

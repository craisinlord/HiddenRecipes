package com.craisinlord.hiddenrecipes.platform;

import com.craisinlord.hiddenrecipes.network.HiddenRecipeDefinitionsSyncPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;
import java.util.Collection;

public interface PlatformHelper {

    static PlatformHelper getInstance() {
        return PlatformHelperImpl.INSTANCE;
    }

    String getPlatformName();

    boolean isModLoaded(String modId);

    boolean isDevelopmentEnvironment();

    boolean isClient();

    boolean isServer();

    String getModVersion();

    Path getConfigDir();

    /** Sends this player's full unlocked-hidden-recipes set to their client. */
    void sendHiddenRecipesSync(ServerPlayer player, Collection<ResourceLocation> unlocked);

    /**
     * Sends every currently-defined hidden recipe's id + hint to this player — see
     * {@link HiddenRecipeDefinitionsSyncPayload} for why this exists as a separate payload
     * from {@link #sendHiddenRecipesSync}.
     */
    void sendHiddenRecipeDefinitionsSync(ServerPlayer player, Collection<HiddenRecipeDefinitionsSyncPayload.Entry> entries);
}

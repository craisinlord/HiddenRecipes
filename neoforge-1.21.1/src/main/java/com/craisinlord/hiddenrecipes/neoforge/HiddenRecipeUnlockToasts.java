package com.craisinlord.hiddenrecipes.neoforge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;

/**
 * NeoForge counterpart to fabric-1.21.1's {@code HiddenRecipeUnlockToasts} — see that
 * class's javadoc for the full rationale. Kept as a near-duplicate per loader rather than shared in common code
 * because it needs client-only Minecraft classes and this project has no common "client"
 * source set (only the optional {@code src/jei}) — adding one would mean editing both
 * modules' build.gradle, which couldn't be verified to build without Gradle access either, so
 * duplication was the lower-risk choice this pass.
 */
final class HiddenRecipeUnlockToasts {

    static void show(Set<ResourceLocation> newlyUnlocked) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        for (ResourceLocation recipeId : newlyUnlocked) {
            Component title = Component.translatable("toast.hidden_recipes.unlocked.title");
            Component message = resolveDisplayName(minecraft, recipeId);
            SystemToast.add(minecraft.getToasts(), SystemToast.SystemToastId.PERIODIC_NOTIFICATION, title, message);
        }
    }

    private static Component resolveDisplayName(Minecraft minecraft, ResourceLocation recipeId) {
        return minecraft.level.getRecipeManager().byKey(recipeId)
            .map(holder -> holder.value().getResultItem(minecraft.level.registryAccess()).getHoverName())
            .orElseGet(() -> Component.literal(recipeId.toString()));
    }

    private HiddenRecipeUnlockToasts() {
    }
}

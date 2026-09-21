package com.craisinlord.hiddenrecipes.fabric;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Set;

/**
 * Player-facing "recipe unlocked" notification — the client-side counterpart to what used to
 * only log to the server console (see {@link com.craisinlord.hiddenrecipes.HiddenRecipeEvents}'s
 * {@code notify} method). Fires from
 * {@link com.craisinlord.hiddenrecipes.network.ClientHiddenRecipeState#addUnlockListener},
 * i.e. only for recipes newly added by a sync (never the initial post-connect snapshot).
 *
 * <p>Uses vanilla's generic {@link SystemToast} rather than the advancement/recipe-styled
 * toast classes, since {@code SystemToast.add(ToastComponent, SystemToast.SystemToastId,
 * Component, Component)} doesn't require an
 * {@code Advancement} or recipe-book-collection object we don't have.
 *
 * <p>The names below match the 1.21.1 Mojmap API: {@code SystemToast.SystemToastId},
 * {@code PERIODIC_NOTIFICATION}, and {@code Minecraft#getToasts()}.
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

    /** Falls back to the raw recipe id if the recipe can't be resolved (e.g. removed from a datapack since unlocking). */
    private static Component resolveDisplayName(Minecraft minecraft, ResourceLocation recipeId) {
        return minecraft.level.getRecipeManager().byKey(recipeId)
            .map(holder -> holder.value().getResultItem(minecraft.level.registryAccess()).getHoverName())
            .orElseGet(() -> Component.literal(recipeId.toString()));
    }

    private HiddenRecipeUnlockToasts() {
    }
}

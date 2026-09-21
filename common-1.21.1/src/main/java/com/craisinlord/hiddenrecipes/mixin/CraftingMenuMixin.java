package com.craisinlord.hiddenrecipes.mixin;

import com.craisinlord.hiddenrecipes.HiddenRecipeManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

/**
 * Blocks manual crafting of a still-hidden recipe. Vanilla's recipe-lock system (the one
 * {@code RecipeManager}/advancement rewards drive) only hides recipes from the recipe
 * *book* — a player who already knows the pattern can place items in the grid and craft it
 * by hand regardless of lock state. This is the actual gate for that.
 *
 * Targets {@code CraftingMenu.slotChangedCraftingGrid}, the static method both the 3x3
 * crafting-table grid ({@code CraftingMenu}) and the 2x2 player-inventory grid
 * ({@code InventoryMenu}) funnel through to compute the result slot — confirmed by reading
 * the decompiled 1.21.1 source (neoforge-21.1.199-sources.jar) rather than assumed, same
 * as {@code HasItemCondition}'s verification. Redirects the
 * {@code RecipeManager#getRecipeFor} call inside it: if the match found is a hidden recipe
 * the crafting player hasn't unlocked, pretend no recipe matched, same as if the pattern
 * were simply wrong.
 */
@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMixin {

    @Redirect(
        method = "slotChangedCraftingGrid",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/crafting/RecipeManager;getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/crafting/RecipeHolder;)Ljava/util/Optional;"
        )
    )
    private static Optional<RecipeHolder<CraftingRecipe>> hiddenrecipes$hideLockedRecipe(
        RecipeManager recipeManager,
        RecipeType<CraftingRecipe> type,
        RecipeInput input,
        Level level,
        RecipeHolder<CraftingRecipe> lastRecipe,
        // Trailing params: CraftingMenu#slotChangedCraftingGrid's own arguments, captured
        // in declaration order (Mixin resolves these against the enclosing method, not
        // the redirected call).
        AbstractContainerMenu menu,
        Level enclosingLevel,
        Player player,
        CraftingContainer craftSlots,
        ResultContainer resultSlots,
        RecipeHolder<CraftingRecipe> requestedRecipe
    ) {
        Optional<RecipeHolder<CraftingRecipe>> found = recipeManager.getRecipeFor(type, (CraftingInput) input, level, lastRecipe);
        if (found.isPresent() && player instanceof ServerPlayer serverPlayer) {
            ResourceLocation recipeId = found.get().id();
            if (HiddenRecipeManager.INSTANCE.isLocked(recipeId, serverPlayer)) {
                return Optional.empty();
            }
        }
        return found;
    }
}

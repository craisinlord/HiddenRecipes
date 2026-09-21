package com.craisinlord.hiddenrecipes.mixin;

/**
 * DISABLED — not a {@code @Mixin} anymore, not registered in either
 * {@code hidden_recipes.mixins.json} or {@code hidden_recipes-fabric.mixins.json}, and
 * therefore inert. Kept as an empty stub rather than deleted, per this project's standing
 * rule against removing files without explicit permission.
 *
 * <p>Scope decision: smithing-table enforcement was dropped. Hard-block enforcement is now
 * crafting table + furnace/blast furnace/smoker only ({@link CraftingMenuMixin},
 * {@link AbstractFurnaceBlockMixin}, {@link AbstractFurnaceBlockEntityMixin}). Smithing
 * recipes are still hidden from the recipe book and JEI (see
 * {@code HiddenRecipesJeiPlugin}) — they just join Create/Ars Nouveau/every other
 * non-hard-blocked recipe type in the "hidden but not enforced" tier described in
 * README.md's "Enforcement scope". This mixin was also, honestly, the least-verified piece
 * from the pass that wrote it (see git history / CHANGELOG "Unreleased (pass 2)" for the
 * verification-risk writeup) — dropping it removes that risk along with the feature.
 *
 * <p>If smithing enforcement is wanted again later, the previous implementation (an
 * {@code @Inject(method = "createResult", at = @At("TAIL"))} into {@code SmithingMenu} that
 * independently re-derived the matched recipe via {@code SmithingRecipeInput} and blanked
 * the result slot when locked) is recoverable from version control history rather than
 * rewritten from scratch.
 */
final class SmithingMenuMixin {

    private SmithingMenuMixin() {
    }
}

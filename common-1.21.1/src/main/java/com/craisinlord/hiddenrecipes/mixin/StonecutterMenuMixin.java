package com.craisinlord.hiddenrecipes.mixin;

/**
 * DISABLED — not a {@code @Mixin} anymore, not registered in either
 * {@code hidden_recipes.mixins.json} or {@code hidden_recipes-fabric.mixins.json}, and
 * therefore inert. Kept as an empty stub rather than deleted, per this project's standing
 * rule against removing files without explicit permission.
 *
 * <p>Scope decision: stonecutter enforcement was dropped, alongside {@code SmithingMenuMixin}
 * — see that class's javadoc for the full rationale (same scope decision, same reasoning).
 * Hard-block enforcement is now crafting table + furnace/blast furnace/smoker only. Stonecutter
 * recipes are still hidden from the recipe book and JEI, just not enforced against manual
 * crafting.
 *
 * <p>If stonecutter enforcement is wanted again later, the previous implementation (filtering
 * locked recipes out of {@code StonecutterMenu}'s candidate list in a
 * {@code slotsChanged(Container)} injection, plus a defense-in-depth guard on
 * {@code clickMenuButton}) is recoverable from version control history rather than rewritten
 * from scratch.
 */
final class StonecutterMenuMixin {

    private StonecutterMenuMixin() {
    }
}

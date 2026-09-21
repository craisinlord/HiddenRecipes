package com.craisinlord.hiddenrecipes.condition;

/**
 * What kind of player-state change a condition needs to be re-tested against — mirrors
 * vanilla's {@code CriterionTrigger} split (inventory-changed, advancement-granted, ...):
 * {@link com.craisinlord.hiddenrecipes.HiddenRecipeManager} indexes hidden-recipe entries
 * by the trigger types their condition tree declares, so an event only re-tests the
 * entries that could plausibly care about it instead of every hidden recipe for every
 * player.
 */
public enum HiddenRecipeTriggerType {
    /** Player's inventory contents changed (item picked up, moved, crafted, etc). */
    INVENTORY,
    /** Player was granted an advancement. */
    ADVANCEMENT,
    /** No precise hook exists yet for this condition — falls back to slow periodic polling. */
    GENERIC
}

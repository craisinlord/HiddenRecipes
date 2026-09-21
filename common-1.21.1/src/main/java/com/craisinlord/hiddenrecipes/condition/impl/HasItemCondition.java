package com.craisinlord.hiddenrecipes.condition.impl;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.craisinlord.hiddenrecipes.condition.HiddenRecipeCondition;
import com.craisinlord.hiddenrecipes.condition.HiddenRecipeConditionType;
import com.craisinlord.hiddenrecipes.condition.HiddenRecipeConditions;
import com.craisinlord.hiddenrecipes.condition.HiddenRecipeTriggerType;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

/**
 * Satisfied once the player has, anywhere in their main inventory, an item stack matching
 * the given {@link ItemPredicate}. Wrapping vanilla's own predicate type (rather than
 * inventing our own) gets two things for free, verified against 1.21.1's actual
 * {@code ItemPredicate}/{@code DataComponentPredicate} classes:
 *
 * <ul>
 *   <li><b>Any number of required data components per item</b> — {@code components} is a
 *       JSON object, so list as many key/value pairs as the item needs
 *       ({@code minecraft:custom_data}, {@code minecraft:enchantments}, a custom
 *       component, ...); all of them must match.</li>
 *   <li><b>Matching by tag instead of a single item id</b> — {@code items} accepts either
 *       a specific item id, a list of item ids, or a tag reference prefixed with
 *       {@code #} (e.g. {@code "#modid:mystic_shards"}), same as any other vanilla item
 *       predicate (loot tables, advancement criteria, etc).</li>
 * </ul>
 *
 * Example requiring a specific item with two components:
 * <pre>{@code
 * "item": {
 *   "items": "modid:mystic_shard",
 *   "components": {
 *     "minecraft:custom_data": { "corrupted": true },
 *     "minecraft:enchantments": { "levels": { "minecraft:sharpness": 3 } }
 *   }
 * }
 * }</pre>
 *
 * Example requiring any item from a tag (no components required):
 * <pre>{@code
 * "item": { "items": "#modid:mystic_shards" }
 * }</pre>
 */
public record HasItemCondition(ItemPredicate predicate) implements HiddenRecipeCondition {

    public static final MapCodec<HasItemCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        ItemPredicate.CODEC.fieldOf("item").forGetter(HasItemCondition::predicate)
    ).apply(instance, HasItemCondition::new));

    @Override
    public boolean test(ServerPlayer player) {
        for (ItemStack stack : player.getInventory().items) {
            if (predicate.test(stack)) {
                return true;
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            if (predicate.test(stack)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public HiddenRecipeConditionType<?> type() {
        return HiddenRecipeConditions.HAS_ITEM;
    }

    @Override
    public Set<HiddenRecipeTriggerType> relevantTriggers() {
        return Set.of(HiddenRecipeTriggerType.INVENTORY);
    }
}

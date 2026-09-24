# Hidden Recipes

Hidden Recipes lets modpack authors add discoverable, condition-based recipes to Minecraft.
Locked recipes stay out of the recipe book and JEI until a player meets their requirement. JEI
can show a hint instead of the real ingredients, so players get a clue without having the recipe
spoiled.

## For players

While a recipe is locked:

- It does not appear in the recipe book.
- JEI hides the real ingredients and can show a hint page instead.
- The crafting grid and furnace family refuse to produce the locked result.
- The player can see live progress when the hint enables it in singleplayer.

When the requirement is met, the player receives an unlock toast, the recipe is added to the
recipe book, and it can be used normally. Unlocks are saved per player and survive logout and
relog.

JEI is optional. Without JEI, the recipe still unlocks and the vanilla recipe book and crafting
behavior work normally.

## For modpack developers

To add your own hidden recipe, create two normal datapack files under your namespace:

1. The real recipe under `data/<namespace>/recipe/`.
2. A matching hidden-recipe entry under `data/<namespace>/hidden_recipes/`.

The real recipe does not need to know that it is hidden. It may use a vanilla recipe type or a
recipe type supplied by another mod.

### Hidden-recipe entry

```json
{
  "recipe": "<namespace>:<path>",
  "condition": { "type": "...", "...": "..." },
  "hint": {
    "translation_key": "hint.<namespace>.<name>",
    "show_progress": true
  }
}
```

- `recipe` is the id of an existing recipe. Use one entry per recipe id.
- `condition` describes when the recipe unlocks.
- `hint.translation_key` is the language key shown on JEI's hidden-recipe page.
- `hint.show_progress` enables an `X/Y requirements met` line when progress can be evaluated.

Add the hint text to your language file, for example:

```json
{
  "hint.mymod.ancient_ingot": "Discover the secret of the ancient forge."
}
```

### Condition types

All condition types use the `hidden_recipes:` namespace.

`has_item` unlocks when the player has a matching item in their main inventory or offhand. The
`item` value uses Minecraft's item-predicate format, including tags and data components.

```json
{
  "type": "hidden_recipes:has_item",
  "item": { "items": "minecraft:paper" }
}
```

`has_advancement` unlocks after the player completes an advancement.

```json
{
  "type": "hidden_recipes:has_advancement",
  "advancement": "minecraft:nether/root"
}
```

`and` requires every child condition:

```json
{
  "type": "hidden_recipes:and",
  "values": [
    { "type": "hidden_recipes:has_item", "item": { "items": "minecraft:nether_star" } },
    { "type": "hidden_recipes:has_advancement", "advancement": "minecraft:end/kill_dragon" }
  ]
}
```

`or` requires at least one child condition:

```json
{
  "type": "hidden_recipes:or",
  "values": [
    { "type": "hidden_recipes:has_advancement", "advancement": "minecraft:husbandry/tame_an_animal" },
    { "type": "hidden_recipes:has_advancement", "advancement": "minecraft:adventure/adventuring_time" }
  ]
}
```

`not` inverts one child condition:

```json
{
  "type": "hidden_recipes:not",
  "value": { "type": "hidden_recipes:has_item", "item": { "items": "minecraft:name_tag" } }
}
```

Conditions can be nested to create progression such as “reach the Nether and carry a key item,”
or “tame an animal and do not already own the reward.”

### Commands

These commands are useful for testing a pack in a development world:

- `/hiddenrecipes list <player>` shows the player's unlock count.
- `/hiddenrecipes unlock <player> <recipe>` force-unlocks a recipe.
- `/hiddenrecipes reload` reminds you to use vanilla `/reload` for datapack changes.

## What is enforced

Hard enforcement currently covers the vanilla crafting grid, furnace, blast furnace, and smoker.
Locked recipes in those systems cannot be completed even if a player already knows the pattern.

Other recipe systems are hide-only. This includes smithing tables, stonecutters, campfires, and
custom machines from other mods. Their locked recipes can be hidden from the recipe book and JEI,
but a player who knows the inputs may still use the machine. Custom machines also need their own
mod-specific enforcement hooks, if available.

## Multiplayer behavior

Unlocks are evaluated and stored server-side. On join and datapack reload, the server syncs hidden
recipe definitions and each player's unlocked ids to the client. JEI hints therefore work on
dedicated servers too. Live requirement progress is available only in singleplayer or an
integrated server because the condition tree remains server-side.

## Example datapack

The repo includes a complete, working example datapack under
[`examples/hidden_recipes_examples/`](examples/hidden_recipes_examples), covering:

- `enchanted_golden_apple_secret`
- `gilded_blackstone_secret`
- `name_tag_secret`
- `saddle_secret`
- `totem_of_undying_secret`
- `smelted_leather_secret`

## Known limitations

- Hard enforcement does not cover custom machines, campfires, smithing, or stonecutting.
- Furnace automation without a player who has opened the furnace (from a hopper) can't be attributed to a player,
  so those recipes are not blocked 

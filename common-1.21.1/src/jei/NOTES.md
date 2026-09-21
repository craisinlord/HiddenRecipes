# JEI integration — wired in

This `src/jei/` source set is added to both `fabric-1.21.1` and `neoforge-1.21.1`'s
`sourceSets`, with `jei_version_1_21_1=19.27.0.336` pinned in `gradle.properties` (real
coordinates, confirmed against this machine's Gradle cache — both `-fabric-api` and
`-neoforge-api` artifacts resolved there already) and `https://maven.blamejared.com`
added to the root `build.gradle`'s repositories.

Fabric discovers `HiddenRecipesJeiPlugin` via the `jei_mod_plugin` entrypoint in
`fabric-1.21.1/src/main/resources/fabric.mod.json` — checked against JEI's own
`fabric.mod.json` rather than assumed; JEI does **not** use `META-INF/services` for plugin
discovery on Fabric the way `PlatformHelper` does. NeoForge discovers it via the
`@JeiPlugin` annotation on the class itself (NeoForge's own classpath scan), no
registration file needed there.

Still open, noted in the class doc on `HiddenRecipesJeiPlugin`:

- Reads `HiddenRecipeManager.all()` directly, which is only populated server-side — works
  in singleplayer (same JVM), empty on a remote dedicated server until the server also
  syncs `HiddenRecipeEntry` definitions to the client, not just the unlocked-id set.
- Real-recipe hide/unhide is scoped to `RecipeType.CRAFTING` only.

REI/EMI plugins should follow the same shape as a `src/rei/` / `src/emi/` source set once
you decide whether the pack needs them too.

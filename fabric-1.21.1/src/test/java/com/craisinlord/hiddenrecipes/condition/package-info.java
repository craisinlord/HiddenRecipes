/**
 * Tests here exercise loader-agnostic <b>common</b> logic (the {@code hidden_recipes}
 * condition tree — {@code AndCondition}/{@code OrCondition}/{@code NotCondition}/
 * {@code HiddenRecipeCondition.leaves()}), not anything Fabric-specific. They live under
 * {@code fabric-1.21.1/src/test} rather than under {@code common-1.21.1} because
 * {@code common-1.21.1} is a source-only module (its own {@code compileJava} task is
 * disabled — see its build.gradle) with no Minecraft/Mojang dependencies wired in, while
 * {@code fabric-1.21.1} already pulls in common's sources plus the full Minecraft/Mojang
 * mapped classpath those sources need just to compile (the condition interface's methods
 * take a {@code ServerPlayer} parameter). Testing loader-agnostic logic once, hosted in
 * whichever loader module already has a working compile classpath, was judged not worth
 * duplicating into neoforge-1.21.1 too — the code under test doesn't change per loader.
 *
 * <p>Run with {@code ./gradlew :fabric-1.21.1:test} once a build environment is available —
 * not run or verified this pass (no Gradle/shell access).
 */
package com.craisinlord.hiddenrecipes.condition;

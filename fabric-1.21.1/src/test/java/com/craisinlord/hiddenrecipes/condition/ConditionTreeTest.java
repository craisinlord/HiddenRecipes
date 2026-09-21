package com.craisinlord.hiddenrecipes.condition;

import com.craisinlord.hiddenrecipes.condition.impl.AndCondition;
import com.craisinlord.hiddenrecipes.condition.impl.NotCondition;
import com.craisinlord.hiddenrecipes.condition.impl.OrCondition;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers {@code test()}/{@code relevantTriggers()}/{@code leaves()} for the three composite
 * condition types, using {@link FakeCondition} test doubles instead of the real
 * {@code has_item}/{@code has_advancement} leaf implementations — those need a real
 * {@code ServerPlayer} to mean anything, which is exactly what this test class avoids
 * needing to construct. See this package's {@code package-info.java} for why these tests
 * live in fabric-1.21.1's source tree despite testing common, loader-agnostic logic.
 *
 * <p>Deliberately does NOT test {@code HasItemCondition}/{@code HasAdvancementCondition}
 * themselves, or {@code HiddenRecipeManager}'s trigger-indexing/retest logic — both need a
 * real (or heavily mocked) {@code ServerPlayer}/{@code MinecraftServer}, which is a
 * meaningfully bigger lift than this pass's scope. That's a real gap, not an oversight: see
 * README.md's "Known limitations".
 */
class ConditionTreeTest {

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void andWithAllTrueChildrenPasses() {
        AndCondition condition = new AndCondition(List.of(
            FakeCondition.always(true), FakeCondition.always(true)));

        assertTrue(condition.test(null));
    }

    @Test
    void andWithOneFalseChildFails() {
        AndCondition condition = new AndCondition(List.of(
            FakeCondition.always(true), FakeCondition.always(false)));

        assertFalse(condition.test(null));
    }

    @Test
    void andWithNoChildrenIsVacuouslyTrue() {
        // Real recipes should never actually define an empty `and`, but the JEI remote-play
        // fallback (HiddenRecipesJeiPlugin.REMOTE_PLACEHOLDER_CONDITION) deliberately relies
        // on this exact behavior — an empty AndCondition being safely "true" and leaf-less.
        AndCondition condition = new AndCondition(List.of());

        assertTrue(condition.test(null));
        assertTrue(condition.leaves().isEmpty());
    }

    @Test
    void orWithAnyTrueChildPasses() {
        OrCondition condition = new OrCondition(List.of(
            FakeCondition.always(false), FakeCondition.always(true)));

        assertTrue(condition.test(null));
    }

    @Test
    void orWithAllFalseChildrenFails() {
        OrCondition condition = new OrCondition(List.of(
            FakeCondition.always(false), FakeCondition.always(false)));

        assertFalse(condition.test(null));
    }

    @Test
    void notInvertsItsChild() {
        assertFalse(new NotCondition(FakeCondition.always(true)).test(null));
        assertTrue(new NotCondition(FakeCondition.always(false)).test(null));
    }

    @Test
    void leavesFlattensAndOrButTreatsNotAsItsOwnLeaf() {
        FakeCondition a = FakeCondition.always(true);
        FakeCondition b = FakeCondition.always(false);
        FakeCondition c = FakeCondition.always(true);
        NotCondition notC = new NotCondition(c);

        // and( or(a, b), not(c) ) — mirrors the real nesting shape used in the shipped
        // name_tag_secret.json example (and + not + has_advancement + has_item). `and`/`or`
        // flatten through; `not` deliberately doesn't — see HiddenRecipeCondition.leaves()'s
        // javadoc for why (flattening straight to `c` would report progress backwards).
        AndCondition tree = new AndCondition(List.of(
            new OrCondition(List.of(a, b)),
            notC));

        assertEquals(List.of(a, b, notC), tree.leaves());
    }

    @Test
    void notIsReportedAsOneLeafWithCorrectlyNegatedResult() {
        // c tests true, so not(c) should test false — and leaves() should expose notC itself
        // (not c) as the single requirement, so testing that leaf gives the negated answer.
        FakeCondition c = FakeCondition.always(true);
        NotCondition notC = new NotCondition(c);

        assertEquals(List.of(notC), notC.leaves());
        assertFalse(notC.leaves().get(0).test(null));
    }

    @Test
    void relevantTriggersUnionsAllChildren() {
        FakeCondition inventoryLeaf = FakeCondition.always(true, HiddenRecipeTriggerType.INVENTORY);
        FakeCondition advancementLeaf = FakeCondition.always(true, HiddenRecipeTriggerType.ADVANCEMENT);

        AndCondition tree = new AndCondition(List.of(inventoryLeaf, new NotCondition(advancementLeaf)));

        assertEquals(
            Set.of(HiddenRecipeTriggerType.INVENTORY, HiddenRecipeTriggerType.ADVANCEMENT),
            tree.relevantTriggers());
    }

    /**
     * Minimal {@link HiddenRecipeCondition} test double: a fixed result, ignoring whatever
     * {@code ServerPlayer} it's asked to test against — {@code type()} deliberately throws,
     * since nothing in these tests serializes a condition tree (that's covered by the real
     * codec wiring, not unit-testable without a working registry/codec environment).
     */
    private static final class FakeCondition implements HiddenRecipeCondition {
        private final boolean result;
        private final Set<HiddenRecipeTriggerType> triggers;

        private FakeCondition(boolean result, Set<HiddenRecipeTriggerType> triggers) {
            this.result = result;
            this.triggers = triggers;
        }

        static FakeCondition always(boolean result, HiddenRecipeTriggerType... triggers) {
            return new FakeCondition(result, Set.of(triggers));
        }

        @Override
        public boolean test(net.minecraft.server.level.ServerPlayer player) {
            return result;
        }

        @Override
        public HiddenRecipeConditionType<?> type() {
            throw new UnsupportedOperationException("FakeCondition is not meant to be serialized");
        }

        @Override
        public Set<HiddenRecipeTriggerType> relevantTriggers() {
            return triggers;
        }
    }
}

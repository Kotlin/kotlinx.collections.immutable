/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.map;

import kotlinx.collections.immutable.ExtensionsKt;
import kotlinx.collections.immutable.ImmutableSet;
import kotlinx.collections.immutable.PersistentMap;
import org.junit.Test;

import java.util.AbstractMap.SimpleEntry;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PersistentMapJavaAccessorsTest {
    /**
     * Java call sites resolve the covariant {@code getEntries()} accessor that returns
     * {@link ImmutableSet}; Kotlin call sites always go through the erased
     * {@code entrySet()} signature and never touch it.
     */
    @Test
    public void entriesOfHashAndOrderedMapsAreImmutableSetsInJava() {
        PersistentMap<String, Integer> hashMap =
                ExtensionsKt.<String, Integer>persistentHashMapOf().putting("a", 1).putting("b", 2);
        ImmutableSet<Map.Entry<String, Integer>> hashEntries = hashMap.getEntries();
        assertEquals(2, hashEntries.size());
        assertTrue(hashEntries.contains(new SimpleEntry<>("a", 1)));
        assertTrue(hashEntries.contains(new SimpleEntry<>("b", 2)));

        PersistentMap<String, Integer> orderedMap =
                ExtensionsKt.<String, Integer>persistentMapOf().putting("b", 2).putting("a", 1);
        ImmutableSet<Map.Entry<String, Integer>> orderedEntries = orderedMap.getEntries();
        assertEquals(2, orderedEntries.size());
        assertTrue(orderedEntries.contains(new SimpleEntry<>("b", 2)));
        assertTrue(orderedEntries.contains(new SimpleEntry<>("a", 1)));
    }
}

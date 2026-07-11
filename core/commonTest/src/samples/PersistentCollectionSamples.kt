/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable.samples

import kotlinx.collections.immutable.*
import kotlin.test.Test

class PersistentCollectionSamples {
    @Test
    fun immutableCollection() {
        fun totalLength(strings: ImmutableCollection<String>): Int = strings.sumOf { it.length }
        check(totalLength(persistentListOf("ab", "cde")) == 5)
        check(totalLength(persistentSetOf("a", "bc", "def")) == 6)
    }

    @Test
    fun persistentCollection() {
        val initial: PersistentCollection<String> = persistentListOf("a", "b")
        val updated = initial + "c" - "a"
        check(updated == listOf("b", "c"))
        check(initial == listOf("a", "b"))
    }

    @Test
    fun adding() {
        val letters: PersistentCollection<String> = persistentListOf("a", "b")
        val extended = letters.adding("c")
        check(extended == listOf("a", "b", "c"))
        check(letters == listOf("a", "b"))
    }

    @Test
    fun addingAll() {
        val letters: PersistentCollection<String> = persistentListOf("a", "b")
        val extended = letters.addingAll(listOf("c", "d"))
        check(extended == listOf("a", "b", "c", "d"))
        check(letters == listOf("a", "b"))
    }

    @Test
    fun removing() {
        val letters: PersistentCollection<String> = persistentListOf("a", "b", "a")
        val shortened = letters.removing("a")
        check(shortened == listOf("b", "a"))
        check(letters == listOf("a", "b", "a"))
    }

    @Test
    fun removingAllElements() {
        val letters: PersistentCollection<String> = persistentListOf("a", "b", "a", "c")
        val remaining = letters.removingAll(listOf("a", "c"))
        check(remaining == listOf("b"))
        check(letters == listOf("a", "b", "a", "c"))
    }

    @Test
    fun removingAllPredicate() {
        val numbers: PersistentCollection<Int> = persistentListOf(1, 2, 3, 4, 5)
        val odds = numbers.removingAll { it % 2 == 0 }
        check(odds == listOf(1, 3, 5))
        check(numbers == listOf(1, 2, 3, 4, 5))
    }

    @Test
    fun retainingAll() {
        val numbers: PersistentCollection<Int> = persistentListOf(1, 2, 3, 4, 5)
        val retained = numbers.retainingAll(listOf(2, 4, 6))
        check(retained == listOf(2, 4))
        check(numbers == listOf(1, 2, 3, 4, 5))
    }

    @Test
    fun builder() {
        val letters: PersistentCollection<String> = persistentListOf("a", "b", "c")
        val builder = letters.builder()
        builder.add("d")
        builder.remove("a")
        val updated = builder.build()
        check(updated == listOf("b", "c", "d"))
        check(letters == listOf("a", "b", "c"))
    }

    @Test
    fun plusElement() {
        val letters: PersistentCollection<String> = persistentListOf("a", "b")
        val extended = letters + "c"
        check(extended == listOf("a", "b", "c"))
        check(letters == listOf("a", "b"))
    }

    @Test
    fun minusElement() {
        val letters: PersistentCollection<String> = persistentListOf("a", "b", "a")
        val shortened = letters - "a"
        check(shortened == listOf("b", "a"))
        check(letters == listOf("a", "b", "a"))
    }

    @Test
    fun plusCollection() {
        val numbers: PersistentCollection<Int> = persistentListOf(1, 2)
        val extended = numbers + listOf(3, 4)
        check(extended == listOf(1, 2, 3, 4))
        check(numbers == listOf(1, 2))
    }

    @Test
    fun minusCollection() {
        val numbers: PersistentCollection<Int> = persistentListOf(1, 2, 1, 3)
        val remaining = numbers - listOf(1, 3)
        check(remaining == listOf(2))
        check(numbers == listOf(1, 2, 1, 3))
    }

    @Test
    fun intersect() {
        val numbers: PersistentCollection<Int> = persistentListOf(1, 2, 2, 3)
        val common = numbers intersect listOf(2, 3, 4)
        check(common == setOf(2, 3))
        check(numbers == listOf(1, 2, 2, 3))
    }
}

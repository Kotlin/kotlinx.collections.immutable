/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable.samples

import kotlinx.collections.immutable.*
import kotlin.test.Test

class PersistentListSamples {
    @Test
    fun immutableList() {
        val letters: ImmutableList<String> = listOf("a", "b", "c").toImmutableList()
        check(letters[0] == "a")
        check(letters.size == 3)
    }

    @Test
    fun persistentList() {
        val version1 = persistentListOf("a", "b")
        val version2 = version1 + "c"
        check(version1 == listOf("a", "b"))
        check(version2 == listOf("a", "b", "c"))
    }

    @Test
    fun subList() {
        val numbers = persistentListOf(1, 2, 3, 4, 5)
        val middle = numbers.subList(1, 4)
        check(middle == listOf(2, 3, 4))
    }

    @Test
    fun adding() {
        val letters = persistentListOf("a", "b")
        val extended = letters.adding("c")
        check(extended == listOf("a", "b", "c"))
        check(letters == listOf("a", "b"))
    }

    @Test
    fun addingAll() {
        val letters = persistentListOf("a", "b")
        val extended = letters.addingAll(listOf("c", "d"))
        check(extended == listOf("a", "b", "c", "d"))
    }

    @Test
    fun removing() {
        val letters = persistentListOf("a", "b", "a")
        val shortened = letters.removing("a")
        check(shortened == listOf("b", "a"))
    }

    @Test
    fun removingAllElements() {
        val letters = persistentListOf("a", "b", "a", "c")
        val remaining = letters.removingAll(listOf("a", "c"))
        check(remaining == listOf("b"))
    }

    @Test
    fun removingAllPredicate() {
        val numbers = persistentListOf(1, 2, 3, 4, 5)
        val odds = numbers.removingAll { it % 2 == 0 }
        check(odds == listOf(1, 3, 5))
    }

    @Test
    fun retainingAll() {
        val numbers = persistentListOf(1, 2, 3, 4, 5)
        val retained = numbers.retainingAll(listOf(2, 4, 6))
        check(retained == listOf(2, 4))
    }

    @Test
    fun addingAt() {
        val letters = persistentListOf("a", "c")
        val inserted = letters.addingAt(1, "b")
        check(inserted == listOf("a", "b", "c"))
    }

    @Test
    fun addingAllAt() {
        val letters = persistentListOf("a", "d")
        val inserted = letters.addingAllAt(1, listOf("b", "c"))
        check(inserted == listOf("a", "b", "c", "d"))
    }

    @Test
    fun replacingAt() {
        val letters = persistentListOf("a", "x", "c")
        val replaced = letters.replacingAt(1, "b")
        check(replaced == listOf("a", "b", "c"))
    }

    @Test
    fun removingAt() {
        val letters = persistentListOf("a", "b", "c")
        val shortened = letters.removingAt(1)
        check(shortened == listOf("a", "c"))
    }

    @Test
    fun persistentListOf() {
        val letters = persistentListOf("a", "b", "c")
        check(letters == listOf("a", "b", "c"))
        val empty = persistentListOf<String>()
        check(empty.isEmpty())
    }

    @Test
    fun plusElement() {
        val letters = persistentListOf("a", "b")
        val extended = letters + "c"
        check(extended == listOf("a", "b", "c"))
        check(letters == listOf("a", "b"))
    }

    @Test
    fun minusElement() {
        val letters = persistentListOf("a", "b", "a")
        val shortened = letters - "a"
        check(shortened == listOf("b", "a"))
    }

    @Test
    fun plusCollection() {
        val letters = persistentListOf("a", "b")
        val extended = letters + listOf("c", "d")
        check(extended == listOf("a", "b", "c", "d"))
    }

    @Test
    fun minusCollection() {
        val numbers = persistentListOf(1, 2, 1, 3)
        val remaining = numbers - listOf(1)
        check(remaining == listOf(2, 3))
    }

    @Test
    fun mutate() {
        val fruits = persistentListOf("apple", "banana")
        val updated = fruits.mutate {
            it.add("cherry")
            it.remove("apple")
        }
        check(updated == listOf("banana", "cherry"))
        check(fruits == listOf("apple", "banana"))
    }

    @Test
    fun toImmutableList() {
        val numbers = listOf(1, 2, 3).toImmutableList()
        check(numbers == listOf(1, 2, 3))
        val characters = "abc".toImmutableList()
        check(characters == listOf('a', 'b', 'c'))
    }

    @Test
    fun toPersistentList() {
        val persisted = listOf("a", "b", "c").toPersistentList()
        check(persisted == listOf("a", "b", "c"))
        check(persisted.toPersistentList() === persisted)
    }
}

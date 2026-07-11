/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable.samples

import kotlinx.collections.immutable.*
import kotlin.test.Test

class PersistentSetSamples {
    @Test
    fun immutableSet() {
        val fruits: ImmutableSet<String> = setOf("apple", "banana", "cherry").toImmutableSet()
        check("banana" in fruits)
        check("durian" !in fruits)
        check(fruits.size == 3)
    }

    @Test
    fun persistentSet() {
        val version1 = persistentSetOf("a", "b")
        val version2 = version1 + "c"
        check(version1 == setOf("a", "b"))
        check(version2 == setOf("a", "b", "c"))
    }

    @Test
    fun adding() {
        val letters = persistentSetOf("a", "b")
        val extended = letters.adding("c")
        check(extended == setOf("a", "b", "c"))
        check(letters.adding("a") === letters)
    }

    @Test
    fun addingAll() {
        val letters = persistentSetOf("a", "b")
        val extended = letters.addingAll(listOf("b", "c", "d"))
        check(extended == setOf("a", "b", "c", "d"))
        check(extended.size == 4)
    }

    @Test
    fun removing() {
        val letters = persistentSetOf("a", "b", "c")
        val shortened = letters.removing("b")
        check(shortened == setOf("a", "c"))
        check(letters.removing("x") === letters)
    }

    @Test
    fun removingAllElements() {
        val letters = persistentSetOf("a", "b", "c", "d")
        val remaining = letters.removingAll(listOf("b", "d"))
        check(remaining == setOf("a", "c"))
    }

    @Test
    fun removingAllPredicate() {
        val numbers = persistentSetOf(1, 2, 3, 4, 5)
        val odds = numbers.removingAll { it % 2 == 0 }
        check(odds == setOf(1, 3, 5))
    }

    @Test
    fun retainingAll() {
        val letters = persistentSetOf("a", "b", "c", "d")
        val retained = letters.retainingAll(listOf("b", "d", "e"))
        check(retained == setOf("b", "d"))
    }

    @Test
    fun persistentSetOf() {
        val letters = persistentSetOf("b", "a", "b", "c")
        check(letters.toList() == listOf("b", "a", "c"))
        check(letters.size == 3)
        val empty = persistentSetOf<String>()
        check(empty.isEmpty())
    }

    @Test
    fun persistentHashSetOf() {
        val numbers = persistentHashSetOf(1, 2, 3, 2)
        check(numbers == setOf(1, 2, 3))
        check(numbers.size == 3)
        check(2 in numbers)
        val empty = persistentHashSetOf<Int>()
        check(empty.isEmpty())
    }

    @Test
    fun plusElement() {
        val letters = persistentSetOf("a", "b")
        val extended = letters + "c"
        check(extended == setOf("a", "b", "c"))
        check(letters + "a" === letters)
    }

    @Test
    fun minusElement() {
        val letters = persistentSetOf("a", "b", "c")
        val shortened = letters - "b"
        check(shortened == setOf("a", "c"))
        check(letters == setOf("a", "b", "c"))
    }

    @Test
    fun plusCollection() {
        val letters = persistentSetOf("a", "b")
        val union = letters + listOf("b", "c", "d")
        check(union == setOf("a", "b", "c", "d"))
        check(union.toList() == listOf("a", "b", "c", "d"))
    }

    @Test
    fun minusCollection() {
        val letters = persistentSetOf("a", "b", "c", "d")
        val difference = letters - listOf("b", "d")
        check(difference == setOf("a", "c"))
    }

    @Test
    fun intersect() {
        val numbers = persistentSetOf(1, 2, 3, 4)
        val common = numbers intersect listOf(4, 2, 5)
        check(common == setOf(2, 4))
        check(common.toList() == listOf(2, 4))
    }

    @Test
    fun mutate() {
        val colors = persistentSetOf("red", "green")
        val updated = colors.mutate {
            it.add("blue")
            it.remove("red")
        }
        check(updated == setOf("green", "blue"))
        check(colors == setOf("red", "green"))
    }

    @Test
    fun toImmutableSet() {
        val tags = listOf("kotlin", "java", "kotlin", "scala")
        val uniqueTags = tags.toImmutableSet()
        check(uniqueTags.toList() == listOf("kotlin", "java", "scala"))
        check(uniqueTags.size == 3)
    }

    @Test
    fun toPersistentSet() {
        val uniqueNumbers = listOf(1, 2, 2, 3).toPersistentSet()
        check(uniqueNumbers.toList() == listOf(1, 2, 3))
        val uniqueChars = "abcb".toPersistentSet()
        check(uniqueChars.toList() == listOf('a', 'b', 'c'))
    }

    @Test
    fun toPersistentHashSet() {
        val numbers = listOf(1, 2, 3, 2)
        val uniqueNumbers = numbers.toPersistentHashSet()
        check(uniqueNumbers == setOf(1, 2, 3))
        check(uniqueNumbers.size == 3)
    }
}

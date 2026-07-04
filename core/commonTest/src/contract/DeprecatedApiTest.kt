/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract

import kotlinx.collections.immutable.*
import tests.assertTypeEquals
import kotlin.test.*

/**
 * A third-party [PersistentCollection] implementation that provides only the abstract deprecated
 * mutable-style methods, the way pre-0.5.0 implementations did.
 *
 * The new `-ing` methods are intentionally not overridden here: they must keep working
 * through the [PersistentCollection] interface defaults that delegate to the deprecated methods.
 */
private class LegacyPersistentCollection<E>(private val impl: PersistentList<E>) :
    PersistentCollection<E>, Collection<E> by impl {

    @Deprecated("Use adding() instead.")
    override fun add(element: E): PersistentCollection<E> = impl.adding(element)

    @Deprecated("Use addingAll() instead.")
    override fun addAll(elements: Collection<E>): PersistentCollection<E> = impl.addingAll(elements)

    @Deprecated("Use removing() instead.")
    override fun remove(element: E): PersistentCollection<E> = impl.removing(element)

    @Deprecated("Use removingAll() instead.")
    override fun removeAll(elements: Collection<E>): PersistentCollection<E> = impl.removingAll(elements)

    @Deprecated("Use removingAll() instead.")
    override fun removeAll(predicate: (E) -> Boolean): PersistentCollection<E> = impl.removingAll(predicate)

    @Deprecated("Use retainingAll() instead.")
    override fun retainAll(elements: Collection<E>): PersistentCollection<E> = impl.retainingAll(elements)

    @Deprecated("Use cleared() instead.")
    override fun clear(): PersistentCollection<E> = impl.cleared()

    override fun builder(): PersistentCollection.Builder<E> = impl.builder()
}

/**
 * A third-party [PersistentMap] implementation that provides only the abstract deprecated
 * mutable-style methods, so that `putting`/`removing` are served by the interface defaults.
 */
private class LegacyPersistentMap<K, V>(private val impl: PersistentMap<K, V>) :
    PersistentMap<K, V>, Map<K, V> by impl {

    override val keys: ImmutableSet<K> get() = impl.keys
    override val values: ImmutableCollection<V> get() = impl.values
    override val entries: ImmutableSet<Map.Entry<K, V>> get() = impl.entries

    @Deprecated("Use putting() instead.")
    override fun put(key: K, value: V): PersistentMap<K, V> = impl.putting(key, value)

    @Deprecated("Use removing() instead.")
    override fun remove(key: K): PersistentMap<K, V> = impl.removing(key)

    @Deprecated("Use removing() instead.")
    override fun remove(key: K, value: V): PersistentMap<K, V> = impl.removing(key, value)

    @Deprecated("Use puttingAll() instead.")
    override fun putAll(m: Map<out K, V>): PersistentMap<K, V> = impl.puttingAll(m)

    @Deprecated("Use cleared() instead.")
    override fun clear(): PersistentMap<K, V> = impl.cleared()

    override fun builder(): PersistentMap.Builder<K, V> = impl.builder()
}

class DeprecatedApiTest {

    @Test
    fun `PersistentCollection interface defaults delegate to the deprecated abstract methods`() {
        val legacy: PersistentCollection<Int> = LegacyPersistentCollection(persistentListOf(1, 2, 3, 2))

        assertEquals(listOf(1, 2, 3, 2, 4), legacy.adding(4).toList())
        assertEquals(listOf(1, 2, 3, 2, 4, 5), legacy.addingAll(listOf(4, 5)).toList())
        assertEquals(listOf(1, 3, 2), legacy.removing(2).toList(), "only the first occurrence is removed")
        assertEquals(listOf(1, 3), legacy.removingAll(listOf(2)).toList(), "all occurrences are removed")
        assertEquals(listOf(1, 3), legacy.removingAll { it % 2 == 0 }.toList())
        assertEquals(listOf(2, 2), legacy.retainingAll(listOf(2)).toList())
        assertTrue(legacy.cleared().isEmpty())
        assertEquals(listOf(1, 2, 3, 2), legacy.toList(), "original collection is unchanged")
    }

    @Test
    fun `PersistentMap interface defaults delegate to the deprecated abstract methods`() {
        val legacy: PersistentMap<String, Int> = LegacyPersistentMap(persistentMapOf("a" to 1, "b" to 2))

        assertEquals(mapOf("a" to 1, "b" to 2, "c" to 3), legacy.putting("c", 3))
        assertEquals(mapOf("a" to 10, "b" to 2), legacy.putting("a", 10), "value for an existing key is replaced")
        assertEquals(mapOf("b" to 2), legacy.removing("a"))
        // the wrapper defines no equals of its own, so compare its content
        assertEquals(mapOf("a" to 1, "b" to 2), legacy.toMap(), "original map is unchanged")
    }

    @Test
    fun `retainingAll on a persistent list delegates to the deprecated retainAll override`() {
        val list = persistentListOf(1, 2, 3, 4, 5)

        assertEquals(listOf(2, 4), list.retainingAll(listOf(4, 2, 6)).toList())
        assertTrue(list.retainingAll(emptyList()).isEmpty())
        assertEquals(listOf(1, 2, 3, 4, 5), list.toList(), "original list is unchanged")
    }

    @Suppress("DEPRECATION")
    @Test
    fun `deprecated putAll overloads behave like puttingAll`() {
        // The out-projection makes the deprecated putAll member inapplicable, so these calls
        // resolve to the deprecated top-level putAll extensions.
        val map: PersistentMap<out String, Int> = persistentMapOf("a" to 1)
        val expected = mapOf("a" to 1, "b" to 2)

        assertEquals(expected, map.putAll(mapOf("b" to 2)))
        assertEquals(expected, map.putAll(listOf("b" to 2)))
        assertEquals(expected, map.putAll(arrayOf("b" to 2)))
        assertEquals(expected, map.putAll(sequenceOf("b" to 2)))
        assertEquals<Map<out String, Int>>(mapOf("a" to 1), map, "original map is unchanged")
    }

    @Suppress("DEPRECATION")
    @Test
    fun `deprecated immutable factory functions return the same collections as their persistent counterparts`() {
        assertEquals(persistentListOf(1, 2, 3), immutableListOf(1, 2, 3))
        assertEquals(persistentListOf<Int>(), immutableListOf<Int>())
        assertTypeEquals(persistentListOf<Int>(), immutableListOf<Int>())

        assertEquals(persistentSetOf(1, 2), immutableSetOf(1, 2))
        assertEquals(listOf(1, 2), immutableSetOf(1, 2).toList(), "insertion order is preserved")
        assertEquals(persistentSetOf<Int>(), immutableSetOf<Int>())
        assertTypeEquals(persistentSetOf<Int>(), immutableSetOf<Int>())

        assertEquals(persistentHashSetOf(1, 2), immutableHashSetOf(1, 2))
        assertTypeEquals(persistentHashSetOf(1), immutableHashSetOf(1))

        assertEquals(persistentMapOf("a" to 1, "b" to 2), immutableMapOf("a" to 1, "b" to 2))
        assertTypeEquals(persistentMapOf("a" to 1), immutableMapOf("a" to 1))

        assertEquals(persistentHashMapOf("a" to 1), immutableHashMapOf("a" to 1))
        assertTypeEquals(persistentHashMapOf("a" to 1), immutableHashMapOf("a" to 1))
    }
}

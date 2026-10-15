/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.implementations.immutableList.MAX_BUFFER_SIZE
import kotlinx.collections.immutable.implementations.immutableList.PersistentVectorBuilder
import kotlinx.collections.immutable.implementations.immutableMap.MAX_BRANCHING_FACTOR
import kotlinx.collections.immutable.minus
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.persistentHashSetOf
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentHashSet
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import tests.IntWrapper
import tests.ObjectWrapper
import tests.remove
import tests.trie.collidingKey1
import tests.trie.collidingKey2
import tests.trie.trieShapes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * A persistent call that changes nothing returns the receiver: adding a stored element, removing a missing one,
 * retaining a superset, putting the stored value, replacing an element with itself. A builder that was not changed, or
 * was only given what it stores, builds the collection it came from, and the same instance again. A call that empties
 * a collection, and the build of an emptied builder, return the empty instance of its kind, the one its empty factory
 * call returns.
 *
 * A write changes nothing when it writes the stored instance or, over a primitive number, an unsigned number, a char
 * or a Boolean, an equal one: NaN over NaN, a number boxed again. An equal instance of a Number subclass is written,
 * as any object. 0.0 and -0.0 are not equal, so one written over the other is stored, also on Kotlin/JS, where
 * 0.0 === -0.0.
 *
 * What a call must return follows from what the same call does to a stdlib copy of the receiver: the receiver when
 * the copy holds what it held, instance by instance, the empty instance when the copy is emptied, a new instance
 * otherwise, always one equal to the copy; a builder's call returns what the copy's did.
 */
class NoOpIdentityTest {

    @Test
    fun list() = runCells { testPersistent(lists, listKind) { listRows(it) + wrapperListRows(it) } }

    @Test
    fun listBuilder() = runCells { testBuilders(listBuildersOfEveryDepth + emptiedListBuilders, listKind, ::listRows) }

    @Test
    fun hashSet() = runCells { testPersistent(hashSets, setKind(persistentHashSetOf())) { setRows(it) + wrapperSetRows(it) } }

    @Test
    fun hashSetBuilder() = runCells { testBuilders(hashSetBuilders + emptiedSetBuilders(persistentHashSetOf()), setKind(persistentHashSetOf()), ::setRows) }

    @Test
    fun orderedSet() = runCells { testPersistent(orderedSets, setKind(persistentSetOf())) { setRows(it) + wrapperSetRows(it) } }

    @Test
    fun orderedSetBuilder() = runCells { testBuilders(orderedSetBuilders + emptiedSetBuilders(persistentSetOf()), setKind(persistentSetOf()), ::setRows) }

    @Test
    fun hashMap() = runCells { testPersistent(hashMaps, mapKind(persistentHashMapOf())) { mapRows(it) + wrapperMapRows(it) } }

    @Test
    fun hashMapBuilder() = runCells { testBuilders(hashMapBuilders + emptiedMapBuilders(persistentHashMapOf()), mapKind(persistentHashMapOf()), ::mapRows) }

    @Test
    fun orderedMap() = runCells { testPersistent(orderedMaps, mapKind(persistentMapOf())) { mapRows(it) + wrapperMapRows(it) } }

    @Test
    fun orderedMapBuilder() = runCells { testBuilders(orderedMapBuilders + emptiedMapBuilders(persistentMapOf()), mapKind(persistentMapOf()), ::mapRows) }

    @Test
    fun `every call that empties a collection returns the empty instance of its kind`() = runCells {
        testEmptied(emptiedLists, persistentListOf())
        testEmptied(emptiedSets("a hash set", persistentHashSetOf()), persistentHashSetOf())
        testEmptied(emptiedSets("an ordered set", persistentSetOf()), persistentSetOf())
        testEmptied(emptiedMaps("a hash map", persistentHashMapOf()), persistentHashMapOf())
        testEmptied(emptiedMaps("an ordered map", persistentMapOf()), persistentMapOf())
    }

    @Test
    fun `putting the stored value under a null key and removing a missing null key change nothing`() = runCells {
        val value = IntWrapper(0, 0)
        val maps = listOf(
            "a hash map" to persistentHashMapOf<Any?, Any>(null to value, collidingKey1 to IntWrapper(1, 1)),
            "an ordered map" to persistentMapOf<Any?, Any>(null to value, collidingKey1 to IntWrapper(1, 1)),
        )
        for ((kind, map) in maps) {
            cell("$kind, putting the stored value under the null key") {
                assertSame(map, map.putting(null, value))
                assertSame(map, map.mutate { it[null] = value })
            }
            cell("$kind, removing a missing null key") {
                val withoutNull = map.removing(null)
                assertSame(withoutNull, withoutNull.removing(null))
                assertSame(withoutNull, withoutNull.mutate { assertNull(it.remove(null)) })
            }
        }
    }

    @Test
    fun `replacingAt of an equal element stores it and is then a no-op`() = runCells {
        for ((size, depth) in listDepths) {
            for (index in boundaryIndices(size)) cell("$depth, index $index") {
                val list = wrappersOf(size).toPersistentList()
                val newElement = IntWrapper(index, index)
                val newList = list.replacingAt(index, newElement)
                assertNotSame(list, newList)
                assertEquals(list, newList)
                assertSame(newElement, newList[index])

                assertSame(newList, newList.replacingAt(index, newElement))
                assertSame(newList, newList.mutate { it[index] = newElement })
            }
        }
    }

    @Test
    fun `replacing a null with a null is a no-op and set returns the replaced null or element`() = runCells {
        val element = Any()
        val nullLists = listOf<Pair<String, (Int) -> List<Any?>>>(
            "a list of nulls" to { size -> List(size) { null } },
            "a list of the nulls of arrayOfNulls, which Kotlin/JS holds as undefined" to { size -> arrayOfNulls<Any>(size).toList() },
        )
        for ((origin, nullsOf) in nullLists) for ((size, depth) in listDepths) {
            for (index in boundaryIndices(size)) cell("$origin, $depth, index $index") {
                val nulls = nullsOf(size).toPersistentList()
                assertSame(nulls, nulls.replacingAt(index, null))
                assertSame(nulls, nulls.mutate { it[index] = null })

                val withElement = nulls.replacingAt(index, element)
                assertNotSame(nulls, withElement)
                assertSame(element, withElement[index])

                val builder = withElement.builder()
                assertSame(element, builder.set(index, null))
                assertNull(builder[index])
                assertNull(builder.set(index, null))

                val nullBuilder = nulls.builder()
                assertNull(nullBuilder.set(index, element))
                assertSame(element, nullBuilder[index])
            }
        }
    }

    @Test
    fun `set of the same element keeps the buffers and the built list`() = runCells {
        for ((origin, newBuilder) in listBuildersOfEveryDepth) cell(origin) {
            val builder = newBuilder() as PersistentVectorBuilder<Any>
            for (afterABuild in listOf(false, true)) {
                val built = if (afterABuild) builder.build() else null
                for (index in boundaryIndices(builder.size)) {
                    val message = "index $index ${if (afterABuild) "after" else "before"} the build"
                    val root = builder.root
                    val tail = builder.tail
                    val trieModCount = builder.trieModCount
                    val element = builder[index]

                    assertSame(element, builder.set(index, element), message)

                    assertSame(root, builder.root, message)
                    assertSame(tail, builder.tail, message)
                    assertEquals(trieModCount, builder.trieModCount, message)
                    if (built != null) assertSame(built, builder.build(), message)
                }
            }
        }
    }

    @Test
    fun `set of the same element in an unowned leaf under an owned root copies nothing`() {
        val builder = wrappersOf(LEAVES).toPersistentList().builder() as PersistentVectorBuilder<Any>
        builder[40] = IntWrapper(-40, -40)
        val root = builder.root
        val leaf = root!![0]
        val trieModCount = builder.trieModCount

        builder[0] = builder[0]

        assertSame(root, builder.root)
        assertSame(leaf, builder.root!![0])
        assertEquals(trieModCount, builder.trieModCount)
    }

    @Test
    fun `set of the same element keeps a live iterator and the built list`() = runCells {
        for ((size, depth) in listDepths) cell(depth) {
            val list = wrappersOf(size).toPersistentList()
            val builder = list.builder()
            val iterator = builder.iterator()
            assertSame(list[0], iterator.next())

            for (index in boundaryIndices(size)) {
                builder[index] = builder[index]
            }

            assertSame(list, builder.build())
            assertSame(list[1], iterator.next())
        }
    }

    @Test
    fun `put and putAll of an equal value keep the stored box in an owned node and in a node copied for another entry`() = runCells {
        val keyPairs = listOf(
            "entries of the root" to (IntWrapper(1, 1) to IntWrapper(2, 2)),
            "entries of a collision node" to (collidingKey1 to collidingKey2),
        )
        val otherValue = IntWrapper(-2, -2)
        for ((node, keys) in keyPairs) {
            val (key, otherKey) = keys
            val stored: Any = 1000
            val entries = listOf<Pair<Any, Any>>(key to stored, otherKey to otherValue)
            val arguments = listOf(
                "a hash map of the equal value" to persistentHashMapOf<Any, Any>(key to 1000),
                "a hash map of the equal value and a new value for the other key" to persistentHashMapOf<Any, Any>(key to 1000, otherKey to NEW),
            )
            val builders = listOf("a hash map" to hashMapBuildersOf(entries), "an ordered map" to orderedMapBuildersOf(entries))
            for ((kind, origins) in builders) for ((origin, newBuilder) in origins) {
                cell("$kind $origin, $node, put of the equal value") {
                    val builder = newBuilder()
                    builder[key] = 1000
                    assertSame(stored, builder[key])
                }
                for ((argument, map) in arguments) cell("$kind $origin, $node, putAll of $argument") {
                    val builder = newBuilder()
                    builder.putAll(map)
                    assertSame(stored, builder[key])
                }
            }
            for ((kind, mapOf) in mapKinds) for ((argument, map) in arguments) cell("$kind, $node, puttingAll of $argument") {
                assertSame(stored, mapOf(entries).puttingAll(map)[key])
            }
        }
    }

    @Test
    fun `set of an equal value keeps the stored box in an owned tail and leaf`() = runCells {
        for (size in listOf(TAIL_ONLY, ONE_LEAF, NODES)) for (index in listOf(0, size - 1)) cell("${listDepths.getValue(size)}, index $index") {
            val stored: Any = 1000
            val builder = persistentListOf<Any>().builder().apply { for (i in 0..<size) add(if (i == index) stored else IntWrapper(i, i)) }
            assertSame(stored, builder.set(index, 1000))
            assertSame(stored, builder[index])
        }
    }

    @Test
    fun `putAll that changes nothing returns a map with a NaN key`() = runCells {
        val entries = listOf<Pair<Any, Any>>(Double.NaN to "v", 1 to "w")
        val arguments = listOf(
            "the other entry" to persistentHashMapOf<Any, Any>(1 to "w"),
            "the NaN entry" to persistentHashMapOf<Any, Any>(Double.NaN to "v"),
            "every entry" to persistentHashMapOf(*entries.toTypedArray()),
        )
        for ((kind, mapOf) in mapKinds) for ((argument, argumentMap) in arguments) {
            val map = mapOf(entries)
            cell("$kind, puttingAll of $argument") { assertSame(map, map.puttingAll(argumentMap)) }
            cell("$kind, a builder's putAll of $argument") { assertSame(map, map.mutate { it.putAll(argumentMap) }) }
        }
    }

    @Test
    fun `an equal number or char written into a list changes nothing and a zero of the other sign or an equal Number object is stored`() = runCells {
        for ((size, depth) in listDepths) for (index in boundaryIndices(size)) for (write in valueWrites) {
            val stored = write.stored()
            val name = "$depth, ${write.description} at $index"
            val list = prepared(name) { wrappersOf(size).toPersistentList().replacingAt(index, stored) } ?: continue
            cell("$name by replacingAt") {
                val written = write.written()
                write.assertResult(list, list.replacingAt(index, written), written) { this[index] }
            }
            cell("$name by a builder's set") {
                val written = write.written()
                val builder = list.builder()
                assertSameValue(stored, builder.set(index, written))
                write.assertResult(list, builder.build(), written) { this[index] }
            }
            cell("$name by the list iterator's set") {
                val written = write.written()
                val builder = list.builder()
                builder.listIterator(index).run { val _ = next(); set(written) }
                write.assertResult(list, builder.build(), written) { this[index] }
            }
        }
    }

    @Test
    fun `an equal number or char put into a hash map changes nothing and a zero of the other sign or an equal Number object is stored`() =
        runCells { testValueWrites("a hash map", setValueOfAnEqualValueWrites = false) { persistentHashMapOf(*it.toTypedArray()) } }

    @Test
    fun `an equal number or char put into an ordered map changes nothing and a zero of the other sign or an equal Number object is stored`() =
        runCells { testValueWrites("an ordered map", setValueOfAnEqualValueWrites = true) { persistentMapOf(*it.toTypedArray()) } }
}

/**
 * A call on a persistent collection and the same call on its builder through the stdlib interface, which a stdlib copy
 * takes too; a call only a builder makes has no persistent form. The builder's call gets the collection built before
 * it, for the one that passes it as the argument.
 */
private class Call<C, M>(
    val description: String,
    val persistent: (C.() -> C)? = null,
    val builder: M.(built: C) -> Any?,
)

private typealias ListCall = Call<PersistentList<Any>, MutableList<Any>>
private typealias SetCall = Call<PersistentSet<Any>, MutableSet<Any>>
private typealias MapCall<V> = Call<PersistentMap<Any, V>, MutableMap<Any, V>>

private enum class Returns { THE_RECEIVER, THE_EMPTY_INSTANCE, A_NEW_INSTANCE }

/** What a call did to a stdlib copy of the receiver. */
private data class Model<M>(val result: Any?, val returns: Returns, val copy: M)

private class Kind<C : Any, M : Any, B : M>(
    val empty: C,
    val builder: C.() -> B,
    val build: B.() -> C,
    val copy: (C) -> M,
) {
    fun model(receiver: C, call: Call<C, M>): Model<M> {
        val model = copy(receiver)
        val result = call.builder(model, receiver)
        return Model(result, returnsOf(receiver, model), model)
    }
}

private val listKind =
    Kind<PersistentList<Any>, MutableList<Any>, PersistentList.Builder<Any>>(persistentListOf(), { builder() }, { build() }, { ArrayList(it) })

private val mapKinds: List<Pair<String, (List<Pair<Any, Any>>) -> PersistentMap<Any, Any>>> = listOf(
    "a hash map" to { persistentHashMapOf(*it.toTypedArray()) },
    "an ordered map" to { persistentMapOf(*it.toTypedArray()) },
)

private fun setKind(empty: PersistentSet<Any>) =
    Kind<PersistentSet<Any>, MutableSet<Any>, PersistentSet.Builder<Any>>(empty, { builder() }, { build() }, { LinkedHashSet(it) })

private fun <V> mapKind(empty: PersistentMap<Any, V>) =
    Kind<PersistentMap<Any, V>, MutableMap<Any, V>, PersistentMap.Builder<Any, V>>(empty, { builder() }, { build() }, { LinkedHashMap(it) })

private fun returnsOf(receiver: Any, copy: Any): Returns {
    val before = contentOf(receiver)
    val after = contentOf(copy)
    return when {
        after.size == before.size && after.indices.all { after[it] === before[it] } -> Returns.THE_RECEIVER
        after.isEmpty() -> Returns.THE_EMPTY_INSTANCE
        else -> Returns.A_NEW_INSTANCE
    }
}

private fun contentOf(collection: Any): List<Any?> =
    if (collection is Map<*, *>) collection.flatMap { listOf(it.key, it.value) } else (collection as Collection<*>).toList()

private fun <C : Any> assertReturns(returns: Returns, receiver: C, result: C, empty: C) {
    when (returns) {
        Returns.THE_RECEIVER -> assertSame(receiver, result)
        Returns.THE_EMPTY_INSTANCE -> assertSame(empty, result)
        Returns.A_NEW_INSTANCE -> assertNotSame(receiver, result)
    }
}

private fun assertSameResult(expected: Any?, actual: Any?) {
    if (expected is Boolean) assertEquals(expected, actual) else assertSame(expected, actual)
}

/** By equals, so the order of a set or a map and the instances do not matter. */
private fun assertEqualContent(copy: Any, result: Any) = assertEquals(copy, result)

/** What [prepare] gives, or null when it throws and a failed cell named [message] reports it. */
private fun <T : Any> Cells.prepared(message: String, prepare: () -> T): T? =
    try {
        prepare()
    } catch (e: Throwable) {
        cell(message) { throw e }
        null
    }

/**
 * Makes every call of [rows] on every collection of [receivers], as the persistent call and through a builder, and
 * checks against the same call on a stdlib copy what each returns and holds, and what the builder's call returned.
 */
private fun <C : Any, M : Any, B : M> Cells.testPersistent(
    receivers: List<Pair<String, () -> C>>,
    kind: Kind<C, M, B>,
    rows: (C) -> List<Call<C, M>>,
) {
    for ((origin, newReceiver) in receivers) {
        val (receiver, calls) = prepared(origin) { newReceiver().let { it to rows(it) } } ?: continue
        for (call in calls) {
            val name = "$origin, ${call.description}"
            val (result, returns, copy) = prepared(name) { kind.model(receiver, call) } ?: continue
            call.persistent?.let { persistent ->
                cell(name) {
                    val persisted = receiver.persistent()
                    assertReturns(returns, receiver, persisted, kind.empty)
                    assertEqualContent(copy, persisted)
                }
            }
            cell("$name through a builder") {
                val builder = kind.builder(receiver)
                assertSameResult(result, call.builder(builder, receiver))
                val built = kind.build(builder)
                assertReturns(returns, receiver, built, kind.empty)
                assertEqualContent(copy, built)
            }
        }
    }
}

/**
 * Makes every call of [rows] on a fresh builder of every origin in [builders] after a build, and checks against the
 * same call on a stdlib copy what build then returns and holds, that it returns the same again, and what the call
 * returned.
 */
private fun <C : Any, M : Any, B : M> Cells.testBuilders(
    builders: List<Pair<String, () -> B>>,
    kind: Kind<C, M, B>,
    rows: (C) -> List<Call<C, M>>,
) {
    for ((origin, newBuilder) in builders) {
        val (built, calls) = prepared(origin) { kind.build(newBuilder()).let { it to rows(it) } } ?: continue
        for (call in calls) {
            val name = "$origin, ${call.description}"
            val (result, returns, copy) = prepared(name) { kind.model(built, call) } ?: continue
            cell(name) {
                val builder = newBuilder()
                val before = kind.build(builder)
                assertSameResult(result, call.builder(builder, before))
                val after = kind.build(builder)
                assertReturns(returns, before, after, kind.empty)
                assertEqualContent(copy, after)
                assertSame(after, kind.build(builder))
            }
        }
    }
}

private fun <C : Any> Cells.testEmptied(emptied: List<Pair<String, () -> C>>, empty: C) {
    for ((origin, emptiedBy) in emptied) cell(origin) { assertSame(empty, emptiedBy()) }
}

/** An element or a value no fixture holds. */
private const val NEW = "new"

/** The element an origin's write stores. */
private const val WRITTEN = "written"

/** An equal element that is not the stored instance; the persistent tables hold wrappers. */
private fun Any.wrapperCopy(): Any = (this as ObjectWrapper<*>).copy()

/** A key no fixture holds, with the given [hash]; the ids of the fixtures are not negative. */
private fun missing(hash: Int): Any = IntWrapper(-1 - hash, hash)

/** A missing key with [stored]'s hash, so with its path to the bottom. */
private fun missingWithTheHashOf(stored: Any): Any = missing(stored.hashCode())

/** A missing key whose path parts from [stored]'s at level 1. */
private fun missingBesideAtLevelOne(stored: Any): Any = missing(stored.hashCode() xor MAX_BRANCHING_FACTOR)

/** Every key wrapped, so that an equal key that is not the stored instance exists. */
private val wrappedShapes: Map<String, List<Any>> = trieShapes.mapValues { (_, keys) -> keys.map { if (it is Int) IntWrapper(it, it) else it } }

private fun wrappersOf(size: Int): List<Any> = List(size) { IntWrapper(it, it) }

private fun wrapperEntries(keys: List<Any>): Array<Pair<Any, Any>> = keys.mapIndexed { index, key -> key to IntWrapper(index, index) }.toTypedArray()

private val emptiedLists: List<Pair<String, () -> PersistentList<Any>>> = listOf(TAIL_ONLY, FULL_TAIL).flatMap { size ->
    val elements = wrappersOf(size)
    val list = { elements.toPersistentList() }
    listOf(
        "a list of $size emptied by removing" to { elements.fold(list()) { rest, element -> rest.removing(element) } },
        "a list of $size emptied by removingAll" to { list().removingAll(elements) },
        "a list of $size emptied through a builder's remove" to { list().mutate { for (element in elements) it.remove(element) } },
        "a list of $size emptied through a builder's removeAll" to { list().mutate { it.removeAll(elements) } },
    )
}

private val lists: List<Pair<String, () -> PersistentList<Any>>> = listOf("the empty list" to { persistentListOf<Any>() }) + listDepths.flatMap { (size, shape) ->
    val elements = wrappersOf(size)
    listOf(
        "a list of $size from a builder, $shape" to { elements.toPersistentList() },
        "a list of $size from adds, $shape" to { elements.fold(persistentListOf<Any>()) { list, element -> list.adding(element) } },
    )
}

/** The four origins of [listBuilders], at every depth. */
private val listBuildersOfEveryDepth: List<Pair<String, () -> PersistentList.Builder<Any>>> = listDepths.flatMap { (size, shape) ->
    val elements = wrappersOf(size)
    listOf(
        "from a list of $size, $shape" to { elements.toPersistentList().builder() },
        "from adds of $size, $shape" to { persistentListOf<Any>().builder().apply { addAll(elements) } },
        "from a list of $size with a set of the first element, $shape" to { elements.toPersistentList().builder().apply { this[0] = WRITTEN } },
        "from a list of $size with a set of the last element, $shape" to { elements.toPersistentList().builder().apply { this[lastIndex] = WRITTEN } },
    )
}

private val emptiedListBuilders: List<Pair<String, () -> PersistentList.Builder<Any>>> = listOf(
    "from the empty list" to { persistentListOf<Any>().builder() },
    "emptied by remove" to { persistentListOf<Any>(7).builder().apply { remove(7) } },
    "emptied by removeAll" to { persistentListOf<Any>(7).builder().apply { removeAll(listOf(7)) } },
)

private fun emptiedSets(kind: String, empty: PersistentSet<Any>): List<Pair<String, () -> PersistentSet<Any>>> {
    val set = { empty.adding(7) }
    return listOf(
        "$kind emptied by removing" to { set().removing(7) },
        "$kind emptied by removingAll of a list" to { set().removingAll(listOf(7)) },
        "$kind emptied by removingAll of a hash set" to { set().removingAll(persistentHashSetOf(7)) },
        "$kind emptied through a builder's remove" to { set().mutate { it.remove(7) } },
        "$kind emptied through a builder's removeAll" to { set().mutate { it.removeAll(listOf(7)) } },
    )
}

private val hashSets: List<Pair<String, () -> PersistentSet<Any>>> = listOf("the empty hash set" to { persistentHashSetOf<Any>() }) +
        wrappedShapes.map { (shape, elements) -> "a hash set of $shape" to { elements.toPersistentHashSet() } }

private val orderedSets: List<Pair<String, () -> PersistentSet<Any>>> = listOf("the empty ordered set" to { persistentSetOf<Any>() }) +
        wrappedShapes.map { (shape, elements) -> "an ordered set of $shape" to { elements.toPersistentSet() } }

private fun emptiedSetBuilders(empty: PersistentSet<Any>): List<Pair<String, () -> PersistentSet.Builder<Any>>> {
    val set = empty.adding(7)
    return listOf(
        "from the empty set" to { empty.builder() },
        "emptied by remove" to { set.builder().apply { remove(7) } },
        "emptied by removeAll of a list" to { set.builder().apply { removeAll(listOf(7)) } },
        "emptied by removeAll of a hash set" to { set.builder().apply { removeAll(persistentHashSetOf(7)) } },
    )
}

private fun emptiedMaps(kind: String, empty: PersistentMap<Any, Any>): List<Pair<String, () -> PersistentMap<Any, Any>>> {
    val map = { empty.putting(7, "v") }
    return listOf(
        "$kind emptied by removing" to { map().removing(7) },
        "$kind emptied through a builder's remove" to { map().mutate { it.remove(7) } },
        "$kind emptied through a builder's keys' removeAll" to { map().mutate { it.keys.removeAll(listOf(7)) } },
    )
}

private val hashMaps: List<Pair<String, () -> PersistentMap<Any, Any>>> = listOf("the empty hash map" to { persistentHashMapOf<Any, Any>() }) +
        wrappedShapes.map { (shape, keys) -> "a hash map of $shape" to { persistentHashMapOf(*wrapperEntries(keys)) } }

private val orderedMaps: List<Pair<String, () -> PersistentMap<Any, Any>>> = listOf("the empty ordered map" to { persistentMapOf<Any, Any>() }) +
        wrappedShapes.map { (shape, keys) -> "an ordered map of $shape" to { persistentMapOf(*wrapperEntries(keys)) } }

private fun emptiedMapBuilders(empty: PersistentMap<Any, String>): List<Pair<String, () -> PersistentMap.Builder<Any, String>>> {
    val map = empty.putting(7, "v")
    return listOf(
        "from the empty map" to { empty.builder() },
        "emptied by remove" to { map.builder().apply { remove(7) } },
        "emptied by the keys' removeAll" to { map.builder().apply { keys.removeAll(listOf(7)) } },
    )
}

private fun listArguments(list: List<Any>): List<Pair<String, Collection<Any>>> = listOf(
    "nothing" to emptyList(),
    "the stored elements" to list.toList(),
    "missing elements" to listOf("missing", "absent"),
    "a superset" to list + "missing",
)

private fun listRows(list: List<Any>): List<ListCall> = buildList {
    val size = list.size
    add(ListCall("no call") { })
    for ((argument, elements) in listArguments(list)) {
        add(ListCall("adding $argument", { addingAll(elements) }) { addAll(elements) })
        add(ListCall("adding $argument at 0", { addingAllAt(0, elements) }) { addAll(0, elements) })
        add(ListCall("adding $argument at the end", { addingAllAt(size, elements) }) { addAll(size, elements) })
        add(ListCall("removing $argument", { removingAll(elements) }) { removeAll(elements) })
        add(ListCall("retaining $argument", { retainingAll(elements) }) { retainAll(elements) })
    }
    add(ListCall("retaining itself", { retainingAll(this) }) { retainAll(this) })
    add(ListCall("removing a missing element", { removing("missing") }) { remove("missing") })
    add(ListCall("removing by a predicate matching nothing", { removingAll { false } }) { removeAll { false } })
    add(ListCall("removing by a predicate matching everything", { removingAll { true } }) { removeAll { true } })
    add(ListCall("retaining by a predicate matching everything") { retainAll { true } })
    add(ListCall("retaining by a predicate matching nothing") { retainAll { false } })
    add(ListCall("cleared", { cleared() }) { clear() })
    add(ListCall("the sublist's clear") { subList(0, size).clear() })
    add(ListCall("the iterator's remove of every element") { iterator().run { while (hasNext()) { val _ = next(); remove() } } })
    add(ListCall("removing every element", { fold(this) { rest, element -> rest.removing(element) } }) { toList().all { remove(it) } })
    add(ListCall("removing every element at 0", { (0..<size).fold(this) { rest, _ -> rest.removingAt(0) } }) { while (isNotEmpty()) removeAt(0) })
    add(ListCall("removing every element at the last index", { (0..<size).fold(this) { rest, _ -> rest.removingAt(rest.lastIndex) } }) { while (isNotEmpty()) removeAt(lastIndex) })
    val everyElement = list.toList()
    add(ListCall("minus an array of every element", { this - everyElement.toTypedArray() }) { removeAll(everyElement) })
    add(ListCall("minus a sequence of every element", { this - everyElement.asSequence() }) { removeAll(everyElement) })
    add(ListCall("minus an iterable that is not a collection of every element", { this - Iterable { everyElement.iterator() } }) { removeAll(everyElement) })
    if (size > 0) for (index in boundaryIndices(size)) {
        val element = list[index]
        val next = (index + 1) % size
        val sameSlot = index xor MAX_BUFFER_SIZE
        add(ListCall("replacing at $index with the stored element", { replacingAt(index, element) }) { set(index, element) })
        add(ListCall("replacing at $index with the element at $next", { replacingAt(index, list[next]) }) { set(index, list[next]) })
        add(ListCall("replacing at $index with a new element", { replacingAt(index, NEW) }) { set(index, NEW) })
        add(ListCall("the list iterator's set at $index of the element it returned") { listIterator(index).run { set(next()) } })
        add(ListCall("removing the element at $index", { removing(element) }) { remove(element) })
        if (sameSlot < size) {
            add(ListCall("replacing at $index with the element at $sameSlot, the same slot of another leaf", { replacingAt(index, list[sameSlot]) }) { set(index, list[sameSlot]) })
        }
    }
}

/** The rows that need an equal copy of a stored element. */
private fun wrapperListRows(list: List<Any>): List<ListCall> = buildList {
    val copies = list.map { it.wrapperCopy() }
    add(ListCall("adding equal copies of the stored elements", { addingAll(copies) }) { addAll(copies) })
    add(ListCall("removing equal copies of the stored elements", { removingAll(copies) }) { removeAll(copies) })
    add(ListCall("retaining equal copies of the stored elements", { retainingAll(copies) }) { retainAll(copies) })
    if (list.isNotEmpty()) for (index in boundaryIndices(list.size)) {
        val copy = list[index].wrapperCopy()
        add(ListCall("replacing at $index with an equal element", { replacingAt(index, copy) }) { set(index, copy) })
        add(ListCall("removing an element equal to the element at $index", { removing(copy) }) { remove(copy) })
    }
}

/** As lists and as hash sets, which a hash set merges trie by trie. */
private fun setArguments(set: Set<Any>): List<Pair<String, Collection<Any>>> = listOf(
    "nothing" to emptyList(),
    "an empty hash set" to persistentHashSetOf(),
    "the stored elements as a list" to set.toList(),
    "an equal hash set" to set.toList().toPersistentHashSet(),
    "an equal ordered set" to set.toList().toPersistentSet(),
    "missing elements as a list" to listOf("missing", "absent"),
    "a hash set of missing elements" to persistentHashSetOf("missing", "absent"),
    "a hash set of missing elements with the stored hashes" to set.map(::missingWithTheHashOf).toPersistentHashSet(),
    "a superset as a list" to set + "missing",
    "a superset as a hash set" to (set + "missing").toPersistentHashSet(),
)

private fun setRows(set: Set<Any>): List<SetCall> = buildList {
    add(SetCall("no call") { })
    for ((argument, elements) in setArguments(set)) {
        add(SetCall("adding $argument", { addingAll(elements) }) { addAll(elements) })
        add(SetCall("removing $argument", { removingAll(elements) }) { removeAll(elements) })
        add(SetCall("retaining $argument", { retainingAll(elements) }) { retainAll(elements) })
    }
    add(SetCall("adding itself", { addingAll(this) }) { addAll(this) })
    add(SetCall("retaining itself", { retainingAll(this) }) { retainAll(this) })
    add(SetCall("adding the built set") { built -> addAll(built) })
    add(SetCall("removing a missing element", { removing("missing") }) { remove("missing") })
    add(SetCall("removing by a predicate matching nothing", { removingAll { false } }) { removeAll { false } })
    add(SetCall("removing by a predicate matching everything", { removingAll { true } }) { removeAll { true } })
    add(SetCall("retaining by a predicate matching everything") { retainAll { true } })
    add(SetCall("retaining by a predicate matching nothing") { retainAll { false } })
    add(SetCall("cleared", { cleared() }) { clear() })
    add(SetCall("the iterator's remove of every element") { iterator().run { while (hasNext()) { val _ = next(); remove() } } })
    add(SetCall("removing every element", { fold(this) { rest, element -> rest.removing(element) } }) { toList().all { remove(it) } })
    val everyElement = set.toList()
    add(SetCall("minus an array of every element", { this - everyElement.toTypedArray() }) { removeAll(everyElement) })
    add(SetCall("minus a sequence of every element", { this - everyElement.asSequence() }) { removeAll(everyElement) })
    add(SetCall("minus an iterable that is not a collection of every element", { this - Iterable { everyElement.iterator() } }) { removeAll(everyElement) })
    for ((position, element) in set.withIndex()) {
        val withTheHash = missingWithTheHashOf(element)
        val beside = missingBesideAtLevelOne(element)
        add(SetCall("adding the element at $position", { adding(element) }) { add(element) })
        add(SetCall("removing the element at $position", { removing(element) }) { remove(element) })
        add(SetCall("adding a missing element with the hash of the element at $position", { adding(withTheHash) }) { add(withTheHash) })
        add(SetCall("removing a missing element with the hash of the element at $position", { removing(withTheHash) }) { remove(withTheHash) })
        add(SetCall("adding a missing element beside the element at $position at level 1", { adding(beside) }) { add(beside) })
        add(SetCall("removing a missing element beside the element at $position at level 1", { removing(beside) }) { remove(beside) })
    }
}

/** The rows that need an equal copy of a stored element. */
private fun wrapperSetRows(set: Set<Any>): List<SetCall> = buildList {
    val copies = set.map { it.wrapperCopy() }.toPersistentHashSet()
    add(SetCall("adding a hash set of equal copies", { addingAll(copies) }) { addAll(copies) })
    add(SetCall("removing a hash set of equal copies", { removingAll(copies) }) { removeAll(copies) })
    add(SetCall("retaining a hash set of equal copies", { retainingAll(copies) }) { retainAll(copies) })
    for ((position, element) in set.withIndex()) {
        val copy = element.wrapperCopy()
        add(SetCall("adding an element equal to the element at $position", { adding(copy) }) { add(copy) })
        add(SetCall("removing an element equal to the element at $position", { removing(copy) }) { remove(copy) })
    }
}

/** As maps and as hash maps, which a hash map merges trie by trie. */
private fun <V> mapArguments(map: Map<Any, V>): List<Pair<String, Map<Any, V>>> = listOf(
    "nothing" to emptyMap(),
    "an empty hash map" to persistentHashMapOf(),
    "the stored entries as a map" to map.toMap(),
    "an equal hash map" to persistentHashMapOf(*map.toList().toTypedArray()),
    "an equal ordered map" to persistentMapOf(*map.toList().toTypedArray()),
    "a hash map of missing keys with the stored hashes" to persistentHashMapOf(*map.map { missingWithTheHashOf(it.key) to it.value }.toTypedArray()),
)

private fun keyArguments(map: Map<Any, *>): List<Pair<String, Collection<Any>>> = listOf(
    "no keys" to emptyList(),
    "every key" to map.keys.toList(),
    "missing keys" to listOf("missing", "absent"),
    "missing keys with the stored hashes" to map.keys.map(::missingWithTheHashOf),
    "a superset of the keys" to map.keys + "missing",
)

private fun <V> mapRows(map: Map<Any, V>): List<MapCall<V>> = buildList {
    add(MapCall("no call") { })
    for ((argument, entries) in mapArguments(map)) add(MapCall("putting $argument", { puttingAll(entries) }) { putAll(entries) })
    add(MapCall("putting itself", { puttingAll(this) }) { putAll(this) })
    add(MapCall("putting the built map") { built -> putAll(built) })
    for ((argument, argumentKeys) in keyArguments(map)) {
        add(MapCall("minus $argument", { this - argumentKeys }) { keys.removeAll(argumentKeys) })
        add(MapCall("the keys' retaining of $argument") { keys.retainAll(argumentKeys) })
    }
    add(MapCall("removing a missing key", { removing("missing") }) { remove("missing") })
    add(MapCall("the entries' removal by a predicate matching nothing") { entries.removeAll { false } })
    add(MapCall("the entries' removal by a predicate matching everything") { entries.removeAll { true } })
    add(MapCall("the entries' retaining by a predicate matching everything") { entries.retainAll { true } })
    add(MapCall("the entries' retaining by a predicate matching nothing") { entries.retainAll { false } })
    add(MapCall("cleared", { cleared() }) { clear() })
    add(MapCall("the values' clear") { values.clear() })
    add(MapCall("the entries' iterator's remove of every entry") { entries.iterator().run { while (hasNext()) { val _ = next(); remove() } } })
    add(MapCall("removing every key", { keys.fold(this) { rest, key -> rest.removing(key) } }) { keys.toList().all { remove(it) != null } })
    add(MapCall("removing every key with its value", { entries.fold(this) { rest, (key, value) -> rest.removing(key, value) } }) { toList().all { (key, value) -> remove(key, value) } })
    val everyKey = map.keys.toList()
    add(MapCall("minus an array of every key", { this - everyKey.toTypedArray() }) { keys.removeAll(everyKey) })
    add(MapCall("minus a sequence of every key", { this - everyKey.asSequence() }) { keys.removeAll(everyKey) })
    add(MapCall("minus an iterable that is not a collection of every key", { this - Iterable { everyKey.iterator() } }) { keys.removeAll(everyKey) })
    for ((position, entry) in map.entries.withIndex()) {
        val (key, value) = entry
        val other = (position + 1) % map.size
        val otherValue = map.values.elementAt(other)
        val withTheHash = missingWithTheHashOf(key)
        val beside = missingBesideAtLevelOne(key)
        add(MapCall("putting the stored value under the key at $position", { putting(key, value) }) { put(key, value) })
        add(MapCall("removing the key at $position", { removing(key) }) { remove(key) })
        add(MapCall("removing the key at $position with the value of the key at $other", { removing(key, otherValue) }) { remove(key, otherValue) })
        add(MapCall("removing a missing key with the value of the key at $position", { removing("missing", value) }) { remove("missing", value) })
        add(MapCall("removing a missing key with the hash of the key at $position", { removing(withTheHash) }) { remove(withTheHash) })
        add(MapCall("removing a missing key beside the key at $position at level 1", { removing(beside) }) { remove(beside) })
    }
}

/** The rows that need an equal copy of a stored key or value. */
private fun wrapperMapRows(map: Map<Any, Any>): List<MapCall<Any>> = buildList {
    val underEqualKeys = persistentHashMapOf(*map.map { it.key.wrapperCopy() to it.value }.toTypedArray())
    add(MapCall("putting a hash map of the stored values under equal keys", { puttingAll(underEqualKeys) }) { putAll(underEqualKeys) })
    for ((position, entry) in map.entries.withIndex()) {
        val (key, value) = entry
        val equalKey = key.wrapperCopy()
        val equalValue = value.wrapperCopy()
        val underAnEqualKey = persistentHashMapOf(equalKey to value)
        add(MapCall("putting the stored value under a key equal to the key at $position", { putting(equalKey, value) }) { put(equalKey, value) })
        add(MapCall("putting a hash map of the stored value under a key equal to the key at $position", { puttingAll(underAnEqualKey) }) { putAll(underAnEqualKey) })
        add(MapCall("putting an equal value under the key at $position", { putting(key, equalValue) }) { put(key, equalValue) })
        add(MapCall("putting an equal value under a key equal to the key at $position", { putting(equalKey, equalValue) }) { put(equalKey, equalValue) })
        add(MapCall("putting a new value under the key at $position", { putting(key, NEW) }) { put(key, NEW) })
        add(MapCall("removing a key equal to the key at $position", { removing(equalKey) }) { remove(equalKey) })
        add(MapCall("removing the key at $position with an equal value", { removing(key, equalValue) }) { remove(key, equalValue) })
    }
}

/** [stored] and [written] box or make the value anew on every call; [changes] tells whether the write is stored. */
private class ValueWrite(val description: String, val changes: Boolean, val stored: () -> Any, val written: () -> Any)

/** A Number that is no primitive box: equal by [value], and written over an equal instance as any object. */
private class ValueNumber(private val value: Int) : Number() {
    override fun equals(other: Any?): Boolean = other is ValueNumber && other.value == value
    override fun hashCode(): Int = value
    override fun toString(): String = "ValueNumber($value)"
    override fun toByte(): Byte = value.toByte()
    override fun toDouble(): Double = value.toDouble()
    override fun toFloat(): Float = value.toFloat()
    override fun toInt(): Int = value
    override fun toLong(): Long = value.toLong()
    override fun toShort(): Short = value.toShort()
}

private val valueWrites = listOf(
    ValueWrite("-0.0 over 0.0", changes = true, { 0.0 }, { -0.0 }),
    ValueWrite("0.0 over -0.0", changes = true, { -0.0 }, { 0.0 }),
    ValueWrite("NaN over NaN", changes = false, { Double.NaN }, { Double.NaN }),
    ValueWrite("an equal Double", changes = false, { 1.5 }, { 1.5 }),
    ValueWrite("an equal Int", changes = false, { 1000 }, { 1000 }),
    ValueWrite("an equal Long", changes = false, { 1000L }, { 1000L }),
    ValueWrite("an equal Float", changes = false, { 1.5f }, { 1.5f }),
    ValueWrite("an equal Short", changes = false, { 1000.toShort() }, { 1000.toShort() }),
    ValueWrite("an equal Byte", changes = false, { 1.toByte() }, { 1.toByte() }),
    ValueWrite("an equal Char", changes = false, { '€' }, { '€' }),
    ValueWrite("an equal UInt", changes = false, { 1u }, { 1u }),
    ValueWrite("an equal ULong", changes = false, { 1uL }, { 1uL }),
    ValueWrite("an equal UByte", changes = false, { 1.toUByte() }, { 1.toUByte() }),
    ValueWrite("an equal UShort", changes = false, { 1.toUShort() }, { 1.toUShort() }),
    ValueWrite("an equal Number object", changes = true, { ValueNumber(1) }, { ValueNumber(1) }),
    ValueNumber(1).let { same -> ValueWrite("the stored Number object", changes = false, { same }, { same }) },
)

private fun <C : Any> ValueWrite.assertResult(receiver: C, result: C, written: Any, read: C.() -> Any?) {
    if (changes) {
        assertNotSame(receiver, result)
        assertSameValue(written, result.read())
    } else {
        assertSame(receiver, result)
    }
}

/** A Double by its bits: only they tell -0.0 from 0.0 on every platform, and on Kotlin/JS NaN is not === itself. */
private fun assertSameValue(expected: Any?, actual: Any?) {
    if (expected is Double && actual is Double) assertEquals(expected.toRawBits(), actual.toRawBits()) else assertSame(expected, actual)
}

/**
 * Makes every write of [valueWrites] under every key of every trie shape: by putting, by puttingAll of a hash map of
 * that key and of every key, which a hash map merges trie by trie, and the same through a builder, whose put returns
 * the stored value and whose live iterator survives a write that changes nothing. An entry's setValue of an equal
 * value is skipped when [setValueOfAnEqualValueWrites], as the ordered map builder's still writes the slot (#366).
 */
private fun Cells.testValueWrites(
    kind: String,
    setValueOfAnEqualValueWrites: Boolean,
    mapOf: (List<Pair<Any, Any>>) -> PersistentMap<Any, Any>,
) {
    for ((shape, keys) in trieShapes) for ((position, key) in keys.withIndex()) for (write in valueWrites) {
        val stored = write.stored()
        val pairs = keys.mapIndexed { index, shapeKey -> shapeKey to if (index == position) stored else IntWrapper(index, index) }
        val name = "$kind of $shape, ${write.description} under the key at $position"
        val map = prepared(name) { mapOf(pairs) } ?: continue
        val arguments = listOf<Pair<String, (written: Any) -> Map<Any, Any>>>(
            "a hash map of the key" to { persistentHashMapOf(key to it) },
            "a hash map of every key" to { written ->
                persistentHashMapOf(*pairs.mapIndexed { index, pair -> if (index == position) key to written else pair }.toTypedArray())
            },
        )

        cell("$name by putting") {
            val written = write.written()
            write.assertResult(map, map.putting(key, written), written) { this[key] }
        }
        for ((argument, argumentWith) in arguments) cell("$name by puttingAll of $argument") {
            val written = write.written()
            write.assertResult(map, map.puttingAll(argumentWith(written)), written) { this[key] }
        }

        fun builderCell(call: String, makeCall: PersistentMap.Builder<Any, Any>.(written: Any) -> Unit) = cell("$name by $call") {
            val written = write.written()
            val builder = map.builder()
            val iterator = builder.entries.iterator()
            val _ = iterator.next()
            builder.makeCall(written)
            write.assertResult(map, builder.build(), written) { this[key] }
            if (!write.changes) {
                val _ = iterator.next()
            }
        }
        builderCell("a builder's put") { assertSameValue(stored, put(key, it)) }
        for ((argument, argumentWith) in arguments) builderCell("a builder's putAll of $argument") { putAll(argumentWith(it)) }
        if (write.changes || !setValueOfAnEqualValueWrites) {
            builderCell("an entry's setValue") { written -> assertSameValue(stored, entries.first { it.key == key }.setValue(written)) }
        }
    }
}

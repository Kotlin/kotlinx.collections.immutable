/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.implementations.immutableMap.PersistentHashMap
import kotlinx.collections.immutable.implementations.immutableMap.PersistentHashMapBuilder
import kotlinx.collections.immutable.implementations.immutableSet.PersistentHashSet
import kotlinx.collections.immutable.implementations.immutableSet.PersistentHashSetBuilder
import kotlinx.collections.immutable.implementations.immutableSet.TrieNode
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.toPersistentHashMap
import kotlinx.collections.immutable.toPersistentHashSet
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.collections.immutable.toPersistentSet
import tests.IntWrapper
import tests.ObjectWrapper
import tests.trie.*
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

/**
 * A bulk merge of two hash tries keeps the instance the receiver stores of a key or element the argument holds too,
 * stores the argument's instance of one only the argument holds and leaves both inputs as they were; putAll takes the
 * argument's value. The ordered map and set, which merge key by key, keep it too. A persistent puttingAll or addingAll
 * whose result is the argument's trie returns that trie, unless the receiver's is the result too; retainingAll shares
 * the argument's bottom node and rebuilds the levels above it.
 */
class StoredInstanceTest {

    @Test
    fun hashMap() = runCells {
        testPutAll({ it.toPersistentHashMap() }, ::hashMapBuildersOf)
        for (merge in putAllsKeepingTheArgument) {
            val (receiver, argument) = merge.entries()
            cell("${merge.description}, puttingAll keeps the argument's trie") {
                val argumentMap = argument.toPersistentHashMap()
                assertSame(argumentMap.trie, receiver.toPersistentHashMap().puttingAll(argumentMap).trie)
            }
            for ((origin, newBuilder) in hashMapBuildersOf(receiver.toList())) cell("${merge.description}, putAll on a builder $origin keeps the argument's trie") {
                val builder = newBuilder()
                val argumentMap = argument.toPersistentHashMap()
                builder.putAll(argumentMap)
                assertSame(argumentMap.trie, builder.trie)
            }
        }
        for (merge in mergesMovingTheSizeCounter) {
            val (receiver, argument) = merge.entries()
            for ((origin, newBuilder) in hashMapBuildersOf(receiver.toList())) cell("${merge.description}, putAll on a builder $origin moves sizeModCount with the size") {
                val builder = newBuilder()
                val sizeModCount = builder.sizeModCount
                builder.putAll(argument.toPersistentHashMap())
                assertEquals(builder.size != receiver.size, builder.sizeModCount != sizeModCount)
            }
        }
    }

    @Test
    fun hashSet() = runCells {
        testSetOperations({ it.toPersistentHashSet() }, ::hashSetBuildersOf)
        for (merge in addAllsKeepingTheArgument) cell("${merge.description}, addingAll keeps the argument's trie") {
            val argument = merge.argument.toPersistentHashSet()
            assertSame(argument.trie, merge.receiver.toPersistentHashSet().addingAll(argument).trie)
        }
        for (merge in retainAllsKeepingTheArgument) cell("${merge.description}, retainingAll keeps the argument's bottom node") {
            val argument = merge.argument.toPersistentHashSet()
            assertSame(argument.trie.bottom(), merge.receiver.toPersistentHashSet().retainingAll(argument).trie.bottom())
        }
        for (merge in mergesMovingTheSizeCounter) {
            for ((origin, newBuilder) in hashSetBuildersOf(merge.receiver)) cell("${merge.description}, addAll on a builder $origin moves modCount with the size") {
                val builder = newBuilder()
                val modCount = builder.modCount
                builder.addAll(merge.argument.toPersistentHashSet())
                assertEquals(builder.size != merge.receiver.size, builder.modCount != modCount)
            }
        }
    }

    @Test
    fun orderedMap() = runCells { testPutAll({ it.toPersistentMap() }, ::orderedMapBuildersOf) }

    @Test
    fun orderedSet() = runCells { testSetOperations({ it.toPersistentSet() }, ::orderedSetBuildersOf) }

    @Test
    fun `putAll takes the values of an argument builder and shares no storage with it`() = runCells {
        for ((origin, newBuilder) in hashMapBuildersOf(listOf(collidingKey1 to 1, collidingKey2 to 2))) {
            for ((argumentOrigin, newArgument) in hashMapBuildersOf(listOf(collidingKey1 to 10, collidingKey2 to 20))) {
                for (argumentWritesFirst in listOf(false, true)) cell("a builder $origin, an argument builder $argumentOrigin, ${if (argumentWritesFirst) "the argument" else "the builder"} writes first") {
                    val builder = newBuilder()
                    val argument = newArgument()
                    builder.putAll(argument)
                    assertEquals(2, builder.size)

                    if (argumentWritesFirst) argument[collidingKey2] = 200
                    builder[collidingKey1] = 100
                    if (!argumentWritesFirst) argument[collidingKey2] = 200

                    assertEquals(persistentHashMapOf(collidingKey1 to 100, collidingKey2 to 20), builder.build())
                    assertEquals(persistentHashMapOf(collidingKey1 to 10, collidingKey2 to 200), argument.build())
                }
            }
        }
    }

    @Test
    fun `addAll and retainAll share no storage with an argument builder`() = runCells {
        for (call in sharingCalls) {
            for ((origin, newBuilder) in hashSetBuildersOf(call.receiver)) {
                for ((argumentOrigin, newArgument) in hashSetBuildersOf(call.argument)) {
                    for (argumentWritesFirst in listOf(false, true)) cell("${call.name}, a builder $origin, an argument builder $argumentOrigin, ${if (argumentWritesFirst) "the argument" else "the builder"} writes first") {
                        val builder = newBuilder()
                        val argument = newArgument()
                        assertEquals(true, call.call(builder, argument), "the result")

                        if (argumentWritesFirst) assertEquals(true, call.argumentWrite(argument), "the argument's write")
                        assertEquals(true, builder.remove(collidingKey1), "the builder's write")
                        if (!argumentWritesFirst) assertEquals(true, call.argumentWrite(argument), "the argument's write")

                        assertHolds(call.builderResult, builder)
                        assertHolds(call.argumentResult, argument)
                    }
                }
            }
        }
    }
}

/** A call whose result shares the argument's collision node, a later write to the argument, and what each holds then; the builder also removes collidingKey1. */
private class SharingCall(
    val name: String,
    val receiver: List<IntWrapper>,
    val argument: List<IntWrapper>,
    val call: PersistentSet.Builder<IntWrapper>.(PersistentSet.Builder<IntWrapper>) -> Boolean,
    val argumentWrite: PersistentSet.Builder<IntWrapper>.() -> Boolean,
    val builderResult: List<IntWrapper>,
    val argumentResult: List<IntWrapper>,
)

private val sharingCalls: List<SharingCall> = listOf(
    SharingCall("addAll", listOf(collidingKey1, collidingKey2), listOf(collidingKey1, collidingKey2, collidingKey3), { addAll(it) }, { remove(collidingKey3) }, listOf(collidingKey2, collidingKey3), listOf(collidingKey1, collidingKey2)),
    SharingCall("retainAll", listOf(collidingKey1, collidingKey2, collidingKey3), listOf(collidingKey1, collidingKey2), { retainAll(it) }, { add(collidingKey3) }, listOf(collidingKey2), listOf(collidingKey1, collidingKey2, collidingKey3)),
)

private class Merge(val description: String, val receiver: List<IntWrapper?>, val argument: List<IntWrapper?>)

/** Follows collidingKey1's path and parts from it at the last level, into cell 30, where a sign error in a mask shows. */
private val negativeHashKey = IntWrapper(11, Int.MIN_VALUE)

private val tries: List<Pair<String, List<IntWrapper?>>> = listOf(
    "collidingKey1 alone" to listOf(collidingKey1),
    "levelOneSibling alone" to listOf(levelOneSibling),
    "two root entries" to listOf(collidingKey1, rootSibling),
    "a level-1 node" to listOf(collidingKey1, levelOneSibling),
    "a level-1 node beside a root entry" to listOf(collidingKey1, levelOneSibling, rootSibling),
    "a level-1 node without collidingKey1" to listOf(levelOneSibling, otherLevelOneSibling),
    "a level-1 node holding levelTwoSibling" to listOf(levelTwoSibling, levelOneSibling),
    "a level-2 node" to listOf(collidingKey1, levelTwoSibling),
    "a last-level node" to listOf(collidingKey1, lastLevelSibling),
    "a last-level node holding collidingKey3" to listOf(collidingKey3, lastLevelSibling),
    "a collision node" to listOf(collidingKey1, collidingKey2),
    "a collision node of three" to listOf(collidingKey1, collidingKey2, collidingKey3),
    "a collision node beside a last-level sibling" to listOf(collidingKey1, collidingKey2, lastLevelSibling),
    "collidingKey3 beside a root entry" to listOf(collidingKey3, rootSibling),
    "otherLevelOneSibling beside a root entry" to listOf(otherLevelOneSibling, rootSibling),
    "a negative hash alone" to listOf(negativeHashKey),
    "a last-level node holding a negative hash" to listOf(collidingKey1, negativeHashKey),
)

private val putAllsKeepingTheArgument: List<Merge> = listOf(
    Merge("a subtree holding the stored key", listOf(collidingKey1), listOf(collidingKey1, levelOneSibling)),
    Merge("a collision node holding the stored key", listOf(collidingKey1), listOf(collidingKey1, collidingKey2)),
)

private val addAllsKeepingTheArgument: List<Merge> = listOf(
    Merge("a subtree holding the stored element", listOf(collidingKey1), listOf(collidingKey1, levelOneSibling)),
    Merge("a collision node holding the stored element", listOf(collidingKey1), listOf(collidingKey1, collidingKey2)),
    Merge("a collision node holding every stored element", listOf(collidingKey1, collidingKey2), listOf(collidingKey1, collidingKey2, collidingKey3)),
)

private val retainAllsKeepingTheArgument: List<Merge> = listOf(
    Merge("a root entry of a subtree", listOf(collidingKey1, levelOneSibling), listOf(collidingKey1)),
    Merge("a collision node of the stored elements", listOf(collidingKey1, collidingKey2, collidingKey3), listOf(collidingKey1, collidingKey2)),
)

/** A merge for each path that adds keys, and one that adds none. */
private val mergesMovingTheSizeCounter: List<Merge> = listOf(
    Merge("new values for every key", listOf(collidingKey1, collidingKey2, lastLevelSibling), listOf(collidingKey1, collidingKey2, lastLevelSibling)),
    Merge("an entry merged into the argument's subtree", listOf(collidingKey1), listOf(collidingKey1.copy(), levelOneSibling)),
    Merge("a disjoint subtree", listOf(collidingKey1), listOf(levelOneSibling, otherLevelOneSibling)),
    Merge("keys added to a collision node", listOf(collidingKey1, lastLevelSibling), listOf(collidingKey2, collidingKey3)),
    Merge("a key added to the receiver's subtree", listOf(collidingKey1, levelOneSibling), listOf(otherLevelOneSibling)),
)

/** Collision nodes sharing one very instance and one equal copy, in either order: the argument's node is shared only when every match is the very instance. */
private val mixedInstanceCollisionMerges: List<Merge> = listOf(
    Merge("a copy, the stored instance and a new element into a collision node", listOf(collidingKey1, collidingKey2), listOf(collidingKey2.copy(), collidingKey1, collidingKey3)),
    Merge("the stored instance and a copy into a collision node of three", listOf(collidingKey2, collidingKey1, collidingKey3), listOf(collidingKey1, collidingKey2.copy())),
)

/** null hashes to 0 like collidingKey1; a lookup must tell a stored null from absence. */
private val nullMerges: List<Merge> = listOf(
    Merge("null alone into a level-1 node holding null", listOf(null), listOf(null, levelOneSibling)),
    Merge("a level-1 node holding null", listOf(null, levelOneSibling), listOf(null)),
    Merge("a collision node holding null", listOf(null, collidingKey1), listOf(null)),
    Merge("a collision node holding null beside a last-level sibling", listOf(null, collidingKey1), listOf(null, lastLevelSibling)),
    Merge("null alone into a level-1 node without null", listOf(null), listOf(collidingKey1, levelOneSibling)),
    Merge("a last-level node holding null into a collision node", listOf(null, lastLevelSibling), listOf(collidingKey1, collidingKey2)),
)

/** Keys reaching every kind of node; [randomTrieKeys] hands their hashes out by id modulo seven, so the ids id and id + 7 collide. */
private val trieLevelKeys: List<IntWrapper> = listOf(
    collidingKey1, rootSibling,
    levelOneSibling, rootSibling.siblingAt(level = 1, id = 9),
    levelTwoSibling,
    lastLevelSibling, levelOneSibling.siblingAt(level = LAST_LEVEL, id = 10),
)

private fun randomTrieKeys(random: Random, ids: IntRange): Map<Int, IntWrapper> =
    ids.filter { random.nextBoolean() }.associateWith { trieLevelKeys[it % trieLevelKeys.size].colliding(it) }

/** Random merges whose two sides hold equal copies, never the same instances. */
private fun randomMerges(seed: Int, ids: IntRange): List<Merge> {
    val random = Random(seed)
    return List(200) { iteration ->
        val receiver = randomTrieKeys(random, ids)
        val argument = randomTrieKeys(random, ids)
        Merge("random $iteration of seed $seed, receiver ids ${receiver.keys}, argument ids ${argument.keys}", receiver.values.toList(), argument.values.toList())
    }
}

private val merges: List<Merge> = run {
    val product = tries.flatMap { (receiverShape, receiver) ->
        tries.flatMap { (argumentShape, argument) ->
            listOf(
                Merge("receiver $receiverShape, argument $argumentShape", receiver, argument),
                Merge("receiver $receiverShape, argument $argumentShape of equal copies", receiver, argument.map { it?.copy() }),
            )
        }
    }
    val tables = (addAllsKeepingTheArgument + retainAllsKeepingTheArgument + mergesMovingTheSizeCounter).filter { row -> product.none { it.sameKeysAs(row) } }
    product + tables + mixedInstanceCollisionMerges + nullMerges +
            randomMerges(313, 0..<14) + randomMerges(316, 0..<14) + randomMerges(322, 0..<21)
}

private fun Merge.sameKeysAs(other: Merge): Boolean = receiver.sameInstancesAs(other.receiver) && argument.sameInstancesAs(other.argument)

private fun List<IntWrapper?>.sameInstancesAs(other: List<IntWrapper?>): Boolean = size == other.size && indices.all { this[it] === other[it] }

/** An object on every platform; JS compares strings by content. */
private typealias Value = ObjectWrapper<String>

/** The argument's value for a key the receiver holds, from the receiver's; null for a new one. */
private val argumentValues: List<Pair<String, (stored: Value?) -> Value?>> = listOf(
    "new values" to { null },
    "the stored values" to { it },
    "equal copies of the stored values" to { it?.copy() },
)

private fun Merge.entries(shared: (stored: Value?) -> Value? = { null }): Pair<Map<IntWrapper?, Value>, Map<IntWrapper?, Value>> {
    val receiverEntries = receiver.associateWith { Value("r${it?.obj}", 0) }
    return receiverEntries to argument.associateWith { shared(receiverEntries[it]) ?: Value("a${it?.obj}", 0) }
}

private fun Cells.testPutAll(
    newMap: (Map<IntWrapper?, Value>) -> PersistentMap<IntWrapper?, Value>,
    buildersOf: (List<Pair<IntWrapper?, Value>>) -> List<Pair<String, () -> PersistentMap.Builder<IntWrapper?, Value>>>,
) {
    for (merge in merges) for ((values, shared) in argumentValues) {
        val (receiver, argument) = merge.entries(shared)
        val expected = (receiver.keys + argument.keys).associateWith { argument[it] ?: receiver.getValue(it) }
        cell("${merge.description}, $values, puttingAll") {
            val receiverMap = newMap(receiver)
            val argumentMap = argument.toPersistentHashMap()
            assertHolds(expected, receiverMap.puttingAll(argumentMap))
            assertHolds(receiver, receiverMap)
            assertHolds(argument, argumentMap)
        }
        for ((origin, newBuilder) in buildersOf(receiver.toList())) cell("${merge.description}, $values, putAll on a builder $origin") {
            val builder = newBuilder()
            val argumentMap = argument.toPersistentHashMap()
            builder.putAll(argumentMap)
            assertHolds(expected, builder)
            assertHolds(argument, argumentMap)
        }
    }
}

private class SetOperation(
    val persistentName: String,
    val builderName: String,
    val persistent: PersistentSet<IntWrapper?>.(PersistentSet<IntWrapper?>) -> PersistentSet<IntWrapper?>,
    val builder: PersistentSet.Builder<IntWrapper?>.(PersistentSet<IntWrapper?>) -> Boolean,
    val result: (receiver: List<IntWrapper?>, argument: List<IntWrapper?>) -> List<IntWrapper?>,
)

private val setOperations: List<SetOperation> = listOf(
    SetOperation("addingAll", "addAll", { addingAll(it) }, { addAll(it) }) { receiver, argument -> receiver + argument.filter { it !in receiver } },
    SetOperation("retainingAll", "retainAll", { retainingAll(it) }, { retainAll(it) }) { receiver, argument -> receiver.filter { it in argument } },
    SetOperation("removingAll", "removeAll", { removingAll(it) }, { removeAll(it) }) { receiver, argument -> receiver.filter { it !in argument } },
)

private fun Cells.testSetOperations(
    newSet: (List<IntWrapper?>) -> PersistentSet<IntWrapper?>,
    buildersOf: (List<IntWrapper?>) -> List<Pair<String, () -> PersistentSet.Builder<IntWrapper?>>>,
) {
    for (merge in merges) for (operation in setOperations) {
        val expected = operation.result(merge.receiver, merge.argument)
        cell("${merge.description}, ${operation.persistentName}") {
            val receiver = newSet(merge.receiver)
            val argument = merge.argument.toPersistentHashSet()
            assertHolds(expected, operation.persistent(receiver, argument))
            assertHolds(merge.receiver, receiver)
            assertHolds(merge.argument, argument)
        }
        for ((origin, newBuilder) in buildersOf(merge.receiver)) cell("${merge.description}, ${operation.builderName} on a builder $origin") {
            val builder = newBuilder()
            val argument = merge.argument.toPersistentHashSet()
            assertEquals(expected.size != merge.receiver.size, operation.builder(builder, argument), "the result")
            assertHolds(expected, builder)
            assertHolds(merge.argument, argument)
        }
    }
}

/** The very instances of [expected]'s keys and values, and no other entries. */
private fun assertHolds(expected: Map<IntWrapper?, Value>, map: Map<IntWrapper?, Value>) {
    assertEquals(expected.size, map.size, "the size")
    val stored = map.keys.groupBy { it }
    for ((key, value) in expected) {
        assertSame(key, stored.getValue(key).single(), "the instance of the key ${key?.obj}")
        assertSame(value, map[key], "the value of the key ${key?.obj}")
    }
}

/** The very instances of [expected], and no other elements. */
private fun assertHolds(expected: List<IntWrapper?>, set: Set<IntWrapper?>) {
    assertEquals(expected.size, set.size, "the size")
    val stored = set.groupBy { it }
    for (element in expected) assertSame(element, stored.getValue(element).single(), "the instance of the element ${element?.obj}")
}

private val Map<*, *>.trie: Any
    get() = if (this is PersistentHashMap<*, *>) node else (this as PersistentHashMapBuilder<*, *>).node

private val Set<*>.trie: TrieNode<*>
    get() = (this as PersistentHashSet<*>).node

/** The node reached through the first cells: a collision node, or the one holding the first element. */
private fun TrieNode<*>.bottom(): TrieNode<*> = (buffer[0] as? TrieNode<*>)?.bottom() ?: this

private val PersistentMap.Builder<*, *>.sizeModCount: Int
    get() = (this as PersistentHashMapBuilder<*, *>).sizeModCount

private val PersistentSet.Builder<*>.modCount: Int
    get() = (this as PersistentHashSetBuilder<*>).modCount

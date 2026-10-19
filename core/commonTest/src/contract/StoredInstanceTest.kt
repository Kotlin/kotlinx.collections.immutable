/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract

import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.implementations.immutableMap.PersistentHashMap
import kotlinx.collections.immutable.implementations.immutableSet.PersistentHashSet
import kotlinx.collections.immutable.implementations.immutableSet.TrieNode
import kotlinx.collections.immutable.persistentHashSetOf
import kotlinx.collections.immutable.toPersistentHashMap
import kotlinx.collections.immutable.toPersistentHashSet
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.collections.immutable.toPersistentSet
import tests.IntWrapper
import tests.equalCopy
import tests.trie.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * A bulk merge keeps the instance the receiver stores of a key or element the argument holds too, stores the argument's
 * instance of one only the argument holds and leaves both inputs as they were; putAll takes the argument's value. A
 * persistent puttingAll or addingAll whose result equals the argument returns the argument's trie, and such a
 * retainingAll shares the argument's bottom node. A builder merged with another builder shares no storage with it.
 *
 * A new trie is a row of [tries], which [merges] crosses with every other one; a merge of two fixed tries is a [Merge]
 * of its own in [merges].
 */
class StoredInstanceTest {

    @Test
    fun hashMap() = runCells { testPutAll({ it.toPersistentHashMap() }, ::hashMapBuildersOf) }

    @Test
    fun orderedMap() = runCells { testPutAll({ it.toPersistentMap() }, ::orderedMapBuildersOf) }

    @Test
    fun hashSet() = runCells { testBulkCalls({ it.toPersistentHashSet() }, ::hashSetBuildersOf) }

    @Test
    fun orderedSet() = runCells { testBulkCalls({ it.toPersistentSet() }, ::orderedSetBuildersOf) }

    @Test
    fun `puttingAll and addingAll return the argument's trie`() = runCells {
        for (merge in unionsEqualToTheArgument) {
            cell("${merge.description}, puttingAll") {
                val receiver = merge.receiverEntries.toPersistentHashMap()
                val argument = merge.argumentEntries.toPersistentHashMap()
                assertSame(argument.trie, receiver.puttingAll(argument).trie)
            }
            cell("${merge.description}, addingAll") {
                val argument = merge.argument.toPersistentHashSet()
                assertSame(argument.trie, merge.receiver.toPersistentHashSet().addingAll(argument).trie)
            }
        }
    }

    @Test
    fun `retainingAll shares the argument's bottom node`() = runCells {
        for (merge in intersectionsEqualToTheArgument) cell(merge.description) {
            val argument = merge.argument.toPersistentHashSet()
            assertSame(argument.trie.bottom(), merge.receiver.toPersistentHashSet().retainingAll(argument).trie.bottom())
        }
    }

    @Test
    fun `addAll of an argument builder shares no storage with it`() =
        forEachBuilder(hashSetBuildersOf(listOf(collidingKey1, collidingKey2))) { argument ->
            val builder = persistentHashSetOf(collidingKey1).builder()
            assertTrue(builder.addAll(argument))
            assertTrue(argument.add(collidingKey3))
            assertTrue(builder.remove(collidingKey1))
            assertEquals(setOf(collidingKey2), builder)
            assertEquals(setOf(collidingKey1, collidingKey2, collidingKey3), argument)
        }

    @Test
    fun `retainAll of an argument builder shares no storage with it`() =
        forEachBuilder(hashSetBuildersOf(listOf(collidingKey1, collidingKey2))) { argument ->
            val builder = persistentHashSetOf(collidingKey1, collidingKey2, collidingKey3).builder()
            assertTrue(builder.retainAll(argument))
            assertTrue(argument.add(collidingKey3))
            assertTrue(builder.remove(collidingKey1))
            assertEquals(setOf(collidingKey2), builder)
            assertEquals(setOf(collidingKey1, collidingKey2, collidingKey3), argument)
        }
}

/** The two sides of a merge; as entries, their values `r<id>` and `a<id>` name the side each value came from. */
private class Merge(val description: String, val receiver: List<IntWrapper?>, val argument: List<IntWrapper?>) {
    val receiverEntries: Map<IntWrapper?, String> get() = receiver.associateWith { "r${it?.obj}" }
    val argumentEntries: Map<IntWrapper?, String> get() = argument.associateWith { "a${it?.obj}" }
}

/** Follows collidingKey1's path and parts from it at the last level, into cell 30, where a sign error in a mask shows. */
private val negativeHashKey = IntWrapper(11, Int.MIN_VALUE)

/** The tries on either side of a merge; null hashes to 0 like collidingKey1, so a lookup must tell a stored null from absence. */
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
    "null alone" to listOf(null),
    "a level-1 node holding null" to listOf(null, levelOneSibling),
    "a collision node holding null" to listOf(null, collidingKey1),
    "a last-level node holding null" to listOf(null, lastLevelSibling),
)

/** Every pair of tries, the argument holding the receiver's instances or equal copies, and collision nodes mixing the two. */
private val merges: List<Merge> = tries.flatMap { (receiverShape, receiver) ->
    tries.flatMap { (argumentShape, argument) ->
        listOf(
            Merge("receiver $receiverShape, argument $argumentShape", receiver, argument),
            Merge("receiver $receiverShape, argument $argumentShape of equal copies", receiver, argument.map { it.equalCopy() }),
        )
    }
} + listOf(
    Merge(
        "a copy, the stored instance and a new element into a collision node",
        receiver = listOf(collidingKey1, collidingKey2),
        argument = listOf(collidingKey2.copy(), collidingKey1, collidingKey3),
    ),
    Merge(
        "the stored instance and a copy into a collision node of three",
        receiver = listOf(collidingKey2, collidingKey1, collidingKey3),
        argument = listOf(collidingKey1, collidingKey2.copy()),
    ),
)

/** Arguments holding the receiver's keys and more, so that with the argument's values the union equals the argument. */
private val unionsEqualToTheArgument: List<Merge> = listOf(
    Merge("a subtree holding the stored key", listOf(collidingKey1), listOf(collidingKey1, levelOneSibling)),
    Merge("a collision node holding the stored key", listOf(collidingKey1), listOf(collidingKey1, collidingKey2)),
    Merge("a collision node holding every stored key", listOf(collidingKey1, collidingKey2), listOf(collidingKey1, collidingKey2, collidingKey3)),
)

/** Receivers holding the argument's elements and more, so that the intersection equals the argument. */
private val intersectionsEqualToTheArgument: List<Merge> = listOf(
    Merge("a root entry of a subtree", listOf(collidingKey1, levelOneSibling), listOf(collidingKey1)),
    Merge("a collision node of the stored elements", listOf(collidingKey1, collidingKey2, collidingKey3), listOf(collidingKey1, collidingKey2)),
)

/** The expected entries are stdlib's `receiver + argument`: the receiver's key instances with the argument's values. */
private fun Cells.testPutAll(
    newMap: (Map<IntWrapper?, String>) -> PersistentMap<IntWrapper?, String>,
    buildersOf: (List<Pair<IntWrapper?, String>>) -> Origins<PersistentMap.Builder<IntWrapper?, String>>,
) {
    for (merge in merges) {
        val receiver = merge.receiverEntries
        val argument = merge.argumentEntries
        val expected = receiver + argument
        cell("${merge.description}, puttingAll") {
            val receiverMap = newMap(receiver)
            val argumentMap = argument.toPersistentHashMap()
            assertHolds(expected, receiverMap.puttingAll(argumentMap), "the result")
            assertHolds(receiver, receiverMap, "the receiver after the call")
            assertHolds(argument, argumentMap, "the argument after the call")
        }
        for ((origin, newBuilder) in buildersOf(receiver.toList())) cell("${merge.description}, putAll on $origin") {
            val builder = newBuilder()
            val argumentMap = argument.toPersistentHashMap()
            builder.putAll(argumentMap)
            assertHolds(expected, builder, "the builder after the call")
            assertHolds(argument, argumentMap, "the argument after the call")
        }
    }
}

/** The expected set and the Boolean come from the same call on a stdlib set, which keeps the receiver's instances. */
private fun Cells.testBulkCalls(
    newSet: (List<IntWrapper?>) -> PersistentSet<IntWrapper?>,
    buildersOf: (List<IntWrapper?>) -> Origins<PersistentSet.Builder<IntWrapper?>>,
) {
    fun testCall(
        persistentName: String,
        builderName: String,
        persistent: PersistentSet<IntWrapper?>.(PersistentSet<IntWrapper?>) -> PersistentSet<IntWrapper?>,
        mutable: MutableSet<IntWrapper?>.(Collection<IntWrapper?>) -> Boolean,
    ) {
        for (merge in merges) {
            val model = merge.receiver.toMutableSet()
            val changed = model.mutable(merge.argument)
            cell("${merge.description}, $persistentName") {
                val receiver = newSet(merge.receiver)
                val argument = merge.argument.toPersistentHashSet()
                assertHolds(model, receiver.persistent(argument), "the result")
                assertHolds(merge.receiver, receiver, "the receiver after the call")
                assertHolds(merge.argument, argument, "the argument after the call")
            }
            for ((origin, newBuilder) in buildersOf(merge.receiver)) cell("${merge.description}, $builderName on $origin") {
                val builder = newBuilder()
                val argument = merge.argument.toPersistentHashSet()
                assertEquals(changed, builder.mutable(argument), "the Boolean the call returns")
                assertHolds(model, builder, "the builder after the call")
                assertHolds(merge.argument, argument, "the argument after the call")
            }
        }
    }

    testCall("addingAll", "addAll", { addingAll(it) }) { addAll(it) }
    testCall("retainingAll", "retainAll", { retainingAll(it) }) { retainAll(it) }
    testCall("removingAll", "removeAll", { removingAll(it) }) { removeAll(it) }
}

/** The entries of [expected], with the very instances of its keys. */
private fun assertHolds(expected: Map<IntWrapper?, String>, map: Map<IntWrapper?, String>, message: String) {
    assertEquals(expected, map, message)
    assertHolds(expected.keys, map.keys, message)
}

/** The very instances of [expected], and no other elements. */
private fun assertHolds(expected: Collection<IntWrapper?>, actual: Collection<IntWrapper?>, message: String) {
    assertEquals(expected.size, actual.size, "$message: the size")
    for (element in expected) assertSame(element, actual.single { it == element }, "$message: the instance of ${element?.obj}")
}

private val Map<*, *>.trie: Any
    get() = (this as PersistentHashMap<*, *>).node

private val Set<*>.trie: TrieNode<*>
    get() = (this as PersistentHashSet<*>).node

/** The node reached through the first cells: a collision node, or the one holding the first element. */
private fun TrieNode<*>.bottom(): TrieNode<*> = (buffer[0] as? TrieNode<*>)?.bottom() ?: this

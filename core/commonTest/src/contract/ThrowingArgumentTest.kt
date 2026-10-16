/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract

import kotlinx.collections.immutable.PersistentCollection
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.persistentHashSetOf
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentHashSet
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import tests.trie.trieShapes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * The contract of a builder call that runs user code which throws: the iterator, contains or predicate of the
 * argument, or the equals or hashCode of an element of the argument or of the builder. The exception reaches the
 * caller and the builder stays a valid collection: as many elements as its size, none twice, none lost that the call
 * would keep, none gained that it would not add. A live iterator fails exactly when the size moved, a build after a
 * call that changed nothing is the one built before, earlier builds keep their content, and further writes work. The
 * list builder, as ArrayList, inserts nothing and removes exactly what its argument matched before the throw. After
 * a next that throws, remove takes out the element next returned last, and the iteration goes on from the element
 * next failed on.
 */
class ThrowingArgumentTest {

    @Test
    fun list() = runCells {
        for (size in listDepths.keys) testOperations(listBuildersOf(size), listKind, listBulkOperations, BulkArguments())
    }

    @Test
    fun hashSet() = runCells {
        for ((shape, keys) in trieShapes) {
            val arguments = BulkArguments(keys)
            val builders = hashSetBuildersOf(arguments.stored).of(shape)
            testOperations(builders, setKind(ordered = false), setBulkOperations, arguments)
            testThrowingNext(builders, setKind(ordered = false))
        }
    }

    @Test
    fun hashMap() = runCells {
        for ((shape, keys) in trieShapes) {
            val arguments = BulkArguments(keys)
            val builders = hashMapBuildersOf(arguments.storedEntries).of(shape)
            testOperations(builders, mapKind(ordered = false), mapBulkOperations, arguments)
            testThrowingNext(builders, mapKind(ordered = false))
            testThrowingSetValue(builders, mapKind(ordered = false))
        }
    }

    @Test
    fun orderedSet() = runCells {
        for ((shape, keys) in trieShapes) {
            val arguments = BulkArguments(keys)
            val builders = orderedSetBuildersOf(arguments.stored).of(shape)
            testOperations(builders, setKind(ordered = true), setBulkOperations, arguments)
            testThrowingNext(builders, setKind(ordered = true))
        }
    }

    @Test
    fun orderedMap() = runCells {
        for ((shape, keys) in trieShapes) {
            val arguments = BulkArguments(keys)
            val builders = orderedMapBuildersOf(arguments.storedEntries).of(shape)
            testOperations(builders, mapKind(ordered = true), mapBulkOperations, arguments)
            testThrowingNext(builders, mapKind(ordered = true))
            testThrowingSetValue(builders, mapKind(ordered = true))
        }
    }

    @Test
    fun `a hash map builder iterator finds its key in a collision node without running the equals of earlier keys on it`() {
        val third = RejectingElement(3, hash = 0)
        val first = RejectingElement(1, hash = 0, rejected = third)
        val second = RejectingElement(2, hash = 0)
        val builder = persistentHashMapOf(first to "1", second to "2", third to "3").builder()
        assertEquals(listOf(first, second, third), builder.keys.toList())
        val overwritten = builder.keys.iterator()
        assertSame(first, overwritten.next())
        assertSame(second, overwritten.next())
        builder[first] = "new"
        assertSame(third, overwritten.next())
        val removing = builder.keys.iterator()
        assertSame(first, removing.next())
        assertSame(second, removing.next())
        removing.remove()
        assertSame(third, removing.next())
        assertEquals(listOf(first, third), builder.keys.toList())
    }

    @Test
    fun `a live iterator of a hash map builder stays anchored after a putAll that threw before any change`() {
        val keys = List(3) { CallbackElement(it, inBuilder = true) }
        val builder = persistentHashMapOf(*keys.map { it to "old" }.toTypedArray()).builder()
        val order = builder.keys.toList()
        val iterator = builder.keys.iterator()
        assertSame(order[0], iterator.next())
        val failure = CallbackFailure()
        val argument = persistentHashMapOf(CallbackElement(0, inBuilder = false) to "new")
        assertSame(failure, Callbacks.run(Armed(Callback.STORED_EQUALS, keys[0]), failure) { builder.putAll(argument) })
        assertEquals(keys.associateWith { "old" }, builder.toMap())
        assertNull(Callbacks.run(Armed(Callback.STORED_HASH_CODE, order[1]), failure) { val _ = iterator.next() })
        assertEquals(order.drop(2), iterator.drain())
    }

    @Test
    fun `a hash set builder keeps its collision node intact when retainAll or removeAll throws partway`() {
        val other = RejectingElement(4, hash = 0)
        val dropped = RejectingElement(1, hash = 0)
        val kept = RejectingElement(2, hash = 0)
        val throwing = RejectingElement(3, hash = 0, rejected = other)
        val calls = listOf<Pair<String, PersistentSet.Builder<RejectingElement>.() -> Boolean>>(
            "retainAll" to { retainAll(persistentHashSetOf(RejectingElement(2, hash = 0), other)) },
            "removeAll" to { removeAll(persistentHashSetOf(RejectingElement(1, hash = 0), other)) },
        )
        for ((call, bulk) in calls) {
            val builder = persistentHashSetOf<RejectingElement>().builder().apply { addAll(listOf(kept, throwing, dropped)) }
            assertEquals(listOf(dropped, kept, throwing), builder.toList(), call)
            assertFailsWith<CallbackFailure>(call) { builder.bulk() }
            val content = builder.toList()
            assertEquals(builder.size, content.size, call)
            assertEquals(content.size, content.toSet().size, "$call: an element twice in $content")
        }
    }

    @Test
    fun `an ordered builder links a new key into an existing collision node without comparing its last key with it`() {
        val new = RejectingElement(3, hash = 0)
        val first = RejectingElement(1, hash = 0)
        val last = RejectingElement(2, hash = 0, rejected = new)
        val set = persistentSetOf(first, last).builder()
        assertTrue(set.add(new))
        assertEquals(listOf(first, last, new), set.toList())
        assertEquals(listOf(first, last, new), set.build().toList())
        val map = persistentMapOf(first to "1", last to "2").builder()
        assertNull(map.put(new, "3"))
        assertEquals(listOf(first, last, new), map.keys.toList())
        assertEquals(listOf(first, last, new), map.build().keys.toList())
    }

    @Test
    fun `a hash set builder stays equal to a set of its content after retainAll throws below the root`() {
        val a = RejectingElement(1, hash = 0)
        val b = RejectingElement(2, hash = 1 shl 10)
        val copyOfC = RejectingElement(3, hash = 1)
        val c = RejectingElement(3, hash = 1, rejected = copyOfC)
        val d = RejectingElement(4, hash = 2)
        val builder = persistentHashSetOf<RejectingElement>().builder()
        for (element in listOf(a, b, c, d)) assertTrue(builder.add(element))
        val retained = persistentHashSetOf(RejectingElement(1, hash = 0), RejectingElement(5, hash = 1 shl 5), copyOfC)
        assertFailsWith<CallbackFailure> { builder.retainAll(retained) }
        val content = builder.toList()
        assertEquals(builder.size, content.size)
        val canonical = content.toPersistentHashSet()
        assertEquals(canonical, builder.build(), "a set of the content equals the build")
        assertEquals(builder.build(), canonical, "the build equals a set of the content")
        assertTrue(builder.remove(a))
        assertEquals(canonical - a, builder.build())
    }

    @Test
    fun `the list builder reads the size of the argument of addAll before its iterator`() {
        for (size in listOf(TAIL_ONLY, ONE_LEAF)) for (count in listOf(3, ONE_LEAF)) for (index in listOf(0, size / 2, size)) {
            val name = "addAll($index) of $count into $size"
            val builder = persistentListOf<Int>().builder().apply { addAll(List(size) { it }) }
            val iterator = builder.iterator()
            val expected = ArrayList(builder).apply { addAll(index, newElements(count)) }
            assertTrue(builder.addAll(index, SizeBeforeIterator(newElements(count))), name)
            assertEquals(expected, builder.toList(), name)
            assertFailsWith<ConcurrentModificationException>(name) { iterator.next() }
        }
    }

    @Test
    fun `the persistent ordered set iterator goes on from the element next failed on`() {
        val elements = List(3) { CallbackElement(it, inBuilder = true) }
        for (callback in listOf(Callback.STORED_HASH_CODE, Callback.STORED_EQUALS)) {
            val iterator = elements.toPersistentSet().iterator()
            assertSame(elements[0], iterator.next())
            val failure = CallbackFailure()
            repeat(2) { assertSame(failure, Callbacks.run(Armed(callback, elements[1]), failure) { val _ = iterator.next() }, callback.description) }
            assertEquals(elements.drop(1), iterator.drain(), callback.description)
        }
    }
}

private class SizeBeforeIterator<E>(private val elements: List<E>) : AbstractCollection<E>() {
    private var iterated = false

    override val size: Int
        get() {
            check(!iterated) { "the size was read after the iterator" }
            return elements.size
        }

    override fun iterator(): Iterator<E> {
        iterated = true
        return elements.iterator()
    }
}

private class BulkOperation<in M>(val description: String, val function: M.(BulkArguments) -> Any?)

private val listBulkOperations: List<BulkOperation<MutableList<Int>>> = buildList {
    val insertions = listOf<Pair<String, MutableList<Int>.(Collection<Int>) -> Unit>>(
        "addAll()" to { addAll(it) },
        "addAll(0)" to { addAll(0, it) },
        "addAll(size / 2)" to { addAll(size / 2, it) },
        "addAll(lastIndex)" to { addAll(lastIndex, it) },
    )
    for ((call, insert) in insertions) for ((elements, count) in listOf("three elements" to 3, "a leaf and more" to ONE_LEAF)) {
        add(BulkOperation("$call of $elements") { insert(it.collection(newElements(count))) })
    }
    val removals = listOf<Pair<String, List<Int>.() -> Set<Int>>>(
        "every other element" to { evenElements() },
        "the last two elements" to { takeLast(2).toSet() },
        "the second half" to { drop(size / 2).toSet() },
    )
    for ((elements, removed) in removals) {
        add(BulkOperation("removeAll() of $elements") { removeAll(it.collection(removed())) })
    }
    add(BulkOperation("retainAll() of every other element") { retainAll(it.collection(evenElements())) })
    add(BulkOperation("removeAll(a predicate) of every other element") { removeAll(it.predicate { element: Int -> element % 2 == 0 }) })
    add(BulkOperation("retainAll(a predicate) of every other element") { retainAll(it.predicate { element: Int -> element % 2 == 0 }) })
    add(BulkOperation("containsAll() of the elements at the leaf boundaries") { containsAll(it.collection(boundaryIndices(size))) })
}

private val setBulkOperations: List<BulkOperation<MutableSet<CallbackElement>>> = listOf(
    BulkOperation("addAll() of a collection") { addAll(it.collection(it.some)) },
    BulkOperation("addAll() of a hash set") { addAll(it.someSet) },
    BulkOperation("addAll() of a hash set builder") { addAll(it.someSet.builder()) },
    BulkOperation("removeAll() of a collection") { removeAll(it.collection(it.some)) },
    BulkOperation("removeAll() of a hash set") { removeAll(it.someSet) },
    BulkOperation("removeAll() of a hash set builder") { removeAll(it.someSet.builder()) },
    BulkOperation("retainAll() of a collection") { retainAll(it.collection(it.some)) },
    BulkOperation("retainAll() of a hash set") { retainAll(it.someSet) },
    BulkOperation("retainAll() of a hash set builder") { retainAll(it.someSet.builder()) },
    BulkOperation("removeAll(a predicate)") { removeAll(it.predicate { element: CallbackElement -> element in it.someSet }) },
    BulkOperation("retainAll(a predicate)") { retainAll(it.predicate { element: CallbackElement -> element in it.someSet }) },
    BulkOperation("containsAll() of a collection") { containsAll(it.collection(it.copies)) },
    BulkOperation("containsAll() of a hash set") { containsAll(it.copiesSet) },
    BulkOperation("containsAll() of a hash set builder") { containsAll(it.copiesSet.builder()) },
    BulkOperation("add() of each new element") { for (element in it.new) add(element) },
    BulkOperation("remove() of the middle element") { remove(it.copies[it.copies.size / 2]) },
)

private typealias StoredEntry = Map.Entry<CallbackElement, String>

private val mapBulkOperations: List<BulkOperation<MutableMap<CallbackElement, String>>> = listOf(
    BulkOperation("putAll() of a map") { putAll(it.map(it.somePairs)) },
    BulkOperation("putAll() of a hash map") { putAll(it.someMap) },
    BulkOperation("putAll() of a hash map builder") { putAll(it.someMap.builder()) },
    BulkOperation("putAll() of an ordered map") { putAll(it.someOrderedMap) },
    BulkOperation("keys.removeAll() of a collection") { keys.removeAll(it.collection(it.some)) },
    BulkOperation("keys.retainAll() of a collection") { keys.retainAll(it.collection(it.some)) },
    BulkOperation("keys.removeAll(a predicate)") { keys.removeAll(it.predicate { key: CallbackElement -> key in it.someSet }) },
    BulkOperation("keys.retainAll(a predicate)") { keys.retainAll(it.predicate { key: CallbackElement -> key in it.someSet }) },
    BulkOperation("values.removeAll(a predicate)") { values.removeAll(it.predicate { value: String -> value in ODD_VALUES }) },
    BulkOperation("values.retainAll(a predicate)") { values.retainAll(it.predicate { value: String -> value !in ODD_VALUES }) },
    BulkOperation("entries.removeAll(a predicate)") { entries.removeAll(it.predicate { entry: StoredEntry -> entry.key in it.someSet }) },
    BulkOperation("entries.retainAll(a predicate)") { entries.retainAll(it.predicate { entry: StoredEntry -> entry.key in it.someSet }) },
    BulkOperation("keys.containsAll() of a collection") { keys.containsAll(it.collection(it.copies)) },
    BulkOperation("put() of each new key") { for (key in it.new) put(key, "new") },
    BulkOperation("remove() of the middle key") { remove(it.copies[it.copies.size / 2]) },
    BulkOperation("setValue() of each entry") { for (entry in entries) entry.setValue("set") },
)

/** Elements no list builder holds. */
private fun newElements(count: Int): List<Int> = List(count) { -1 - it }

private fun List<Int>.evenElements(): Set<Int> = filterTo(HashSet()) { it % 2 == 0 }

/** The values the map builders hold for the keys at odd positions. */
private val ODD_VALUES = setOf("v1", "v3")

/** An element no list builder holds. */
private const val WRITTEN = -1000

/** An element no set or map builder holds. */
private val SPARE = CallbackElement("spare", inBuilder = true)

/** The user code a call may run, each reported with the element it runs on. */
private enum class Callback(val description: String) {
    NEXT("next"),
    CONTAINS("contains"),
    PREDICATE("the predicate"),
    EQUALS("equals"),
    HASH_CODE("hashCode"),
    STORED_EQUALS("equals of a stored element"),
    STORED_HASH_CODE("hashCode of a stored element"),
}

/** The callbacks whose answer decides whether the element they are asked about stays. */
private val answering = setOf(Callback.CONTAINS, Callback.PREDICATE)

private class Armed(val callback: Callback, val element: Any?) {
    override fun toString(): String = "${callback.description} throws on $element"
}

private class CallbackFailure : Error()

private class RejectingElement(private val id: Int, private val hash: Int, private val rejected: Any? = null) {
    override fun equals(other: Any?): Boolean {
        if (other === rejected) throw CallbackFailure()
        return other is RejectingElement && other.id == id
    }

    override fun hashCode(): Int = hash

    override fun toString(): String = "element $id"
}

/** The armed callback throws on every call on its element, as an element whose code always throws would. */
private object Callbacks {
    private var live = false
    private var armed: Armed? = null
    private var failure: Throwable? = null

    /** The elements each callback ran on during the last [run], one per call. */
    var ran = mutableMapOf<Callback, MutableList<Any?>>()
        private set

    fun call(callback: Callback, reported: Any?) {
        if (!live) return
        val element = if (reported is Map.Entry<*, *>) reported.key else reported
        ran.getOrPut(callback) { mutableListOf() } += element
        val armed = armed ?: return
        if (callback != armed.callback || !element.isSameElement(armed.element)) return
        throw failure!!
    }

    fun run(armed: Armed?, failure: Throwable, block: () -> Unit): Throwable? {
        ran = mutableMapOf()
        this.armed = armed
        this.failure = failure
        live = true
        return runCatching(block).exceptionOrNull().also { live = false }
    }
}

/** A CallbackElement is told apart by its instance, since its equals would report a callback. */
private fun Any?.isSameElement(other: Any?): Boolean = if (this is CallbackElement) this === other else this == other

/**
 * A key of a trie shape as an element of a builder or of an argument, equal to the elements of the same key. A
 * comparison of one of each reports the equals of the argument's element, whichever of the two is the receiver.
 */
private class CallbackElement(private val key: Any, private val inBuilder: Boolean) {
    override fun equals(other: Any?): Boolean {
        if (inBuilder) Callbacks.call(Callback.STORED_EQUALS, this)
        if (other !is CallbackElement) return false
        if (other.inBuilder != inBuilder) Callbacks.call(Callback.EQUALS, if (inBuilder) other else this)
        return other.key == key
    }

    override fun hashCode(): Int {
        Callbacks.call(if (inBuilder) Callback.STORED_HASH_CODE else Callback.HASH_CODE, this)
        return key.hashCode()
    }

    override fun toString(): String = key.toString()
}

private class ArgumentCollection<E>(private val elements: Collection<E>) : AbstractCollection<E>() {
    override val size: Int get() = elements.size

    override fun iterator(): Iterator<E> = object : Iterator<E> {
        private val iterator = elements.iterator()
        override fun hasNext(): Boolean = iterator.hasNext()
        override fun next(): E = iterator.next().also { Callbacks.call(Callback.NEXT, it) }
    }

    override fun contains(element: E): Boolean {
        Callbacks.call(Callback.CONTAINS, element)
        return element in elements
    }
}

private class ArgumentMap<K, V>(pairs: List<Pair<K, V>>) : AbstractMap<K, V>() {
    private val pairEntries = ArgumentCollection(pairs.map { (key, value) -> Entry(key, value) })

    override val entries: Set<Map.Entry<K, V>> = object : AbstractSet<Map.Entry<K, V>>() {
        override val size: Int get() = pairEntries.size
        override fun iterator(): Iterator<Map.Entry<K, V>> = pairEntries.iterator()
    }

    private class Entry<K, V>(override val key: K, override val value: V) : Map.Entry<K, V>
}

private val allTrieKeys: List<Any> = trieShapes.values.flatten().distinct()

/** The elements of the builders of the trie shape [keys] and the arguments of their calls, made before the calls. */
private class BulkArguments(keys: List<Any> = emptyList()) {
    val stored: List<CallbackElement> = keys.map { CallbackElement(it, inBuilder = true) }
    val storedEntries: List<Pair<CallbackElement, String>> = stored.mapIndexed { index, element -> element to "v$index" }

    val copies: List<CallbackElement> = keys.map { CallbackElement(it, inBuilder = false) }
    val copiesSet: PersistentSet<CallbackElement> = copies.toPersistentHashSet()

    val new: List<CallbackElement> = (allTrieKeys - keys.toSet()).map { CallbackElement(it, inBuilder = false) }

    val some: List<CallbackElement> = copies.filterIndexed { index, _ -> index % 2 == 0 } + new
    val someSet: PersistentSet<CallbackElement> = some.toPersistentHashSet()
    val somePairs: List<Pair<CallbackElement, String>> = some.map { it to "new" }
    val someMap: PersistentMap<CallbackElement, String> = persistentHashMapOf(*somePairs.toTypedArray())
    val someOrderedMap: PersistentMap<CallbackElement, String> = persistentMapOf(*somePairs.toTypedArray())

    fun <E> collection(elements: Collection<E>): Collection<E> = ArgumentCollection(elements)

    fun <K, V> map(pairs: List<Pair<K, V>>): Map<K, V> = ArgumentMap(pairs)

    fun <E> predicate(test: (E) -> Boolean): (E) -> Boolean = { element ->
        Callbacks.call(Callback.PREDICATE, element)
        test(element)
    }
}

private fun <B> List<Pair<String, () -> B>>.of(shape: String): List<Pair<String, () -> B>> =
    map { (origin, newBuilder) -> "$origin of $shape" to newBuilder }

/** A kind of builder [B] and the stdlib model [M] it shares its calls with. */
private class BuilderKind<M : Any, B : M>(
    /** A stdlib collection of the content, whose answer to the call is the content to reach. */
    val model: (B) -> M,
    val ordered: Boolean,
    /** A write that leaves the content as it was and copies the path to one element. */
    val touch: B.() -> Unit,
    val write: B.() -> Any?,
    /** The content as a collection built from nothing, which a build must equal both ways. */
    val canonical: (List<Any?>) -> Any,
    /** Whether the builder, as ArrayList, adds nothing and removes just what its argument matched before a throw. */
    val exact: Boolean = false,
    val iterators: List<Pair<String, B.() -> MutableIterator<*>>> = emptyList(),
    /** A write that keeps the size, after which the next of a hash map iterator runs the code of the key it returns. */
    val overwrite: Pair<String, B.() -> Unit>? = null,
)

private val listKind = BuilderKind<MutableList<Int>, PersistentList.Builder<Int>>(
    model = { ArrayList(it) },
    ordered = true,
    touch = { val first = this[0]; this[0] = WRITTEN; this[0] = first },
    write = { if (isNotEmpty()) removeAt(0); add(WRITTEN); WRITTEN },
    canonical = { it.toPersistentList() },
    exact = true,
)

private fun setKind(ordered: Boolean) =
    BuilderKind<MutableSet<CallbackElement>, PersistentSet.Builder<CallbackElement>>(
        model = { LinkedHashSet(it) },
        ordered = ordered,
        touch = { add(SPARE); remove(SPARE) },
        write = { if (isNotEmpty()) remove(first()); add(SPARE); SPARE },
        canonical = { if (ordered) it.toPersistentSet() else it.toPersistentHashSet() },
        iterators = listOf("iterator" to { iterator() }),
    )

private fun mapKind(ordered: Boolean) =
    BuilderKind<MutableMap<CallbackElement, String>, PersistentMap.Builder<CallbackElement, String>>(
        model = { LinkedHashMap(it) },
        ordered = ordered,
        touch = { put(SPARE, "spare"); remove(SPARE) },
        write = { if (isNotEmpty()) remove(keys.first()); put(SPARE, "spare"); SPARE to "spare" },
        canonical = { content ->
            @Suppress("UNCHECKED_CAST")
            val pairs = (content as List<Pair<Any?, Any?>>).toTypedArray()
            if (ordered) persistentMapOf(*pairs) else persistentHashMapOf(*pairs)
        },
        iterators = listOf("entries" to { entries.iterator() }, "keys" to { keys.iterator() }, "values" to { values.iterator() }),
        overwrite = if (ordered) null else "a put of a new value for the first key" to { put(keys.first(), "changed") },
    )

private enum class Preparation(val description: String) {
    NOTHING(""),
    BUILD(" after a build"),
    BUILD_AND_WRITE(" after a build and a write"),
}

/**
 * Makes every operation on a fresh builder of every origin after each preparation, first with no callback armed, which
 * finds the elements each callback runs on, then with each callback throwing on the first, the middle and the last.
 */
private fun <M : Any, B : M> Cells.testOperations(
    builders: List<Pair<String, () -> B>>,
    kind: BuilderKind<M, B>,
    operations: List<BulkOperation<M>>,
    arguments: BulkArguments,
) {
    for ((origin, newBuilder) in builders) for (preparation in Preparation.entries) for (operation in operations) {
        val name = "$origin${preparation.description}, ${operation.description}"
        val test = { armed: Armed? -> testOperation(newBuilder, preparation, kind, operation, arguments, armed) }
        var ran = emptyMap<Callback, List<Any?>>()
        cell("$name, nothing armed") { ran = test(null) }
        for ((callback, calls) in ran) {
            val elements = calls.distinct()
            for (index in listOf(0, (elements.size - 1) / 2, elements.lastIndex).distinct()) {
                val armed = Armed(callback, elements[index])
                cell("$name, $armed") { val _ = test(armed) }
            }
        }
    }
}

/** Returns the elements each callback ran on, one per call. */
private fun <M : Any, B : M> testOperation(
    newBuilder: () -> B,
    preparation: Preparation,
    kind: BuilderKind<M, B>,
    operation: BulkOperation<M>,
    arguments: BulkArguments,
    armed: Armed?,
): Map<Callback, List<Any?>> {
    val builds = mutableListOf<Pair<Any, List<Any?>>>()
    val builder = newBuilder()
    if (preparation != Preparation.NOTHING) builds += buildOf(builder)
    if (preparation == Preparation.BUILD_AND_WRITE) kind.touch(builder)
    val lastBuild = if (preparation == Preparation.BUILD) builds.single().first else null
    val before = contentOf(builder)
    val model = kind.model(builder)
    val modelResult = operation.function(model, arguments)
    val complete = contentOf(model)
    val iterator = iteratorOf(builder)

    val failure = CallbackFailure()
    var result: Any? = null
    val thrown = Callbacks.run(armed, failure) { result = operation.function(builder, arguments) }
    val ran = Callbacks.ran
    val asked = if (armed == null) emptyList() else ran[armed.callback].orEmpty()

    val expected = when {
        thrown == null -> complete.also { assertEquals(modelResult, result, "the result of the call") }
        thrown !== failure -> throw thrown
        kind.exact && armed!!.callback == Callback.NEXT -> before
        kind.exact && armed!!.callback in answering -> {
            assertEquals(before.take(asked.size), asked, "the elements the argument was asked about")
            val answered = asked.dropLast(1).toSet()
            val kept = complete.toSet()
            before.filter { it !in answered || it in kept }
        }
        else -> null
    }
    if (armed != null) {
        assertSame(failure, thrown, "the failure of the callback reaches the caller")
        val afterTheThrow = asked.drop(asked.indexOfFirst { it.isSameElement(armed.element) } + 1)
        assertEquals(emptyList(), afterTheThrow, "the elements ${armed.callback.description} ran on after it threw")
    }
    assertConsistent(builder, kind, before, complete, expected, iterator, builds, lastBuild)
    return ran
}

/** Checks the builder after a call against the [expected] content, or against one between [before] and [complete]. */
private fun <M : Any, B : M> assertConsistent(
    builder: B,
    kind: BuilderKind<M, B>,
    before: List<Any?>,
    complete: List<Any?>,
    expected: List<Any?>?,
    iterator: Iterator<*>,
    builds: MutableList<Pair<Any, List<Any?>>>,
    lastBuild: Any?,
) {
    val after = contentOf(builder)
    assertEquals(sizeOf(builder), after.size, "the size of ${after.brief()}")
    assertEquals(after.size, after.map(::keyOf).toSet().size, "an element twice in ${after.brief()}")
    if (builder is Map<*, *>) {
        assertEquals(after.map(::keyOf), builder.keys.iterator().drain(), "the keys")
        assertEquals(after.map { (it as Pair<*, *>).second }, builder.values.iterator().drain(), "the values")
    }
    if (expected != null) assertContent(expected, after, kind.ordered, "the content") else assertBetween(before, complete, after, kind.ordered)

    if (after.size == before.size) {
        val rest = try {
            iterator.drain()
        } catch (e: ConcurrentModificationException) {
            fail("a live iterator threw $e after a call that kept the size")
        }
        assertContent(after, rest, kind.ordered, "a live iterator after a call that kept the size")
    } else {
        assertFailsWith<ConcurrentModificationException>("a live iterator after a call that moved the size") { iterator.next() }
    }

    builds += buildOf(builder)
    assertContent(after, builds.last().second, kind.ordered, "a build after the call")
    val canonical = kind.canonical(after)
    assertEquals(canonical, builds.last().first, "a collection of the content, against the build")
    assertEquals(builds.last().first, canonical, "the build, against a collection of the content")
    if (lastBuild != null && after == before) assertSame(lastBuild, builds.last().first, "a build after a call that changed nothing")
    val written = kind.write(builder)
    assertContent(after.drop(1) + listOf(written), contentOf(builder), kind.ordered, "the content after a further write")
    for ((built, content) in builds) {
        assertEquals(content, contentOf(built), "a collection built from the builder")
        assertEquals(content.size, sizeOf(built), "the size of a collection built from the builder")
    }
}

/**
 * Opens every iterator of the [kind] on a fresh builder of every origin, calls next 0 to size - 1 times, with and
 * without the overwrite after them, then once more with each callback that runs in it throwing on each of its elements,
 * and then remove.
 */
private fun <M : Any, B : M> Cells.testThrowingNext(builders: List<Pair<String, () -> B>>, kind: BuilderKind<M, B>) {
    for ((origin, newBuilder) in builders) for ((view, open) in kind.iterators) for (write in listOf(null, kind.overwrite).distinct()) {
        val keys = keysOf("$origin, $view", newBuilder) ?: continue
        for (returned in keys.indices) {
            val name = "$origin, $view, next() and remove() after $returned next${write?.let { " and ${it.first}" }.orEmpty()}"
            val test = { armed: Armed? ->
                val builder = newBuilder()
                val iterator = builder.open()
                repeat(returned) { val _ = iterator.next() }
                write?.second?.invoke(builder)
                val content = builder.open().drain()

                val failure = CallbackFailure()
                val thrown = Callbacks.run(armed, failure) { val _ = iterator.next() }
                val ran = Callbacks.ran
                if (armed != null) assertSame(failure, thrown, "the failure of the callback reaches the caller") else if (thrown != null) throw thrown

                val next = if (thrown == null) returned + 1 else returned
                if (next == 0) assertFailsWith<IllegalStateException> { iterator.remove() } else iterator.remove()
                val kept = content.filterIndexed { index, _ -> index != next - 1 }
                assertContent(kept, builder.open().drain(), kind.ordered, "the content after remove")
                assertEquals(kept.size, sizeOf(builder), "the size after remove")
                assertEquals(content.drop(next), iterator.drain(), "the rest of the iteration")
                ran
            }
            var ran = emptyMap<Callback, List<Any?>>()
            cell("$name, nothing armed") { ran = test(null) }
            for ((callback, calls) in ran) for (element in calls.distinct()) {
                val armed = Armed(callback, element)
                cell("$name, $armed") { val _ = test(armed) }
            }
        }
    }
}

/**
 * Opens the entries iterator on a fresh builder of every origin after a build, calls next 1 to size times and sets the
 * value of the last entry, with each callback that runs in it throwing on each of its elements.
 */
private fun Cells.testThrowingSetValue(
    builders: List<Pair<String, () -> PersistentMap.Builder<CallbackElement, String>>>,
    kind: BuilderKind<MutableMap<CallbackElement, String>, PersistentMap.Builder<CallbackElement, String>>,
) {
    for ((origin, newBuilder) in builders) {
        val keys = keysOf(origin, newBuilder) ?: continue
        for (returned in 1..keys.size) {
            val name = "$origin after a build, entries, setValue() after $returned next"
            val test = { armed: Armed? ->
                val builder = newBuilder()
                val builds = mutableListOf(buildOf(builder))
                val iterator = builder.entries.iterator()
                repeat(returned - 1) { val _ = iterator.next() }
                val entry = iterator.next()
                val before = contentOf(builder)
                val complete = contentOf(kind.model(builder).apply { put(entry.key, "set") })
                val live = iteratorOf(builder)

                val failure = CallbackFailure()
                val thrown = Callbacks.run(armed, failure) { entry.setValue("set") }
                val ran = Callbacks.ran
                if (armed != null) assertSame(failure, thrown, "the failure of the callback reaches the caller") else if (thrown != null) throw thrown

                val expected = if (thrown == null) complete else before
                assertEquals(expected.drop(returned), iterator.drain(), "the rest of the iteration")
                assertConsistent(builder, kind, before, complete, expected, live, builds, builds.single().first)
                ran
            }
            var ran = emptyMap<Callback, List<Any?>>()
            cell("$name, nothing armed") { ran = test(null) }
            for ((callback, calls) in ran) for (element in calls.distinct()) {
                val armed = Armed(callback, element)
                cell("$name, $armed") { val _ = test(armed) }
            }
        }
    }
}

/** The keys a fresh builder holds, or null when reading them throws and a failed cell named [message] reports it. */
private fun Cells.keysOf(message: String, newBuilder: () -> Any): List<Any?>? =
    try {
        contentOf(newBuilder()).map(::keyOf)
    } catch (e: Throwable) {
        cell(message) { throw e }
        null
    }

/** A collection built from [builder], with the content it holds when built. */
private fun buildOf(builder: Any): Pair<Any, List<Any?>> {
    val built = if (builder is PersistentCollection.Builder<*>) builder.build() else (builder as PersistentMap.Builder<*, *>).build()
    return built to contentOf(built)
}

private fun iteratorOf(collection: Any): Iterator<*> =
    if (collection is Map<*, *>) collection.entries.iterator() else (collection as Iterable<*>).iterator()

/** The content in the order of its iteration, a map's as key to value pairs. */
private fun contentOf(collection: Any): List<Any?> = iteratorOf(collection).drain()

private fun sizeOf(collection: Any): Int = if (collection is Map<*, *>) collection.size else (collection as Collection<*>).size

private fun keyOf(item: Any?): Any? = if (item is Pair<*, *>) item.first else item

private fun List<Any?>.brief(): String = if (size <= 10) toString() else "${take(10).toString().dropLast(1)}, ... ($size elements)]"

private fun assertContent(expected: List<Any?>, actual: List<Any?>, ordered: Boolean, message: String) {
    if (ordered) {
        assertEquals(expected, actual, message)
    } else {
        assertEquals(expected.groupingBy { it }.eachCount(), actual.groupingBy { it }.eachCount(), message)
    }
}

/** Checks that [after] lies between [before] and [complete] in keys, in elements and, when [ordered], in order. */
private fun assertBetween(before: List<Any?>, complete: List<Any?>, after: List<Any?>, ordered: Boolean) {
    val either = before.toSet() + complete
    for (item in after) assertTrue(item in either, "$item came from neither the content before the call nor after it: ${after.brief()}")
    val completeKeys = complete.mapTo(HashSet(), ::keyOf)
    val heldKeys = after.mapTo(HashSet(), ::keyOf)
    for (item in before) if (keyOf(item) in completeKeys) assertTrue(keyOf(item) in heldKeys, "$item, held before the call and after it, is lost: ${after.brief()}")
    if (ordered) {
        val keys = after.map(::keyOf)
        val beforeKeys = before.map(::keyOf)
        val completeKeys = complete.map(::keyOf)
        assertTrue(
            keys.isSubsequenceOf(beforeKeys) && completeKeys.isSubsequenceOf(keys) ||
                beforeKeys.isSubsequenceOf(keys) && keys.isSubsequenceOf(completeKeys),
            "the order of ${after.brief()} is between neither ${before.brief()} nor ${complete.brief()}",
        )
    }
}

private fun List<Any?>.isSubsequenceOf(other: List<Any?>): Boolean {
    val elements = toSet()
    return other.filter { it in elements } == this
}

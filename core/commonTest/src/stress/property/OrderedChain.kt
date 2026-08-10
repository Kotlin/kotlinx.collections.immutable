/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.stress.property

import kotlinx.collections.immutable.implementations.persistentOrderedMap.LinkedValue
import kotlinx.collections.immutable.implementations.persistentOrderedMap.PersistentOrderedMap
import kotlinx.collections.immutable.implementations.persistentOrderedMap.PersistentOrderedMapBuilder
import kotlinx.collections.immutable.internal.EndOfChain

/**
 * Walks the doubly-linked chain that gives [PersistentOrderedMap] its iteration order, and checks
 * every invariant of it.
 *
 * The chain needs its own oracle because none of the content checks can see it. `size` comes from
 * the backing hash map, and the iterators stop after `size` steps rather than at the terminator, so
 * a chain that is cyclic — or that visits the right keys in the wrong order — answers every question
 * about contents correctly. The library's own checks are single-hop `assert` calls at the mutation
 * sites, and those compile to nothing on JS and Wasm.
 */
internal class OrderedChain(val forward: List<Any?>, val problems: List<String>)

internal fun <K, V> PersistentOrderedMap<K, V>.chain(): OrderedChain =
    walkChain(size, firstKey, lastKey) { key ->
        @Suppress("UNCHECKED_CAST")
        hashMap[key as K]
    }

/**
 * The builder keeps `lastKey` private, so the tail is derived from the terminator instead of read
 * from the field. That is the stronger check anyway: it makes the walk prove the chain ends.
 */
internal fun <K, V> PersistentOrderedMapBuilder<K, V>.chain(): OrderedChain =
    walkChain(size, firstKey, lastKey = null) { key ->
        @Suppress("UNCHECKED_CAST")
        hashMapBuilder[key as K]
    }

private fun <V> walkChain(
    size: Int,
    firstKey: Any?,
    lastKey: Any?,
    linksOf: (Any?) -> LinkedValue<V>?
): OrderedChain {
    val problems = mutableListOf<String>()
    val forward = mutableListOf<Any?>()

    if (size == 0) {
        if (firstKey !== EndOfChain) problems += "an empty map must start at EndOfChain, starts at $firstKey"
        if (lastKey != null && lastKey !== EndOfChain) {
            problems += "an empty map must end at EndOfChain, ends at $lastKey"
        }
        return OrderedChain(forward, problems)
    }

    if (firstKey === EndOfChain) {
        problems += "a map of $size entries starts at EndOfChain"
        return OrderedChain(forward, problems)
    }

    val seen = HashSet<Any?>()
    var key: Any? = firstKey
    var previous: Any? = EndOfChain
    // One step past the declared size is enough to tell a cycle from a well-formed chain.
    while (key !== EndOfChain && forward.size <= size) {
        val links = linksOf(key)
        if (links == null) {
            problems += "the chain reaches $key, which the backing map does not hold"
            return OrderedChain(forward, problems)
        }
        if (!seen.add(key)) {
            problems += "the chain revisits $key, so it is cyclic"
            return OrderedChain(forward, problems)
        }
        // Checking the back pointer at every step is what makes the two directions agree, so a
        // separate backward walk would add nothing: it could only fail where this already did.
        if (links.previous != previous) {
            problems += "$key points back at ${links.previous}, but it is reached from $previous"
        }
        forward += key
        previous = key
        key = links.next
    }

    if (key !== EndOfChain) {
        problems += "the chain has not ended after $size steps"
        return OrderedChain(forward, problems)
    }
    if (forward.size != size) {
        problems += "the chain holds ${forward.size} keys but size reports $size"
    }
    if (lastKey != null && forward.isNotEmpty() && forward.last() != lastKey) {
        problems += "the chain ends at ${forward.last()} but lastKey is $lastKey"
    }

    return OrderedChain(forward, problems)
}

/**
 * Everything the chain must satisfy, plus the well-formedness of the hash trie underneath it — the
 * ordered map is a `PersistentHashMap<K, LinkedValue<V>>`, so it inherits every trie invariant.
 */
internal fun <K, V> PersistentOrderedMap<K, V>.orderedViolations(): List<String> {
    val chain = chain()
    val problems = chain.problems.toMutableList()
    problems += hashMap.shape().violations(expectedSize = size)

    // Iterating a map whose chain is already broken walks off the end and throws, so what follows
    // only runs once the chain itself is sound.
    if (chain.problems.isNotEmpty()) return problems

    // The iterators are separate code from the chain and stop after `size` steps rather than at the
    // terminator, so what they yield is worth comparing even when the chain is well formed.
    val iterated = keys.toList()
    if (iterated != chain.forward) {
        problems += "iteration yields $iterated but the chain says ${chain.forward}"
    }
    return problems
}

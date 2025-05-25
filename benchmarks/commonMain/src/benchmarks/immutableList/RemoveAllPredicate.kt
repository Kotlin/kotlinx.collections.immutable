/*
 * Copyright 2016-2023 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package benchmarks.immutableList

import benchmarks.*
import kotlinx.collections.immutable.PersistentList
import kotlinx.benchmark.*
import kotlinx.collections.immutable.emptyPersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlin.random.Random

@State(Scope.Benchmark)
open class RemoveAllPredicate {
    @Param(BM_1, BM_10, BM_100, BM_1000, BM_10000, BM_100000, BM_1000000, BM_10000000)
    var size: Int = 0

    private var persistentList = emptyPersistentList<String>()
    private val truePredicate: (String) -> Boolean = { true }
    private val falsePredicate: (String) -> Boolean = { false }
    private var randomHalfElementsPredicate: (String) -> Boolean = truePredicate
    private var randomTenElementsPredicate: (String) -> Boolean = truePredicate
    private var randomOneElementPredicate: (String) -> Boolean = truePredicate
    private var tailElementsPredicate: (String) -> Boolean = truePredicate

    @Setup
    fun prepare() {
        val randomHalfElements = randomIndexes(size / 2).map { it.toString() }.toHashSet()
        randomHalfElementsPredicate = { it in randomHalfElements }

        val randomTenElements = randomIndexes(10).map { it.toString() }.toHashSet()
        randomTenElementsPredicate = { it in randomTenElements }

        val randomOneElement = Random.nextInt(size).toString()
        randomOneElementPredicate = { it == randomOneElement }

        val tailElements = List(tailSize()) { (size - 1 - it).toString() }.toHashSet()
        tailElementsPredicate = { it in tailElements }

        val allElements = List(size) { it.toString() }
        persistentList = emptyPersistentList<String>().addAll(allElements)
    }

    // The benchmarks measure (time and memory spent in `removeAll` operation) / size
    //
    // Expected time: nearly constant
    // Expected memory: nearly constant

    /** Removes all elements. */
    @Benchmark
    fun removeAll_All(): PersistentList<String> {
        return persistentList.removeAll(truePredicate)
    }

    /** Removes no elements. */
    @Benchmark
    fun removeAll_Non(): PersistentList<String> {
        return persistentList.removeAll(falsePredicate)
    }

    /** Removes half of the elements randomly selected. */
    @Benchmark
    fun removeAll_RandomHalf(): PersistentList<String> {
        return persistentList.removeAll(randomHalfElementsPredicate)
    }

    /** Removes 10 random elements. */
    @Benchmark
    fun removeAll_RandomTen(): PersistentList<String> {
        return persistentList.removeAll(randomTenElementsPredicate)
    }

    /** Removes a random element. */
    @Benchmark
    fun removeAll_RandomOne(): PersistentList<String> {
        return persistentList.removeAll(randomOneElementPredicate)
    }

    /** Removes last [tailSize] elements. */
    @Benchmark
    fun removeAll_Tail(): PersistentList<String> {
        return persistentList.removeAll(tailElementsPredicate)
    }

    private fun randomIndexes(count: Int): List<Int> {
        return List(count) { Random.nextInt(size) }
    }

    private fun tailSize(): Int {
        val bufferSize = 32
        return (size and (bufferSize - 1)).let { if (it == 0) bufferSize else it }
    }
}

/*
 * Copyright 2016-2019 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlinx.collections.immutable.contractTests.immutableSet

import com.google.common.collect.testing.SetTestSuiteBuilder
import com.google.common.collect.testing.TestSetGenerator
import com.google.common.collect.testing.features.CollectionFeature
import com.google.common.collect.testing.features.CollectionSize
import com.google.common.collect.testing.features.Feature
import kotlinx.collections.immutable.contractTests.GuavaImmutableCollectionBaseTest
import kotlinx.collections.immutable.contractTests.immutableSet.PersistentSetGenerator.HashSet
import kotlinx.collections.immutable.contractTests.immutableSet.PersistentSetGenerator.OrderedSet
import kotlin.test.Test

class GuavaImmutableSetTest: GuavaImmutableCollectionBaseTest() {
    private fun <E> runImmutableSetTestsUsing(generator: TestSetGenerator<E>, knownOrder: Boolean) {
        val features = mutableListOf<Feature<*>>(
                CollectionSize.ANY,
                CollectionFeature.ALLOWS_NULL_VALUES,
                CollectionFeature.SUBSET_VIEW,
                CollectionFeature.DESCENDING_VIEW/*,
                CollectionFeature.REJECTS_DUPLICATES_AT_CREATION*/
        )
        if (knownOrder)
            features.add(CollectionFeature.KNOWN_ORDER)

        SetTestSuiteBuilder
                .using(generator)
                .named(generator.javaClass.simpleName)
                .withFeatures(features)
                .createTestSuite()
                .run { runTestSuite(this) }

    }

    private fun <E> runMutableSetTestsUsing(generator: TestSetGenerator<E>, knownOrder: Boolean) {
        val features = mutableListOf<Feature<*>>(
                CollectionSize.ANY,
                CollectionFeature.ALLOWS_NULL_VALUES,
                CollectionFeature.GENERAL_PURPOSE,
                CollectionFeature.FAILS_FAST_ON_CONCURRENT_MODIFICATION,
                CollectionFeature.SUBSET_VIEW,
                CollectionFeature.DESCENDING_VIEW/*,
                CollectionFeature.REJECTS_DUPLICATES_AT_CREATION*/
        )
        if (knownOrder)
            features.add(CollectionFeature.KNOWN_ORDER)

        SetTestSuiteBuilder
                .using(generator)
                .named(generator.javaClass.simpleName)
                .withFeatures(features)
                .createTestSuite()
                .run { runTestSuite(this) }

    }

    @Test
    fun hashSet() {
        listOf(
                HashSet.Of,
                HashSet.AddAll,
                HashSet.AddEach,
                HashSet.MutateAddAll,
                HashSet.MutateAddEach
        ).forEach {
            runImmutableSetTestsUsing(it, knownOrder = false)
        }
    }

    @Test
    fun hashSetBuilder() {
        listOf(
                HashSet.Builder.Of,
                HashSet.Builder.AddAll,
                HashSet.Builder.AddEach
        ).forEach {
            runMutableSetTestsUsing(it, knownOrder = false)
        }
    }


    @Test
    fun orderedSet() {
        listOf(
                OrderedSet.Of,
                OrderedSet.AddAll,
                OrderedSet.AddEach,
                OrderedSet.MutateAddAll,
                OrderedSet.MutateAddEach
        ).forEach {
            runImmutableSetTestsUsing(it, knownOrder = true)
        }
    }

    @Test
    fun orderedSetBuilder() {
        listOf(
                OrderedSet.Builder.Of,
                OrderedSet.Builder.AddAll,
                OrderedSet.Builder.AddEach
        ).forEach {
            runMutableSetTestsUsing(it, knownOrder = true)
        }
    }
}
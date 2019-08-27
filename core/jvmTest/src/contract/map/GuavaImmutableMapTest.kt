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

package tests.contract.map

import com.google.common.collect.testing.MapTestSuiteBuilder
import com.google.common.collect.testing.TestMapGenerator
import com.google.common.collect.testing.features.CollectionFeature
import com.google.common.collect.testing.features.CollectionSize
import com.google.common.collect.testing.features.Feature
import com.google.common.collect.testing.features.MapFeature
import tests.contract.GuavaImmutableCollectionBaseTest
import tests.contract.map.PersistentMapGenerator.HashMap
import tests.contract.map.PersistentMapGenerator.OrderedMap
import kotlin.test.Test

class GuavaImmutableMapTest: GuavaImmutableCollectionBaseTest() {
    private fun <K, V> runImmutableMapTestsUsing(generator: TestMapGenerator<K, V>, knownOrder: Boolean) {
        val features = mutableListOf<Feature<*>>(
                CollectionSize.ANY,
                MapFeature.ALLOWS_NULL_KEYS,
                MapFeature.ALLOWS_NULL_VALUES,
                MapFeature.ALLOWS_NULL_ENTRY_QUERIES/*,
                MapFeature.REJECTS_DUPLICATES_AT_CREATION*/
        )
        if (knownOrder)
            features.add(CollectionFeature.KNOWN_ORDER)

        MapTestSuiteBuilder
                .using(generator)
                .named(generator.javaClass.simpleName)
                .withFeatures(features)
                .createTestSuite()
                .run { runTestSuite(this) }
    }

    private fun <K, V> runMutableMapTestsUsing(generator: TestMapGenerator<K, V>, knownOrder: Boolean) {
        val features = mutableListOf<Feature<*>>(
                CollectionSize.ANY,
                MapFeature.ALLOWS_NULL_KEYS,
                MapFeature.ALLOWS_NULL_VALUES,
                MapFeature.ALLOWS_NULL_ENTRY_QUERIES,
                MapFeature.GENERAL_PURPOSE,
                MapFeature.FAILS_FAST_ON_CONCURRENT_MODIFICATION,
                CollectionFeature.SUPPORTS_ITERATOR_REMOVE, // iteration over entries/keys/values
                CollectionFeature.SUPPORTS_REMOVE/*,
                MapFeature.REJECTS_DUPLICATES_AT_CREATION*/
        )
        if (knownOrder)
            features.add(CollectionFeature.KNOWN_ORDER)

        MapTestSuiteBuilder
                .using(generator)
                .named(generator.javaClass.simpleName)
                .withFeatures(features)
                .createTestSuite()
                .run { runTestSuite(this) }
    }

    @Test
    fun hashMap() {
        listOf(
                HashMap.Of,
                HashMap.PutAll,
                HashMap.PutEach,
                HashMap.MutatePutAll,
                HashMap.MutatePutEach
        ).forEach {
            runImmutableMapTestsUsing(it, knownOrder = false)
        }
    }

    @Test
    fun hashMapBuilder() {
        listOf(
                HashMap.Builder.Of,
                HashMap.Builder.PutAll,
                HashMap.Builder.PutEach
        ).forEach {
            runMutableMapTestsUsing(it, knownOrder = false)
        }
    }


    @Test
    fun orderedMap() {
        listOf(
                OrderedMap.Of,
                OrderedMap.PutAll,
                OrderedMap.PutEach,
                OrderedMap.MutatePutAll,
                OrderedMap.MutatePutEach
        ).forEach {
            runImmutableMapTestsUsing(it, knownOrder = true)
        }
    }

    @Test
    fun orderedMapBuilder() {
        listOf(
                OrderedMap.Builder.Of,
                OrderedMap.Builder.PutAll,
                OrderedMap.Builder.PutEach
        ).forEach {
            runMutableMapTestsUsing(it, knownOrder = true)
        }
    }
}
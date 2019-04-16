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

package kotlinx.collections.immutable.contractTests.immutableList

import com.google.common.collect.testing.ListTestSuiteBuilder
import com.google.common.collect.testing.TestListGenerator
import com.google.common.collect.testing.features.CollectionFeature
import com.google.common.collect.testing.features.CollectionSize
import com.google.common.collect.testing.features.ListFeature
import kotlinx.collections.immutable.contractTests.GuavaImmutableCollectionBaseTest
import kotlinx.collections.immutable.contractTests.immutableList.PersistentListGenerator.PList
import kotlin.test.Test

class GuavaImmutableListTest: GuavaImmutableCollectionBaseTest() {
    private fun <E> runImmutableListTestsUsing(generator: TestListGenerator<E>) {
        ListTestSuiteBuilder
                .using(generator)
                .named(generator.javaClass.simpleName)
                .withFeatures(
                        CollectionSize.ANY,
                        CollectionFeature.ALLOWS_NULL_VALUES
                )
                .createTestSuite()
                .run { runTestSuite(this) }

    }

    private fun <E> runMutableListTestsUsing(generator: TestListGenerator<E>) {
        ListTestSuiteBuilder
                .using(generator)
                .named(generator.javaClass.simpleName)
                .withFeatures(
                        CollectionSize.ANY,
                        CollectionFeature.ALLOWS_NULL_VALUES,
                        CollectionFeature.FAILS_FAST_ON_CONCURRENT_MODIFICATION,
                        ListFeature.GENERAL_PURPOSE
                )
                .createTestSuite()
                .run { runTestSuite(this) }

    }

    @Test
    fun list() {
        listOf(
                PList.Of,
                PList.AddAll,
                PList.AddEach,
                PList.MutateAddAll,
                PList.MutateAddEach,
                PList.HeadSubList,
                PList.TailSubList,
                PList.MiddleSubList
        ).forEach {
            runImmutableListTestsUsing(it)
        }
    }

    @Test
    fun listBuilder() {
        listOf(
                PList.Builder.Of,
                PList.Builder.AddAll,
                PList.Builder.AddEach,
                PList.Builder.AddAt,
                PList.Builder.HeadSubList,
                PList.Builder.TailSubList,
                PList.Builder.MiddleSubList
        ).forEach {
            runMutableListTestsUsing(it)
        }
    }
}
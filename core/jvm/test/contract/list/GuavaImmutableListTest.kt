/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.list

import com.google.common.collect.testing.ListTestSuiteBuilder
import com.google.common.collect.testing.TestListGenerator
import com.google.common.collect.testing.features.CollectionFeature
import com.google.common.collect.testing.features.CollectionSize
import com.google.common.collect.testing.features.ListFeature
import tests.contract.GuavaImmutableCollectionBaseTest
import tests.contract.list.PersistentListGenerator.PList
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
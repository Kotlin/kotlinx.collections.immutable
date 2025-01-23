/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable.serialization

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.serialization.util.JsonConfigurationFactory
import kotlinx.collections.immutable.serialization.util.JsonExt.encodeAndDecode
import kotlinx.serialization.Serializable

class ImmutableCollectionSerializerTest {

    @Serializable
    private class ImmutableListHolder<T>(
        @Serializable(with = ImmutableListSerializer::class)
        val immutableList: ImmutableList<T>
    )

    @Test
    fun testImmutableList() {
        val json = JsonConfigurationFactory.createJsonConfiguration()
        persistentListOf(1, 2, 3)
            .let(::ImmutableListHolder)
            .let { expectedList -> assertContentEquals(expectedList.immutableList, json.encodeAndDecode(expectedList).immutableList) }
    }

    @Serializable
    private class PersistentListHolder<T>(
        @Serializable(with = PersistentListSerializer::class)
        val persistentList: PersistentList<T>
    )

    @Test
    fun testPersistentList() {
        val json = JsonConfigurationFactory.createJsonConfiguration()
        persistentListOf(1, 2, 3)
            .let(::PersistentListHolder)
            .let { expectedList -> assertContentEquals(expectedList.persistentList, json.encodeAndDecode(expectedList).persistentList) }
    }

    @Serializable
    private class ImmutableSetHolder<T>(
        @Serializable(with = ImmutableSetSerializer::class)
        val immutableSet: ImmutableSet<T>
    )

    @Test
    fun testImmutableSet() {
        val json = JsonConfigurationFactory.createJsonConfiguration()
        persistentSetOf(1, 2, 3)
            .let(::ImmutableSetHolder)
            .let { expectedSet -> assertEquals(expectedSet.immutableSet, json.encodeAndDecode(expectedSet).immutableSet) }
    }

    @Serializable
    private class PersistentSetHolder<T>(
        @Serializable(with = PersistentSetSerializer::class)
        val persistentSet: PersistentSet<T>
    )

    @Test
    fun testPersistentSet() {
        val json = JsonConfigurationFactory.createJsonConfiguration()
        persistentSetOf(1, 2, 3)
            .let(::PersistentSetHolder)
            .let { expectedSet -> assertEquals(expectedSet.persistentSet, json.encodeAndDecode(expectedSet).persistentSet) }
    }

    @Serializable
    private class PersistentHashSetHolder<T>(
        @Serializable(with = PersistentHashSetSerializer::class)
        val persistentHashSet: PersistentSet<T>
    )

    @Test
    fun testPersistentHashSet() {
        val json = JsonConfigurationFactory.createJsonConfiguration()
        persistentSetOf(1, 2, 3)
            .let(::PersistentHashSetHolder)
            .let { expectedSet -> assertEquals(expectedSet.persistentHashSet, json.encodeAndDecode(expectedSet).persistentHashSet) }
    }

}
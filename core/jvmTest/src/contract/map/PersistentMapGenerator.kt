/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.map

import com.google.common.collect.testing.TestStringMapGenerator
import kotlinx.collections.immutable.*

class PersistentMapGenerator {
    object HashMap {
        object Of : TestStringMapGenerator() {
            override fun create(entries: Array<out Map.Entry<String, String>>): Map<String, String> {
                return persistentHashMapOf(*entries.map { it.key to it.value }.toTypedArray())
            }
        }

        object PutAll : TestStringMapGenerator() {
            override fun create(entries: Array<out Map.Entry<String, String>>): Map<String, String> {
                val map = mutableMapOf<String, String>().apply { entries.forEach { this[it.key] = it.value } }
                return emptyPersistentHashMap<String, String>().putAll(map)
            }
        }

        object PutEach : TestStringMapGenerator() {
            override fun create(entries: Array<out Map.Entry<String, String>>): Map<String, String> {
                return entries.fold(emptyPersistentHashMap()) { map, entry -> map.put(entry.key, entry.value) }
            }
        }

        object MutatePutAll : TestStringMapGenerator() {
            override fun create(entries: Array<out Map.Entry<String, String>>): Map<String, String> {
                val map = mutableMapOf<String, String>().apply { entries.forEach { this[it.key] = it.value } }
                return emptyPersistentHashMap<String, String>().mutate { it.putAll(map) }
            }
        }

        object MutatePutEach : TestStringMapGenerator() {
            override fun create(entries: Array<out Map.Entry<String, String>>): Map<String, String> {
                return emptyPersistentHashMap<String, String>().mutate { builder ->
                    entries.forEach {
                        builder[it.key] = it.value
                    }
                }
            }
        }

        object Builder {
            object Of : TestStringMapGenerator() {
                override fun create(entries: Array<out Map.Entry<String, String>>): MutableMap<String, String> {
                    return persistentHashMapOf(*entries.map { it.key to it.value }.toTypedArray()).builder()
                }
            }

            object PutAll : TestStringMapGenerator() {
                override fun create(entries: Array<out Map.Entry<String, String>>): MutableMap<String, String> {
                    val map = mutableMapOf<String, String>().apply { entries.forEach { this[it.key] = it.value } }
                    return emptyPersistentHashMap<String, String>().builder().apply { putAll(map) }
                }
            }

            object PutEach : TestStringMapGenerator() {
                override fun create(entries: Array<out Map.Entry<String, String>>): MutableMap<String, String> {
                    return emptyPersistentHashMap<String, String>().builder()
                        .apply { entries.forEach { this[it.key] = it.value } }
                }
            }
        }
    }

    object OrderedMap {
        object Of : TestStringMapGenerator() {
            override fun create(entries: Array<out Map.Entry<String, String>>): Map<String, String> {
                return persistentMapOf(*entries.map { it.key to it.value }.toTypedArray())
            }
        }

        object PutAll : TestStringMapGenerator() {
            override fun create(entries: Array<out Map.Entry<String, String>>): Map<String, String> {
                val map = mutableMapOf<String, String>().apply { entries.forEach { this[it.key] = it.value } }
                return emptyPersistentMap<String, String>().putAll(map)
            }
        }

        object PutEach : TestStringMapGenerator() {
            override fun create(entries: Array<out Map.Entry<String, String>>): Map<String, String> {
                return entries.fold(emptyPersistentMap()) { map, entry -> map.put(entry.key, entry.value) }
            }
        }

        object MutatePutAll : TestStringMapGenerator() {
            override fun create(entries: Array<out Map.Entry<String, String>>): Map<String, String> {
                val map = mutableMapOf<String, String>().apply { entries.forEach { this[it.key] = it.value } }
                return emptyPersistentMap<String, String>().mutate { it.putAll(map) }
            }
        }

        object MutatePutEach : TestStringMapGenerator() {
            override fun create(entries: Array<out Map.Entry<String, String>>): Map<String, String> {
                return emptyPersistentMap<String, String>().mutate { builder ->
                    entries.forEach {
                        builder[it.key] = it.value
                    }
                }
            }
        }

        object Builder {
            object Of : TestStringMapGenerator() {
                override fun create(entries: Array<out Map.Entry<String, String>>): MutableMap<String, String> {
                    return persistentMapOf(*entries.map { it.key to it.value }.toTypedArray()).builder()
                }
            }

            object PutAll : TestStringMapGenerator() {
                override fun create(entries: Array<out Map.Entry<String, String>>): MutableMap<String, String> {
                    val map = mutableMapOf<String, String>().apply { entries.forEach { this[it.key] = it.value } }
                    return emptyPersistentMap<String, String>().builder().apply { putAll(map) }
                }
            }

            object PutEach : TestStringMapGenerator() {
                override fun create(entries: Array<out Map.Entry<String, String>>): MutableMap<String, String> {
                    return emptyPersistentMap<String, String>().builder()
                        .apply { entries.forEach { this[it.key] = it.value } }
                }
            }
        }
    }
}
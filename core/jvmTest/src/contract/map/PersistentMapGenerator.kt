/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.map

import com.google.common.collect.testing.TestStringMapGenerator
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.persistentMapOf

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
                return persistentHashMapOf<String, String>().puttingAll(map)
            }
        }

        object PutEach : TestStringMapGenerator() {
            override fun create(entries: Array<out Map.Entry<String, String>>): Map<String, String> {
                return entries.fold(persistentHashMapOf()) { map, entry -> map.putting(entry.key, entry.value) }
            }
        }

        object MutatePutAll : TestStringMapGenerator() {
            override fun create(entries: Array<out Map.Entry<String, String>>): Map<String, String> {
                val map = mutableMapOf<String, String>().apply { entries.forEach { this[it.key] = it.value } }
                return persistentHashMapOf<String, String>().mutate { it.putAll(map) }
            }
        }

        object MutatePutEach : TestStringMapGenerator() {
            override fun create(entries: Array<out Map.Entry<String, String>>): Map<String, String> {
                return persistentHashMapOf<String, String>().mutate { builder -> entries.forEach { builder[it.key] = it.value } }
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
                    return persistentHashMapOf<String, String>().builder().apply { putAll(map) }
                }
            }

            object PutEach : TestStringMapGenerator() {
                override fun create(entries: Array<out Map.Entry<String, String>>): MutableMap<String, String> {
                    return persistentHashMapOf<String, String>().builder().apply { entries.forEach { this[it.key] = it.value } }
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
                return persistentMapOf<String, String>().puttingAll(map)
            }
        }

        object PutEach : TestStringMapGenerator() {
            override fun create(entries: Array<out Map.Entry<String, String>>): Map<String, String> {
                return entries.fold(persistentMapOf()) { map, entry -> map.putting(entry.key, entry.value) }
            }
        }

        object MutatePutAll : TestStringMapGenerator() {
            override fun create(entries: Array<out Map.Entry<String, String>>): Map<String, String> {
                val map = mutableMapOf<String, String>().apply { entries.forEach { this[it.key] = it.value } }
                return persistentMapOf<String, String>().mutate { it.putAll(map) }
            }
        }

        object MutatePutEach : TestStringMapGenerator() {
            override fun create(entries: Array<out Map.Entry<String, String>>): Map<String, String> {
                return persistentMapOf<String, String>().mutate { builder -> entries.forEach { builder[it.key] = it.value } }
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
                    return persistentMapOf<String, String>().builder().apply { putAll(map) }
                }
            }

            object PutEach : TestStringMapGenerator() {
                override fun create(entries: Array<out Map.Entry<String, String>>): MutableMap<String, String> {
                    return persistentMapOf<String, String>().builder().apply { entries.forEach { this[it.key] = it.value } }
                }
            }
        }
    }
}

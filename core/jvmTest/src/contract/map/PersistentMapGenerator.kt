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
                return persistentHashMapOf<String, String>().putAll(map)
            }
        }

        object PutEach : TestStringMapGenerator() {
            override fun create(entries: Array<out Map.Entry<String, String>>): Map<String, String> {
                return entries.fold(persistentHashMapOf()) { map, entry -> map.put(entry.key, entry.value) }
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
                return persistentMapOf<String, String>().putAll(map)
            }
        }

        object PutEach : TestStringMapGenerator() {
            override fun create(entries: Array<out Map.Entry<String, String>>): Map<String, String> {
                return entries.fold(persistentMapOf()) { map, entry -> map.put(entry.key, entry.value) }
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
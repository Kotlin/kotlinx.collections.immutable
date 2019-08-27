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

package tests.contract.set

import com.google.common.collect.testing.TestStringSetGenerator
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentHashSetOf
import kotlinx.collections.immutable.persistentSetOf

class PersistentSetGenerator {
    object HashSet {
        object Of : TestStringSetGenerator() {
            override fun create(elements: Array<out String>): Set<String> {
                return persistentHashSetOf(*elements)
            }
        }

        object AddAll : TestStringSetGenerator() {
            override fun create(elements: Array<out String>): Set<String> {
                return persistentHashSetOf<String>().addAll(elements.toList())
            }
        }

        object AddEach : TestStringSetGenerator() {
            override fun create(elements: Array<out String>): Set<String> {
                return elements.fold(persistentHashSetOf()) { set, element -> set.add(element) }
            }
        }

        object MutateAddAll : TestStringSetGenerator() {
            override fun create(elements: Array<out String>): Set<String> {
                return persistentHashSetOf<String>().mutate { it.addAll(elements) }
            }
        }

        object MutateAddEach : TestStringSetGenerator() {
            override fun create(elements: Array<out String>): Set<String> {
                return persistentHashSetOf<String>().mutate { builder -> elements.forEach { builder.add(it) } }
            }
        }


        object Builder {
            object Of : TestStringSetGenerator() {
                override fun create(elements: Array<out String>): MutableSet<String> {
                    return persistentHashSetOf(*elements).builder()
                }
            }

            object AddAll : TestStringSetGenerator() {
                override fun create(elements: Array<out String>): MutableSet<String> {
                    return persistentHashSetOf<String>().builder().apply { addAll(elements) }
                }
            }

            object AddEach : TestStringSetGenerator() {
                override fun create(elements: Array<out String>): MutableSet<String> {
                    return persistentHashSetOf<String>().builder().apply { elements.forEach { add(it) } }
                }
            }
        }
    }


    object OrderedSet {
        object Of : TestStringSetGenerator() {
            override fun create(elements: Array<out String>): Set<String> {
                return persistentSetOf(*elements)
            }
        }

        object AddAll : TestStringSetGenerator() {
            override fun create(elements: Array<out String>): Set<String> {
                return persistentSetOf<String>().addAll(elements.toList())
            }
        }

        object AddEach : TestStringSetGenerator() {
            override fun create(elements: Array<out String>): Set<String> {
                return elements.fold(persistentSetOf()) { set, element -> set.add(element) }
            }
        }

        object MutateAddAll : TestStringSetGenerator() {
            override fun create(elements: Array<out String>): Set<String> {
                return persistentSetOf<String>().mutate { it.addAll(elements) }
            }
        }

        object MutateAddEach : TestStringSetGenerator() {
            override fun create(elements: Array<out String>): Set<String> {
                return persistentSetOf<String>().mutate { builder -> elements.forEach { builder.add(it) } }
            }
        }


        object Builder {
            object Of : TestStringSetGenerator() {
                override fun create(elements: Array<out String>): MutableSet<String> {
                    return persistentSetOf(*elements).builder()
                }
            }

            object AddAll : TestStringSetGenerator() {
                override fun create(elements: Array<out String>): MutableSet<String> {
                    return persistentSetOf<String>().builder().apply { addAll(elements) }
                }
            }

            object AddEach : TestStringSetGenerator() {
                override fun create(elements: Array<out String>): MutableSet<String> {
                    return persistentSetOf<String>().builder().apply { elements.forEach { add(it) } }
                }
            }
        }
    }
}
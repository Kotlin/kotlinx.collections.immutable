/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
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
                return persistentHashSetOf<String>().addingAll(elements.toList())
            }
        }

        object AddEach : TestStringSetGenerator() {
            override fun create(elements: Array<out String>): Set<String> {
                return elements.fold(persistentHashSetOf()) { set, element -> set.adding(element) }
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
                return persistentSetOf<String>().addingAll(elements.toList())
            }
        }

        object AddEach : TestStringSetGenerator() {
            override fun create(elements: Array<out String>): Set<String> {
                return elements.fold(persistentSetOf()) { set, element -> set.adding(element) }
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

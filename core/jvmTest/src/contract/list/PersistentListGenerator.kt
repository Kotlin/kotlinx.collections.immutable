/*
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.contract.list

import com.google.common.collect.testing.TestStringListGenerator
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf

class PersistentListGenerator {
    object PList {
        object Of : TestStringListGenerator() {
            override fun create(elements: Array<out String>): List<String> {
                return persistentListOf(*elements)
            }
        }

        object AddAll : TestStringListGenerator() {
            override fun create(elements: Array<out String>): List<String> {
                return persistentListOf<String>().copyingAddAll(elements.toList())
            }
        }

        object AddEach : TestStringListGenerator() {
            override fun create(elements: Array<out String>): List<String> {
                return elements.fold(persistentListOf()) { list, element -> list.copyingAdd(element) }
            }
        }

        object MutateAddAll : TestStringListGenerator() {
            override fun create(elements: Array<out String>): List<String> {
                return persistentListOf<String>().mutate { it.addAll(elements.toList()) }
            }
        }

        object MutateAddEach : TestStringListGenerator() {
            override fun create(elements: Array<out String>): List<String> {
                return persistentListOf<String>().mutate { list -> elements.forEach { list.add(it) } }
            }
        }

        object HeadSubList : TestStringListGenerator() {
            override fun create(elements: Array<out String>): List<String> {
                return persistentListOf<String>()
                        .copyingAddAll(listOf(*elements, "f", "g"))
                        .subList(0, elements.size)
            }
        }

        object TailSubList : TestStringListGenerator() {
            override fun create(elements: Array<String>): List<String> {
                return persistentListOf<String>()
                        .copyingAddAll(listOf("f", "g", *elements))
                        .subList(2, elements.size + 2)
            }
        }

        object MiddleSubList : TestStringListGenerator() {
            override fun create(elements: Array<String>): List<String> {

                return persistentListOf<String>()
                        .copyingAddAll(listOf("f", "g", *elements, "h", "i"))
                        .subList(2, elements.size + 2)
            }
        }


        object Builder {
            object Of : TestStringListGenerator() {
                override fun create(elements: Array<out String>): MutableList<String> {
                    return persistentListOf(*elements).builder()
                }
            }

            object AddAll : TestStringListGenerator() {
                override fun create(elements: Array<out String>): MutableList<String> {
                    return persistentListOf<String>().builder().apply { this.addAll(elements.toList()) }
                }
            }

            object AddEach : TestStringListGenerator() {
                override fun create(elements: Array<out String>): MutableList<String> {
                    return persistentListOf<String>().builder().apply { elements.forEach { this.add(it) } }
                }
            }

            object AddAt : TestStringListGenerator() {
                override fun create(elements: Array<out String>): MutableList<String> {
                    val builder = persistentListOf<String>().builder()

                    val list = elements.mapIndexed { i, e -> Pair(i, e) }.toMutableList().apply { shuffle() }
                    list.forEachIndexed { index, pair ->
                        val preceded = list.subList(0, index).count { it.first < pair.first }
                        builder.add(preceded, pair.second)
                    }

                    return builder
                }
            }

            object HeadSubList : TestStringListGenerator() {
                override fun create(elements: Array<out String>): MutableList<String> {
                    return persistentListOf<String>().builder()
                            .apply { addAll(listOf(*elements, "f", "g")) }
                            .subList(0, elements.size)
                }
            }

            object TailSubList : TestStringListGenerator() {
                override fun create(elements: Array<String>): MutableList<String> {
                    return persistentListOf<String>().builder()
                            .apply { addAll(listOf("f", "g", *elements)) }
                            .subList(2, elements.size + 2)
                }
            }

            object MiddleSubList : TestStringListGenerator() {
                override fun create(elements: Array<String>): MutableList<String> {
                    return persistentListOf<String>().builder()
                            .apply { addAll(listOf("f", "g", *elements, "h", "i")) }
                            .subList(2, elements.size + 2)
                }
            }
        }
    }
}

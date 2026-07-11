/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable.samples

import kotlinx.collections.immutable.*
import kotlin.test.Test

class PersistentMapSamples {
    @Test
    fun immutableMap() {
        val temperatures: ImmutableMap<String, Int> = mapOf("Berlin" to 18, "Oslo" to 12).toImmutableMap()
        check(temperatures["Berlin"] == 18)
        check(temperatures.size == 2)
        check("Oslo" in temperatures)
        check("Paris" !in temperatures)
    }

    @Test
    fun persistentMap() {
        val version1 = persistentMapOf("width" to 800, "height" to 600)
        val version2 = version1.putting("depth", 32)
        check(version1 == mapOf("width" to 800, "height" to 600))
        check(version2 == mapOf("width" to 800, "height" to 600, "depth" to 32))
    }

    @Test
    fun putting() {
        val inventory = persistentMapOf("apple" to 3)
        val withBanana = inventory.putting("banana", 5)
        check(withBanana == mapOf("apple" to 3, "banana" to 5))
        val restocked = withBanana.putting("apple", 10)
        check(restocked == mapOf("apple" to 10, "banana" to 5))
    }

    @Test
    fun removingKey() {
        val settings = persistentMapOf("volume" to 70, "brightness" to 50)
        val withoutVolume = settings.removing("volume")
        check(withoutVolume == mapOf("brightness" to 50))
        val unchanged = settings.removing("contrast")
        check(unchanged === settings)
    }

    @Test
    fun removingKeyValue() {
        val votes = persistentMapOf("alice" to 3, "bob" to 5)
        val exactMatch = votes.removing("alice", 3)
        check(exactMatch == mapOf("bob" to 5))
        val mismatch = votes.removing("bob", 1)
        check(mismatch === votes)
    }

    @Test
    fun puttingAll() {
        val defaults = persistentMapOf("theme" to "light", "fontSize" to "12")
        val overrides = mapOf("theme" to "dark", "language" to "en")
        val config = defaults.puttingAll(overrides)
        check(config == mapOf("theme" to "dark", "fontSize" to "12", "language" to "en"))
        check(defaults == mapOf("theme" to "light", "fontSize" to "12"))
    }

    @Test
    fun builder() {
        val scores = persistentMapOf("alice" to 1, "bob" to 2)
        val builder = scores.builder()
        builder["carol"] = 3
        val removed = builder.remove("alice")
        check(removed == 1)
        val updated = builder.build()
        check(updated == mapOf("bob" to 2, "carol" to 3))
        check(scores == mapOf("alice" to 1, "bob" to 2))
    }

    @Test
    fun persistentMapOf() {
        val ranking = persistentMapOf("gold" to "alice", "silver" to "bob", "gold" to "carol")
        check(ranking == mapOf("gold" to "carol", "silver" to "bob"))
        check(ranking.keys.toList() == listOf("gold", "silver"))
        val empty = persistentMapOf<String, Int>()
        check(empty.isEmpty())
    }

    @Test
    fun persistentHashMapOf() {
        val ages = persistentHashMapOf("alice" to 30, "bob" to 25)
        check(ages == mapOf("alice" to 30, "bob" to 25))
        check(ages["alice"] == 30)
        check(ages.size == 2)
        val empty = persistentHashMapOf<String, Int>()
        check(empty.isEmpty())
    }

    @Test
    fun plusPair() {
        val prices = persistentMapOf("coffee" to 4)
        val withTea = prices + ("tea" to 3)
        check(withTea == mapOf("coffee" to 4, "tea" to 3))
        val discounted = withTea + ("coffee" to 2)
        check(discounted == mapOf("coffee" to 2, "tea" to 3))
    }

    @Test
    fun plusPairs() {
        val menu = persistentMapOf("soup" to 5)
        val extended = menu + listOf("salad" to 4, "bread" to 2)
        check(extended == mapOf("soup" to 5, "salad" to 4, "bread" to 2))
        check(menu == mapOf("soup" to 5))
    }

    @Test
    fun plusMap() {
        val base = persistentMapOf("host" to "localhost", "port" to "80")
        val overrides = mapOf("port" to "8080", "protocol" to "https")
        val merged = base + overrides
        check(merged == mapOf("host" to "localhost", "port" to "8080", "protocol" to "https"))
    }

    @Test
    fun puttingAllPairs() {
        val capitals = persistentMapOf("France" to "Paris")
        val extended = capitals.puttingAll(listOf("Japan" to "Tokyo", "Chile" to "Santiago"))
        check(extended == mapOf("France" to "Paris", "Japan" to "Tokyo", "Chile" to "Santiago"))
        check(capitals == mapOf("France" to "Paris"))
    }

    @Test
    fun minusKey() {
        val stock = persistentMapOf("hammer" to 10, "wrench" to 4)
        val withoutWrench = stock - "wrench"
        check(withoutWrench == mapOf("hammer" to 10))
        val unchanged = stock - "saw"
        check(unchanged === stock)
    }

    @Test
    fun minusKeys() {
        val fruits = persistentMapOf("apple" to 1, "banana" to 2, "cherry" to 3)
        val remaining = fruits - listOf("apple", "cherry")
        check(remaining == mapOf("banana" to 2))
        check(fruits.size == 3)
    }

    @Test
    fun mutate() {
        val scores = persistentMapOf("alice" to 1)
        val updated = scores.mutate {
            it["bob"] = 2
            it -= "alice"
        }
        check(updated == mapOf("bob" to 2))
        check(scores == mapOf("alice" to 1))
    }

    @Test
    fun toImmutableMap() {
        val original = mapOf("one" to 1, "two" to 2, "three" to 3)
        val immutable = original.toImmutableMap()
        check(immutable == original)
        check(immutable.keys.toList() == listOf("one", "two", "three"))
    }

    @Test
    fun toPersistentMap() {
        val original = mapOf("a" to 1, "b" to 2)
        val persistent = original.toPersistentMap()
        check(persistent.keys.toList() == listOf("a", "b"))
        val extended = persistent.putting("c", 3)
        check(extended == mapOf("a" to 1, "b" to 2, "c" to 3))
        check(persistent == original)
    }

    @Test
    fun toPersistentHashMap() {
        val original = mapOf("x" to 1, "y" to 2)
        val hashMap = original.toPersistentHashMap()
        check(hashMap == original)
        check(hashMap["x"] == 1)
    }
}

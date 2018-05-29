package kotlinx.collections.immutable

fun main(args: Array<String>) {

    mapOfMap()
    val set = immutableHashSetOf("d", "b", "c") + null as String?
    set.run {
        val builder = set.builder()
        builder.removeAll { it.orEmpty() > "b" }
        println(builder)
    }

    var map = immutableHashMapOf(1 to "a", 2 to "b", -1 to "d", 3 to "c", null to "z")

    println(map.entries)

    val builder = map.builder()

    val it = builder.entries.iterator()
    for (e in it) {
        if (e.key?:0 > 1)
            e.setValue(e.value + "x")
        else if (e.key?:0 < 0)
            it.remove()
    }

    println(builder)

    builder.keys.removeAll { it?:0 > 1 }
    println(builder)

    map = builder.build()
}


fun mapOfMap() {
    var map = immutableHashMapOf(1 to immutableHashMapOf("x" to 2 as Any)).toPersistentMap().put(1, immutableHashMapOf())

    println(map)
}


fun foo() {
    var list: PersistentList<String> = ImmutableVectorList.emptyOf()

    list = list.mutate { it.removeAll { it.length > 2 } }
    list = list.removeAll { it.length >  2}

    (list as List<String>) + 2


    //val builder: PVectorImmutableList.Builder<String> = list.builder<Nothing, String>()
    val list2 = list + StringBuilder() as CharSequence
    val builder = list.builder()
    builder.add("x")
    list = builder.build()
}




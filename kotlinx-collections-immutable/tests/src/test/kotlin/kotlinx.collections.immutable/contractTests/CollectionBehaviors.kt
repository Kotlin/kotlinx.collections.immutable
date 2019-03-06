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

package kotlinx.collections.immutable.contractTests

public fun <T> CompareContext<List<T>>.listBehavior() {
    equalityBehavior()
    collectionBehavior()
    compareProperty( { listIterator() }, { listIteratorBehavior() })
    compareProperty( { listIterator(0) }, { listIteratorBehavior() })
    compareProperty( { listIterator(size / 2) }, { listIteratorBehavior() })

    propertyFails { listIterator(-1) }
    propertyFails { listIterator(size + 1) }

    for (index in expected.indices)
        propertyEquals { this[index] }

    propertyFailsWith<IndexOutOfBoundsException> { this[size] }

    propertyEquals { indexOf(elementAtOrNull(0)) }
    propertyEquals { lastIndexOf(elementAtOrNull(0)) }

    propertyFails { subList(0, size + 1)}
    propertyFails { subList(-1, 0)}
    propertyEquals { subList(0, size) }
}

public fun <T> CompareContext<ListIterator<T>>.listIteratorBehavior() {
    listIteratorProperties()

    while (expected.hasNext()) {
        propertyEquals { next() }
        listIteratorProperties()
    }
    propertyFails { next() }

    while (expected.hasPrevious()) {
        propertyEquals { previous() }
        listIteratorProperties()
    }
    propertyFails { previous() }
}

public fun CompareContext<ListIterator<*>>.listIteratorProperties() {
    propertyEquals { hasNext() }
    propertyEquals { hasPrevious() }
    propertyEquals { nextIndex() }
    propertyEquals { previousIndex() }
}

public fun <T> CompareContext<Iterator<T>>.iteratorBehavior() {
    propertyEquals { hasNext() }

    while (expected.hasNext()) {
        propertyEquals { next() }
        propertyEquals { hasNext() }
    }
    propertyFails { next() }
}

public fun <T> CompareContext<Set<T>>.setBehavior(objectName: String = "", ordered: Boolean) {
    equalityBehavior(objectName, ordered)
    collectionBehavior(objectName, ordered)

    propertyEquals { containsAll(actual) }
    propertyEquals { containsAll(expected) }
}




public fun <K, V> CompareContext<Map<K, V>>.mapBehavior(ordered: Boolean) {
    propertyEquals { size }
    propertyEquals { isEmpty() }
    equalityBehavior(ordered = ordered)

    (object {}).let { propertyEquals { containsKey(it as Any?) }  }

    if (expected.isEmpty().not())
        propertyEquals { contains(keys.first()) }

    propertyEquals { containsKey(keys.firstOrNull()) }
    propertyEquals { containsValue(values.firstOrNull()) }
    propertyEquals { get(null as Any?) }

    compareProperty( { keys }, { setBehavior("keySet", ordered) } )
    compareProperty( { entries }, { setBehavior("entrySet", ordered) } )
    compareProperty( { values }, { collectionBehavior("values", ordered) })
}


public fun <T> CompareContext<T>.equalityBehavior(objectName: String = "", ordered: Boolean = true) {
    val prefix = objectName +  if (objectName.isNotEmpty()) "." else ""
    equals(objectName)
    propertyEquals(prefix + "hashCode") { hashCode() }
    if (ordered)
        propertyEquals(prefix + "toString") { toString() }
}


public fun <T> CompareContext<Collection<T>>.collectionBehavior(objectName: String = "", ordered: Boolean = true) {
    val prefix = objectName +  if (objectName.isNotEmpty()) "." else ""
    propertyEquals (prefix + "size") { size }
    propertyEquals (prefix + "isEmpty") { isEmpty() }

    (object {}).let { propertyEquals { contains(it as Any?) }  }
    propertyEquals { contains(firstOrNull()) }
    propertyEquals { containsAll(this) }
    if (ordered) {
        compareProperty({iterator()}, { iteratorBehavior() })
    }
}



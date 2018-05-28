package kotlinx.collections.immutable.implementations.immutableMap

private const val TRIE_MAX_HEIGHT = 7

internal class PersistentHashMapIterator<out K, out V>(node: TrieNode<K, V>) {
    private val path: Array<TrieNodeIterator<K, V>> = Array(TRIE_MAX_HEIGHT + 1) {   TrieNodeIterator<K, V>() }
    private var pathLastIndex = 0
    private var hasNext = true

    init {
        path[0].reset(node.buffer, 2 * Integer.bitCount(node.dataMap))
        pathLastIndex = 0
        ensureNextEntryIsReady()
    }

    private fun moveToNextNodeWithData(pathIndex: Int): Int {
        if (path[pathIndex].hasNextEntry()) {
            return pathIndex
        }
        if (path[pathIndex].hasNextNode()) {
            val node = path[pathIndex].currentNode()
            if (pathIndex == TRIE_MAX_HEIGHT - 1) {
                path[pathIndex + 1].reset(node.buffer, node.buffer.size)
            } else {
                path[pathIndex + 1].reset(node.buffer, 2 * Integer.bitCount(node.dataMap))
            }
            return moveToNextNodeWithData(pathIndex + 1)
        }
        return -1
    }

    private fun ensureNextEntryIsReady() {
        if (path[pathLastIndex].hasNextEntry()) {
            return
        }
        for(i in pathLastIndex downTo 0) {
            var result = moveToNextNodeWithData(i)

            if (result == -1 && path[i].hasNextNode()) {
                path[i].moveToNextNode()
                result = moveToNextNodeWithData(i)
            }
            if (result != -1) {
                pathLastIndex = result
                return
            }
            if (i > 0) {
                path[i - 1].moveToNextNode()
            }
        }
        hasNext = false
    }

    fun hasNext(): Boolean {
        return hasNext
    }

    fun nextKey(): K {
        assert(hasNext())
        val result = path[pathLastIndex].nextKey()
        ensureNextEntryIsReady()
        return result
    }

    fun nextValue(): V {
        assert(hasNext())
        val result = path[pathLastIndex].nextValue()
        ensureNextEntryIsReady()
        return result
    }

    fun nextEntry(): Map.Entry<K, V> {
        assert(hasNext())
        val result = path[pathLastIndex].nextEntry()
        ensureNextEntryIsReady()
        return result
    }
}

internal class TrieNodeIterator<out K, out V> {
    private var buffer = emptyArray<Any?>()
    private var dataSize = 0
    private var index = 0

    fun reset(buffer: Array<Any?>, dataSize: Int) {
        this.buffer = buffer
        this.dataSize = dataSize
        this.index = 0
    }

    fun hasNextEntry(): Boolean {
        return index < dataSize
    }

    fun nextKey(): K {
        assert(hasNextEntry())
        index += 2
        return buffer[index - 2] as K
    }

    fun nextValue(): V {
        assert(hasNextEntry())
        index += 2
        return buffer[index - 1] as V
    }

    fun nextEntry(): Map.Entry<K, V> {
        assert(hasNextEntry())
        index += 2
        return MapEntry(buffer[index - 2] as K, buffer[index - 1] as V)
    }

    fun currentNode(): TrieNode<out K, out V> {
        assert(hasNextNode())
        return buffer[index] as TrieNode<K, V>
    }

    fun hasNextNode(): Boolean {
        return index < buffer.size
    }

    fun moveToNextNode() {
        assert(hasNextNode())
        index++
    }
}

private class MapEntry<out K, out V>(override val key: K, override val value: V) : Map.Entry<K, V>

internal fun <K, V> Map.Entry<K, V>.toMutable(): MutableMap.MutableEntry<K, V> {
    return MapMutableEntry(key, value)
}

private class MapMutableEntry<K, V>(override val key: K, override var value: V) : MutableMap.MutableEntry<K, V> {
    override fun setValue(newValue: V): V {
        val previousValue = value
        value = newValue
        return previousValue
    }
}


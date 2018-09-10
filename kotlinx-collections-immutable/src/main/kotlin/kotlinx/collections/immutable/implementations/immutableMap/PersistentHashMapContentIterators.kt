package kotlinx.collections.immutable.implementations.immutableMap

internal const val TRIE_MAX_HEIGHT = 7

internal abstract class TrieNodeBaseIterator<out K, out V, out T> : Iterator<T> {
    protected var buffer = emptyArray<Any?>()
    private var dataSize = 0
    protected var index = 0

    fun reset(buffer: Array<Any?>, dataSize: Int, index: Int) {
        this.buffer = buffer
        this.dataSize = dataSize
        this.index = index
    }

    fun reset(buffer: Array<Any?>, dataSize: Int) {
        reset(buffer, dataSize, 0)
    }

    fun hasNextKey(): Boolean {
        return index < dataSize
    }

    fun currentKey(): K {
        assert(hasNextKey())
        return buffer[index] as K
    }

    fun moveToNextKey() {
        assert(hasNextKey())
        index += 2
    }

    fun hasNextNode(): Boolean {
        assert(index >= dataSize)
        return index < buffer.size
    }

    fun currentNode(): TrieNode<out K, out V> {
        assert(hasNextNode())
        return buffer[index] as TrieNode<K, V>
    }

    fun moveToNextNode() {
        assert(hasNextNode())
        index++
    }

    override fun hasNext(): Boolean {
        return hasNextKey()
    }
}

internal class TrieNodeKeysIterator<out K, out V> : TrieNodeBaseIterator<K, V, K>() {
    override fun next(): K {
        assert(hasNextKey())
        index += 2
        return buffer[index - 2] as K
    }
}

internal class TrieNodeValuesIterator<out K, out V> : TrieNodeBaseIterator<K, V, V>() {
    override fun next(): V {
        assert(hasNextKey())
        index += 2
        return buffer[index - 1] as V
    }
}

internal class TrieNodeEntriesIterator<out K, out V> : TrieNodeBaseIterator<K, V, Map.Entry<K, V>>() {
    override fun next(): Map.Entry<K, V> {
        assert(hasNextKey())
        index += 2
        return MapEntry(buffer[index - 2] as K, buffer[index - 1] as V)
    }
}

private class MapEntry<out K, out V>(override val key: K, override val value: V) : Map.Entry<K, V>


internal abstract class PersistentHashMapBaseIterator<K, V, T>(node: TrieNode<K, V>,
                                                                           protected val path: Array<TrieNodeBaseIterator<K, V, T>>) : Iterator<T> {
    private var pathLastIndex = 0
    private var hasNext = true

    init {
        path[0].reset(node.buffer, 2 * Integer.bitCount(node.dataMap))
        pathLastIndex = 0
        ensureNextEntryIsReady()
    }

    private fun moveToNextNodeWithData(pathIndex: Int): Int {
        if (path[pathIndex].hasNextKey()) {
            return pathIndex
        }
        if (path[pathIndex].hasNextNode()) {
            val node = path[pathIndex].currentNode()
            if (pathIndex == TRIE_MAX_HEIGHT - 1) {     // collision
                path[pathIndex + 1].reset(node.buffer, node.buffer.size)
            } else {
                path[pathIndex + 1].reset(node.buffer, 2 * Integer.bitCount(node.dataMap))
            }
            return moveToNextNodeWithData(pathIndex + 1)
        }
        return -1
    }

    private fun ensureNextEntryIsReady() {
        if (path[pathLastIndex].hasNextKey()) {
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

    protected fun currentKey(): K {
        return path[pathLastIndex].currentKey()
    }

    override fun hasNext(): Boolean {
        return hasNext
    }

    override fun next(): T {
        assert(hasNext())
        val result = path[pathLastIndex].next()
        ensureNextEntryIsReady()
        return result
    }
}

internal class PersistentHashMapEntriesIterator<K, V>(node: TrieNode<K, V>)
    : PersistentHashMapBaseIterator<K, V, Map.Entry<K, V>>(node, Array(TRIE_MAX_HEIGHT + 1) { TrieNodeEntriesIterator<K, V>() })

internal class PersistentHashMapKeysIterator<K, V>(node: TrieNode<K, V>)
    : PersistentHashMapBaseIterator<K, V, K>(node, Array(TRIE_MAX_HEIGHT + 1) { TrieNodeKeysIterator<K, V>() })

internal class PersistentHashMapValuesIterator<K, V>(node: TrieNode<K, V>)
    : PersistentHashMapBaseIterator<K, V, V>(node, Array(TRIE_MAX_HEIGHT + 1) { TrieNodeValuesIterator<K, V>() })

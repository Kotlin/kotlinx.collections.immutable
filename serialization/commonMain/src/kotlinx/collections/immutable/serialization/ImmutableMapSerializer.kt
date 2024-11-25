/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable.serialization

import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toPersistentHashMap
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

internal class DefaultImmutableMapSerializer<K, V, M : ImmutableMap<K, V>>(
    keySerializer: KSerializer<K>,
    valueSerializer: KSerializer<V>,
    private val transform: (Map<K, V>) -> M
) : KSerializer<M> {
    private val mapSerializer = MapSerializer(keySerializer, valueSerializer)

    override val descriptor: SerialDescriptor = mapSerializer.descriptor

    override fun serialize(encoder: Encoder, value: M) {
        return mapSerializer.serialize(encoder, value.toMap())
    }

    override fun deserialize(decoder: Decoder): M {
        return mapSerializer.deserialize(decoder).let(transform)
    }
}

public class ImmutableMapSerializer<T, V>(
    keySerializer: KSerializer<T>,
    valueSerializer: KSerializer<V>,
) : KSerializer<ImmutableMap<T, V>> by DefaultImmutableMapSerializer(
    keySerializer = keySerializer,
    valueSerializer = valueSerializer,
    transform = { decodedMap -> decodedMap.toImmutableMap() }
)

public class PersistentMapSerializer<T, V>(
    keySerializer: KSerializer<T>,
    valueSerializer: KSerializer<V>,
) : KSerializer<PersistentMap<T, V>> by DefaultImmutableMapSerializer(
    keySerializer = keySerializer,
    valueSerializer = valueSerializer,
    transform = { decodedMap -> decodedMap.toPersistentMap() }
)

public class PersistentHashMapSerializer<T, V>(
    keySerializer: KSerializer<T>,
    valueSerializer: KSerializer<V>,
) : KSerializer<PersistentMap<T, V>> by DefaultImmutableMapSerializer(
    keySerializer = keySerializer,
    valueSerializer = valueSerializer,
    transform = { decodedMap -> decodedMap.toPersistentHashMap() }
)

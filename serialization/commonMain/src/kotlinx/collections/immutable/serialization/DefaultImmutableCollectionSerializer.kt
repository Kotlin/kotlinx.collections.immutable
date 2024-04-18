/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.collections.immutable.serialization

import kotlinx.collections.immutable.ImmutableCollection
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.collections.immutable.toPersistentHashSet
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

internal class DefaultImmutableCollectionSerializer<T, C : ImmutableCollection<T>>(
    serializer: KSerializer<T>,
    private val transform: (Collection<T>) -> C
) : KSerializer<C> {
    private val listSerializer = ListSerializer(serializer)

    override val descriptor: SerialDescriptor = serializer.descriptor

    override fun serialize(encoder: Encoder, value: C) {
        return listSerializer.serialize(encoder, value.toList())
    }

    override fun deserialize(decoder: Decoder): C {
        return listSerializer.deserialize(decoder).let(transform)
    }
}

public class ImmutableListSerializer<T>(
    serializer: KSerializer<T>
) : KSerializer<ImmutableList<T>> by DefaultImmutableCollectionSerializer(
    serializer = serializer,
    transform = { decodedList -> decodedList.toImmutableList() }
)

public class PersistentListSerializer<T>(
    serializer: KSerializer<T>
) : KSerializer<PersistentList<T>> by DefaultImmutableCollectionSerializer(
    serializer = serializer,
    transform = { decodedList -> decodedList.toPersistentList() }
)

public class ImmutableSetSerializer<T>(
    serializer: KSerializer<T>
) : KSerializer<ImmutableSet<T>> by DefaultImmutableCollectionSerializer(
    serializer = serializer,
    transform = { decodedList -> decodedList.toImmutableSet() }
)

public class PersistentSetSerializer<T>(
    serializer: KSerializer<T>
) : KSerializer<PersistentSet<T>> by DefaultImmutableCollectionSerializer(
    serializer = serializer,
    transform = { decodedList -> decodedList.toPersistentSet() }
)

public class PersistentHashSetSerializer<T>(
    serializer: KSerializer<T>
) : KSerializer<PersistentSet<T>> by DefaultImmutableCollectionSerializer(
    serializer = serializer,
    transform = { decodedList -> decodedList.toPersistentHashSet() }
)

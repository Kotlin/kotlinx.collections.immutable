# Immutable Collections Library for Kotlin

[![JetBrains incubator project](https://jb.gg/badges/incubator.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub) [![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0) [ ![Download](https://api.bintray.com/packages/kotlin/kotlinx/kotlinx.collections.immutable/images/download.svg) ](https://bintray.com/kotlin/kotlinx/kotlinx.collections.immutable/_latestVersion)

Immutable collection interfaces and implementation prototypes for Kotlin.

For further details see the [proposal](proposal.md).

## What's in this library
### Interfaces and implementations

This library provides interfaces for immutable persistent collections:
    
| Interface | Bases | Implementations |
| ----------| ----- | --------------- |
| `ImmutableCollection` | `Collection`
| `ImmutableList` | `ImmutableCollection`, `List` | `immutableListOf` |
| `ImmutableSet` | `ImmutableCollection`, `Set` | `immutableSetOf`, `immutableHashSetOf` |
| `ImmutableMap` | `Map` | `immutableMapOf`, `immutableHashMapOf` |

The default implementations of `ImmutableSet` and `ImmutableMap`, which are returned by `immutableSetOf` and `immutableMapOf`
preserve the element insertion order during iteration. This comes at expense of maintaining more complex data structures.
If the order of elements doesn't matter, more efficient `immutableHashSetOf` and `immutableHashMapOf` could be used.

### Operations

#### toImmutableList/Set/Map
Converts a read-only or mutable collection to an immutable one.
If the receiver is already immutable and has the required type, returns itself.

```kotlin
fun Iterable<T>.toImmutableList(): ImmutableList<T>
fun Iterable<T>.toImmutableSet(): ImmutableSet<T>
```

#### `+` and `-` operators

`plus` and `minus` operators on immutable collections exploit their immutability
and delegate the implementation to the collections themselves. 
The operation is performed with persistence in mind: the returned immutable collection may share storage 
with the original collection.

```kotlin
val newList = immutableListOf("a", "b") + "c"
// newList is also ImmutableList
```

> **Note:** you need to import these operators from `kotlinx.collections.immutable` package
in order for them to take the precedence over the ones from the 
standard library.

```kotlin
import kotlinx.collections.immutable.*
```
   
#### Mutate

`mutate` extension function simplifies quite common pattern of immutable collection modification: 
get a builder, apply some mutating operations on it, transform it back to an immutable collection:

```kotlin
collection.builder().apply { some_actions_on(this) }.build()
```
    
With `mutate` it transforms to:

```kotlin
collection.mutate { some_actions_on(it) }
```

## Using in your projects

> Note that these libraries are experimental and are subject to change.

The libraries are published to [kotlinx](https://bintray.com/kotlin/kotlinx/kotlinx.collections.immutable) bintray repository.

These libraries require kotlin compiler version to be at least `1.1.0` and 
require kotlin runtime of the same version as a dependency.

### Maven

Add the bintray repository to `<repositories>` section:

```xml
<repository>
    <snapshots>
        <enabled>false</enabled>
    </snapshots>
    <id>kotlinx</id>
    <name>bintray</name>
    <url>https://dl.bintray.com/kotlin/kotlinx</url>
</repository>
```

Add dependencies (you can also add other modules that you need):

```xml
<dependency>
    <groupId>org.jetbrains.kotlinx</groupId>
    <artifactId>kotlinx-collections-immutable</artifactId>
    <version>0.1</version>
</dependency>
```

### Gradle

Add the bintray repository:

```groovy
repositories {
    maven {
        url "https://dl.bintray.com/kotlin/kotlinx"
    }
}
```

Add the dependency:

```groovy
compile 'org.jetbrains.kotlinx:kotlinx-collections-immutable:0.1'
```


## Building from source

You can build and install artifacts to maven local with:

    gradlew build install

# Immutable Collections Library for Kotlin

[![JetBrains incubator project](https://jb.gg/badges/incubator.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub) 
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0) 
[![Build status](https://teamcity.jetbrains.com/guestAuth/app/rest/builds/buildType:(id:KotlinTools_KotlinxCollectionsImmutable_Build_All)/statusIcon.svg)](https://teamcity.jetbrains.com/viewType.html?buildTypeId=KotlinTools_KotlinxCollectionsImmutable_Build_All)
[![Maven Central](https://img.shields.io/maven-central/v/org.jetbrains.kotlinx/kotlinx-collections-immutable.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22org.jetbrains.kotlinx%22%20AND%20a:%22kotlinx-collections-immutable%22)

Immutable collection interfaces and implementation prototypes for Kotlin.

This is a multiplatform library providing implementations for `jvm`, `js`, `mingwX64`, `linuxX64`, `macosX64`, `iosX64`, `iosArm64`, `iosArm32` Kotlin targets. 

For further details see the [proposal](proposal.md).

## What's in this library
### Interfaces and implementations

This library provides interfaces for immutable and persistent collections.

#### Immutable collection interfaces    
 
| Interface | Bases 
| ----------| ----- 
| `ImmutableCollection` | `Collection` |
| `ImmutableList` | `ImmutableCollection`, `List` | 
| `ImmutableSet` | `ImmutableCollection`, `Set` | 
| `ImmutableMap` | `Map` |

#### Persistent collection interfaces

| Interface | Bases 
| ----------| ----- 
| `PersistentCollection` | `ImmutableCollection` | 
| `PersistentList` | `PersistentCollection`, `ImmutableList` | 
| `PersistentSet` | `PersistentCollection`, `ImmutableSet` | 
| `PersistentMap` | `ImmutableMap` |

#### Persistent collection builder interfaces

| Interface | Bases 
| ----------| ----- 
| `PersistentCollection.Builder` | `MutableCollection` | 
| `PersistentList.Builder` | `PersistentCollection.Builder`, `MutableList` | 
| `PersistentSet.Builder` | `PersistentCollection.Builder`, `MutableSet` | 
| `PersistentMap.Builder` | `MutableMap` |


To instantiate an empty persistent collection or a collection with the specified elements use the functions 
`persistentListOf`, `persistentSetOf`, and `persistentMapOf`.

The default implementations of `PersistentSet` and `PersistentMap`, which are returned by `persistentSetOf` and `persistentMapOf`,
preserve the element insertion order during iteration. This comes at expense of maintaining more complex data structures.
If the order of elements doesn't matter, the more efficient implementations returned by the functions 
`persistentHashSetOf` and `persistentHashMapOf` can be used.

### Operations

#### toImmutableList/Set/Map

Converts a read-only or mutable collection to an immutable one.
If the receiver is already immutable and has the required type, returns it as is.

```kotlin
fun Iterable<T>.toImmutableList(): ImmutableList<T>
fun Iterable<T>.toImmutableSet(): ImmutableSet<T>
```

#### toPersistentList/Set/Map

Converts a read-only or mutable collection to a persistent one.
If the receiver is already persistent and has the required type, returns it as is.
If the receiver is a builder of the required persistent collection type, calls `build` on it and returns the result.

```kotlin
fun Iterable<T>.toPersistentList(): PersistentList<T>
fun Iterable<T>.toPersistentSet(): PersistentSet<T>
```

#### `+` and `-` operators

`plus` and `minus` operators on persistent collections exploit their immutability
and delegate the implementation to the collections themselves. 
The operation is performed with persistence in mind: the returned immutable collection may share storage 
with the original collection.

```kotlin
val newList = persistentListOf("a", "b") + "c"
// newList is also a PersistentList
```

> **Note:** you need to import these operators from `kotlinx.collections.immutable` package
in order for them to take the precedence over the ones from the 
standard library.

```kotlin
import kotlinx.collections.immutable.*
```
   
#### Mutate

`mutate` extension function simplifies quite common pattern of persistent collection modification: 
get a builder, apply some mutating operations on it, transform it back to a persistent collection:

```kotlin
collection.builder().apply { some_actions_on(this) }.build()
```
    
With `mutate` it transforms to:

```kotlin
collection.mutate { some_actions_on(it) }
```

## Using in your projects

> Note that the library is experimental and the API is subject to change.

The library is published to [kotlinx](https://bintray.com/kotlin/kotlinx/kotlinx.collections.immutable) bintray repository and available in jcenter too.

The library depends on the Kotlin Standard Library of the version at least `1.4.0`.

### Gradle

Add the bintray repository:

```groovy
repositories {
    jcenter()
}
```

In multiplatform projects add the following dependency to the common source set:

```groovy
kotlin {
    sourceSets {
        commonMain {
             dependencies {
                 implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.3")
             }
        }
    }
}
```

To use the library in a JVM-only project add the platform to the artifact name, e.g.:

```groovy
implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm:0.3.3")
```

### Maven

Add the bintray repository to `<repositories>` section:

```xml
<repository>
    <snapshots>
        <enabled>false</enabled>
    </snapshots>
    <id>jcenter</id>
    <name>jcenter</name>
    <url>https://jcenter.bintray.com/</url>
</repository>
```

Add dependencies (you can also add other modules that you need):

```xml
<dependency>
    <groupId>org.jetbrains.kotlinx</groupId>
    <artifactId>kotlinx-collections-immutable-jvm</artifactId>
    <version>0.3.3</version>
</dependency>
```

## Building from source

> :information_source: To build this project you will need to have a path to JDK 6 specified with the `JDK_6` environment variable or gradle property.
> For the local development purposes any JDK newer than that can be used instead.

You can build and install artifacts to maven local with:

    gradlew build install

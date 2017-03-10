# Immutable Collections Library for Kotlin

Immutable collection interfaces and implementation prototypes for Kotlin ([proposal](proposal.md))

Prototype implementation is based on [pcollections](http://pcollections.org/) (Copyright 2015 The pcollections Authors.)


## Basic usage

### Instantiating immutable lists

```kotlin

// create an empty immutable list
val emptyList = immutableListOf<String>()

// create an immutable list from the given elements:
val stringList = immutableListOf("foo", "bar")

// convert an existing list to an immutable list
val someList: List<Int> // = ...
val immutableList = someList.toImmutableList()
```

### Instantiating immutable sets

```kotlin
// create an empty immutable set
val emptyList = immutableSetOf<String>()

// create an immutable set from the given elements:
val stringList = immutableSetOf("foo", "bar")

// convert an existing set to an immutable set
val someSet: Set<Int> // = ...
val immutableSet = someSet.toImmutableSet()
```

Note that the `immutableSetOf` creates an ordered set that preserves element insertion order. 
If you don't care about the element order, you can create an unordered set with the 
`immutableHashSetOf` function. 


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
    <url>http://dl.bintray.com/kotlin/kotlinx</url>
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
        url "http://dl.bintray.com/kotlin/kotlinx"
    }
}
```

Add the dependency:

```groovy
compile 'org.jetbrains.kotlinx:kotlinx-collections-immutable:0.1'
```


## Building from source

To initialize submodules after checkout, use the following commands:

    git submodule init
    git submodule update

Then you can build and install artifacts to maven local with:

    gradlew build install

plugins {
    kotlin("jvm")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.openjdk.jmh:jmh-core:1.21")

    runtimeOnly(project(path = ":benchmarks", configuration = "benchmarksJar"))
    runtimeOnly(project(path = ":kotlinx-collections-immutable"))
}

sourceSets.main {
    kotlin.srcDirs("src")
}

tasks {
    val benchmarkParams = listOf(
        "remote",
        "forks",
        "measurementIterations",
        "measurementTime",
        "warmupIterations",
        "warmupTime",
        // "exclude",
        // "include",
        "size",
        "hashCodeType",
        "immutablePercentage",
    )

    mapOf(
        // map
        "benchmarkHashMap" to "HashMapRunnerKt",
        "benchmarkHashMapBuilder" to "HashMapBuilderRunnerKt",
        "benchmarkOrderedMap" to "OrderedMapRunnerKt",
        "benchmarkOrderedMapBuilder" to "OrderedMapBuilderRunnerKt",
        // set
        "benchmarkHashSet" to "HashSetRunnerKt",
        "benchmarkHashSetBuilder" to "HashSetBuilderRunnerKt",
        "benchmarkOrderedSet" to "OrderedSetRunnerKt",
        "benchmarkOrderedSetBuilder" to "OrderedSetBuilderRunnerKt",
        // list
        "benchmarkList" to "ListRunnerKt",
        "benchmarkListBuilder" to "ListBuilderRunnerKt",
    ).forEach { (taskName, taskRunner) ->
        create<JavaExec>(taskName) {
            group = "Benchmark"
            mainClass = "runners.$taskRunner"

            classpath = sourceSets.main.get().runtimeClasspath

            benchmarkParams.forEach { param ->
                project.findProperty(param)?.let { systemProperty(param, it) }
            }
        }
    }

    create("benchmarkAllMaps") {
        group = "Benchmark"
        dependsOn(
            "benchmarkHashMap",
            "benchmarkHashMapBuilder",
            "benchmarkOrderedMap",
            "benchmarkOrderedMapBuilder",
        )
    }

    create("benchmarkAllSets") {
        group = "Benchmark"
        dependsOn(
            "benchmarkHashSet",
            "benchmarkHashSetBuilder",
            "benchmarkOrderedSet",
            "benchmarkOrderedSetBuilder",
        )
    }

    create("benchmarkAllLists") {
        group = "Benchmark"
        dependsOn("benchmarkList", "benchmarkListBuilder")
    }

    create("benchmarkAll") {
        group = "Benchmark"
        dependsOn("benchmarkAllMaps", "benchmarkAllSets", "benchmarkAllLists")
    }
}
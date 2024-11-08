plugins {
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.openjdk.jmh:jmh-core:1.21")

    runtimeOnly(project(path = ":benchmarks", configuration = "benchmarksJar"))
    runtimeOnly(project(":kotlinx-collections-immutable"))
}

sourceSets.main {
    kotlin.srcDirs("src")
}

// map
val benchmarkHashMap by tasks.registering(JavaExec::class) {
    group = "Benchmark"
    mainClass = "runners.HashMapRunnerKt"
}

val benchmarkHashMapBuilder by tasks.registering(JavaExec::class) {
    group = "Benchmark"
    mainClass = "runners.HashMapBuilderRunnerKt"
}

val benchmarkOrderedMap by tasks.registering(JavaExec::class) {
    group = "Benchmark"
    mainClass = "runners.OrderedMapRunnerKt"
}

val benchmarkOrderedMapBuilder by tasks.registering(JavaExec::class) {
    group = "Benchmark"
    mainClass = "runners.OrderedMapBuilderRunnerKt"
}

val benchmarkAllMaps by tasks.registering {
    group = "Benchmark"
    dependsOn(benchmarkHashMap)
    dependsOn(benchmarkHashMapBuilder)
    dependsOn(benchmarkOrderedMap)
    dependsOn(benchmarkOrderedMapBuilder)
}

// set
val benchmarkHashSet by tasks.registering(JavaExec::class) {
    group = "Benchmark"
    mainClass = "runners.HashSetRunnerKt"
}

val benchmarkHashSetBuilder by tasks.registering(JavaExec::class) {
    group = "Benchmark"
    mainClass = "runners.HashSetBuilderRunnerKt"
}

val benchmarkOrderedSet by tasks.registering(JavaExec::class) {
    group = "Benchmark"
    mainClass = "runners.OrderedSetRunnerKt"
}

val benchmarkOrderedSetBuilder by tasks.registering(JavaExec::class) {
    group = "Benchmark"
    mainClass = "runners.OrderedSetBuilderRunnerKt"
}

val benchmarkAllSets by tasks.registering {
    group = "Benchmark"
    dependsOn(benchmarkHashSet)
    dependsOn(benchmarkHashSetBuilder)
    dependsOn(benchmarkOrderedSet)
    dependsOn(benchmarkOrderedSetBuilder)
}

// list
val benchmarkList by tasks.registering(JavaExec::class) {
    group = "Benchmark"
    mainClass = "runners.ListRunnerKt"
}

val benchmarkListBuilder by tasks.registering(JavaExec::class) {
    group = "Benchmark"
    mainClass = "runners.ListBuilderRunnerKt"
}

val benchmarkAllLists by tasks.registering {
    group = "Benchmark"
    dependsOn(benchmarkList)
    dependsOn(benchmarkListBuilder)
}

// all
val benchmarkAll by tasks.registering {
    group = "Benchmark"
    dependsOn(benchmarkAllMaps)
    dependsOn(benchmarkAllSets)
    dependsOn(benchmarkAllLists)
}

// configure runner tasks

val benchmarkParams = listOf(
    "remote",
    "forks",
    "measurementIterations",
    "measurementTime",
    "warmupIterations",
    "warmupTime",
//      "exclude",
//      "include",
    "size",
    "hashCodeType",
    "immutablePercentage"
)

tasks.withType<JavaExec>().configureEach {
    if (group == "Benchmark") {
        classpath = sourceSets.main.get().runtimeClasspath

        benchmarkParams.forEach { param ->
            if (project.hasProperty(param)) {
                systemProperty(param, requireNotNull(project.property(param)))
            }
        }
    }
}

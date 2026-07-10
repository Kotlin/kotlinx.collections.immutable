# Test coverage campaign

Part of the stabilization effort: bring the core module to 100% line coverage with tests that are necessary and sufficient. Every new test is justified by specific lines that no existing test covers deterministically, and asserts real behavior; no test exists only to tick coverage.

Result after the validation pass ([06-validation.md](06-validation.md)): 2821 of 2827 lines covered on the deterministic subset. The 6 uncovered lines are provably dead defensive code in the set trie (see the findings in [02-gap-analysis.md](02-gap-analysis.md)); removing them in a follow-up would make the report a literal 100%. Every line reachable by any real consumer, from Kotlin or Java, is covered. Classifying the branch-coverage gaps during validation also exposed two shipped production bugs, both fixed here: `putAll` kept stale values for hash-colliding keys, and `removeAll` lost the last 32 survivors when they filled whole leaves exactly. Branch coverage is 94.87%.

## Ground truth: the deterministic subset

Coverage is measured with Kover on the JVM target over the full `jvmTest` suite minus `tests.stress.*`. The stress tests draw sizes and hash codes from an unseeded `Random`, so which trie branches they reach differs from run to run: at baseline they covered 437 lines by luck (see [01-baseline.md](01-baseline.md)). A 100% claim on a run that includes them would not be reproducible. Reaching 100% on the deterministic subset makes every full-suite run trivially 100% as well.

The Guava testlib compliance suites in `jvmTest` are deterministic and stay in the measured subset.

## Reproducing

```
./gradlew :kotlinx-collections-immutable:koverVerify -PjvmTestExcludes=tests.stress.*   # gate: at most 6 missed lines (the dead ones)
./gradlew :kotlinx-collections-immutable:koverHtmlReport -PjvmTestExcludes=tests.stress.*
```

The gate is a missed-lines bound of 6 rather than a percentage: the allowance is exactly the dead code, and any net-new missed line pushes the count past the bound (the bound counts lines; it cannot pin these six). No Kover exclusion is used: a class-level filter would hide the ~800 covered lines of `TrieNode`, and an annotation-based one would require annotating production code; dead code stays visible in the report instead.

The `jvmTestExcludes` property is a comma-separated list of test-class patterns fed to `Test.filter.excludeTestsMatching`; Gradle's `--tests` flag cannot express exclusion.

## Scope

JVM-measured coverage covers all of `commonMain` plus the `jvmMain` `assert` actual, i.e. the entire library logic. Out of scope: the three non-JVM `assert` actuals (`core/{js,native,wasm}Main/src/internal/commonFunctions.kt`, one trivial function each), which no JVM-based tool can measure. The new tests were executed on JVM, JS and both Wasm targets, so the JS/Wasm actuals run there, just not counted. Native targets compile in CI but were not executed here: the working machine cannot run them (see [06-validation.md](06-validation.md)).

## Decisions

- Metric: 100% line coverage. Branch coverage is reported and improved, not gated. The validation classified every missed branch and closed the reachable ones; the remainder is defensive guards, statically dead sides and compiler artifacts, plus four named marginal slots ([06-validation.md](06-validation.md)).
- Code unreachable through the public API gets a direct internal-API test from `commonTest` (internals are visible within the module). Kover exclusions: none. Provably dead code stays visible in the report and is documented as a finding instead of being filtered away.
- The necessity rule applies to the tests added here. No pre-existing test was modified or removed (one existing white-box file gained a new test method); the stress and Guava suites overlap intentionally for regression confidence.

## Contents

- [01-baseline.md](01-baseline.md): tooling setup and baseline numbers.
- [02-gap-analysis.md](02-gap-analysis.md): why 575 lines were uncovered, by category.
- [03-test-plan.md](03-test-plan.md): gap partition and test-file ownership.
- [04-coverage-map.md](04-coverage-map.md): new test -> claimed lines mapping, knockout checks.
- [05-verification.md](05-verification.md): the campaign's final numbers and cross-platform runs.
- [06-validation.md](06-validation.md): independent validation: every number re-measured, tests adjudicated, two production bugs found and fixed, branch gaps classified, final numbers.

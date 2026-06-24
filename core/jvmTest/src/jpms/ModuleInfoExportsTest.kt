/*
 * Copyright 2016-2026 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package tests.jpms

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class ModuleInfoExportsTest {
    @Test
    fun everyPublicApiPackageIsExported() {
        val missing = readPublicApiPackages() - readExportedPackages()
        assertTrue(
            missing.isEmpty(),
            """
            Packages with public API (in api/kotlinx-collections-immutable.api) that are not exported by jvmMain/java9/module-info.java: $missing.
            Add `exports <package>;` for each.
            """.trimIndent()
        )
    }

    @Test
    fun everyExportedPackageHasPublicApi() {
        val staleExports = readExportedPackages() - readPublicApiPackages()
        assertTrue(
            staleExports.isEmpty(),
            """
            Packages exported by jvmMain/java9/module-info.java with no public API in api/kotlinx-collections-immutable.api: $staleExports.
            Remove each stale `exports`.
            """.trimIndent()
        )
    }

    private fun readExportedPackages(): Set<String> =
        readFile("moduleInfoPath").readLines()
            .mapNotNull { EXPORTS.matchEntire(it.trim())?.groupValues?.get(1) }
            .toSet()

    private fun readPublicApiPackages(): Set<String> =
        readFile("apiDumpPath").readLines()
            .mapNotNull { CLASS_DECL.find(it)?.groupValues?.get(1) }
            .map { fqn -> fqn.substringBeforeLast('/').replace('/', '.') }
            .toSet()

    private fun readFile(prop: String): File =
        File(System.getProperty(prop) ?: error("System property '$prop' is not set; run via Gradle"))

    private companion object {
        val EXPORTS = Regex("""exports\s+([\w.]+)\s*;""")
        val CLASS_DECL = Regex("""^(?:public|protected).*\bclass\s+(\S+)""")
    }
}

package com.visionrt.core.quality

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Enforces the module dependency allowlist from docs/ARCHITECTURE.md §4.
 *
 * Rule of thumb: nothing above core may talk to modules it does not own, and
 * no Android framework surface is reachable except through its owner module.
 * The allowlist is a DAG, so cycles are structurally impossible.
 */
class ModuleBoundaryTest {

    private val repoRoot: File by lazy {
        var dir = File(System.getProperty("user.dir"))
        while (dir.parentFile != null && !File(dir, "settings.gradle.kts").exists()) {
            dir = dir.parentFile
        }
        val cwd = System.getProperty("user.dir")
        require(File(dir, "settings.gradle.kts").exists()) { "repo root not found from $cwd" }
        dir
    }

    private val allowlist: Map<String, Set<String>> = mapOf(
        "core" to emptySet(),
        "feature" to setOf("core"),
        "perception" to setOf("core"),
        "inference" to setOf("core", "perception"),
        "feedback" to setOf("core"),
        "data" to setOf("core"),
        "benchmark" to setOf("core"),
        "app" to setOf("core", "feature", "data"),
    )

    @Test
    fun everyModuleIsCoveredByTheAllowlist() {
        val present = modulesWithBuildFile()
        assertEquals("Every module with a build file must be in the allowlist", allowlist.keys, present)
    }

    @Test
    fun modulesRespectTheDependencyAllowlist() {
        allowlist.forEach { (module, allowed) ->
            val declared = projectDeps(File(repoRoot, module))
            val forbidden = declared - allowed
            assertTrue(
                "Module :$module declares forbidden project dependencies: $forbidden (allowed: $allowed)",
                forbidden.isEmpty()
            )
        }
    }

    @Test
    fun declaredDependenciesExist() {
        allowlist.forEach { (module, allowed) ->
            allowed.forEach { dep ->
                assertTrue("Module :$module depends on missing module :$dep", File(repoRoot, dep).isDirectory)
            }
        }
    }

    @Test
    fun noModuleDependsOnItself() {
        allowlist.forEach { (module, allowed) ->
            assertTrue("Module :$module must not depend on itself", module !in allowed)
        }
    }

    private val projectDepRegex = Regex("project\\(\":([a-zA-Z0-9_-]+)\"\\)")

    private fun projectDeps(dir: File): Set<String> =
        projectDepRegex.findAll(File(dir, "build.gradle.kts").readText())
            .map { it.groupValues[1] }
            .toSet()

    private fun modulesWithBuildFile(): Set<String> =
        repoRoot.listFiles { f -> f.isDirectory && File(f, "build.gradle.kts").exists() }!!
            .map { it.name }
            .toSet()
}

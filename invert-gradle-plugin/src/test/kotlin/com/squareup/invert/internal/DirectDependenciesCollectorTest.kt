package com.squareup.invert.internal

import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DirectDependenciesCollectorTest {

  @Test
  fun collect_includesExternalDependenciesForFilteredConfigurations() {
    val root = ProjectBuilder.builder().withName("root").build()
    val app = ProjectBuilder.builder().withName("app").withParent(root).build()

    app.configurations.maybeCreate("implementation")

    app.dependencies.add("implementation", "com.acme:lib:1.2.3")

    val result = DirectDependenciesCollector().collect(
      configurations = app.configurations.toSet(),
      filteredConfigurationNames = setOf("implementation"),
    )

    assertTrue(result.containsKey("implementation"))
    assertEquals(setOf("com.acme:lib:1.2.3"), result["implementation"]) 
  }

  @Test
  fun collect_includesProjectDependenciesForFilteredConfigurations() {
    val root = ProjectBuilder.builder().withName("root").build()
    val lib = ProjectBuilder.builder().withName("lib").withParent(root).build()
    val app = ProjectBuilder.builder().withName("app").withParent(root).build()

    app.configurations.maybeCreate("implementation")

    app.dependencies.add("implementation", app.dependencies.project(mapOf("path" to ":lib")))

    val result = DirectDependenciesCollector().collect(
      configurations = app.configurations.toSet(),
      filteredConfigurationNames = setOf("implementation"),
    )

    assertTrue(result.containsKey("implementation"))
    assertEquals(setOf(":lib"), result["implementation"]) 
  }

  @Test
  fun collect_includesSpecialConfigurationsAndFiltersOutEmptyConfigs() {
    val root = ProjectBuilder.builder().withName("root").build()
    val app = ProjectBuilder.builder().withName("app").withParent(root).build()

    app.configurations.maybeCreate("kapt")
    app.configurations.maybeCreate("ksp")
    app.configurations.maybeCreate("annotationProcessor")
    app.configurations.maybeCreate("kotlinCompilerPluginClasspathMain")
    app.configurations.maybeCreate("empty")

    app.dependencies.add("kapt", "com.foo:processor:0.1")
    app.dependencies.add("ksp", "com.bar:proc:0.2")
    app.dependencies.add("annotationProcessor", "com.baz:ap:0.3")
    app.dependencies.add("kotlinCompilerPluginClasspathMain", "com.qux:plugin:0.4")
    // leave "empty" without dependencies

    val result = DirectDependenciesCollector().collect(
      configurations = app.configurations.toSet(),
      filteredConfigurationNames = setOf("empty"),
    )

    assertTrue(result.containsKey("kapt"))
    assertTrue(result.containsKey("ksp"))
    assertTrue(result.containsKey("annotationProcessor"))
    assertTrue(result.containsKey("kotlinCompilerPluginClasspathMain"))
    // "empty" is in the filtered set but has no deps, so it should be filtered out
    assertFalse(result.containsKey("empty"))
  }
}



package com.squareup.invert

import com.squareup.invert.internal.IncludeAnySubprojectCalculator
import com.squareup.invert.internal.IncludeRuntimeClasspathConfigurationsCalculator
import com.squareup.invert.internal.NoOpInvertOwnershipCollector
import com.squareup.invert.models.ConfigurationName
import com.squareup.invert.models.js.TechDebtInitiative
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile

/**
 * Extension for configuring the [InvertGradlePlugin]
 */
open class InvertExtension(project: Project) {

  private val objects = project.objects

  /**
   * User-provided [StatCollector]s via [InvertExtension].
   */
  @get:Input
  internal val statCollectors = objects.domainObjectContainer(StatCollector::class.java)

  /**
   * User-provided [com.squareup.invert.models.js.TechDebtInitiative]s via [com.squareup.invert.InvertExtension].
   */
  @get:Input
  internal val techDebtInitiatives = objects.listProperty(TechDebtInitiative::class.java)

  /**
   * User-provided [InvertOwnershipCollector]s via [InvertExtension].
   */
  @get:Input
  internal val ownershipCollectorProperty = objects.property(InvertOwnershipCollector::class.java)

  /**
   * A user provided lambda which calculates if a project should be included in invert analysis.
   */
  @get:Input
  internal val includeSubProjectCalculatorProperty =
    objects.property(InvertIncludeSubProjectCalculator::class.java)

  /**
   * User-provided [InvertIncludeConfigurationCalculator]s via [InvertExtension].
   *
   * A user can specify this property to include only certain configurations to scan with Invert.
   *
   * NOTE: If the [Project] is determined by [includeSubProjectCalculatorProperty] to not be
   * included, then this method will NOT be called for that [Project]
   */
  @get:Input
  internal val includeConfigurationProperty =
    objects.property(InvertIncludeConfigurationCalculator::class.java)

  @get:InputFile
  @get:Input
  internal val historicalDataFileProperty = objects.property<String?>(String::class.java)

  fun ownershipCollector(ownershipCollector: InvertOwnershipCollector) {
    ownershipCollectorProperty.set(ownershipCollector)
  }

  fun historicalData(historicalDataFile: String) {
    this.historicalDataFileProperty.set(historicalDataFile)
  }

  fun includeSubproject(invertShouldIncludeSubProject: (subproject: Project) -> Boolean) {
    includeSubProjectCalculatorProperty.set(object : InvertIncludeSubProjectCalculator {
      override fun invoke(
        subproject: Project,
      ): Boolean = invertShouldIncludeSubProject(subproject)
    })
  }

  fun includeConfigurations(
    includeConfigurationsCalculator: (
      project: Project,
      configurationNames: Collection<String>
    ) -> Collection<ConfigurationName>
  ) {
    includeConfigurationProperty.set(object : InvertIncludeConfigurationCalculator {
      override fun invoke(
        project: Project,
        configurationNames: Collection<ConfigurationName>
      ): Collection<ConfigurationName> =
        includeConfigurationsCalculator(
          project,
          configurationNames
        )
    })
  }

  fun addStatCollector(statCollector: StatCollector) {
    statCollectors.add(statCollector)
  }

  fun addTechDebtInitiative(techDebtInitiative: TechDebtInitiative) {
    techDebtInitiatives.add(techDebtInitiative)
  }

  internal fun getHistoricalDataFilePath(): String? {
    return historicalDataFileProperty.orNull
  }

  internal fun getStatCollectors(): Collection<StatCollector> {
    return statCollectors
  }

  internal fun getTechDebtInitiatives(): Collection<TechDebtInitiative> {
    return techDebtInitiatives.getOrElse(emptyList())
  }

  internal fun getConfigurationsForProjectCalculator(): InvertIncludeConfigurationCalculator {
    return includeConfigurationProperty.getOrElse(IncludeRuntimeClasspathConfigurationsCalculator)
  }

  internal fun getShouldIncludeSubprojectCalculator(): InvertIncludeSubProjectCalculator {
    return includeSubProjectCalculatorProperty.getOrElse(IncludeAnySubprojectCalculator)
  }

  internal fun getOwnershipCollector(): InvertOwnershipCollector {
    return ownershipCollectorProperty.getOrElse(
      NoOpInvertOwnershipCollector
    )
  }
}

fun Project.getInvertExtension(): InvertExtension {
  return project.extensions.findByType(InvertExtension::class.java) ?: project.extensions.create(
    "invert",
    InvertExtension::class.java,
    project
  )
}

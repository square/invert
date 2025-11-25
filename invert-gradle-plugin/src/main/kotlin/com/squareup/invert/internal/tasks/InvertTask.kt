package com.squareup.invert.internal.tasks

import com.squareup.invert.InvertExtension
import com.squareup.invert.InvertOwnershipCollector
import com.squareup.invert.ReportOutputConfig
import com.squareup.invert.StatCollector
import com.squareup.invert.internal.AggregatedCodeReferences
import com.squareup.invert.internal.CollectedStatAggregator
import com.squareup.invert.internal.InvertFileUtils
import com.squareup.invert.internal.NoOpInvertOwnershipCollector
import com.squareup.invert.internal.isRootProject
import com.squareup.invert.internal.models.CollectedStatsForProject
import com.squareup.invert.internal.models.InvertCombinedCollectedData
import com.squareup.invert.internal.report.GradleProjectAnalysisCombiner
import com.squareup.invert.internal.report.InvertReportWriter
import com.squareup.invert.internal.report.json.InvertJsonReportWriter
import com.squareup.invert.internal.report.sarif.InvertSarifReportWriter
import com.squareup.invert.logging.GradleInvertLogger
import com.squareup.invert.logging.InvertLogger
import com.squareup.invert.models.ExtraDataType
import com.squareup.invert.models.ExtraMetadata
import com.squareup.invert.models.InvertSerialization.InvertJson
import com.squareup.invert.models.ModulePath
import com.squareup.invert.models.OwnerName
import com.squareup.invert.models.Stat
import com.squareup.invert.models.StatMetadata
import com.squareup.invert.models.js.HistoricalData
import com.squareup.invert.models.js.TechDebtInitiative
import com.squareup.invert.models.js.TechDebtInitiativeConfig
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.ListSerializer
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.UrlArtifactRepository
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * This task orchestrates analysis on submodules and the result creates 3 formats:
 * 1. HTML/JS report
 * 2. JSON Artifacts that contain all data collected about submodules.
 * 3. SARIF artifacts that contain data collected about submodules using the SARIF[https://docs.oasis-open.org/sarif/sarif/v2.1.0/] format.
 */
abstract class InvertTask : DefaultTask() {

  companion object {
    const val TASK_NAME = "invert"
  }

  @get:Input
  abstract val forRootProject: Property<Boolean>

  @get:Internal
  abstract var statCollectors: List<StatCollector>?

  @get:Internal
  abstract var techDebtInitiatives: List<TechDebtInitiative>?

  @get:Internal
  abstract var ownershipCollector: InvertOwnershipCollector?

  @get:Optional
  @get:Input
  abstract val historicalDataFileProperty: Property<String?>

  @get:Input
  abstract val projectPath: Property<String>

  @get:Internal
  abstract val rootProjectDirPath: Property<String>

  @get:Input
  abstract val subprojectInvertReportDirs: ListProperty<String>

  @get:Input
  abstract val timeZoneId: Property<String>

  @get:Input
  abstract val mavenRepoUrls: ListProperty<String>

  @get:OutputDirectory
  abstract val rootBuildReportsDir: DirectoryProperty

  private fun invertLogger(): InvertLogger = GradleInvertLogger(logger)

  @TaskAction
  internal fun execute() {
    val timeZoneId = this.timeZoneId.get()
    val datePatternFormat = "MMMM dd, yyyy[ 'at' HH:mm:ss]"
    val mavenRepoUrls = this.mavenRepoUrls.get()
    val ownershipCollector = ownershipCollector ?: NoOpInvertOwnershipCollector
    runBlocking {
      val invertReportDir = rootBuildReportsDir.get().asFile
      val gitProjectDir = File(".") // TODO Pass in the root of the Git Repo
      val reportMetadata = ProjectMetadataCollector.gatherProjectMetadata(
        timeZoneId = timeZoneId,
        datePatternFormat = datePatternFormat,
        logger = invertLogger(),
        repoUrls = mavenRepoUrls,
        gitProjectDir = gitProjectDir,
        ownershipCollector = ownershipCollector,
      )
      invertLogger().lifecycle("Finished project metadata collection.")

      val allCollectedDataOrig: InvertCombinedCollectedData = GradleProjectAnalysisCombiner
        .combineAnalysisResults(subprojectInvertReportDirs.get())

      val reportOutputConfig = ReportOutputConfig(
        gitCloneDir = File(rootProjectDirPath.get()),
        invertReportDirectory = invertReportDir,
        ownershipCollector = ownershipCollector,
      )

      invertLogger().info("Report output config: $reportOutputConfig")

      val allCollectedData = CollectedStatAggregator.aggregate(
        origAllCollectedData = allCollectedDataOrig,
        reportMetadata = reportMetadata,
        statCollectorsForAggregation = statCollectors,
        reportOutputConfig = reportOutputConfig
      )

      invertLogger().lifecycle("Finished aggregated data collection pass.")

      val historicalDataFile: File? = historicalDataFileProperty.orNull?.let { File(it) }
      val historicalData: Set<HistoricalData> =
        if (historicalDataFile?.isFile == true && historicalDataFile.length() > 0) {
          try {
            val fileContents = historicalDataFile.readText()
            InvertJson.decodeFromString(ListSerializer(HistoricalData.serializer()), fileContents)
          } catch (e: Exception) {
            invertLogger().warn("Failed to read historical data file: $e")
            listOf()
          }
        } else {
          listOf()
        }.toSet()

      InvertReportWriter(
        invertLogger = invertLogger(),
        rootBuildReportsDir = invertReportDir,
      ).writeProjectData(
        reportMetadata = reportMetadata,
        collectedData = allCollectedData,
        historicalData = historicalData,
        techDebtInitiatives = techDebtInitiatives ?: emptyList(),
      )
    }
  }

  fun setParams(
    project: Project,
    extension: InvertExtension,
    subprojectInvertReportDirs: List<String>,
  ) {
    val timeZoneId = "America/New_York"
    this.timeZoneId.set(timeZoneId)

    this.subprojectInvertReportDirs.set(subprojectInvertReportDirs)
    this.forRootProject.set(project.isRootProject())
    this.projectPath.set(project.path)
    this.rootBuildReportsDir.set(
      project.layout.buildDirectory.dir(
        InvertFileUtils.REPORTS_SLASH_INVERT_PATH
      )
    )
    this.rootProjectDirPath.set(
      project.rootProject.layout.projectDirectory.asFile.canonicalPath
    )

    this.historicalDataFileProperty.set(extension.getHistoricalDataFilePath())
    this.statCollectors = extension.getStatCollectors().toList()
    this.techDebtInitiatives = extension.getTechDebtInitiatives().toList()
    this.ownershipCollector = extension.getOwnershipCollector()

    this.mavenRepoUrls.set(
      project.rootProject.buildscript.repositories
        .plus(project.rootProject.project.repositories)
        .filterIsInstance<UrlArtifactRepository>()
        .map { it.url.toURL().toString() }
        .map {
          if (it.endsWith("/")) {
            it.dropLast(1)
          } else {
            it
          }
        }
        .distinct()
    )
  }
}

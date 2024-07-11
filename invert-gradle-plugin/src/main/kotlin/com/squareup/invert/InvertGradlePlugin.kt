package com.squareup.invert

import com.squareup.invert.internal.InvertFileUtils
import com.squareup.invert.internal.isRootProject
import com.squareup.invert.internal.tasks.InvertCleanTask
import com.squareup.invert.internal.tasks.InvertCollectDependenciesTask
import com.squareup.invert.internal.tasks.InvertCollectOwnershipTask
import com.squareup.invert.internal.tasks.InvertCollectStatsTask
import com.squareup.invert.internal.tasks.InvertCollectTask
import com.squareup.invert.internal.tasks.InvertTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import java.io.File

/**
 * "Invert"s your view of your Gradle Project to allow you to view it after static analysis.
 *
 * Your custom collectors can be provided via the [InvertExtension] on [InvertGradlePlugin].
 */
@Suppress("unused")
class InvertGradlePlugin : Plugin<Project> {

    companion object {
        const val ID = "com.squareup.invert"
    }

    override fun apply(rootProject: Project): Unit = rootProject.run {
        val extension = this.getInvertExtension()

        if (!rootProject.isRootProject()) {
            throw IllegalStateException("Cannot apply $ID to a non-root project")
        }

        val invertCleanTask = registerInvertCleanTask(rootProject)
        rootProject.afterEvaluate { rootProject ->
            val subprojectsToRegisterOn = rootProject.subprojects
                .filter { shouldRegisterOnSubproject(it, extension) }

            val rootInvertTask = rootProject.tasks.register(
                InvertTask.TASK_NAME,
                InvertTask::class.java
            ) { reportTask ->
                reportTask.setParams(
                    project = rootProject,
                    subprojectInvertReportDirs = subprojectsToRegisterOn.map {
                        File(
                            it.layout.buildDirectory.get().asFile,
                            InvertFileUtils.REPORTS_SLASH_INVERT_PATH
                        ).absolutePath
                    }
                )
            }

            subprojectsToRegisterOn.forEach { subproject ->
                // Without evaluationDependsOn, subproject won't be configured to access the dependency graph.
                rootProject.evaluationDependsOn(subproject.path)
                registerOnSubproject(subproject, rootInvertTask, invertCleanTask, extension)
            }
        }
    }

    private fun shouldRegisterOnSubproject(
        subproject: Project,
        extension: InvertExtension
    ): Boolean {
        if (hasBuildGradleFile(subproject)) {
            return extension.getShouldIncludeSubprojectCalculator().invoke(subproject)
        }
        return false
    }

    private fun hasBuildGradleFile(subproject: Project): Boolean {
        return subproject.file("build.gradle").exists() || subproject.file("build.gradle.kts").exists()
    }

    private fun registerInvertCleanTask(target: Project): TaskProvider<InvertCleanTask> {
        return target.tasks.register(
            InvertCleanTask.TASK_NAME,
            InvertCleanTask::class.java
        ) { task ->
            task.setParams(
                project = target,
            )
        }
    }

    private fun registerOnSubproject(
        subproject: Project,
        rootInvertTaskProvider: TaskProvider<InvertTask>,
        rootInvertCleanTaskProvider: TaskProvider<InvertCleanTask>,
        extension: InvertExtension
    ) {
        val ownershipTask = subproject.tasks.register(
            InvertCollectOwnershipTask.TASK_NAME,
            InvertCollectOwnershipTask::class.java
        ) { task ->
            task.setParams(
                project = subproject,
                extension = extension,
            )
        }

        val collectStatsTask = subproject.tasks.register(
            InvertCollectStatsTask.TASK_NAME,
            InvertCollectStatsTask::class.java
        ) { task ->
            task.setParams(
                project = subproject,
                extension = extension,
            )
        }

        val collectDependenciesTask = subproject.tasks.register(
            InvertCollectDependenciesTask.TASK_NAME,
            InvertCollectDependenciesTask::class.java
        ) { task ->
            task.setParams(
                project = subproject,
                extension = extension,
            )
        }

        val subprojectCollectTask = subproject.tasks.register(
            InvertCollectTask.TASK_NAME,
            InvertCollectTask::class.java
        ) { collectTask ->
            collectTask.setParams(
                project = subproject,
            )

            collectTask.dependsOn(collectStatsTask)
            collectTask.dependsOn(ownershipTask)
            collectTask.dependsOn(collectDependenciesTask)
        }

        rootInvertTaskProvider.configure { rootInvertTask ->
            rootInvertTask.dependsOn(subprojectCollectTask)
        }

        rootInvertCleanTaskProvider.configure { rootInvertCleanTask ->
            val subprojectCleanTask = registerInvertCleanTask(subproject)
            rootInvertCleanTask.dependsOn(subprojectCleanTask)
        }
    }
}

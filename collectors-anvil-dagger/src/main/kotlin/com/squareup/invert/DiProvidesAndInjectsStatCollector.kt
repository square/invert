package com.squareup.invert

import com.squareup.invert.models.CollectedStatType
import com.squareup.invert.models.Stat
import com.squareup.invert.models.StatMetadata
import java.io.File

/**
 * This collector invokes the [FindAnvilContributesBinding] class collect all instances
 * of the Anvil ContributesBinding annotation, and puts it into a Stat that can be used collected
 * by the [InvertGradlePlugin]
 */
class DiProvidesAndInjectsStatCollector : StatCollector {
    override fun collect(
        rootProjectFolder: File,
        projectPath: String,
        kotlinSourceFiles: List<File>
    ): List<CollectedStat>? {
        val findAnvil = FindAnvilContributesBinding()
        kotlinSourceFiles.forEach { kotlinFile ->
            findAnvil.handleKotlinFile(
                file = kotlinFile,
                relativeFilePath = kotlinFile.absolutePath.replace(rootProjectFolder.absolutePath, "").drop(1)
            )
        }

        val contributionsAndConsumption = findAnvil.getCollectedContributionsAndConsumptions()
        return if (contributionsAndConsumption.isNotEmpty()) {
            listOf(
                CollectedStat(
                    metadata = statMetadata,
                    stat = Stat.DiProvidesAndInjectsStat(contributionsAndConsumption)
                )
            )
        } else {
            null
        }

    }

    val statMetadata: StatMetadata = StatMetadata(
        key = "DiProvidesAndInjects",
        description = "Dependency Inject Provides and Injects",
        statType = CollectedStatType.DI_PROVIDES_AND_INJECTS
    )

    override fun getName(): String {
        return this::class.java.name
    }
}
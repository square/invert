package com.squareup.invert

import com.squareup.invert.models.Stat
import com.squareup.invert.models.StatMetadata
import org.gradle.api.Named
import java.io.File


data class CollectedStat(
    val metadata: StatMetadata,
    val stat: Stat?
)

/**
 * Interface representing all Invert [StatCollector]s.
 *
 * We must extend the [Named] interface to be used as a task input.
 */
interface StatCollector : Named {


//    /**
//     * Metadata about what data is being collected
//     */
//    val statMetadata: StatMetadata

    fun collect(
        rootProjectFolder: File,
        projectPath: String,
        kotlinSourceFiles: List<File>,
    ): List<CollectedStat>?

//    /**
//     * A [StatCollector] that collects [Stat.DiProvidesAndInjectsStat] data.
//     */
//    interface AllPurpose : StatCollector {
//        fun collect(
//            rootProjectFolder: File,
//            projectPath: String,
//            kotlinSourceFiles: List<File>,
//        ): List<Stat>?
//    }

//    /**
//     * A [StatCollector] that collects [BooleanStat] data.
//     */
//    interface BooleanStatCollector : StatCollector {
//        fun collect(
//            kotlinSourceFiles: List<File>
//        ): BooleanStat?
//    }
//
//    /**
//     * A [StatCollector] that collects [GenericStatCollector] data.
//     */
//    interface GenericStatCollector : StatCollector {
//        fun collect(
//            srcFolder: File,
//            projectPath: String,
//            kotlinSourceFiles: List<File>
//        ): Stat.StringStat?
//    }
//
//    /**
//     * A [StatCollector] that collects [Stat.DiProvidesAndInjectsStat] data.
//     */
//    interface ProvidesAndInjectsStatCollector : StatCollector {
//        fun collect(
//            rootProjectFolder: File,
//            projectPath: String,
//            kotlinSourceFiles: List<File>
//        ): Stat.DiProvidesAndInjectsStat?
//    }
}

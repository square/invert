package com.squareup.invert

import org.gradle.api.Named
import java.io.File


/**
 * Interface representing all Invert [StatCollector]s.
 *
 * We must extend the [Named] interface to be used as a task input.
 */
interface StatCollector : Named {

    fun collect(
        rootProjectFolder: File,
        projectPath: String,
        sourceFiles: List<File>,
    ): List<CollectedStat>?

}

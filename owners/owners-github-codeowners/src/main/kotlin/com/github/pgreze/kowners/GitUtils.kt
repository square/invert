/**
 * Implementation from https://github.com/pgreze/kowners
 */
package com.github.pgreze.kowners

import java.io.File

fun File.findGitRootPath(): File? = when {
    name == GIT_DIRECTORY -> parentFile
    list()?.contains(GIT_DIRECTORY) == true -> this
    else -> parentFile?.findGitRootPath()
}

private const val GIT_DIRECTORY = ".git"

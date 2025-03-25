package  com.squareup.invert.models.utils

import com.squareup.invert.models.DependencyId
import com.squareup.invert.models.ModulePath
import com.squareup.invert.models.js.BuildSystem

object BuildSystemUtils {

  private enum class YesNoUnknown {
    YES, NO, UNKNOWN
  }

  /**
   * Returns whether the given module path is a source module for the given build system.
   *
   * We safely return everything when we aren't sure for the build system.
   */
  private fun isSourceModuleInternal(buildSystem: BuildSystem, modulePath: ModulePath): YesNoUnknown {
    val isSourceModuleForBuildSystem = when (buildSystem) {
      BuildSystem.GRADLE -> modulePath.startsWith(":")
      // We don't really know so we'll say yes
      BuildSystem.OTHER -> null
    }
    return when (isSourceModuleForBuildSystem) {
      true -> YesNoUnknown.YES
      false -> YesNoUnknown.NO
      null -> YesNoUnknown.UNKNOWN
    }

  }

  /**
   * Returns whether the given module path is an artifact for the given build system.
   *
   * We safely return everything when we aren't sure for the build system.
   */
  fun isArtifact(buildSystem: BuildSystem, dependencyId: DependencyId): Boolean {
    return when (isSourceModuleInternal(buildSystem, dependencyId)) {
      YesNoUnknown.YES -> false
      YesNoUnknown.NO,
      YesNoUnknown.UNKNOWN -> true
    }
  }

  /**
   * Returns whether the given module path is a source module for the given build system.
   *
   * We safely return everything when we aren't sure for the build system.
   */
  fun isSourceModule(buildSystem: BuildSystem, dependencyId: DependencyId): Boolean {
    return when (isSourceModuleInternal(buildSystem, dependencyId)) {
      YesNoUnknown.YES -> true
      YesNoUnknown.NO,
      YesNoUnknown.UNKNOWN -> false
    }
  }
}

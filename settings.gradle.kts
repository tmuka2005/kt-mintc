rootProject.name = "kt-mintc"

pluginManagement {
  includeBuild("build-logic")

  repositories {
    gradlePluginPortal()
    google()
  }
}

@Suppress("UnstableApiUsage")
gradle.lifecycle.beforeProject {
  buildscript.dependencyLocking.lockAllConfigurations()
  dependencyLocking.lockAllConfigurations()
}

include(":app")
include(":app:syntax")
include(":app:evaluation")
include(":app:repl")
include(":libs:unicode")
include(":libs:parser")
include(":benchmark")

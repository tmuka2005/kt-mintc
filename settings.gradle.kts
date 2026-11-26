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
include(":libs:unicode")

plugins {
  id("dev.detekt")
}

detekt {
  config.setFrom(rootProject.file("detekt.yml"))
  buildUponDefaultConfig = true
  allRules = true
}

tasks.detekt {
  dependsOn("detektCommonMainSourceSet", "detektJvmMainSourceSet")
}

tasks.withType<dev.detekt.gradle.Detekt>().configureEach {
  val buildDirectory =
    layout.buildDirectory
      .get()
      .asFile
  exclude { it.file.startsWith(buildDirectory) }
}

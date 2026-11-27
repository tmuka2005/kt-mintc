import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
  id("convention.kotlin-multiplatform")
  id("convention.kotest")
  id("convention.detekt")
}

kotlin {
  @OptIn(ExperimentalKotlinGradlePluginApi::class)
  jvm {
    val main = "com.t_mukai.mint.repl.MainKt"
    binaries {
      executable {
        mainClass = main
      }
    }
    mainRun {
      mainClass = main
    }
  }

  sourceSets {
    commonMain {
      dependencies {
        implementation(project(":app:evaluation"))
      }
    }
  }
}

tasks.withType<JavaExec>().configureEach {
  standardInput = System.`in`
}

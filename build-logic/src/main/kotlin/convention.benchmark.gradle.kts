plugins {
  id("convention.kotlin-multiplatform")
  id("org.jetbrains.kotlinx.benchmark")
  id("org.jetbrains.kotlin.plugin.allopen")
}

val libs: VersionCatalog = the<VersionCatalogsExtension>().named("libs")

allOpen {
  annotation("org.openjdk.jmh.annotations.State")
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        implementation(libs.findLibrary("kotlinx-benchmark-runtime").get())
      }
    }
  }
}

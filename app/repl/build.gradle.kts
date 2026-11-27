plugins {
  id("convention.kotlin-multiplatform")
  id("convention.kotest")
  id("convention.detekt")
}

kotlin {
  jvm()

  sourceSets {
    commonMain {
      dependencies {
        implementation(project(":app:evaluation"))
      }
    }
  }
}

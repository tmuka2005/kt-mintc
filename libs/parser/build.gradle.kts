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
        api(libs.arrow.core)
        api(project(":libs:unicode"))
      }
    }

    commonTest {
      dependencies {
        implementation(libs.kotest.assertions.arrow)
      }
    }
  }
}

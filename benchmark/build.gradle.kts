plugins {
  id("convention.benchmark")
  id("convention.detekt")
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        implementation(project(":libs:parser"))
        implementation(libs.better.parse)
        implementation(libs.parsus)
      }
    }
  }
}

benchmark {
  targets {
    register("jvm")
  }
  configurations {
    named("main") {
      warmups = 5
      iterations = 5
      iterationTime = 1
      iterationTimeUnit = "s"
    }
  }
}

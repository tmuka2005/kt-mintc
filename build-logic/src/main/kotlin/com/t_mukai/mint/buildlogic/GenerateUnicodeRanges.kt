package com.t_mukai.mint.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File

@CacheableTask
abstract class GenerateUnicodeRanges : DefaultTask() {
  @get:InputFiles
  @get:PathSensitive(PathSensitivity.NONE)
  abstract val sources: ConfigurableFileCollection

  @get:Input
  abstract val ranges: MapProperty<String, String>

  @get:Input
  abstract val packageName: Property<String>

  @get:OutputDirectory
  abstract val outputDirectory: DirectoryProperty

  @TaskAction
  fun generate() {
    val rangesByProperty =
      sources.files
        .flatMap { readRanges(it).entries }
        .groupBy({ it.key }) { it.value }
        .mapValues { it.value.flatten() }
    val outputRoot = outputDirectory.get().asFile
    outputRoot.deleteRecursively()
    val packageDirectory =
      outputRoot.resolve(packageName.get().replace('.', '/'))
    packageDirectory.mkdirs()
    for ((valName, propertyName) in ranges.get()) {
      val propertyRanges =
        rangesByProperty[propertyName]
          ?: error("$propertyName is not found in ${sources.files}")
      packageDirectory
        .resolve("${valName.replaceFirstChar(Char::uppercaseChar)}.kt")
        .writeText(render(valName, merge(propertyRanges)))
    }
  }

  private fun readRanges(
    file: File,
  ): Map<String, List<IntRange>> =
    file
      .readLines()
      .asSequence()
      .map { it.substringBefore('#').trim() }
      .filter { it.isNotEmpty() }
      .map { line -> line.split(';').map { it.trim() } }
      .filter { it.size in 2..3 }
      .groupBy({ it.drop(1).joinToString("=") }) { parseRange(it[0]) }

  private fun parseRange(
    text: String,
  ): IntRange {
    val first = text.substringBefore("..").toInt(16)
    val last = text.substringAfter("..", text).toInt(16)
    return first..last
  }

  private fun merge(
    ranges: List<IntRange>,
  ): List<IntRange> =
    ranges
      .sortedBy { it.first }
      .fold(mutableListOf()) { merged, range ->
        val last = merged.lastOrNull()
        if (last != null && last.last + 1 >= range.first) {
          merged[merged.lastIndex] =
            last.first..maxOf(last.last, range.last)
        } else {
          merged.add(range)
        }
        merged
      }

  private fun render(
    valName: String,
    ranges: List<IntRange>,
  ): String =
    buildString {
      appendLine("package ${packageName.get()}")
      appendLine()
      appendLine("internal val $valName: IntArray =")
      appendLine("  intArrayOf(")
      for (range in ranges) {
        appendLine("    ${hex(range.first)}, ${hex(range.last)},")
      }
      appendLine("  )")
    }

  private fun hex(
    value: Int,
  ): String =
    "0x" +
      value
        .toString(16)
        .uppercase()
        .padStart(4, '0')
}

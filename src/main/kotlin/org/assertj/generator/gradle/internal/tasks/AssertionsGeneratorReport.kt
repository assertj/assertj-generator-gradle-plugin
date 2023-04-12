package org.assertj.generator.gradle.internal.tasks

import com.google.common.reflect.TypeToken
import org.apache.commons.lang3.exception.ExceptionUtils
import org.assertj.assertions.generator.AssertionsEntryPointType
import java.io.File
import java.util.TreeSet

@Suppress("UnstableApiUsage")
internal class AssertionsGeneratorReport(
  private val directoryPathWhereAssertionFilesAreGenerated: File,
  private val inputClasses: Collection<String>,
  private val excludedClassesFromAssertionGeneration: Collection<TypeToken<*>>,
) {
  private val inputPackages = inputClasses.asSequence()
    .map { it.substringBeforeLast('.') }
    .toSet()

  private var reportedException: Exception? = null
  private val isGenerationError: Boolean
    get() = reportedException != null

  private val generatedCustomAssertionFileNames = TreeSet<String>()
  private val isNothingGenerated: Boolean
    get() = generatedCustomAssertionFileNames.isEmpty()

  private val assertionsEntryPointFilesByType = mutableMapOf<AssertionsEntryPointType, File>()

  private val inputClassesNotFound = TreeSet<String>()
  private val userTemplates = mutableListOf<String>()

  fun addGeneratedAssertionFile(generatedCustomAssertionFile: File) {
    generatedCustomAssertionFileNames.add(generatedCustomAssertionFile.canonicalPath)
  }

  fun reportEntryPointGeneration(
    assertionsEntryPointType: AssertionsEntryPointType,
    assertionsEntryPointFile: File
  ) {
    assertionsEntryPointFilesByType[assertionsEntryPointType] = assertionsEntryPointFile
  }

  fun setException(exception: Exception?) {
    reportedException = exception
  }

  fun getReportContent(): String = buildString {
    appendLine()
    appendLine("====================================")
    appendLine("AssertJ assertions generation report")
    appendLine("====================================")
    buildGeneratorParametersReport()
    appendLine()
    append(SECTION_START)
      .append("Generator results")
      .append(SECTION_END)

    when {
      isGenerationError -> buildGeneratorReportError()
      isNothingGenerated -> buildGeneratorReportWhenNothingWasGenerated()
      else -> buildGeneratorReportSuccess()
    }
  }

  private fun StringBuilder.buildGeneratorReportSuccess() {
    appendLine()
    appendLine("Directory where custom assertions files have been generated:")
    append(INDENT).append(directoryPathWhereAssertionFilesAreGenerated).appendLine()
    appendLine()
    appendLine("Custom assertions files generated:")
    for (fileName in generatedCustomAssertionFileNames) {
      append(INDENT).append(fileName).appendLine()
    }
    if (inputClassesNotFound.isNotEmpty()) {
      appendLine()
      appendLine("No custom assertions files generated for the following input classes as they were not found:")
      for (inputClassNotFound in inputClassesNotFound) {
        append(INDENT).append(inputClassNotFound).appendLine()
      }
    }

    reportEntryPointClassesGeneration()
  }

  private fun StringBuilder.reportEntryPointClassesGeneration() {
    for ((type, file) in assertionsEntryPointFilesByType) {
      val entryPointClassName = type.fileName.substringBefore(".java")
      appendLine()
        .append(entryPointClassName)
        .appendLine(" entry point class has been generated in file:")

      append(INDENT).append(file.absolutePath).appendLine()
    }
  }

  private fun StringBuilder.buildGeneratorReportWhenNothingWasGenerated() {
    appendLine()
    append("No assertions generated as no classes have been found from given classes/packages.\n")
    if (inputClasses.isNotEmpty()) {
      append(INDENT)
        .append("Given classes : ")
        .append(inputClasses)
        .appendLine()
    }
    if (inputPackages.isNotEmpty()) {
      append(INDENT)
        .append("Given packages : ")
        .append(inputPackages)
        .appendLine()
    }
    if (!excludedClassesFromAssertionGeneration.isEmpty()) {
      append(INDENT).append("Excluded classes : ").append(excludedClassesFromAssertionGeneration)
    }
  }

  private fun StringBuilder.buildGeneratorReportError() {
    appendLine()
    append("Assertions failed with error : ").append(reportedException!!.message)
    appendLine()

    if (inputClasses.isNotEmpty()) {
      append(INDENT)
        .append("Given classes were : ")
        .append(inputClasses)
        .appendLine()
    }

    if (inputPackages.isNotEmpty()) {
      append(INDENT)
        .append("Given packages were : ")
        .append(inputPackages)
        .appendLine()
    }

    appendLine()
    append("Full error stack : ").append(ExceptionUtils.getStackTrace(reportedException))
  }

  private fun StringBuilder.buildGeneratorParametersReport() {
    appendLine()
    append(SECTION_START)
      .append("Generator input parameters")
      .appendLine(SECTION_END)

    if (userTemplates.isNotEmpty()) {
      appendLine(
        "The following templates will replace the ones provided by AssertJ when generating AssertJ assertions :"
      )
      for (inputPackage in userTemplates) {
        append(INDENT).appendLine(inputPackage)
      }
      appendLine()
    }
    if (inputPackages.isNotEmpty()) {
      append("Generating AssertJ assertions for classes in following packages and subpackages:\n")
      for (inputPackage in inputPackages) {
        append(INDENT).appendLine(inputPackage)
      }
    }
    if (!inputClasses.isEmpty()) {
      if (inputPackages.isNotEmpty()) {
        appendLine()
      }

      appendLine("Generating AssertJ assertions for classes:")
      for (inputClass in inputClasses) {
        append(INDENT).appendLine(inputClass)
      }
    }
    if (!excludedClassesFromAssertionGeneration.isEmpty()) {
      appendLine()
      appendLine("Input classes excluded from assertions generation:")
      for (excludedClass in excludedClassesFromAssertionGeneration) {
        append(INDENT).appendLine(excludedClass.type.typeName)
      }
    }
  }

  companion object {
    private const val INDENT = "- "
    private const val SECTION_START = "--- "
    private const val SECTION_END = " ---\n"
  }
}

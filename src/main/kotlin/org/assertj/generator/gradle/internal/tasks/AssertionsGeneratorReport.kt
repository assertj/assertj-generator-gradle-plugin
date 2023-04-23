/*
 * Copyright 2023. assertj-generator-gradle-plugin contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.assertj.generator.gradle.internal.tasks

import com.google.common.reflect.TypeToken
import org.assertj.assertions.generator.AssertionsEntryPointType
import java.io.File
import java.util.TreeSet

internal class AssertionsGeneratorReport(
  private val directoryPathWhereAssertionFilesAreGenerated: File,
  private val inputClasses: Collection<String>,
  private val excludedClassesFromAssertionGeneration: Collection<TypeToken<*>>,
) {
  private val inputPackages = inputClasses.asSequence()
    .map { it.substringBeforeLast('.') }
    .toSet()

  var exception: Exception? = null

  private val assertionsEntryPointFilesByType = mutableMapOf<AssertionsEntryPointType, File>()
  private val generatedCustomAssertionFileNames = TreeSet<String>()
  private val inputClassesNotFound = TreeSet<String>()
  private val userTemplates = mutableListOf<String>()

  fun addGeneratedAssertionFiles(vararg generatedCustomAssertionFiles: File) {
    generatedCustomAssertionFileNames += generatedCustomAssertionFiles.map { it.canonicalPath }
  }

  fun getReportContent(): String = buildString {
    appendLine()
    appendLine("====================================")
    appendLine("AssertJ assertions generation report")
    appendLine("====================================")
    buildGeneratorParametersReport()
    appendLine()
    append(SECTION).append("Generator results").appendLine(SECTION)
    when {
      isGenerationError -> buildGeneratorReportError()
      isNothingGenerated -> buildGeneratorReportWhenNothingWasGenerated()
      else -> buildGeneratorReportSuccess()
    }
  }

  private fun StringBuilder.buildGeneratorReportSuccess(): StringBuilder = apply {
    appendLine()
    appendLine("Directory where custom assertions files have been generated:")
    append(INDENT).appendLine(directoryPathWhereAssertionFilesAreGenerated)
    appendLine()
    appendLine("Custom assertions files generated:")
    for (fileName in generatedCustomAssertionFileNames) {
      append(INDENT).appendLine(fileName)
    }
    if (inputClassesNotFound.isNotEmpty()) {
      appendLine()
      appendLine("No custom assertions files generated for the following input classes as they were not found:")
      for (inputClassNotFound in inputClassesNotFound) {
        append(INDENT).appendLine(inputClassNotFound)
      }
    }

    reportEntryPointClassesGeneration()
  }

  private fun StringBuilder.reportEntryPointClassesGeneration(): StringBuilder = apply {
    for ((type, file) in assertionsEntryPointFilesByType) {
      val entryPointClassName = type.fileName.removeSuffix(".java")
      appendLine()
        .append(entryPointClassName)
        .appendLine(" entry point class has been generated in file:")
        .append(INDENT)
        .appendLine(file.absolutePath)
    }
  }

  private fun StringBuilder.buildGeneratorReportWhenNothingWasGenerated(): StringBuilder = apply {
    appendLine()
    appendLine("No assertions generated as no classes have been found from given classes/packages.")
    if (inputClasses.isNotEmpty()) {
      append(INDENT).append("Given classes : ").appendLine(inputClasses)
    }
    if (inputPackages.isNotEmpty()) {
      append(INDENT).append("Given packages : ").appendLine(inputPackages)
    }
    if (excludedClassesFromAssertionGeneration.isNotEmpty()) {
      append(INDENT).append("Excluded classes : ").append(excludedClassesFromAssertionGeneration)
    }
  }

  private fun StringBuilder.buildGeneratorReportError(): StringBuilder = apply {
    appendLine()
    append("Assertions failed with error : ").append(exception!!.message)
    appendLine()
    if (inputClasses.isNotEmpty()) {
      append(INDENT).append("Given classes were : ").append(inputClasses)
      appendLine()
    }
    if (inputPackages.isNotEmpty()) {
      append(INDENT).append("Given packages were : ").append(inputPackages)
      appendLine()
    }
    appendLine()
    append("Full error stack : ").append(exception!!.stackTraceToString())
  }

  private fun StringBuilder.buildGeneratorParametersReport(): StringBuilder = apply {
    appendLine()
    append(SECTION).append("Generator input parameters").appendLine(SECTION)
    if (userTemplates.isNotEmpty()) {
      append("The following templates will replace the ones provided by")
        .appendLine(" AssertJ when generating AssertJ assertions:")
      for (inputPackage in userTemplates) {
        append(INDENT).append(inputPackage).appendLine()
      }
      appendLine()
    }
    if (inputPackages.isNotEmpty()) {
      appendLine("Generating AssertJ assertions for classes in following packages and subpackages:")
      for (inputPackage in inputPackages) {
        append(INDENT).append(inputPackage).appendLine()
      }
    }
    if (inputClasses.isNotEmpty()) {
      if (inputPackages.isNotEmpty()) {
        appendLine()
      }
      appendLine("Generating AssertJ assertions for classes:")
      for (inputClass in inputClasses) {
        append(INDENT).append(inputClass).appendLine()
      }
    }
    if (!excludedClassesFromAssertionGeneration.isEmpty()) {
      appendLine()
      appendLine("Input classes excluded from assertions generation:")
      for (excludedClass in excludedClassesFromAssertionGeneration) {
        append(INDENT).append(excludedClass.type.typeName).appendLine()
      }
    }
  }

  private val isGenerationError: Boolean get() = exception != null

  private val isNothingGenerated: Boolean get() = generatedCustomAssertionFileNames.isEmpty()

  fun reportEntryPointGeneration(assertionsEntryPointType: AssertionsEntryPointType, assertionsEntryPointFile: File) {
    assertionsEntryPointFilesByType[assertionsEntryPointType] = assertionsEntryPointFile
  }

  fun reportInputClassesNotFound(classes: Set<Class<*>>, inputClassNames: Array<String>) {
    val classesFound = TreeSet<String>()
    for (clazz in classes) {
      classesFound.add(clazz.name)
    }
    for (inputClass in inputClassNames) {
      if (!classesFound.contains(inputClass)) {
        inputClassesNotFound.add(inputClass)
      }
    }
  }

  fun registerUserTemplate(userTemplateDescription: String) {
    userTemplates.add(userTemplateDescription)
  }

  companion object {
    private const val INDENT = "- "
    private const val SECTION = "--- "
  }
}

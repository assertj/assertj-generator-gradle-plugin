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
package org.assertj.generator.gradle.tasks

import com.google.common.reflect.TypeToken
import org.assertj.assertions.generator.AssertionsEntryPointType
import org.assertj.assertions.generator.BaseAssertionGenerator
import org.assertj.assertions.generator.description.ClassDescription
import org.assertj.assertions.generator.description.converter.ClassToClassDescriptionConverter
import org.assertj.assertions.generator.util.ClassUtil
import org.assertj.generator.gradle.internal.tasks.AssertionsGeneratorReport
import org.assertj.generator.gradle.tasks.config.AssertJGeneratorExtension
import org.assertj.generator.gradle.tasks.config.SerializedTemplate
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.setProperty
import org.gradle.work.InputChanges
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Path
import java.nio.file.Paths
import javax.inject.Inject
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.relativeTo

/**
 * Executes AssertJ generation against provided sources using the configured templates.
 */
@CacheableTask
open class AssertJGenerationTask @Inject internal constructor(
  objects: ObjectFactory,
  sourceSet: SourceSet,
) : DefaultTask() {

  @get:InputFiles
  @get:Classpath
  val generationClasspath: ConfigurableFileCollection = objects.fileCollection()
    .from(sourceSet.runtimeClasspath)

  @get:Classpath
  @get:InputFiles
  @get:SkipWhenEmpty
  @get:IgnoreEmptyDirectories
  val classDirectories: SourceDirectorySet

  @OutputDirectory
  val outputDir: DirectoryProperty

  @get:Input
  internal val skip: Property<Boolean> = objects.property<Boolean>()

  @get:Input
  internal val hierarchical: Property<Boolean> = objects.property<Boolean>()

  @get:Input
  internal val entryPoints: SetProperty<AssertionsEntryPointType> = objects.setProperty()

  @get:Input
  @get:Optional
  internal val entryPointsClassPackage: Property<String> = objects.property()

  @get:InputFiles
  @get:Classpath
  internal val templateFiles: FileCollection

  @get:Input
  internal val generatorTemplates: ListProperty<SerializedTemplate>

  init {
    description = "Generates AssertJ assertions for the  ${sourceSet.name} sources."

    dependsOn(sourceSet.compileJavaTaskName)

    val assertJOptions = sourceSet.extensions.getByType<AssertJGeneratorExtension>()

    outputDir = assertJOptions.outputDir
    templateFiles = assertJOptions.templates.templateFiles
    generatorTemplates = assertJOptions.templates.generatorTemplates
    classDirectories = assertJOptions.classDirectories

    skip.set(project.provider { assertJOptions.skip })
    hierarchical.set(project.provider { assertJOptions.hierarchical })
    entryPoints.set(project.provider { assertJOptions.entryPoints.entryPoints })
    entryPointsClassPackage.set(project.provider { assertJOptions.entryPoints.classPackage })
  }

  @TaskAction
  fun execute(inputs: InputChanges) {
    if (skip.getOrElse(false)) {
      return
    }

    // We always regen every time
    project.delete(outputDir)

    val classLoader = URLClassLoader((generationClasspath + classDirectories).map { it.toURI().toURL() }.toTypedArray())

    val allClassNames = getClassNames(classDirectories)

    @Suppress("SpreadOperator") // Java interop
    val allClasses = ClassUtil.collectClasses(
      classLoader,
      *allClassNames.toTypedArray(),
    )

    val changedFiles = if (inputs.isIncremental) {
      inputs.getFileChanges(classDirectories).asSequence().map { it.file }.filter { it.isFile }.toSet()
    } else {
      classDirectories.files
    }

    runGeneration(allClasses, changedFiles)
  }

  private fun runGeneration(
    allClasses: Set<TypeToken<*>>,
    changedFiles: Set<File>,
  ) {
    val generator = BaseAssertionGenerator()
    val converter = ClassToClassDescriptionConverter()

    val absOutputDir = project.rootDir.toPath().resolve(this.outputDir.asFile.get().toPath()).toFile()

    val filteredClasses = removeAssertClasses(allClasses)
    val report = AssertionsGeneratorReport(
      directoryPathWhereAssertionFilesAreGenerated = absOutputDir,
      inputClasses = changedFiles.map { it.absolutePath },
      excludedClassesFromAssertionGeneration = allClasses - filteredClasses,
    )

    val templates = generatorTemplates.get().mapNotNull { it.maybeLoadTemplate() }
    for (template in templates) {
      generator.register(template)
    }

    try {
      generator.setDirectoryWhereAssertionFilesAreGenerated(absOutputDir)

      val classDescriptions = if (hierarchical.get()) {
        generateHierarchical(converter, generator, report, filteredClasses)
      } else {
        generateFlat(generator, converter, report, filteredClasses)
      }

      // only generate the entry points if there are classes that have changed (or exist..)
      for (assertionsEntryPointType in entryPoints.get()) {
        val assertionsEntryPointFile = generator.generateAssertionsEntryPointClassFor(
          classDescriptions.toSet(),
          assertionsEntryPointType,
          entryPointsClassPackage.orNull,
        )
        report.reportEntryPointGeneration(assertionsEntryPointType, assertionsEntryPointFile)
      }
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
      report.exception = e
    }

    logger.info(report.getReportContent())
  }

  private fun generateHierarchical(
    converter: ClassToClassDescriptionConverter,
    generator: BaseAssertionGenerator,
    report: AssertionsGeneratorReport,
    classes: Set<TypeToken<*>>,
  ): List<ClassDescription> {
    return classes.map { clazz ->
      val classDescription = converter.convertToClassDescription(clazz)
      val generatedCustomAssertionFiles = generator.generateHierarchicalCustomAssertionFor(
        classDescription,
        classes,
      )
      report.addGeneratedAssertionFiles(files = generatedCustomAssertionFiles)
      classDescription
    }
  }

  private fun generateFlat(
    generator: BaseAssertionGenerator,
    converter: ClassToClassDescriptionConverter,
    report: AssertionsGeneratorReport,
    classes: Set<TypeToken<*>>,
  ): List<ClassDescription> {
    return classes.map { clazz ->
      val classDescription = converter.convertToClassDescription(clazz)

      val generatedCustomAssertionFile = generator.generateCustomAssertionFor(classDescription)
      report.addGeneratedAssertionFiles(generatedCustomAssertionFile)

      classDescription
    }
  }
}

private fun removeAssertClasses(classList: Set<TypeToken<*>>): Set<TypeToken<*>> {
  val filteredClassList = mutableSetOf<TypeToken<*>>()
  for (clazz in classList) {
    val classSimpleName = clazz.rawType.simpleName
    if (!classSimpleName.endsWith("Assert") && !classSimpleName.endsWith("Assertions")) {
      filteredClassList.add(clazz)
    }
  }
  return filteredClassList
}

private fun getClassNames(source: SourceDirectorySet): Set<String> {
  val srcDirs = source.sourceDirectories.map { it.toPath() }

  val classFiles = source.asFileTree.filter { it.isFile && it.extension == "class" }
  return classFiles.asSequence()
    .map { it.toPath() }
    .mapNotNull { getFullyQualifiedClassForFile(srcDirs, it) }
    .toSet()
}

private val PACKAGE_INFO_PATH = Paths.get("package-info.class")

private fun getFullyQualifiedClassForFile(srcDirs: List<Path>, path: Path): String? {
  if (path.isDirectory() || path.extension != "class") return null

  // Ignore package-info.java, it's not supposed to be included.
  if (path.fileName == PACKAGE_INFO_PATH) return null
  val className = path.nameWithoutExtension
  if ('$' in className) return null

  val srcDir = srcDirs.single { path.startsWith(it) }
  val relativePath = path.relativeTo(srcDir).parent

  return "$relativePath.$className"
    .replace(File.separatorChar, '.')
    .replace('$', '.')
}

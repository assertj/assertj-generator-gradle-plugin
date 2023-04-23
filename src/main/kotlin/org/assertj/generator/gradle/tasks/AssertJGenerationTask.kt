/*
 * Copyright 2017. assertj-generator-gradle-plugin contributors.
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

import com.google.common.collect.Sets
import com.google.common.reflect.TypeToken
import org.assertj.assertions.generator.AssertionsEntryPointType
import org.assertj.assertions.generator.BaseAssertionGenerator
import org.assertj.assertions.generator.description.ClassDescription
import org.assertj.assertions.generator.description.converter.ClassToClassDescriptionConverter
import org.assertj.assertions.generator.util.ClassUtil
import org.assertj.generator.gradle.internal.tasks.AssertionsGeneratorReport
import org.assertj.generator.gradle.tasks.config.AssertJGeneratorExtension
import org.assertj.generator.gradle.tasks.config.SerializedTemplate
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.setProperty
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Path
import java.nio.file.Paths
import javax.inject.Inject
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.useLines

private val logger = Logging.getLogger(AssertJGenerationTask::class.java)

/**
 * Executes AssertJ generation against provided sources using the configured templates.
 */
@CacheableTask
open class AssertJGenerationTask @Inject internal constructor(objects: ObjectFactory, sourceSet: SourceSet) :
  SourceTask() {

  @get:InputFiles
  @get:Classpath
  val generationClasspath: ConfigurableFileCollection = objects.fileCollection()
    .from(sourceSet.runtimeClasspath)

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

    source(sourceSet.allJava)
    dependsOn(sourceSet.compileJavaTaskName)

    val assertJOptions = sourceSet.extensions.getByType<AssertJGeneratorExtension>()

    outputDir = assertJOptions.outputDir
    templateFiles = assertJOptions.templates.templateFiles
    generatorTemplates = assertJOptions.templates.generatorTemplates

    skip.set(project.provider { assertJOptions.skip })
    hierarchical.set(project.provider { assertJOptions.hierarchical })
    entryPoints.set(project.provider { assertJOptions.entryPoints.entryPoints })
    entryPointsClassPackage.set(project.provider { assertJOptions.entryPoints.classPackage })
  }

  @TaskAction
  fun execute(inputs: IncrementalTaskInputs) {
    if (skip.get()) {
      return
    }

    val sourceFiles = source.files

    var classesToGenerate = mutableSetOf<File>()
    var fullRegenRequired = false
    inputs.outOfDate { change ->
      if (generationClasspath.contains(change.file)) {
        // file is part of classpath
        fullRegenRequired = true
      } else if (sourceFiles.contains(change.file)) {
        // source file changed
        classesToGenerate += change.file
      } else if (templateFiles.contains(change.file)) {
        fullRegenRequired = true
      }
    }

    inputs.removed { change ->
      // TODO Handle deleted file
//            def targetFile = project.file("$outputDir/${change.file.name}")
//            if (targetFile.exists()) {
//                targetFile.delete()
//            }
    }

    if (fullRegenRequired || !inputs.isIncremental) {
      project.delete(outputDir.asFileTree.files)
      classesToGenerate = sourceFiles
    }

    val classLoader = URLClassLoader(generationClasspath.map { it.toURI().toURL() }.toTypedArray())

    val inputClassNames = getClassNames()

    @Suppress("SpreadOperator") // Java interop
    val classes = ClassUtil.collectClasses(
      classLoader,
      *inputClassNames.values.flatten().toTypedArray(),
    )

    val classesByTypeName = classes.associateBy { it.type.typeName }

    val inputClassesToFile = inputClassNames.asSequence()
      .flatMap { (file, classDefs) ->
        classDefs.map { classesByTypeName.getValue(it) to file }
      }
      .filter { (_, file) -> file in classesToGenerate }
      .toMap()

    runGeneration(classes, inputClassNames, inputClassesToFile)
  }

  private fun runGeneration(
    allClasses: Set<TypeToken<*>>,
    inputClassNames: Map<File, Set<String>>,
    inputClassesToFile: Map<TypeToken<*>, File>
  ) {
    val generator = BaseAssertionGenerator()
    val converter = ClassToClassDescriptionConverter()

    val absOutputDir = project.rootDir.toPath().resolve(this.outputDir.asFile.get().toPath()).toFile()

    val filteredClasses = removeAssertClasses(allClasses)
    val report = AssertionsGeneratorReport(
      directoryPathWhereAssertionFilesAreGenerated = absOutputDir,
      inputClasses = inputClassNames.values.flatten(),
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
        generateFlat(generator, converter, report, filteredClasses, inputClassesToFile)
      }.toSet()

      if (inputClassesToFile.isNotEmpty()) {
        // only generate the entry points if there are classes that have changed (or exist..)
        for (assertionsEntryPointType in entryPoints.get()) {
          val assertionsEntryPointFile = generator.generateAssertionsEntryPointClassFor(
            classDescriptions,
            assertionsEntryPointType,
            entryPointsClassPackage.orNull,
          )
          report.reportEntryPointGeneration(assertionsEntryPointType, assertionsEntryPointFile)
        }
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
  ): Collection<ClassDescription> {
    return classes.map { clazz ->
      val classDescription = converter.convertToClassDescription(clazz)
      val generatedCustomAssertionFiles = generator.generateHierarchicalCustomAssertionFor(
        classDescription,
        classes,
      )
      report.addGeneratedAssertionFiles(generatedCustomAssertionFiles = generatedCustomAssertionFiles)
      classDescription
    }
  }

  private fun generateFlat(
    generator: BaseAssertionGenerator,
    converter: ClassToClassDescriptionConverter,
    report: AssertionsGeneratorReport,
    classes: Set<TypeToken<*>>,
    inputClassesToFile: Map<TypeToken<*>, File>,
  ): Collection<ClassDescription> {
    return classes.map { clazz ->
      val classDescription = converter.convertToClassDescription(clazz)

      if (clazz in inputClassesToFile) {
        val generatedCustomAssertionFile = generator.generateCustomAssertionFor(classDescription)
        report.addGeneratedAssertionFiles(generatedCustomAssertionFile)
      }

      classDescription
    }
  }

  /**
   * Returns the source for this task, after the include and exclude patterns have been applied. Ignores source files
   * which do not exist.
   *
   * @return The source.
   */
  // This method is here as the Gradle DSL generation can't handle properties with setters and getters in different
  // classes.
  @InputFiles
  @SkipWhenEmpty
  override fun getSource(): FileTree = super.getSource()

  private fun getClassNames(): Map<File, Set<String>> {
    val fullyQualifiedNames = mutableMapOf<File, Set<String>>()

    source.visit { fileVisitDetails: FileVisitDetails ->
      val file = fileVisitDetails.file.toPath()
      fullyQualifiedNames[fileVisitDetails.file] = getClassesInFile(file)
    }

    return fullyQualifiedNames.filterValues { it.isNotEmpty() }
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

private val PACKAGE_JAVA_PATH = Paths.get("package-info.java")

private fun getClassesInFile(path: Path): Set<String> {
  if (path.isDirectory()) return setOf()

  // Ignore package-info.java, it's not supposed to be included.
  if (path.fileName == PACKAGE_JAVA_PATH) return setOf()

  val extension = path.extension
  val fileNameWithoutExtension = path.fileName.nameWithoutExtension

  val (packageName, classNames) = when (extension) {
    "java" -> {
      path.useLines { lines ->
        val packageLine = lines.first { it.startsWith("package ") }

        val packageName = packageLine.removePrefix("package ").trimEnd(';')
        val classNames = Sets.newHashSet(fileNameWithoutExtension)
        Pair(packageName, classNames)
      }
    }

    else -> error("Unsupported extension: $extension")
  }

  return classNames.asSequence().map { "$packageName.$it" }.toSet()
}

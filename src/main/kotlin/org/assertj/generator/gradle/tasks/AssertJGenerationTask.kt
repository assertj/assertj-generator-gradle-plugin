@file:Suppress("UnstableApiUsage")

package org.assertj.generator.gradle.tasks

import com.google.common.reflect.TypeToken
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
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.listProperty
import org.gradle.work.ChangeType
import org.gradle.work.InputChanges
import java.io.File
import java.net.URLClassLoader
import java.nio.file.Path
import java.nio.file.Paths
import javax.inject.Inject
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.useLines

private val PACKAGE_JAVA_PATH = Paths.get("package-info.java")

/**
 * Runs the
 */
open class AssertJGenerationTask @Inject internal constructor(
  objects: ObjectFactory,
  sourceSet: SourceSet,
) : DefaultTask() {

  private val extension: AssertJGeneratorExtension = sourceSet.extensions.getByType<AssertJGeneratorExtension>()

  @get:InputFiles
  @Classpath
  val generationClasspath: ConfigurableFileCollection = objects.fileCollection()
    .from(sourceSet.runtimeClasspath)

  @get:InputFiles
  @get:PathSensitive(PathSensitivity.RELATIVE)
  @get:SkipWhenEmpty
  val source: ConfigurableFileCollection = objects.fileCollection()
    .from(sourceSet.allSource.matching(extension.source))

  @get:InputFiles
  @Classpath
  protected val templateFiles: FileCollection = objects.fileCollection()
    .from(extension.templatesExtension.map { it.templateFiles })

  @get:Input
  protected val templates: ListProperty<SerializedTemplate> = objects.listProperty<SerializedTemplate>().apply {
    set(extension.templatesExtension.flatMap { it.templates })
  }

  @OutputDirectory
  val outputDir: DirectoryProperty = objects.directoryProperty().apply {
    set(extension.outputDirectory)
  }

  init {
    description = "Generates AssertJ assertions for the $sourceSet sources."

    dependsOn(sourceSet.compileJavaTaskName)
  }

  @TaskAction
  fun execute(inputs: InputChanges) {
    if (extension.skip.getOrElse(false)) return

    val classesToGenerate = mutableSetOf<File>()
    var fullRegenRequired = false

    for (change in inputs.getFileChanges(source)) {
      when (change.changeType) {
        ChangeType.ADDED, ChangeType.MODIFIED -> {
          classesToGenerate += change.file
        }

        ChangeType.REMOVED -> {
          // TODO Handle removed files
          fullRegenRequired = true
        }

        else -> {
          error("Invalid change: $change")
        }
      }
    }

    if (fullRegenRequired || !inputs.isIncremental) {
      project.delete(outputDir.asFileTree.files)
    }

    val classLoader = URLClassLoader(generationClasspath.map { it.toURI().toURL() }.toTypedArray())

    val inputClassNames = computeClassNames(source)
    val allInputClassNames = inputClassNames.values.asSequence().flatten().toSet()

    @Suppress("SpreadOperator")
    val classes = ClassUtil.collectClasses(classLoader, *allInputClassNames.toTypedArray<String>())

    val inputClassesToFile = computeInputClassesToFile(classes, inputClassNames, classesToGenerate)

    val filteredClasses = removeAssertClasses(classes)

    val report = executeGeneration(allInputClassNames, classes, filteredClasses) { generator, converter, report ->
      val classDescriptions = if (extension.hierarchical.getOrElse(false)) {
        filteredClasses.generateHierarchicalDescriptions(converter, generator, report)
      } else {
        filteredClasses.generateDescriptions(converter, generator, report, inputClassesToFile)
      }.toSet()

      if (inputClassesToFile.isNotEmpty()) {
        // only generate the entry points if there are classes that have changed (or exist..)
        val entryPoints = extension.entryPoints.get()
        for (assertionsEntryPointType in entryPoints) {
          val assertionsEntryPointFile = generator.generateAssertionsEntryPointClassFor(
            classDescriptions,
            assertionsEntryPointType,
            entryPoints.classPackage,
          )
          report.reportEntryPointGeneration(assertionsEntryPointType, assertionsEntryPointFile)
        }
      }
    }

    logger.info(report.getReportContent())
  }

  private fun Set<TypeToken<*>>.generateDescriptions(
    converter: ClassToClassDescriptionConverter,
    generator: BaseAssertionGenerator,
    report: AssertionsGeneratorReport,
    inputClassesToFile: Map<TypeToken<*>, File>,
  ): Sequence<ClassDescription> {
    return asSequence().map { clazz ->
      val classDescription = converter.convertToClassDescription(clazz)

      if (clazz in inputClassesToFile) {
        val generatedCustomAssertionFile = generator.generateCustomAssertionFor(classDescription)
        report.addGeneratedAssertionFile(generatedCustomAssertionFile)
      }

      classDescription
    }
  }

  private fun Set<TypeToken<*>>.generateHierarchicalDescriptions(
    converter: ClassToClassDescriptionConverter,
    generator: BaseAssertionGenerator,
    report: AssertionsGeneratorReport
  ): Sequence<ClassDescription> {
    val allClasses = this
    return asSequence().map { clazz ->
      val classDescription = converter.convertToClassDescription(clazz)

      val (abstractBaseAssertionFile, concreteBaseAssertionFile) = generator
        .generateHierarchicalCustomAssertionFor(classDescription, allClasses)
      report.addGeneratedAssertionFile(abstractBaseAssertionFile)
      report.addGeneratedAssertionFile(concreteBaseAssertionFile)

      classDescription
    }
  }

  private fun computeInputClassesToFile(
    classes: MutableSet<TypeToken<*>>,
    inputClassNames: Map<File, Set<String>>,
    classesToGenerate: MutableSet<File>
  ): Map<TypeToken<*>, File> {
    val classesByTypeName = classes.associateBy { it.type.typeName!! }
    return inputClassNames.asSequence()
      .filter { (file, _) -> file in classesToGenerate }
      .flatMap { (file, typeNames) ->
        typeNames.asSequence()
          .map { classesByTypeName.getValue(it) }
          .map { it to file }
      }
      .toMap()
  }

  private fun executeGeneration(
    allInputClassNames: Set<String>,
    allClasses: Set<TypeToken<*>>,
    filteredClasses: Set<TypeToken<*>>,
    block: (
      generator: BaseAssertionGenerator,
      converter: ClassToClassDescriptionConverter,
      report: AssertionsGeneratorReport
    ) -> Unit,
  ): AssertionsGeneratorReport {
    val generator = BaseAssertionGenerator()
    val converter = ClassToClassDescriptionConverter()

    val absOutputDir = project.rootDir.toPath().resolve(outputDir.asFile.get().toPath())

    val report = AssertionsGeneratorReport(
      directoryPathWhereAssertionFilesAreGenerated = absOutputDir.toFile(),
      inputClasses = allInputClassNames,
      excludedClassesFromAssertionGeneration = allClasses - filteredClasses,
    )

    for (template in extension.templatesExtension.get().templates.get().mapNotNull { it.maybeLoadTemplate() }) {
      generator.register(template)
    }

    generator.setDirectoryWhereAssertionFilesAreGenerated(absOutputDir.toFile())

    try {
      block(generator, converter, report)
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
      report.setException(e)
    }

    return report
  }
}

private fun removeAssertClasses(classList: Set<TypeToken<*>>): Set<TypeToken<*>> {
  return classList.asSequence()
    .filter { clazz ->
      val classSimpleName = clazz.rawType.simpleName
      !classSimpleName.endsWith("Assert") && !classSimpleName.endsWith("Assertions")
    }
    .toSet()
}

private fun computeClassNames(source: FileCollection): Map<File, Set<String>> {
  val fullyQualifiedNames = HashMap<File, Set<String>>(source.files.size)

  source.asFileTree.visit { details ->
    val file = details.file.toPath()
    fullyQualifiedNames[details.file] = getClassesInFile(file)
  }

  return fullyQualifiedNames.filterValues { it.isNotEmpty() }
}

private fun getClassesInFile(path: Path): Set<String> {
  if (path.isDirectory()) return setOf()

  // Ignore package-info.java, it's not supposed to be included.
  if (path.fileName == PACKAGE_JAVA_PATH) return setOf()

  val (packageName, classNames) = when (path.extension) {
    "java" -> {
      path.useLines { lines ->
        val packageName = lines.first { it.startsWith("package ") }
          .substringAfter("package ")
          .trim(';')

        packageName to setOf(path.nameWithoutExtension)
      }
    }

    else -> error("Unsupported path: $path")
  }

  return classNames.asSequence()
    .map { "$packageName.$it" }
    .toSet()
}

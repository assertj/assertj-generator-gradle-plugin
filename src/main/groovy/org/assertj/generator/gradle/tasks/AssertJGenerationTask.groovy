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
import org.gradle.api.file.*
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import org.gradle.api.tasks.incremental.IncrementalTaskInputs

import javax.inject.Inject
import java.nio.file.Path
import java.nio.file.Paths

import static com.google.common.collect.Sets.newLinkedHashSet

/**
 * Executes AssertJ generation against provided sources using the configured templates.
 */
@CacheableTask
class AssertJGenerationTask extends SourceTask {

    private static final logger = Logging.getLogger(AssertJGenerationTask)

    @InputFiles
    @Classpath
    final ConfigurableFileCollection generationClasspath

    @OutputDirectory
    final DirectoryProperty outputDir

    // TODO `internal` when Konverted
    @Input
    final Property<Boolean> skip

    // TODO `internal` when Konverted
    @Input
    final Property<Boolean> hierarchical

    // TODO `internal` when Konverted
    @Input
    final SetProperty<AssertionsEntryPointType> entryPoints

    // TODO `internal` when Konverted
    @Input
    @Optional
    final Property<String> entryPointsClassPackage

    // TODO `internal` when Konverted
    @InputFiles
    @Classpath
    final FileCollection templateFiles

    // TODO `internal` when Konverted
    @Input
    final ListProperty<SerializedTemplate> generatorTemplates

    @Inject
    AssertJGenerationTask(ObjectFactory objects, SourceSet sourceSet) {
        description = "Generates AssertJ assertions for the  ${sourceSet.name} sources."

        def assertJOptions = sourceSet.extensions.getByType(AssertJGeneratorExtension)
        source(sourceSet.allJava)
        dependsOn sourceSet.compileJavaTaskName

        this.generationClasspath = objects.fileCollection()
                .from(sourceSet.runtimeClasspath)

        this.skip = objects.property(Boolean).tap {
            set(project.provider { assertJOptions.skip })
        }

        this.hierarchical = objects.property(Boolean).tap {
            set(project.provider { assertJOptions.hierarchical })
        }

        this.entryPoints = objects.setProperty(AssertionsEntryPointType).tap {
            set(project.provider { assertJOptions.entryPoints.entryPoints })
        }

        this.entryPointsClassPackage = objects.property(String).tap {
            set(project.provider { assertJOptions.entryPoints.classPackage })
        }

        this.outputDir = assertJOptions.outputDir
        // TODO Make `templates.templateFiles` `internal` once `AssertJGenerationTask` is Kotlin
        this.templateFiles = assertJOptions.templates.templateFiles
        // TODO Make `templates.generatorTemplates` `internal` once `AssertJGenerationTask` is Kotlin
        this.generatorTemplates = assertJOptions.templates.generatorTemplates
    }

    @TaskAction
    def execute(IncrementalTaskInputs inputs) {
        if (skip.get()) {
            return
        }

        Set<File> sourceFiles = source.files

        def classesToGenerate = []
        def fullRegenRequired = false
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

        if (fullRegenRequired || !inputs.incremental) {
            project.delete(outputDir.asFileTree.files)
            classesToGenerate = sourceFiles
        }

        def classLoader = new URLClassLoader(generationClasspath.collect { it.toURI().toURL() } as URL[])

        def inputClassNames = getClassNames()
        Set<TypeToken<?>> classes = ClassUtil.collectClasses(
                classLoader,
                inputClassNames.values().flatten() as String[],
        )

        def classesByTypeName = classes.collectEntries { [(it.type.typeName): it] }

        def inputClassesToFile = inputClassNames
                .collectMany { file, classDefs ->
                    classDefs.collect {
                        [(classesByTypeName[it]): file]
                    }
                }
                .collectEntries() as Map<TypeToken<?>, File>

        inputClassesToFile.values().removeAll {
            !classesToGenerate.contains(it)
        }

        runGeneration(classes, inputClassNames, inputClassesToFile)
    }

    private def runGeneration(
            Set<TypeToken<?>> allClasses,
            Map<File, Set<String>> inputClassNames,
            Map<TypeToken<?>, File> inputClassesToFile
    ) {
        BaseAssertionGenerator generator = new BaseAssertionGenerator()
        ClassToClassDescriptionConverter converter = new ClassToClassDescriptionConverter()

        def absOutputDir = project.rootDir.toPath().resolve(this.outputDir.getAsFile().get().toPath()).toFile()

        Set<TypeToken<?>> filteredClasses = removeAssertClasses(allClasses)
        def report = new AssertionsGeneratorReport(
                absOutputDir,
                inputClassNames.values().flatten().collect { it.toString() },
                allClasses - filteredClasses,
        )

        def templates = generatorTemplates.get().collect { it.maybeLoadTemplate() }.findAll()
        for (template in templates) {
            generator.register(template)
        }

        try {
            generator.directoryWhereAssertionFilesAreGenerated = absOutputDir

            def classDescriptions
            if (hierarchical.get()) {
                classDescriptions = generateHierarchical(converter, generator, report, filteredClasses)
            } else {
                classDescriptions = generateFlat(converter, generator, report, filteredClasses, inputClassesToFile)
            }

            if (!inputClassesToFile.isEmpty()) {
                // only generate the entry points if there are classes that have changed (or exist..)
                for (assertionsEntryPointType in entryPoints.get()) {
                    File assertionsEntryPointFile = generator.generateAssertionsEntryPointClassFor(
                            classDescriptions.toSet(),
                            assertionsEntryPointType,
                            entryPointsClassPackage.getOrNull(),
                    )
                    report.reportEntryPointGeneration(assertionsEntryPointType, assertionsEntryPointFile)
                }
            }
        } catch (Exception e) {
            report.exception = e
        }

        logger.info(report.getReportContent())
    }

    private static Collection<ClassDescription> generateHierarchical(
            ClassToClassDescriptionConverter converter,
            BaseAssertionGenerator generator,
            AssertionsGeneratorReport report,
            Set<TypeToken<?>> classes
    ) {
        classes.collect { clazz ->
            def classDescription = converter.convertToClassDescription(clazz)
            def generatedCustomAssertionFiles = generator.generateHierarchicalCustomAssertionFor(
                    classDescription,
                    classes,
            )
            report.addGeneratedAssertionFiles(generatedCustomAssertionFiles)
            classDescription
        }
    }

    private static Collection<ClassDescription> generateFlat(
            ClassToClassDescriptionConverter converter,
            BaseAssertionGenerator generator,
            AssertionsGeneratorReport report,
            Set<TypeToken<?>> classes,
            Map<TypeToken<?>, File> inputClassesToFile
    ) {
        classes.collect { clazz ->
            def classDescription = converter.convertToClassDescription(clazz)

            if (inputClassesToFile.containsKey(clazz)) {
                def generatedCustomAssertionFile = generator.generateCustomAssertionFor(classDescription)
                report.addGeneratedAssertionFiles(generatedCustomAssertionFile)
            }

            classDescription
        }
    }

    /**
     * Returns the source for this task, after the include and exclude patterns have been applied. Ignores source files which do not exist.
     *
     * @return The source.
     */
    // This method is here as the Gradle DSL generation can't handle properties with setters and getters in different classes.
    @InputFiles
    @SkipWhenEmpty
    FileTree getSource() {
        super.getSource()
    }

    private Map<File, Set<String>> getClassNames() {
        Map<File, Set<String>> fullyQualifiedNames = new HashMap<>(source.files.size())

        source.visit { FileVisitDetails fileVisitDetails ->
            Path file = fileVisitDetails.file.toPath()
            fullyQualifiedNames[fileVisitDetails.file] = AssertJGenerationTask.getClassesInFile(file)
        }

        fullyQualifiedNames.removeAll { it.value.isEmpty() }
        fullyQualifiedNames
    }

    private static Set<TypeToken<?>> removeAssertClasses(Set<TypeToken<?>> classList) {
        Set<TypeToken<?>> filteredClassList = newLinkedHashSet()
        for (TypeToken<?> clazz : classList) {
            String classSimpleName = clazz.rawType.simpleName
            if (!classSimpleName.endsWith("Assert") && !classSimpleName.endsWith("Assertions")) {
                filteredClassList.add(clazz)
            }
        }
        return filteredClassList
    }

    private static def PACKAGE_JAVA_PATH = Paths.get("package-info.java")

    private static Set<String> getClassesInFile(Path path) {
        if (path.toFile().isDirectory()) return Sets.newHashSet()

        // Ignore package-info.java, it's not supposed to be included.
        if (path.fileName == PACKAGE_JAVA_PATH) return Sets.newHashSet()
        def fileName = path.fileName.toString()
        def extension = fileName.substring(fileName.findLastIndexOf { it == '.' } + 1)
        def fileNameWithoutExtension = fileName.substring(0, fileName.size() - extension.size() - 1)

        String packageName
        Set<String> classNames

        switch (extension) {
            case "java":
                def lines = path.readLines()
                def packageLine = lines.find { it.startsWith("package ") }

                packageName = packageLine.substring("package ".size(), packageLine.size() - 1)
                classNames = Sets.newHashSet(fileNameWithoutExtension)
                break

            default: throw new IllegalStateException("Unsupported path: $path")
        }

        return classNames.collect { "$packageName.$it" }.toSet()
    }
}

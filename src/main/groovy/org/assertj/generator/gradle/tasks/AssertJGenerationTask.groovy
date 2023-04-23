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
import org.gradle.api.tasks.*
import org.gradle.api.tasks.incremental.IncrementalTaskInputs

import javax.inject.Inject
import java.nio.file.Path
import java.nio.file.Paths

import static com.google.common.collect.Sets.newLinkedHashSet

/**
 * Executes AssertJ generation against provided sources using the configured templates.
 */
class AssertJGenerationTask extends SourceTask {

    private static final logger = Logging.getLogger(AssertJGenerationTask)

    @Classpath
    final ConfigurableFileCollection generationClasspath

    @InputFiles
    @Classpath
    final FileCollection templateFiles

    @Input
    final ListProperty<SerializedTemplate> templates

    @OutputDirectory
    final DirectoryProperty outputDir

    private final AssertJGeneratorExtension assertJOptions

    @Inject
    AssertJGenerationTask(ObjectFactory objects, SourceSet sourceSet) {
        description = "Generates AssertJ assertions for the  ${sourceSet.name} sources."

        assertJOptions = sourceSet.extensions.getByType(AssertJGeneratorExtension)
        source(sourceSet.allJava)
        dependsOn sourceSet.compileJavaTaskName

        this.generationClasspath = objects.fileCollection()
                .from(sourceSet.runtimeClasspath)

        this.outputDir = assertJOptions.outputDir
        this.templateFiles = assertJOptions.templates.templateFiles
        this.templates = assertJOptions.templates.templates
    }

    @TaskAction
    def execute(IncrementalTaskInputs inputs) {
        if (assertJOptions.skip) {
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
                .collectEntries()

        inputClassesToFile.values().removeAll {
            !classesToGenerate.contains(it)
        }

        BaseAssertionGenerator generator = new BaseAssertionGenerator()
        ClassToClassDescriptionConverter converter = new ClassToClassDescriptionConverter()

        def absOutputDir = project.rootDir.toPath().resolve(this.outputDir.getAsFile().get().toPath()).toFile()

        Set<TypeToken<?>> filteredClasses = removeAssertClasses(classes)
        def report = new AssertionsGeneratorReport(
                absOutputDir,
                inputClassNames.values().flatten().collect { it.toString() },
                classes - filteredClasses,
        )

        def templates = assertJOptions.templates.templates.get().collect { it.maybeLoadTemplate() }.findAll()
        for (template in templates) {
            generator.register(template)
        }

        try {
            generator.directoryWhereAssertionFilesAreGenerated = absOutputDir

            Set<ClassDescription> classDescriptions = new LinkedHashSet<>()

            if (assertJOptions.hierarchical) {
                for (clazz in filteredClasses) {
                    ClassDescription classDescription = converter.convertToClassDescription(clazz)
                    File[] generatedCustomAssertionFiles = generator.generateHierarchicalCustomAssertionFor(
                            classDescription,
                            filteredClasses,
                    )
                    report.addGeneratedAssertionFiles(generatedCustomAssertionFiles)
                    classDescriptions.add(classDescription)
                }
            } else {
                for (clazz in filteredClasses) {
                    def classDescription = converter.convertToClassDescription(clazz)
                    classDescriptions.add(classDescription)

                    if (inputClassesToFile.containsKey(clazz)) {
                        File generatedCustomAssertionFile = generator.generateCustomAssertionFor(classDescription)
                        report.addGeneratedAssertionFiles(generatedCustomAssertionFile)
                    }
                }
            }

            if (!inputClassesToFile.isEmpty()) {
                // only generate the entry points if there are classes that have changed (or exist..)
                for (assertionsEntryPointType in assertJOptions.entryPoints.entryPoints) {
                    File assertionsEntryPointFile = generator.generateAssertionsEntryPointClassFor(
                            classDescriptions,
                            assertionsEntryPointType,
                            assertJOptions.entryPoints.classPackage,
                    )
                    report.reportEntryPointGeneration(assertionsEntryPointType, assertionsEntryPointFile)
                }
            }
        } catch (Exception e) {
            report.exception = e
        }

        logger.info(report.getReportContent())
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

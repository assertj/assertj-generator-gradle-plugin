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
import org.assertj.assertions.generator.AssertionsEntryPointType
import org.assertj.assertions.generator.BaseAssertionGenerator
import org.assertj.assertions.generator.description.ClassDescription
import org.assertj.assertions.generator.description.converter.ClassToClassDescriptionConverter
import org.assertj.assertions.generator.util.ClassUtil
import org.assertj.generator.gradle.internal.tasks.AssertionsGeneratorReport
import org.assertj.generator.gradle.tasks.config.AssertJGeneratorOptions
import org.gradle.api.DefaultTask
import org.gradle.api.file.*
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs

import java.nio.file.Path

import static com.google.common.collect.Sets.newLinkedHashSet

/**
 *
 */
class AssertJGenerationTask extends DefaultTask {

    private static final logger = Logging.getLogger(AssertJGenerationTask)

    @InputFiles
    SourceDirectorySet sourceDirectorySet

    @InputFiles
    FileCollection generationClasspath

    @Input
    AssertJGeneratorOptions assertJOptions

    @OutputDirectory
    File outputDir

    void setOutputDir(Path newDir) {
        this.outputDir = project.buildDir.toPath()
                .resolve(newDir)
                .toFile()
    }

    void setOutputDir(File newDir) {
        setOutputDir(newDir.toPath())
    }

    @TaskAction
    def execute(IncrementalTaskInputs inputs) {
        if (assertJOptions.skip) {
            return
        }

        if (!inputs.incremental) {
            project.delete(outputDir.listFiles())
        }

        def classesToGenerate = []
        def classpathChanged = false
        inputs.outOfDate { change ->
            if (generationClasspath.contains(change.file)) {
                // file is part of classpath
                classpathChanged = true
            } else if (sourceDirectorySet.contains(change.file)) {
                // source file changed
                classesToGenerate += change.file
            }
        }

        inputs.removed { change ->
            // TODO Handle deleted file
//            def targetFile = project.file("$outputDir/${change.file.name}")
//            if (targetFile.exists()) {
//                targetFile.delete()
//            }
        }

        def classLoader = new URLClassLoader(generationClasspath.collect { it.toURI().toURL() } as URL[])

        def inputClassNames = getClassNames(sourceDirectorySet)
        def classes = ClassUtil.collectClasses(classLoader, inputClassNames.values().toArray(new String[0]))

        def inputClassesToFile = inputClassNames.collectEntries { file, classDef ->
            [(classes.find { it.name == classDef }): file]
        }
        inputClassesToFile.values().removeAll {
            !classesToGenerate.contains(it)
        }

        BaseAssertionGenerator generator = new BaseAssertionGenerator()
        ClassToClassDescriptionConverter converter = new ClassToClassDescriptionConverter()

        AssertionsGeneratorReport report = new AssertionsGeneratorReport()

        Path absOutputDir = project.rootDir.toPath().resolve(this.outputDir.toPath())
        report.setDirectoryPathWhereAssertionFilesAreGenerated(absOutputDir.toFile())
        assertJOptions.templates.getTemplates(report).each {
            generator.register(it)
        }

        try {
            generator.setDirectoryWhereAssertionFilesAreGenerated(absOutputDir.toString())

            report.setInputClasses(inputClassNames.values())

            Set<Class<?>> filteredClasses = removeAssertClasses(classes)
            report.setExcludedClassesFromAssertionGeneration(Sets.difference(classes, filteredClasses))

            Set<ClassDescription> classDescriptions = new LinkedHashSet<>()

            if (assertJOptions.hierarchical) {
                for (Class<?> clazz : filteredClasses) {
                    ClassDescription classDescription = converter.convertToClassDescription(clazz)
                    File[] generatedCustomAssertionFiles = generator.generateHierarchicalCustomAssertionFor(classDescription,
                            filteredClasses)
                    report.addGeneratedAssertionFile(generatedCustomAssertionFiles[0])
                    report.addGeneratedAssertionFile(generatedCustomAssertionFiles[1])
                    classDescriptions.add(classDescription)
                }
            } else {
                for (Class<?> clazz : filteredClasses) {
                    def classDescription = converter.convertToClassDescription(clazz)
                    classDescriptions.add(classDescription)

                    if (inputClassesToFile.containsKey(clazz)) {
                        File generatedCustomAssertionFile = generator.generateCustomAssertionFor(classDescription)
                        report.addGeneratedAssertionFile(generatedCustomAssertionFile)
                    }
                }
            }

            if (!inputClassesToFile.empty) {
                // only generate the entry points if there are classes that have changed (or exist..)

                for (AssertionsEntryPointType assertionsEntryPointType : assertJOptions.entryPoints) {
                    File assertionsEntryPointFile = generator.generateAssertionsEntryPointClassFor(classDescriptions,
                            assertionsEntryPointType,
                            assertJOptions.entryPointClassPackage)
                    report.reportEntryPointGeneration(assertionsEntryPointType, assertionsEntryPointFile)
                }
            }
        } catch (Exception e) {
            report.setException(e)
        }

        logger.info(report.reportContent)
    }

    private static def getClassNames(SourceDirectorySet sourceDirectorySet) {
        Map<File, String> fullyQualifiedNames = new HashMap<>(sourceDirectorySet.files.size())

        for (DirectoryTree tree : sourceDirectorySet.srcDirTrees) {
            Path root = tree.dir.toPath()
            tree.visit(new FileVisitor() {
                @Override
                void visitDir(FileVisitDetails fileVisitDetails) { }

                @Override
                void visitFile(FileVisitDetails fileVisitDetails) {
                    Path file = root.relativize(fileVisitDetails.file.toPath())

                    // Remove the extension and replace the dir separator with dots
                    String outPath = file.toString()
                    outPath = outPath[0..<outPath.size() - ".java".size()]
                    outPath = outPath.replace(File.separatorChar, '.' as char)

                    fullyQualifiedNames[fileVisitDetails.file] = outPath
                }
            })
        }

        fullyQualifiedNames
    }

    private static Set<Class<?>> removeAssertClasses(Set<Class<?>> classList) {
        Set<Class<?>> filteredClassList = newLinkedHashSet()
        for (Class<?> clazz : classList) {
            String classSimpleName = clazz.getSimpleName()
            if (!classSimpleName.endsWith("Assert") && !classSimpleName.endsWith("Assertions")) {
                filteredClassList.add(clazz)
            }
        }
        return filteredClassList
    }
}

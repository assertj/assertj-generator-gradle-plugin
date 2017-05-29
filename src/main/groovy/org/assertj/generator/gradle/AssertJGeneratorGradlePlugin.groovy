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
package org.assertj.generator.gradle

import org.assertj.generator.gradle.internal.tasks.DefaultAssertJGeneratorSourceSet
import org.assertj.generator.gradle.internal.tasks.config.GlobalAssertJGeneratorOptions
import org.assertj.generator.gradle.tasks.AssertJGenerationTask
import org.assertj.generator.gradle.tasks.AssertJGeneratorSourceSet
import org.codehaus.groovy.runtime.InvokerHelper
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.file.SourceDirectorySetFactory
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.Convention
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.testing.Test

import javax.inject.Inject

/**
 * Defines the entry point for applying the AssertJGeneration plugin
 */
class AssertJGeneratorGradlePlugin implements Plugin<Project> {

    static final ASSERTJ_GEN_CONFIGURATION_NAME = "assertJ"

    private final SourceDirectorySetFactory sourceDirectorySetFactory

    private static final logger = Logging.getLogger(AssertJGeneratorGradlePlugin)

    @Inject
    AssertJGeneratorGradlePlugin(SourceDirectorySetFactory sourceDirectorySetFactory) {
        this.sourceDirectorySetFactory = sourceDirectorySetFactory
    }

    @Override
    void apply(Project project) {

        project.getPluginManager().apply(JavaPlugin)

        project.configurations.create(ASSERTJ_GEN_CONFIGURATION_NAME)
                .setVisible(false)
                .setDescription("AssertJ Generator configuration")

        logger.info("after assertJConfig created")

        project.extensions.create("assertJ", GlobalAssertJGeneratorOptions)

        def javaPlugin = project.getConvention().getPlugin(JavaPluginConvention)
        // So now we have to go through and add the properties that we want
        javaPlugin.getSourceSets().all { sourceSet ->
            // For each sourceSet we're enacting an action on each one that adds an assertJ generation task to it
            logger.info("sourceSet: ${sourceSet} creating tasks")

            // Get the convention and add the properties
            Convention sourceSetConvention = (Convention) InvokerHelper.getProperty(sourceSet, "convention")

            // Create the assertJ closure within the source set, e.g. main { assertJ { } }
            DefaultAssertJGeneratorSourceSet assertJSourceSet = new DefaultAssertJGeneratorSourceSet(sourceSet.name, sourceDirectorySetFactory)
            sourceSetConvention.plugins.put("assertJ", assertJSourceSet)

            // get the source directory set from the AssertJ source set so we can modify it
            final SourceDirectorySet sourceDirectorySet = assertJSourceSet.assertJ

            // Add the source to all of the required sources
            sourceSet.allSource.source sourceDirectorySet

            // ignore the sources in the resources folder
            sourceSet.resources.filter.exclude { element -> sourceDirectorySet.contains element.file }
            addAndConfigureAssertJGenerate(project, javaPlugin, sourceSet, assertJSourceSet)
        }
    }

    // Configures the "generate*" tasks to generate files
    private static void addAndConfigureAssertJGenerate(final Project project,
                                                       final JavaPluginConvention javaPlugin,
                                                       final SourceSet sourceSet,
                                                       final AssertJGeneratorSourceSet assertJSS) {
        String generateTaskName = sourceSet.getTaskName('generate', 'assertJ')

        logger.info("generationTask: ${generateTaskName}, sourceSet: ${sourceSet}")

        // When we get a new sourceSet, per [sub-]project, we create a "compileUmpleTask" that consists of building
        // a configuration per source set

        // Create a new task for the source set
        AssertJGenerationTask assertJGenerate = project.tasks.findByName(generateTaskName) as AssertJGenerationTask

        if (!assertJGenerate) {
            assertJGenerate = project.tasks.create(generateTaskName, AssertJGenerationTask) {
                description = "Generates AssertJ assertions for the " + sourceSet + "."
                generationClasspath = sourceSet.runtimeClasspath // Get the classes used when creating the ClassLoader for
                // Generation
                sourceDirectorySet = assertJSS.assertJ  //source directory for the generateAssertJ task is the
                // SourceDirectorySet in AssertJGeneratorSourceSet
                assertJOptions     = assertJSS // Set the config options too
            }

            final def compileJavaTask = project.tasks.findByName(sourceSet.compileJavaTaskName)
            assertJGenerate.dependsOn compileJavaTask
        }

        project.afterEvaluate {
            // We need to figure out task dependencies now
            assertJSS.defaultFromGlobals(project)

            assertJGenerate.configure {
                outputDir = assertJSS.getOutputDir(sourceSet)
            }

            // Only add the source if we are not working with a "test" set
            if (!sourceSet.name.toLowerCase().contains('test')) {
                // Get the test source set or create a new one if it doesn't already exist
                final def testTaskName = sourceSet.getTaskName('test', '')

                def testSourceSet = javaPlugin.getSourceSets().findByName(testTaskName)
                if (!testSourceSet) {
                    testSourceSet = javaPlugin.sourceSets.create(testTaskName) {
                        compileClasspath += sourceSet.runtimeClasspath
                    }
                }

                testSourceSet.java.srcDirs += assertJGenerate.outputDir
                project.tasks.findByName(testSourceSet.compileJavaTaskName).dependsOn assertJGenerate

                Test testTask = project.tasks.findByName(testTaskName) as Test
                if (!testTask) {
                    testTask = project.task(testTaskName, type: Test) as Test

                    project.tasks.test.dependsOn testTask
                }

                testTask.classpath += testSourceSet.runtimeClasspath
                testTask.dependsOn testSourceSet.compileJavaTaskName
            }
        } // end after evaluate

    }
}

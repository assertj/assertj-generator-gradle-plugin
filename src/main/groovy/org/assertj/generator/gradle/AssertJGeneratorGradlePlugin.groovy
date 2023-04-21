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

import org.assertj.generator.gradle.tasks.AssertJGenerationTask
import org.assertj.generator.gradle.tasks.AssertJGeneratorSourceSet
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.internal.file.SourceDirectorySetFactory
import org.gradle.api.internal.plugins.DslObject
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

        final Configuration assertJGeneratorConfiguration = project.configurations.create(ASSERTJ_GEN_CONFIGURATION_NAME)
                .setVisible(false)
                .setDescription("AssertJ Generator configuration")
        assertJGeneratorConfiguration.defaultDependencies {
            add(project.dependencies.create("org.assertj:assertj-assertions-generator:2.0.0"))
        }

        Configuration compileTestConfig = project.configurations.findByName(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME)
        if (compileTestConfig) {
            assertJGeneratorConfiguration.extendsFrom(compileTestConfig)
        }

        project.getTasks().withType(AssertJGenerationTask) { task ->
            task.conventionMapping.map("generationClasspath") {
                project.configurations.getByName(ASSERTJ_GEN_CONFIGURATION_NAME)
            }
        }

        def javaPlugin = project.getConvention().getPlugin(JavaPluginConvention)
        // So now we have to go through and add the properties that we want
        javaPlugin.sourceSets.all { sourceSet ->
            // For each sourceSet we're enacting an action on each one that adds an assertJ generation task to it
            logger.info("sourceSet: ${sourceSet} creating tasks")

            // Get the convention and add the properties
            Convention sourceSetConvention = new DslObject(sourceSet).convention

            // Create the assertJ closure within the source set, e.g. main { assertJ { } }
            DefaultAssertJGeneratorSourceSet assertJSourceSet = new DefaultAssertJGeneratorSourceSet(
                    sourceSet, sourceDirectorySetFactory)
            sourceSetConvention.plugins[AssertJGeneratorSourceSet.NAME] = assertJSourceSet
            sourceSet.allSource.source(assertJSourceSet.assertJ)

            addAndConfigureAssertJGenerate(project, javaPlugin, sourceSet, assertJSourceSet)
        }
    }

    // Configures the "generate*" tasks to generate files
    private static void addAndConfigureAssertJGenerate(final Project project,
                                                       final JavaPluginConvention javaPlugin,
                                                       final SourceSet sourceSet,
                                                       final AssertJGeneratorSourceSet assertJSS) {
        // Use the name via calling sourceSet#getTaskName(String, String)
        String generateTaskName = sourceSet.getTaskName('generate', 'assertJ')

        logger.info("generationTask: ${generateTaskName}, sourceSet: ${sourceSet}")

        // Create a new task for the source set
        AssertJGenerationTask generationTask = project.tasks.findByName(generateTaskName) as AssertJGenerationTask

        if (!generationTask) {
            generationTask = project.tasks.create(generateTaskName, AssertJGenerationTask) {
                description = "Generates AssertJ assertions for the ${sourceSet} sources."
                generationClasspath = sourceSet.runtimeClasspath // Get the classes used when creating the ClassLoader for
                                                                 // Generation

                source             = assertJSS.assertJ  // Set up the conventional sources
                assertJOptions     = assertJSS          // Set the config options, too
            }

            final def compileJavaTask = project.tasks.findByName(sourceSet.compileJavaTaskName)
            generationTask.dependsOn compileJavaTask
        }
        
        project.afterEvaluate {
            generationTask.configure {
                outputDir = assertJSS.getOutputDir(sourceSet)
            }

            // We need to figure out task dependencies now

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

                // With the test task, we add it to the _test_ source set
                testSourceSet.allSource.source(assertJSS.assertJ)

                testSourceSet.java.srcDirs += generationTask.outputDir
                project.tasks.findByName(testSourceSet.compileJavaTaskName).dependsOn generationTask

                Test testTask = project.tasks.findByName(testTaskName) as Test
                if (testTask && testTaskName != "test") {
                    testTask.classpath += testSourceSet.runtimeClasspath

                    // make sure generator is run before compilation for the test
                    testTask.dependsOn testSourceSet.compileJavaTaskName

                    project.tasks.test.dependsOn testTask
                }


            }
        } // end after evaluate

    }
}

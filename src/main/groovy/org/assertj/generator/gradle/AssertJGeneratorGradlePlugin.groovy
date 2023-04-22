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

import org.assertj.generator.gradle.internal.tasks.config.DefaultAssertJGeneratorOptions
import org.assertj.generator.gradle.tasks.AssertJGenerationTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.testing.Test

import javax.inject.Inject

/**
 * Defines the entry point for applying the AssertJGeneration plugin
 */
class AssertJGeneratorGradlePlugin implements Plugin<Project> {

    static final ASSERTJ_GEN_CONFIGURATION_NAME = "assertJ"

    private final ObjectFactory objects

    private static final logger = Logging.getLogger(AssertJGeneratorGradlePlugin)

    @Inject
    AssertJGeneratorGradlePlugin(ObjectFactory objects) {
        this.objects = objects
    }

    @Override
    void apply(Project project) {
        project.pluginManager.withPlugin("java") {
            final Configuration assertJGeneratorConfiguration = project.configurations.create(ASSERTJ_GEN_CONFIGURATION_NAME)
                    .setVisible(false)
                    .setDescription("AssertJ Generator configuration")
            assertJGeneratorConfiguration.defaultDependencies {
                add(project.dependencies.create("org.assertj:assertj-assertions-generator:2.0.0"))
            }

            project.configurations.named(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME) {
                assertJGeneratorConfiguration.extendsFrom(it)
            }

            def javaPluginExtension = project.extensions.getByType(JavaPluginExtension)

            // So now we have to go through and add the properties that we want
            javaPluginExtension.sourceSets.all { SourceSet sourceSet ->
                if (sourceSet.name == "test") return

                // For each sourceSet we're enacting an action on each one that adds an assertJ generation task to it
                logger.info("sourceSet: ${sourceSet} creating tasks")

                sourceSet.extensions.create(
                    "assertJ",
                    DefaultAssertJGeneratorOptions,
                    objects,
                    project,
                    sourceSet,
                )

                addAndConfigureAssertJGenerate(project, javaPluginExtension, sourceSet)
            }
        }
    }

    // Configures the "generate*" tasks to generate files
    private void addAndConfigureAssertJGenerate(final Project project,
                                                final JavaPluginExtension javaPlugin,
                                                final SourceSet sourceSet) {
        // Use the name via calling sourceSet#getTaskName(String, String)
        String generateTaskName = sourceSet.getTaskName('generate', 'assertJ')

        logger.info("generationTask: ${generateTaskName}, sourceSet: ${sourceSet}")

        // Create a new task for the source set
        def generationTask = project.tasks.register(
                generateTaskName,
                AssertJGenerationTask,
                objects,
                sourceSet,
        )

        project.afterEvaluate {
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
                testSourceSet.java.srcDir(generationTask.flatMap { it.outputDir })
                project.tasks.named(testSourceSet.compileJavaTaskName) { dependsOn generationTask }

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
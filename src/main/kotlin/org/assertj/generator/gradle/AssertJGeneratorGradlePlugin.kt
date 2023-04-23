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
package org.assertj.generator.gradle

import org.assertj.generator.gradle.tasks.AssertJGenerationTask
import org.assertj.generator.gradle.tasks.config.AssertJGeneratorExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSet
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import javax.inject.Inject

/**
 * Defines the entry point for applying the AssertJGeneration plugin
 */
open class AssertJGeneratorGradlePlugin @Inject internal constructor(
  private val objects: ObjectFactory,
) : Plugin<Project> {

  override fun apply(project: Project) {
    project.pluginManager.withPlugin("java") {
      val assertJGeneratorConfiguration = project.configurations.create(ASSERTJ_GEN_CONFIGURATION_NAME)
        .setVisible(false)
        .setDescription("AssertJ Generator configuration")
      assertJGeneratorConfiguration.defaultDependencies {
        it.add(project.dependencies.create("org.assertj:assertj-assertions-generator:2.0.0"))
      }

      project.configurations.named(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME) {
        assertJGeneratorConfiguration.extendsFrom(it)
      }

      val javaPluginExtension = project.extensions.getByType<JavaPluginExtension>()

      // So now we have to go through and add the properties that we want
      javaPluginExtension.sourceSets.all { sourceSet ->
        if (sourceSet.name == "test") return@all

        // For each sourceSet we're enacting an action on each one that adds an assertJ generation task to it
        logger.info("sourceSet: $sourceSet creating tasks")

        sourceSet.extensions.create<AssertJGeneratorExtension>(
          "assertJ",
          objects,
          project,
          sourceSet,
        )

        addAndConfigureAssertJGenerate(project, javaPluginExtension, sourceSet)
      }
    }
  }

  private fun addAndConfigureAssertJGenerate(
    project: Project,
    javaPlugin: JavaPluginExtension,
    sourceSet: SourceSet
  ) {
    val generateTaskName = sourceSet.getTaskName("generate", "assertJ")

    logger.info("generationTask: $generateTaskName, sourceSet: $sourceSet")

    // Create a new task for the source set
    val generationTask = project.tasks.register<AssertJGenerationTask>(generateTaskName, objects, sourceSet)

    javaPlugin.sourceSets.named("test").configure { sourceSet ->
      sourceSet.java.srcDir(generationTask.flatMap { it.outputDir })
    }
  }

  companion object {
    const val ASSERTJ_GEN_CONFIGURATION_NAME = "assertJ"

    private val logger = Logging.getLogger(AssertJGeneratorGradlePlugin::class.java)
  }
}

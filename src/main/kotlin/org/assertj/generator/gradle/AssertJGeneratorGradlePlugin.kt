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
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import javax.inject.Inject

private val logger = Logging.getLogger(AssertJGeneratorGradlePlugin::class.java)

/**
 * Defines the entry point for applying the AssertJGeneration plugin
 */
open class AssertJGeneratorGradlePlugin @Inject internal constructor(
  private val objects: ObjectFactory,
) : Plugin<Project> {

  override fun apply(project: Project) {
    project.pluginManager.withPlugin("java") {
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

        sourceSet.addAndConfigureAssertJGenerate(project, javaPluginExtension)
      }

      // If we don't have the java plugin, we can't compile the generated sources. Thus, this is in the
      // withPlugin() block
      project.pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
        project.applyForKotlin(javaPluginExtension)
      }
    }
  }

  private fun Project.applyForKotlin(javaPluginExtension: JavaPluginExtension) {
    kotlinExtension.sourceSets.configureEach { kotlinSourceSet ->
      if (kotlinSourceSet.name == "test") return@configureEach

      javaPluginExtension.sourceSets.named(kotlinSourceSet.name) { javaSourceSet ->
        val kotlinCompile = project.tasks.named<KotlinCompile>(javaSourceSet.getCompileTaskName("kotlin"))

        project.configureGenerateTask(javaSourceSet) {
          dependsOn(kotlinCompile)

          classDirectories.srcDir(kotlinCompile.flatMap { it.destinationDirectory })
        }
      }
    }
  }

  private fun SourceSet.addAndConfigureAssertJGenerate(project: Project, javaPlugin: JavaPluginExtension) {
    val generationTask = project.registerGenerateTask(this) {
      classDirectories.srcDir(this@addAndConfigureAssertJGenerate.java.classesDirectory)
    }

    javaPlugin.sourceSets.named("test").configure { sourceSet ->
      sourceSet.java.srcDir(generationTask.flatMap { it.outputDir })
    }
  }

  private fun Project.registerGenerateTask(
    sourceSet: SourceSet,
    configureBlock: AssertJGenerationTask.() -> Unit,
  ): TaskProvider<AssertJGenerationTask> {
    return tasks.register<AssertJGenerationTask>(sourceSet.generateAssertJTaskName, objects, sourceSet).apply {
      configure(configureBlock)
    }
  }

  private fun Project.configureGenerateTask(
    sourceSet: SourceSet,
    configureBlock: AssertJGenerationTask.() -> Unit,
  ): TaskProvider<AssertJGenerationTask> {
    return tasks.named<AssertJGenerationTask>(sourceSet.generateAssertJTaskName) {
      configureBlock()
    }
  }
}

private val SourceSet.generateAssertJTaskName: String get() = getTaskName("generate", "assertJ")

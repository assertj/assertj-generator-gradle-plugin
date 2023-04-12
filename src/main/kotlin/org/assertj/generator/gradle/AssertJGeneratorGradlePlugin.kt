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
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.provideDelegate
import javax.inject.Inject

/**
 * Defines the entry point for applying the AssertJGeneration plugin
 */
class AssertJGeneratorGradlePlugin @Inject constructor(
  private val objects: ObjectFactory,
) : Plugin<Project> {
  override fun apply(project: Project) {
    project.pluginManager.apply(JavaPlugin::class.java)

    project.pluginManager.withPlugin("java") {
      val assertJGeneratorConfiguration =
        project.configurations.create(ASSERTJ_GEN_CONFIGURATION_NAME).setVisible(false)
          .setDescription("AssertJ Generator configuration")
      assertJGeneratorConfiguration.defaultDependencies { dependencySet ->
        // TODO this should be configurable
        dependencySet.add(project.dependencies.create("org.assertj:assertj-assertions-generator:2.0.0"))
      }

      val compileTestConfig = project.configurations.findByName(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME)
      if (compileTestConfig != null) {
        assertJGeneratorConfiguration.extendsFrom(compileTestConfig)
      }

      val javaPluginExtension = project.extensions.getByType<JavaPluginExtension>()

      // So now we have to go through and add the properties that we want
      javaPluginExtension.sourceSets.all { sourceSet ->
        if (sourceSet.name == "test") {
          // test sources do not ever participate
          return@all
        }

        // For each sourceSet we're enacting an action on each one that adds an assertJ generation task to it
        logger.info("sourceSet: $sourceSet creating tasks")

        sourceSet.extensions.create<AssertJGeneratorExtension>("assertJ", objects, project, sourceSet)

        javaPluginExtension.addAndConfigureAssertJGenerate(project, sourceSet)
      }
    }
  }

  private val logger = Logging.getLogger(AssertJGeneratorGradlePlugin::class.java)

  private fun JavaPluginExtension.addAndConfigureAssertJGenerate(project: Project, sourceSet: SourceSet) {
    // Use the name via calling sourceSet#getTaskName(String, String)
    val generateTaskName = sourceSet.getTaskName("generate", "assertJ")
    logger.info("generationTask: $generateTaskName, sourceSet: $sourceSet")

    // Create a new task for the source set
    val generationTask by project.tasks.register(generateTaskName, AssertJGenerationTask::class.java, sourceSet)

    sourceSets.named("test") { testSourceSet ->
      testSourceSet.java.srcDir(generationTask.outputDir)
    }

//        project.afterEvaluate {
//            // Only add the source if we are not working with a "test" set
//            if (sourceSet.name.lowercase(Locale.getDefault()).contains("test")) {
//                return@afterEvaluate
//            }
//
//            // Get the test source set or create a new one if it doesn't already exist
//            val testTaskName = sourceSet.getTaskName("test", "")
//            val testTask by project.tasks.named<Test>(testTaskName)
//
//            var testSourceSet = sourceSets.findByName(testTaskName)
//            if (testSourceSet == null) {
//                testSourceSet = sourceSets.create(testTaskName) { newSourceSet ->
//                    newSourceSet.compileClasspath += sourceSet.runtimeClasspath
//                }
//
//                checkNotNull(testSourceSet)
//            }
//
//            // With the test task, we add it to the _test_ source set
// //            testSourceSet.allSource.source(assertJSS.assertJ)
//            testSourceSet.java.srcDirs.add(generationTask.outputDir.asFile.get())
//
//            val testCompileTask by project.tasks.named(testSourceSet.compileJavaTaskName)
//            testCompileTask.dependsOn(generationTask)
//
//            if (testTaskName != "test") {
//                testTask.classpath += testSourceSet.runtimeClasspath
//
//                // make sure generator is run before compilation for the test
//                testTask.dependsOn(testSourceSet.compileJavaTaskName)
//
//                project.tasks.named("test") { task ->
//                    task.dependsOn(testTask)
//                }
//            }
//        }
  }

  companion object {
    const val ASSERTJ_GEN_CONFIGURATION_NAME = "assertJ"
  }
}

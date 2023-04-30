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
package org.assertj.generator.gradle.parameter

import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.assertj.generator.gradle.TestUtils.writeBuildFile
import org.assertj.generator.gradle.isSuccessful
import org.assertj.generator.gradle.writeJava
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Checks the behaviour of overriding globals in a project
 */
internal class OutputDirectoryParameter {

  @get:Rule
  val testProjectDir = TemporaryFolder()

  private lateinit var buildFile: File
  private lateinit var mainPackagePath: Path
  private lateinit var packagePath: Path

  @Before
  fun setup() {
    buildFile = testProjectDir.newFile("build.gradle")

    val srcDir = testProjectDir.newFolder("src")
    val mainDir = srcDir.toPath().resolve("main/java")

    packagePath = Paths.get("org/example/")

    mainPackagePath = mainDir.resolve(packagePath)
    mainPackagePath.toFile().mkdirs()
    val mainJava = mainPackagePath.resolve("Main.java").toFile()

    mainJava.writeJava(
      """
             package org.example;
             
             public final class Main {
                 // Field
                 public boolean hasSomeBrains = false;
             }
             """
    )
  }

  @Test
  fun change_output_dir_locally() {
    buildFile.writeBuildFile(
      """            
          sourceSets {
              main { 
                  assertJ {
                      // default: generated-srcs/SOURCE_SET_NAME-test/java
                      outputDir = file('src-gen/foo-bar/java')
                  }
              }
          }
          """
    )

    val result = GradleRunner.create()
      .withProjectDir(testProjectDir.root)
      .withDebug(true)
      .withPluginClasspath()
      .withArguments("-i", "-s", "test")
      .build()

    assertThat(result.task(":generateAssertJ")).isSuccessful()
    assertThat(result.task(":test")).isSuccessful()

    assertFiles("foo-bar", true)
  }

  private fun assertFiles(folderName: String, exists: Boolean) {
    val sourceSet = "main"
    val generatedPackagePath = testProjectDir.root.toPath()
      .resolve("src-gen/$folderName/java")
      .resolve(packagePath)

    val buildPath = testProjectDir.root.toPath().resolve("build")

    val path = generatedPackagePath.resolve("${sourceSet.capitalized()}Assert.java")

    assertThat(path.toFile().exists())
      .`as` { "$sourceSet file: ${buildPath.relativize(path)} exists" }
      .isEqualTo(exists)
  }
}

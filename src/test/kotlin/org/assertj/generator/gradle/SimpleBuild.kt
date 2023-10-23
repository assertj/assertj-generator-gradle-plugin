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

import net.navatwo.gradle.testkit.junit5.GradleProject
import org.assertj.core.api.Assertions.assertThat
import org.assertj.generator.gradle.TestUtils.writeBuildFile
import org.assertj.generator.gradle.TestUtils.writeDefaultBuildFile
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Checks the behaviour of overriding globals in a project
 */
internal class SimpleBuild {

  private val File.buildFile: File
    get() = resolve("build.gradle")

  @Test
  @GradleProject("simple-build")
  fun `for single class`(
    @GradleProject.Root root: File,
    @GradleProject.Runner runner: GradleRunner,
  ) {
    root.buildFile.writeDefaultBuildFile()

    val result = runner.withArguments("-i", "-s", "test").build()

    assertThat(result.task(":generateAssertJ")).isSuccessOrCached()
    assertThat(result.task(":test")).isSuccessOrCached()
  }

  @Test
  @GradleProject("simple-build")
  fun `exclude class`(
    @GradleProject.Root root: File,
    @GradleProject.Runner runner: GradleRunner,
  ) {
    root.buildFile.writeBuildFile(
      """
      sourceSets {
          main {
              assertJ {
                  classDirectories.exclude "**/org/example/OtherWorld*"
              }
          }
      }
      """
    )

    val result = runner.withArguments("-i", "-s", "test").build()

    assertThat(result.task(":generateAssertJ")).isSuccessOrCached()
    assertThat(result.task(":test")).isSuccessOrCached()

    val packagePath = root.toPath()
      .resolve("build/generated-src/main-test/java")
      .resolve("org/example")
    assertThat(packagePath).isDirectoryNotContaining("glob:**/OtherWorldAssert.java")
  }
}

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
package org.assertj.generator.gradle.parameter

import net.navatwo.gradle.testkit.junit5.GradleProject
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.assertj.generator.gradle.TestUtils.writeBuildFile
import org.assertj.generator.gradle.capitalized
import org.assertj.generator.gradle.isSuccessful
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Checks the behaviour of overriding globals in a project
 */
internal class SkipParameter {
  private val File.buildFile: File
    get() = resolve("build.gradle")
  private val packagePath: File
    get() = File("org/example")

  @Test
  @GradleProject("skip-parameter")
  fun `skip other set`(
    @GradleProject.Root root: File,
    @GradleProject.Runner runner: GradleRunner,
  ) {
    root.buildFile.writeBuildFile(
      """
        sourceSets {
            main {
                assertJ { skip = true }
            }
        }
        """
    )

    val result = runner.withArguments("-i", "-s", "build").build()

    assertThat(result.task(":generateAssertJ")).isSuccessful()
    assertThat(result.task(":build")).isSuccessful()

    root.assertFiles(false)
  }

  @Test
  @GradleProject("skip-parameter")
  fun `generate default`(
    @GradleProject.Root root: File,
    @GradleProject.Runner runner: GradleRunner,
  ) {
    root.buildFile.writeBuildFile(
      """
        sourceSets {
            main {
                assertJ { skip = false }
             }
        }
        """
    )

    val result = runner.withArguments("-i", "-s", "test").build()

    assertThat(result.task(":generateAssertJ")).isSuccessful()
    assertThat(result.task(":test")).isSuccessful()

    root.assertFiles(true)
  }

  private fun File.assertFiles(exists: Boolean) {
    val sourceSet = "main"
    val generatedPackagePath = resolve("build/generated-src/main-test/java")
      .resolve(packagePath)

    val buildPath = resolve("build")

    val path = generatedPackagePath.resolve("${sourceSet.capitalized()}Assert.java")

    assertThat(path.exists())
      .`as` { "$sourceSet file: ${buildPath.toPath().relativize(path.toPath())} exists" }
      .isEqualTo(exists)
  }
}

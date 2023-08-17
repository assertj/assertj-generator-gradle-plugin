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
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import java.io.File
import java.util.function.Consumer

/**
 * Checks the behaviour of overriding globals in a project
 */
internal class IncrementalBuild {

  // TODO Add more checks for incremental conditions and outputs
  // * Entry point files
  // * File deletion

  private val packagePath: File
    get() = File("org/example")
  private val File.h1Java: File
    get() = resolve("src/main/java/org/example/H1.java")

  @Test
  @GradleProject("incremental-build")
  fun `incremental build does not rebuild everything`(
    @GradleProject.Root root: File,
    @GradleProject.Runner runner: GradleRunner,
  ) {
    val buildRunner = runner.withArguments("-i", "-s", "build")

    val firstBuild = buildRunner.build()

    assertThat(firstBuild.task(":generateAssertJ")).isSuccessful()
    assertThat(firstBuild.task(":test")).isSuccessful()

    root.assertFiles()

    val secondBuild = buildRunner.build()
    assertThat(secondBuild.task(":generateAssertJ")).isUpToDate()
    assertThat(secondBuild.task(":test")).isUpToDate()

    root.assertFiles()
  }

  @Test
  @GradleProject("incremental-build")
  fun `incremental build rebuild changed single file contents`(
    @GradleProject.Root root: File,
    @GradleProject.Runner runner: GradleRunner,
  ) {
    val buildRunner = runner.withArguments("-i", "-s", "build")

    val firstBuild = buildRunner.build()

    assertThat(firstBuild.task(":generateAssertJ")).isSuccessful()
    assertThat(firstBuild.task(":test")).isSuccessful()

    // get files
    root.assertFiles()

    // modify the contents of H1 that doesn't change the compiled `.class`
    root.h1Java.appendText("\n// some changed data")

    val secondBuild = buildRunner.build()
    // Make sure it did run
    assertThat(secondBuild.task(":generateAssertJ")).isUpToDate()

    // However, since we didn't actually change anything, there's nothing to test, thus UP_TO_DATE
    assertThat(secondBuild.task(":test")).isUpToDate()

    root.assertFiles()
  }

  private fun File.assertFiles() {
    val generatedPackagePath = resolve("build/generated-src/main-test/java").resolve(packagePath)

    val files = listOf("H1", "H2").map {
      generatedPackagePath.resolve("${it}Assert.java")
    }

    assertThat(files).allSatisfy(
      Consumer { file ->
        assertThat(file).exists()
      }
    )
  }
}

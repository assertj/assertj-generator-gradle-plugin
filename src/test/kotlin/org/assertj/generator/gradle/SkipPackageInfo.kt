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

/**
 * Checks the behaviour of overriding globals in a project
 */
internal class SkipPackageInfo {

  private val File.generatedPackagePath: File
    get() = resolve("build/generated-src/main-test/java")
      .resolve("org/example")

  @Test
  @GradleProject("simple-build")
  fun `does not include package info file`(
    @GradleProject.Root root: File,
    @GradleProject.Runner runner: GradleRunner,
  ) {
    val result = runner.withArguments("-i", "-s", "check").build()

    assertThat(result.task(":generateAssertJ")).isSuccessful()
    assertThat(result.task(":test")).isSuccessful()

    assertThat(root.generatedPackagePath.resolve("Package-InfoAssertions.java"))
      .`as` { "${root.generatedPackagePath}/Package-InfoAssertions does not exist" }
      .doesNotExist()
    assertThat(root.generatedPackagePath.resolve("Assertions.java"))
      .content()
      .`as` { "${root.generatedPackagePath}/Assertions does not have \"package-info\" in it" }
      .doesNotContain("package-info")
  }
}

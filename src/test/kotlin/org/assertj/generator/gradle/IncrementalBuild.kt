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

import org.assertj.core.api.Assertions.assertThat
import org.assertj.generator.gradle.TestUtils.writeDefaultBuildFile
import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.function.Consumer

/**
 * Checks the behaviour of overriding globals in a project
 */
internal class IncrementalBuild {

  // TODO Add more checks for incremental conditions and outputs
  // * Entry point files
  // * File deletion

  @get:Rule
  val testProjectDir: TemporaryFolder = TemporaryFolder()

  private lateinit var srcPackagePath: Path
  private lateinit var packagePath: Path
  private lateinit var h1Java: File
  private lateinit var h2Java: File

  @Before
  fun setup() {
    testProjectDir.newFile("build.gradle").writeDefaultBuildFile()

    val srcDir = testProjectDir.newFolder("src", "main", "java")

    packagePath = Paths.get("org/example/")

    srcPackagePath = srcDir.toPath().resolve(this.packagePath)
    this.srcPackagePath.toFile().mkdirs()
    h1Java = this.srcPackagePath.resolve("H1.java").toFile()

    this.h1Java.writeJava(
      """
      package org.example;
      
      public final class H1 {
          public boolean isBrainy = false;
      }
      """
    )

    h2Java = this.srcPackagePath.resolve("H2.java").toFile()

    this.h2Java.writeJava(
      """
      package org.example;
      
      public final class H2 {
          public boolean isBrainy = false;
      }
      """
    )

    val testDir = testProjectDir.newFolder("src", "test", "java")

    testDir.toPath().resolve(this.packagePath).toFile().mkdirs()
    val testPackagePath = testDir.toPath().resolve(this.packagePath)
    testPackagePath.toFile().mkdirs()
    val helloWorldTestJava = testPackagePath.resolve("H1Test.java").toFile()

    helloWorldTestJava.writeJava(
      """
      package org.example;
      
      import org.junit.Test;
      import static org.example.H1Assert.assertThat;
      
      public final class H1Test {
          @Test
          public void check() {
              H1 hw = new H1();
              assertThat(hw).isNotBrainy();
          }
      }
      """
    )
  }

  @Test
  fun `incremental build does not rebuild everything`() {
    val runner = GradleRunner.create()
      .withProjectDir(testProjectDir.root)
      .withDebug(true)
      .withPluginClasspath()
      .withArguments("-i", "-s", "build")

    val firstBuild = runner.build()

    assertThat(firstBuild.task(":generateAssertJ")).isSuccessful()
    assertThat(firstBuild.task(":test")).isSuccessful()

    assertFiles()

    val secondBuild = runner.build()
    assertThat(secondBuild.task(":generateAssertJ")).isUpToDate()
    assertThat(secondBuild.task(":test")).isUpToDate()

    assertFiles()
  }

  @Test
  fun `incremental build rebuild changed single file contents`() {
    val runner = GradleRunner.create()
      .withProjectDir(testProjectDir.root)
      .withDebug(true)
      .withPluginClasspath()
      .withArguments("-i", "-s", "build")

    val firstBuild = runner.build()

    assertThat(firstBuild.task(":generateAssertJ")).isSuccessful()
    assertThat(firstBuild.task(":test")).isSuccessful()

    // get files
    assertFiles()

    // modify the contents of H1 that doesn't change the compiled `.class`
    h1Java.appendText("\n// some changed data")

    val secondBuild = runner.build()
    // Make sure it did run
    assertThat(secondBuild.task(":generateAssertJ")).isUpToDate()

    // However, since we didn't actually change anything, there's nothing to test, thus UP_TO_DATE
    assertThat(secondBuild.task(":test")).isUpToDate()

    assertFiles()
  }

  private fun assertFiles() {
    val generatedPackagePath = testProjectDir.root.toPath()
      .resolve("build/generated-src/main-test/java")
      .resolve(packagePath)

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

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
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Checks the behaviour of overriding globals in a project
 */
internal class SkipPackageInfo {

  @get:Rule
  val testProjectDir: TemporaryFolder = TemporaryFolder()

  private lateinit var generatedPackagePath: Path

  @Before
  fun setup() {
    testProjectDir.newFile("build.gradle").writeDefaultBuildFile()

    val srcDir = testProjectDir.newFolder("src", "main", "java")

    val packagePath = Paths.get("org/example/")

    val srcPackagePath = srcDir.toPath().resolve(packagePath)
    srcPackagePath.toFile().mkdirs()
    val helloWorldJava = srcPackagePath.resolve("HelloWorld.java").toFile()

    helloWorldJava.writeJava(
      """
      package org.example;
      
      public final class HelloWorld {
          
          // Field
          public boolean hasSomeBrains = false;
          
          // Getter
          public int getFoo() {
              return -1;
          }
      }
      """
    )

    val otherWorldJava = srcPackagePath.resolve("OtherWorld.java").toFile()

    otherWorldJava.writeJava(
      """
      package org.example;
      
      public final class OtherWorld {
          public boolean isBrainy = false;
      }
      """
    )

    val pkgInfo = srcPackagePath.resolve("package-info.java").toFile()

    pkgInfo.writeJava(
      """
      /** 
       * Some javadoc comments about the package.
       */
      package org.example;
      """
    )

    val testDir = testProjectDir.newFolder("src", "test", "java")

    testDir.toPath().resolve(packagePath).toFile().mkdirs()
    val testPackagePath = testDir.toPath().resolve(packagePath)
    testPackagePath.toFile().mkdirs()
    val helloWorldTestJava = testPackagePath.resolve("HelloWorldTest.java").toFile()

    helloWorldTestJava.writeJava(
      """
      package org.example;
      
      import org.junit.Test;
      import static org.example.HelloWorldAssert.assertThat;
      
      public final class HelloWorldTest {
          
          @Test
          public void check() {
              HelloWorld hw = new HelloWorld();
              assertThat(hw).hasFoo(-1)
                            .doesNotHaveSomeBrains();
          }
      }
      """
    )

    generatedPackagePath = testProjectDir.newFolder("build", "generated-src", "main-test", "java").toPath()
      .resolve(packagePath)
  }

  @Test
  fun `does not include package info file`() {
    val result = GradleRunner.create()
      .withProjectDir(testProjectDir.root)
      .withDebug(true)
      .withPluginClasspath()
      .withArguments("-i", "-s", "check")
      .build()

    assertThat(result.task(":generateAssertJ")).isSuccessful()
    assertThat(result.task(":test")).isSuccessful()

    assertThat(generatedPackagePath.resolve("Package-InfoAssertions.java"))
      .`as` { "$generatedPackagePath/Package-InfoAssertions does not exist" }
      .doesNotExist()
    assertThat(generatedPackagePath.resolve("Assertions.java"))
      .content()
      .`as` { "$generatedPackagePath/Assertions does not have \"package-info\" in it" }
      .doesNotContain("package-info")
  }
}

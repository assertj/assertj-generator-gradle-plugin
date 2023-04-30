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
import org.assertj.generator.gradle.TestUtils.writeDefaultBuildKts
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Paths

internal class KotlinSourcesBuild {

  @get:Rule
  val testProjectDir = TemporaryFolder()

  @Test
  fun `only have kotlin sources`() {
    testProjectDir.newFile("build.gradle.kts").writeDefaultBuildKts()

    val srcDir = testProjectDir.newFolder("src", "main", "kotlin")

    val packagePath = Paths.get("org/example/")

    val srcPackagePath = srcDir.toPath().resolve(packagePath)
    srcPackagePath.toFile().mkdirs()
    val helloWorldKt = srcPackagePath.resolve("HelloWorld2.kt").toFile()

    helloWorldKt.writeKotlin(
      """
      package org.example
      
      class HelloWorld2 {
          // Getter & Setter
          val isBrains = false
          
          // Getter
          val foo = -1
      }
      """
    )

    val testDir = testProjectDir.newFolder("src", "test", "kotlin")

    testDir.toPath().resolve(packagePath).toFile().mkdirs()
    val testPackagePath = testDir.toPath().resolve(packagePath)
    testPackagePath.toFile().mkdirs()
    val helloWorldTestKt = testPackagePath.resolve("HelloWorldTest.kt").toFile()

    helloWorldTestKt.writeKotlin(
      """
      package org.example
      
      import org.junit.Test
      import org.example.Assertions.assertThat
      
      internal class HelloWorldTest {                
          @Test
          fun check() {
              val hw = HelloWorld2()
              assertThat(hw).hasFoo(-1)
                            .isNotBrains()
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

    val generatedPackagePath = testProjectDir.root.toPath()
      .resolve("build/generated-src/main-test/java")
      .resolve("org/example")

    assertThat(generatedPackagePath)
      .isDirectoryContaining("glob:**/Assertions.java")
      .isDirectoryContaining("glob:**/HelloWorld2Assert.java")
  }
}

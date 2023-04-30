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

import org.assertj.assertions.generator.AssertionsEntryPointType
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.assertj.generator.gradle.TestUtils.writeBuildFile
import org.assertj.generator.gradle.isSuccessful
import org.assertj.generator.gradle.isUpToDate
import org.assertj.generator.gradle.writeJava
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
internal class EntryPointGeneration {

  @get:Rule
  val testProjectDir = TemporaryFolder()

  private lateinit var buildFile: File
  private lateinit var srcPackagePath: Path
  private lateinit var packagePath: Path

  @Before
  fun setup() {
    buildFile = testProjectDir.newFile("build.gradle")

    val srcDir = testProjectDir.newFolder("src", "main", "java")
    val testDir = testProjectDir.newFolder("src", "test", "java")

    packagePath = Paths.get("org/example/")

    srcPackagePath = srcDir.toPath().resolve(packagePath)
    srcPackagePath.toFile().mkdirs()
    val helloWorldJava = srcPackagePath.resolve("HelloWorld.java").toFile()

    helloWorldJava.writeJava(
      """
        package org.example;
        
        public final class HelloWorld {
            // Field
            public boolean hasSomeBrains = false;
        }
        """
    )

    val testSrcDir = testDir.toPath().resolve(packagePath)
    testSrcDir.toFile().mkdirs()

    val helloWorldTestJava = testSrcDir.resolve("HelloWorldTest.java")
    helloWorldTestJava.writeJava(
      """
        package org.example;
        
        import org.junit.*;
        
        public class HelloWorldTest {
            @Test
            public void test() {
              HelloWorld ut = new HelloWorld();
              HelloWorldAssert.assertThat(ut).doesNotHaveSomeBrains();
              
              ut.hasSomeBrains = true;
              HelloWorldAssert.assertThat(ut).hasSomeBrains();
            }
        }
        """
    )
  }

  @Test
  fun `change generate from sourceSet`() {
    buildFile.writeBuildFile(
      """
          sourceSets {
              main {
                  assertJ {
                      entryPoints {
                          standard = true
                          junitSoft = true
                          soft = true
                      }     
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

    val generatedPackage = testProjectDir.root.toPath()
      .resolve("build/generated-src/main-test/java")
      .resolve(packagePath)
    val files = listOf(
      "HelloWorldAssert.java",
      AssertionsEntryPointType.STANDARD.fileName,
//          AssertionsEntryPointType.BDD.fileName,
      AssertionsEntryPointType.JUNIT_SOFT.fileName,
      AssertionsEntryPointType.SOFT.fileName,
    )

    assertThat(files).allSatisfy {
      assertThat(generatedPackage.resolve(it)).exists()
    }
  }

  @Test
  fun `change generate from global`() {
    buildFile.writeBuildFile(
      """
        sourceSets {
            main {
                assertJ { 
                    entryPoints { only('bdd') }
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

    val generatedPackage = testProjectDir.root.toPath()
      .resolve("build/generated-src/main-test/java")
      .resolve(packagePath)
    val files = listOf(
      "HelloWorldAssert.java",
//          AssertionsEntryPointType.STANDARD.fileName,
      AssertionsEntryPointType.BDD.fileName,
//          AssertionsEntryPointType.JUNIT_SOFT.fileName,
//          AssertionsEntryPointType.SOFT.fileName,
    )

    assertThat(files).allSatisfy {
      assertThat(generatedPackage.resolve(it)).exists()
    }
  }

  @Test
  fun `change entry point package`() {
    buildFile.writeBuildFile(
      """
        sourceSets {
            main {
                assertJ {
                    entryPoints {
                        classPackage = 'org.other'
                        only()
                        
                        bdd = true
                    }
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

    val generatedSrcDir = testProjectDir.root.toPath()
      .resolve("build/generated-src/main-test/java")

    val generatedPackageDir = generatedSrcDir.resolve(packagePath)
    val otherPackageDir = generatedSrcDir.resolve("org/other/")

    assertThat(generatedPackageDir.resolve("HelloWorldAssert.java")).exists()
    assertThat(otherPackageDir.resolve("BddAssertions.java")).exists()
  }

  @Test
  fun `change entry point package and entry points`() {
    buildFile.writeBuildFile(
      """
        sourceSets {
            main {
                assertJ {
                    entryPoints {
                        classPackage = 'org.other'
                        only() // none others
                        
                        bdd = true
                    }
                }
            }
        }
        """
    )

    val runner = GradleRunner.create()
      .withProjectDir(testProjectDir.root)
      .withDebug(true)
      .withPluginClasspath()
      .withArguments("-i", "-s", "test")

    val result = runner.build()

    assertThat(result.task(":generateAssertJ")).isSuccessful()
    assertThat(result.task(":test")).isSuccessful()

    val generatedSrcDir = testProjectDir.root.toPath()
      .resolve("build/generated-src/main-test/java")

    val generatedPackageDir = generatedSrcDir.resolve(packagePath)
    val otherPackageDir = generatedSrcDir.resolve("org/other/")

    assertThat(generatedPackageDir.resolve("HelloWorldAssert.java")).exists()
    assertThat(otherPackageDir.resolve("BddAssertions.java")).exists()

    // Run it again, there may be issues with serialization
    val result2 = runner.build()

    assertThat(result2.task(":generateAssertJ")).isUpToDate()
    assertThat(result2.task(":test")).isUpToDate()

    assertThat(generatedPackageDir.resolve("HelloWorldAssert.java")).exists()
    assertThat(otherPackageDir.resolve("BddAssertions.java")).exists()
  }
}

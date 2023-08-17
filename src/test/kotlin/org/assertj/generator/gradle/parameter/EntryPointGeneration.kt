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

import net.navatwo.gradle.testkit.junit5.GradleProject
import org.assertj.assertions.generator.AssertionsEntryPointType
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.assertj.generator.gradle.TestUtils.writeBuildFile
import org.assertj.generator.gradle.isSuccessful
import org.assertj.generator.gradle.isUpToDate
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Checks the behaviour of overriding globals in a project
 */
internal class EntryPointGeneration {

  private val File.buildFile: File
    get() = resolve("build.gradle")

  private val packagePath: File
    get() = File("org/example")

  @Test
  @GradleProject("entry-point-generation")
  fun `change generate from sourceSet`(
    @GradleProject.Root root: File,
    @GradleProject.Runner runner: GradleRunner,
  ) {
    root.buildFile.writeBuildFile(
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

    val result = runner.withArguments("-i", "-s", "test").build()

    assertThat(result.task(":generateAssertJ")).isSuccessful()
    assertThat(result.task(":test")).isSuccessful()

    val generatedPackage = root
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
  @GradleProject("entry-point-generation")
  fun `change generate from global`(
    @GradleProject.Root root: File,
    @GradleProject.Runner runner: GradleRunner,
  ) {
    root.buildFile.writeBuildFile(
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

    val result = runner.withArguments("-i", "-s", "test").build()

    assertThat(result.task(":generateAssertJ")).isSuccessful()
    assertThat(result.task(":test")).isSuccessful()

    val generatedPackage = root
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
  @GradleProject("entry-point-generation")
  fun `change entry point package`(
    @GradleProject.Root root: File,
    @GradleProject.Runner runner: GradleRunner,
  ) {
    root.buildFile.writeBuildFile(
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

    val result = runner.withArguments("-i", "-s", "test").build()

    assertThat(result.task(":generateAssertJ")).isSuccessful()
    assertThat(result.task(":test")).isSuccessful()

    val generatedSrcDir = root.resolve("build/generated-src/main-test/java")

    val generatedPackageDir = generatedSrcDir.resolve(packagePath)
    val otherPackageDir = generatedSrcDir.resolve("org/other/")

    assertThat(generatedPackageDir.resolve("HelloWorldAssert.java")).exists()
    assertThat(otherPackageDir.resolve("BddAssertions.java")).exists()
  }

  @Test
  @GradleProject("entry-point-generation")
  fun `change entry point package and entry points`(
    @GradleProject.Root root: File,
    @GradleProject.Runner runner: GradleRunner,
  ) {
    root.buildFile.writeBuildFile(
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

    val testRunner = runner.withArguments("-i", "-s", "test")

    val result = testRunner.build()

    assertThat(result.task(":generateAssertJ")).isSuccessful()
    assertThat(result.task(":test")).isSuccessful()

    val generatedSrcDir = root.resolve("build/generated-src/main-test/java")

    val generatedPackageDir = generatedSrcDir.resolve(packagePath)
    val otherPackageDir = generatedSrcDir.resolve("org/other/")

    assertThat(generatedPackageDir.resolve("HelloWorldAssert.java")).exists()
    assertThat(otherPackageDir.resolve("BddAssertions.java")).exists()

    // Run it again, there may be issues with serialization
    val result2 = testRunner.build()

    assertThat(result2.task(":generateAssertJ")).isUpToDate()
    assertThat(result2.task(":test")).isUpToDate()

    assertThat(generatedPackageDir.resolve("HelloWorldAssert.java")).exists()
    assertThat(otherPackageDir.resolve("BddAssertions.java")).exists()
  }
}

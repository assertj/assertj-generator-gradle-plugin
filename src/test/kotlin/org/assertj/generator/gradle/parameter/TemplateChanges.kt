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
import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.assertj.generator.gradle.TestUtils.writeBuildFile
import org.assertj.generator.gradle.isSuccessful
import org.assertj.generator.gradle.isUpToDate
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import java.io.File

private const val TEMPLATE_CONTENT = "/* %%% \${property} %%% */"

/**
 * Checks the behaviour of overriding globals in a project
 */
internal class TemplateChanges {

  private val File.buildFile: File
    get() = resolve("build.gradle")
  private val File.resolvedPackagePath: File
    get() = resolve("build/generated-src/main-test/java")
      .resolve(packagePath)
  private val packagePath: File
    get() = File("org/example")

  @Test
  @GradleProject("template-changes")
  fun `change default template from sourceSet`(
    @GradleProject.Root root: File,
    @GradleProject.Runner runner: GradleRunner,
  ) {
    root.buildFile.writeBuildFile(
      """
        sourceSets {
            main {
                assertJ {
                    templates {
                        methods {
                            wholeNumberPrimitive.template('$TEMPLATE_CONTENT')
                        }
                    }              
                }
            }
        }
        """
    )

    val result = runner.withArguments("-i", "-s", "test").build()

    assertThat(result.task(":generateAssertJ")).isSuccessful()
    assertThat(result.task(":test")).isSuccessful()

    val generatedAssert = root.resolvedPackagePath.resolve("HelloWorldAssert.java")

    assertThat(generatedAssert).content().contains("/* %%% foo %%% */")
  }

  @Test
  @GradleProject("template-changes")
  fun `change default template from global`(
    @GradleProject.Root root: File,
    @GradleProject.Runner runner: GradleRunner,
  ) {
    root.buildFile.writeBuildFile(
      """
      sourceSets {
          main {
              assertJ { 
                  templates {
                      methods {
                          wholeNumberPrimitive.template('$TEMPLATE_CONTENT')
                      }
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

    val generatedAssert = root.resolvedPackagePath.resolve("HelloWorldAssert.java")

    assertThat(generatedAssert).content().contains("/* %%% foo %%% */")
  }

  @Test
  @GradleProject("template-changes")
  fun `incremental templates with no changes`(
    @GradleProject.Root root: File,
    @GradleProject.Runner runner: GradleRunner,
  ) {
    root.buildFile.writeBuildFile(
      """            
      sourceSets {
          main {
              assertJ {
                  templates {
                      methods {
                          wholeNumberPrimitive.template('$TEMPLATE_CONTENT')
                      }
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

    val generatedAssert = root.resolvedPackagePath.resolve("HelloWorldAssert.java")

    assertThat(generatedAssert).content().contains("/* %%% foo %%% */")

    val result2 = testRunner.build()

    assertThat(result2.task(":generateAssertJ")).isUpToDate()
    assertThat(result2.task(":test")).isUpToDate()

    assertThat(generatedAssert).content().contains("/* %%% foo %%% */")
  }

  @Test
  @GradleProject("template-changes")
  fun `incremental templates after string change`(
    @GradleProject.Root root: File,
    @GradleProject.Runner runner: GradleRunner,
  ) {
    root.buildFile.writeBuildFile(
      """
        sourceSets {
            main {
                assertJ { 
                    templates {
                        methods { wholeNumberPrimitive.template('$TEMPLATE_CONTENT') }
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

    val generatedAssert = root.resolvedPackagePath.resolve("HelloWorldAssert.java")

    assertThat(generatedAssert).content().contains("/* %%% foo %%% */")

    // Now we update the content of the template assertion to make sure that the task is re-run

    val newTemplateContent = "/* % NEW CONTENT % */"
    root.buildFile.writeBuildFile(
      """
        sourceSets {
            main {
                assertJ { 
                    templates {
                        methods {
                            wholeNumberPrimitive.template('$newTemplateContent')
                        }
                    }
                }
            }
        }
        """
    )

    val result2 = testRunner.build()

    assertThat(result2.task(":generateAssertJ")).isSuccessful()
    assertThat(result2.task(":test")).isUpToDate() // no test changes

    assertThat(generatedAssert).content().contains(newTemplateContent)
  }

  @Test
  @GradleProject("template-changes")
  fun `incremental templates change type`(
    @GradleProject.Root root: File,
    @GradleProject.Runner runner: GradleRunner,
  ) {
    root.buildFile.writeBuildFile(
      """
        sourceSets {
            main {
                assertJ { 
                    templates {
                        methods { wholeNumberPrimitive.template('$TEMPLATE_CONTENT') }
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

    val generatedAssert = root.resolvedPackagePath.resolve("HelloWorldAssert.java")

    assertThat(generatedAssert).content().contains("/* %%% foo %%% */")

    // Now we update the content of the template assertion to make sure that the task is re-run

    val newTemplateContent = "/* % NEW CONTENT % */"

    val templateFolder = root.resolve("templates").apply { mkdirs() }
    val content = templateFolder.toPath().resolve("template.txt").toFile()
    content.writeText(newTemplateContent)

    val contentPath = root.toPath().relativize(content.toPath()).toString().replace("\\", "/")

    root.buildFile.writeBuildFile(
      """
        sourceSets {
            main {
                assertJ {
                    templates {
                        methods { wholeNumberPrimitive.file('$contentPath') }
                    }  
                }
            }
        }
        """
    )

    val result2 = testRunner.build()

    assertThat(result2.task(":generateAssertJ")).isSuccessful()
    assertThat(result2.task(":test")).isUpToDate() // no tests

    assertThat(generatedAssert).content().contains(newTemplateContent)
  }

  @Test
  @GradleProject("template-changes")
  fun `incremental templates after file change`(
    @GradleProject.Root root: File,
    @GradleProject.Runner runner: GradleRunner,
  ) {
    val templateFolder = root.resolve("templates").apply { mkdirs() }
    val content = templateFolder.toPath().resolve("template.txt").toFile()
    content.writeText(TEMPLATE_CONTENT)

    val contentPath = root.toPath().relativize(content.toPath()).toString().replace("\\", "/")

    root.buildFile.writeBuildFile(
      """
        sourceSets {
            main {
                 assertJ {
                    templates {
                        methods { wholeNumberPrimitive.file('$contentPath') }
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

    val generatedAssert = root.resolvedPackagePath.resolve("HelloWorldAssert.java")

    assertThat(generatedAssert).content().contains("/* %%% foo %%% */")

    // Now we update the content of the template assertion to make sure that the task is re-run

    val newTemplateContent = "/* % NEW CONTENT % */"
    content.writeText(newTemplateContent)

    val result2 = testRunner.build()

    assertThat(result2.task(":generateAssertJ")).isSuccessful()
    assertThat(result2.task(":test")).isUpToDate() // no test files!

    assertThat(generatedAssert).content().contains(newTemplateContent)
  }

  @Test
  @GradleProject("template-changes")
  fun `bad template name should fail`(
    @GradleProject.Root root: File,
    @GradleProject.Runner runner: GradleRunner,
  ) {
    root.buildFile.writeBuildFile(
      """
        sourceSets {
            main {
                assertJ {
                    templates {
                        methods { wholeNumberAssertion.template('$TEMPLATE_CONTENT') }
                    }   
                }
            }
        }
        """
    )

    val result = runner.withArguments("-i", "-s", "generateAssertJ").buildAndFail()

    assertThat(result.output).contains("wholeNumberAssertion")
  }
}

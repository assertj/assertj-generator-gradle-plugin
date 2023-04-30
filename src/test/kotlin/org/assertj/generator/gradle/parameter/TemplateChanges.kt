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

private const val TEMPLATE_CONTENT = "/* %%% \${property} %%% */"

/**
 * Checks the behaviour of overriding globals in a project
 */
internal class TemplateChanges {
  @get:Rule
  val testProjectDir = TemporaryFolder()

  private lateinit var buildFile: File
  private lateinit var srcPackagePath: Path
  private lateinit var packagePath: Path
  private lateinit var resolvedPackagePath: Path

  @Before
  fun setup() {
    buildFile = testProjectDir.newFile("build.gradle")

    val srcDir = testProjectDir.newFolder("src", "main", "java")

    packagePath = Paths.get("org/example/")

    srcPackagePath = srcDir.toPath().resolve(packagePath)
    srcPackagePath.toFile().mkdirs()
    val helloWorldJava = srcPackagePath.resolve("HelloWorld.java").toFile()

    helloWorldJava.writeJava(
      """
      package org.example;
      
      public final class HelloWorld {
          // Getter
          public int getFoo() {
              return -1;
          }
      }
      """
    )

    resolvedPackagePath = testProjectDir.root.toPath()
      .resolve("build/generated-src/main-test/java")
      .resolve(packagePath)
  }

  @Test
  fun `change default template from sourceSet`() {
    buildFile.writeBuildFile(
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

    val result = GradleRunner.create()
      .withProjectDir(testProjectDir.root)
      .withDebug(true)
      .withPluginClasspath()
      .withArguments("-i", "-s", "test")
      .build()

    assertThat(result.task(":generateAssertJ")).isSuccessful()
    assertThat(result.task(":test")).isSuccessful()

    val generatedAssert = resolvedPackagePath.resolve("HelloWorldAssert.java")

    assertThat(generatedAssert).content().contains("/* %%% foo %%% */")
  }

  @Test
  fun `change default template from global`() {
    buildFile.writeBuildFile(
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

    val runner = GradleRunner.create()
      .withProjectDir(testProjectDir.root)
      .withDebug(true)
      .withPluginClasspath()
      .withArguments("-i", "-s", "test")

    val result = runner.build()

    assertThat(result.task(":generateAssertJ")).isSuccessful()
    assertThat(result.task(":test")).isSuccessful()

    val generatedAssert = resolvedPackagePath.resolve("HelloWorldAssert.java")

    assertThat(generatedAssert).content().contains("/* %%% foo %%% */")
  }

  @Test
  fun `incremental templates with no changes`() {
    buildFile.writeBuildFile(
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

    val runner = GradleRunner.create()
      .withProjectDir(testProjectDir.root)
      .withDebug(true)
      .withPluginClasspath()
      .withArguments("-i", "-s", "test")

    val result = runner.build()

    assertThat(result.task(":generateAssertJ")).isSuccessful()
    assertThat(result.task(":test")).isSuccessful()

    val generatedAssert = resolvedPackagePath.resolve("HelloWorldAssert.java")

    assertThat(generatedAssert).content().contains("/* %%% foo %%% */")

    val result2 = runner.build()

    assertThat(result2.task(":generateAssertJ")).isUpToDate()
    assertThat(result2.task(":test")).isUpToDate()

    assertThat(generatedAssert).content().contains("/* %%% foo %%% */")
  }

  @Test
  fun `incremental templates after string change`() {
    buildFile.writeBuildFile(
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

    val runner = GradleRunner.create()
      .withProjectDir(testProjectDir.root)
      .withDebug(true)
      .withPluginClasspath()
      .withArguments("-i", "-s", "test")

    val result = runner.build()

    assertThat(result.task(":generateAssertJ")).isSuccessful()
    assertThat(result.task(":test")).isSuccessful()

    val generatedAssert = resolvedPackagePath.resolve("HelloWorldAssert.java")

    assertThat(generatedAssert).content().contains("/* %%% foo %%% */")

    // Now we update the content of the template assertion to make sure that the task is re-run

    val newTemplateContent = "/* % NEW CONTENT % */"
    buildFile.writeBuildFile(
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

    val result2 = runner.build()

    assertThat(result2.task(":generateAssertJ")).isSuccessful()
    assertThat(result2.task(":test")).isUpToDate() // no test changes

    assertThat(generatedAssert).content().contains(newTemplateContent)
  }

  @Test
  fun `incremental templates change type`() {
    buildFile.writeBuildFile(
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

    val runner = GradleRunner.create()
      .withProjectDir(testProjectDir.root)
      .withDebug(true)
      .withPluginClasspath()
      .withArguments("-i", "-s", "test")

    val result = runner.build()

    assertThat(result.task(":generateAssertJ")).isSuccessful()
    assertThat(result.task(":test")).isSuccessful()

    val generatedAssert = resolvedPackagePath.resolve("HelloWorldAssert.java")

    assertThat(generatedAssert).content().contains("/* %%% foo %%% */")

    // Now we update the content of the template assertion to make sure that the task is re-run

    val newTemplateContent = "/* % NEW CONTENT % */"

    val templateFolder = testProjectDir.newFolder("templates")
    val content = templateFolder.toPath().resolve("template.txt").toFile()
    content.writeText(newTemplateContent)

    val contentPath = testProjectDir.root.toPath().relativize(content.toPath()).toString().replace("\\", "/")

    buildFile.writeBuildFile(
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

    val result2 = runner.build()

    assertThat(result2.task(":generateAssertJ")).isSuccessful()
    assertThat(result2.task(":test")).isUpToDate() // no tests

    assertThat(generatedAssert).content().contains(newTemplateContent)
  }

  @Test
  fun `incremental templates after file change`() {
    val templateFolder = testProjectDir.newFolder("templates")
    val content = templateFolder.toPath().resolve("template.txt").toFile()
    content.writeText(TEMPLATE_CONTENT)

    val contentPath = testProjectDir.root.toPath().relativize(content.toPath()).toString().replace("\\", "/")

    buildFile.writeBuildFile(
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

    val runner = GradleRunner.create()
      .withProjectDir(testProjectDir.root)
      .withDebug(true)
      .withPluginClasspath()
      .withArguments("-i", "-s", "test")

    val result = runner.build()

    assertThat(result.task(":generateAssertJ")).isSuccessful()
    assertThat(result.task(":test")).isSuccessful()

    val generatedAssert = resolvedPackagePath.resolve("HelloWorldAssert.java")

    assertThat(generatedAssert).content().contains("/* %%% foo %%% */")

    // Now we update the content of the template assertion to make sure that the task is re-run

    val newTemplateContent = "/* % NEW CONTENT % */"
    content.writeText(newTemplateContent)

    val result2 = runner.build()

    assertThat(result2.task(":generateAssertJ")).isSuccessful()
    assertThat(result2.task(":test")).isUpToDate() // no test files!

    assertThat(generatedAssert).content().contains(newTemplateContent)
  }

  @Test
  fun `bad template name should fail`() {
    buildFile.writeBuildFile(
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

    val result = GradleRunner.create()
      .withProjectDir(testProjectDir.root)
      .withDebug(true)
      .withPluginClasspath()
      .withArguments("-i", "-s", "generateAssertJ")
      .buildAndFail()

    assertThat(result.output).contains("wholeNumberAssertion")
  }
}

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
import org.assertj.core.api.Assertions.assertThat
import org.assertj.generator.gradle.TestUtils.withCiGradle
import org.assertj.generator.gradle.isSuccessful
import org.assertj.generator.gradle.writeGroovy
import org.assertj.generator.gradle.writeJava
import org.gradle.testkit.runner.GradleRunner
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Checks that we can include/exclude classes via the `classes` filter.
 */
internal class ClassesFilter {
  private val File.helloWorldTestJava: File
    get() = resolve("src/test/java/org/example/HelloWorldTest.java")

  private val File.generatedBasePackagePath: File
    get() = resolve("build/generated-src/main-test/java/org/example")

  private val File.buildFile: File
    get() = resolve("build.gradle")

  private fun File.testFile(@Language("java") testContent: String): File {
    helloWorldTestJava.writeJava(
      """
        package org.example;
        
        import org.example.hello.*;
        import org.example.hello.sub.*;
        import org.example.other.*;
        import org.junit.Test;
        import org.assertj.core.api.Assertions;
        import static org.example.Assertions.assertThat;
        
        public final class HelloWorldTest {
            $testContent
        }
        """
    )

    return helloWorldTestJava
  }

  @Test
  @GradleProject("classes-filter")
  fun `include class simple`(
    @GradleProject.Root root: File,
    @GradleProject.Runner runner: GradleRunner,
  ) {
    root.buildFile(
      """
        assertJ {
            entryPoints {
                classPackage = "org.example"
            }
            classes {
                include "org.example.hello.HelloWorld"
            }
        }
        """
    )

    root.setupTestHelloWorld()

    runner.withCiGradle().runAndAssertBuild()

    assertThat(root.generatedBasePackagePath.resolve("hello")).exists()
    assertThat(root.generatedBasePackagePath.resolve("other")).doesNotExist()
    assertThat(root.generatedBasePackagePath.resolve("hello/sub")).doesNotExist()
  }

  @Test
  @GradleProject("classes-filter")
  fun `include class pattern`(
    @GradleProject.Root root: File,
    @GradleProject.Runner runner: GradleRunner,
  ) {
    root.buildFile(
      """
        assertJ {
            entryPoints {
                classPackage = "org.example"
            }
            classes {
                include "org.example.hello.Hello*"
            }
        }
        """
    )

    root.setupTestHelloWorld()

    runner.runAndAssertBuild()

    assertThat(root.generatedBasePackagePath.resolve("hello")).exists()
    assertThat(root.generatedBasePackagePath.resolve("other")).doesNotExist()
    assertThat(root.generatedBasePackagePath.resolve("hello/sub")).doesNotExist()
  }

  @Test
  @GradleProject("classes-filter")
  fun `include class that does not exist and valid`(
    @GradleProject.Root root: File,
    @GradleProject.Runner runner: GradleRunner,
  ) {
    root.buildFile(
      """
        assertJ {
            entryPoints {
                classPackage = "org.example"
            }
            classes {
                include "org.example.hello.*", "org.example.does_not_exist"
            }
        }
        """
    )

    root.setupTestHelloWorld()

    runner.runAndAssertBuild()

    assertThat(root.generatedBasePackagePath.resolve("hello")).exists()
    assertThat(root.generatedBasePackagePath.resolve("other")).doesNotExist()
    assertThat(root.generatedBasePackagePath.resolve("hello/sub")).doesNotExist()
  }

  @Test
  @GradleProject("classes-filter")
  fun `include class double wildcard`(
    @GradleProject.Root root: File,
    @GradleProject.Runner runner: GradleRunner,
  ) {
    root.buildFile(
      """
        assertJ {
            entryPoints {
                classPackage = "org.example"
            }
            classes {
                include "org.example.hello**"
            }
        }
        """
    )

    root.setupTestHelloAndSub()

    runner.runAndAssertBuild()

    assertThat(root.generatedBasePackagePath.resolve("other")).doesNotExist()
    assertThat(root.generatedBasePackagePath.resolve("hello")).exists()
    assertThat(root.generatedBasePackagePath.resolve("hello/sub")).exists()
  }

  @Test
  @GradleProject("classes-filter")
  fun `exclude class simple`(
    @GradleProject.Root root: File,
    @GradleProject.Runner runner: GradleRunner,
  ) {
    root.buildFile(
      """
        assertJ {
            entryPoints {
                classPackage = "org.example"
            }
            classes {
                exclude "org.example.other.OtherWorld"
            }
        }
        """
    )

    root.setupTestHelloAndSub()

    runner.runAndAssertBuild()

    assertThat(root.generatedBasePackagePath.resolve("hello")).exists()
    assertThat(root.generatedBasePackagePath.resolve("hello/sub")).exists()
    assertThat(root.generatedBasePackagePath.resolve("other")).doesNotExist()
  }

  @Test
  @GradleProject("classes-filter")
  fun `exclude class pattern`(
    @GradleProject.Root root: File,
    @GradleProject.Runner runner: GradleRunner,
  ) {
    root.buildFile(
      """
        assertJ {
            entryPoints {
                classPackage = "org.example"
            }
            classes {
                exclude "org.example.other.*"
            }
        }
        """
    )

    root.setupTestHelloAndSub()

    runner.runAndAssertBuild()

    assertThat(root.generatedBasePackagePath.resolve("other")).doesNotExist()
    assertThat(root.generatedBasePackagePath.resolve("hello")).exists()
    assertThat(root.generatedBasePackagePath.resolve("hello/sub")).exists()
  }

  @Test
  @GradleProject("classes-filter")
  fun `exclude class that does not exist and valid`(
    @GradleProject.Root root: File,
    @GradleProject.Runner runner: GradleRunner,
  ) {
    root.buildFile(
      """
        assertJ {
            entryPoints {
                classPackage = "org.example"
            }
            classes {
                exclude "org.example.other.*", "org.example.does_not_exist"
            }
        }
        """
    )

    root.testFile(
      """
        @Test
        public void checkHello() {
            HelloWorld hw = new HelloWorld();
            assertThat(hw).doesNotHaveSomeBrains();
        }
        
        @Test
        public void checkOther() {
            SubHelloWorld shw = new SubHelloWorld();
            assertThat(shw).hasFoo(-1);
        }
        """
    )

    runner.runAndAssertBuild()

    assertThat(root.generatedBasePackagePath.resolve("other")).doesNotExist()
    assertThat(root.generatedBasePackagePath.resolve("hello")).exists()
    assertThat(root.generatedBasePackagePath.resolve("hello/sub")).exists()
  }

  @Test
  @GradleProject("classes-filter")
  fun `exclude class double wildcard`(
    @GradleProject.Root root: File,
    @GradleProject.Runner runner: GradleRunner,
  ) {
    root.buildFile(
      """
        assertJ {
            entryPoints {
                classPackage = "org.example"
            }
            classes {
                exclude "org.example.**"
            }
        }
        """
    )

    // Since we are excluding _everything_ we need to add a custom test
    root.helloWorldTestJava.writeJava(
      """
        package org.example;
        
        import org.example.hello.*;
        import org.example.other.*;
        import org.junit.Test;
        import static org.assertj.core.api.Assertions.assertThat;
        
        public final class HelloWorldTest {
            @Test
            public void checkNull() {
               assertThat(true).isTrue();
            }
        }
        """
    )

    runner.runAndAssertBuild()

    assertThat(root.generatedBasePackagePath.resolve("other")).doesNotExist()
    assertThat(root.generatedBasePackagePath.resolve("hello")).doesNotExist()
  }

  @Test
  @GradleProject("classes-filter")
  fun `include double wildcard but exclude specific class`(
    @GradleProject.Root root: File,
    @GradleProject.Runner runner: GradleRunner,
  ) {
    root.buildFile(
      """
        assertJ {
            entryPoints {
                classPackage = "org.example"
            }
            classes {
                include "org.example.hello**"
                exclude "org.example.hello.sub.*"
            }
        }
        """
    )

    root.setupTestHelloWorld()

    runner.runAndAssertBuild()

    assertThat(root.generatedBasePackagePath.resolve("other")).doesNotExist()
    assertThat(root.generatedBasePackagePath.resolve("hello")).exists()
    assertThat(root.generatedBasePackagePath.resolve("hello/sub")).doesNotExist()
  }

  private fun File.setupTestHelloWorld(): File = testFile(
    """
      @Test
      public void checkHello() {
          HelloWorld hw = new HelloWorld();
          assertThat(hw).doesNotHaveSomeBrains();
      }
      """
  )

  private fun GradleRunner.runAndAssertBuild() {
    val result = withDebug(true).withArguments("-i", "-s", "test").build()

    assertThat(result.task(":generateAssertJ")).isSuccessful()
    assertThat(result.task(":test")).isSuccessful()
  }

  private fun File.buildFile(@Language("groovy") configuration: String) {
    buildFile.writeGroovy(
      """
        // Add required plugins and source sets to the sub projects
        plugins {
          id "org.assertj.generator" // Note must use this syntax
          id "java" 
        } 

        // Override defaults
        sourceSets {
            main {
                ${configuration.trimIndent()}
            }
        }
        
        // add some classpath dependencies
        repositories {
            mavenCentral()
        }
                    
        dependencies { 
            implementation group: 'javax.annotation', name: 'javax.annotation-api', version: '1.3.2'
            
            testImplementation group: 'org.assertj', name: 'assertj-core', version: '3.24.2'
            testImplementation group: 'junit', name: 'junit', version: '4.13.1'
        }
        """
    )
  }

  private fun File.setupTestHelloAndSub(): File = testFile(
    """
      @Test
      public void checkHello() {
          HelloWorld hw = new HelloWorld();
          assertThat(hw).doesNotHaveSomeBrains();
      }
      
      @Test
      public void checkOther() {
          SubHelloWorld shw = new SubHelloWorld();
          assertThat(shw).hasFoo(-1);
      }
      """
  )
}

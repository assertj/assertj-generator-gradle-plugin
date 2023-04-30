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

import org.assertj.core.api.Assertions.assertThat
import org.assertj.generator.gradle.isSuccessful
import org.assertj.generator.gradle.writeGroovy
import org.assertj.generator.gradle.writeJava
import org.gradle.testkit.runner.GradleRunner
import org.intellij.lang.annotations.Language
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Checks that we can include/exclude classes via the `classes` filter.
 */
internal class ClassesFilter {

  @get:Rule
  val testProjectDir = TemporaryFolder()

  private lateinit var buildFile: File
  private lateinit var helloWorldTestJava: File
  private lateinit var generatedBasePackagePath: Path

  @Before
  fun setup() {
    buildFile = testProjectDir.newFile("build.gradle")

    val srcDir = testProjectDir.newFolder("src", "main", "java")

    val packagePath = Paths.get("org/example/")

    val srcPackagePath = srcDir.toPath().resolve(packagePath)
    srcPackagePath.toFile().mkdirs()
    val helloWorldJava = srcPackagePath.resolve("HelloWorld.java").toFile()

    helloWorldJava.writeJava(
      """
        package org.example.hello;
        
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

    val subHelloWorldJava = srcPackagePath.resolve("sub/SubHelloWorld.java").toFile()
    subHelloWorldJava.parentFile.mkdirs()

    subHelloWorldJava.writeJava(
      """
        package org.example.hello.sub;
        
        public final class SubHelloWorld {
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
        package org.example.other;
        
        public final class OtherWorld {
            public boolean isBrainy = false;
        }
        """
    )

    val testDir = testProjectDir.newFolder("src", "test", "java")

    testDir.toPath().resolve(packagePath).toFile().mkdirs()
    val testPackagePath = testDir.toPath().resolve(packagePath)
    testPackagePath.toFile().mkdirs()

    helloWorldTestJava = testPackagePath.resolve("HelloWorldTest.java").toFile()

    generatedBasePackagePath = testProjectDir.root.toPath()
      .resolve("build/generated-src/main-test/java")
      .resolve(packagePath)
  }

  @Test
  fun `include class simple`() {
    buildFile(
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

    setupTestHelloWorld()

    runAndAssertBuild()

    assertThat(generatedBasePackagePath.resolve("hello")).exists()
    assertThat(generatedBasePackagePath.resolve("other")).doesNotExist()
    assertThat(generatedBasePackagePath.resolve("hello/sub")).doesNotExist()
  }

  @Test
  fun `include class pattern`() {
    buildFile(
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

    setupTestHelloWorld()

    runAndAssertBuild()

    assertThat(generatedBasePackagePath.resolve("hello")).exists()
    assertThat(generatedBasePackagePath.resolve("other")).doesNotExist()
    assertThat(generatedBasePackagePath.resolve("hello/sub")).doesNotExist()
  }

  @Test
  fun `include class that does not exist and valid`() {
    buildFile(
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

    setupTestHelloWorld()

    runAndAssertBuild()

    assertThat(generatedBasePackagePath.resolve("hello")).exists()
    assertThat(generatedBasePackagePath.resolve("other")).doesNotExist()
    assertThat(generatedBasePackagePath.resolve("hello/sub")).doesNotExist()
  }

  @Test
  fun `include class double wildcard`() {
    buildFile(
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

    setupTestHelloAndSub()

    runAndAssertBuild()

    assertThat(generatedBasePackagePath.resolve("other")).doesNotExist()
    assertThat(generatedBasePackagePath.resolve("hello")).exists()
    assertThat(generatedBasePackagePath.resolve("hello/sub")).exists()
  }

  @Test
  fun `exclude class simple`() {
    buildFile(
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

    setupTestHelloAndSub()

    runAndAssertBuild()

    assertThat(generatedBasePackagePath.resolve("hello")).exists()
    assertThat(generatedBasePackagePath.resolve("hello/sub")).exists()
    assertThat(generatedBasePackagePath.resolve("other")).doesNotExist()
  }

  @Test
  fun `exclude class pattern`() {
    buildFile(
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

    setupTestHelloAndSub()

    runAndAssertBuild()

    assertThat(generatedBasePackagePath.resolve("other")).doesNotExist()
    assertThat(generatedBasePackagePath.resolve("hello")).exists()
    assertThat(generatedBasePackagePath.resolve("hello/sub")).exists()
  }

  @Test
  fun `exclude class that does not exist and valid`() {
    buildFile(
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

    testFile(
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

    runAndAssertBuild()

    assertThat(generatedBasePackagePath.resolve("other")).doesNotExist()
    assertThat(generatedBasePackagePath.resolve("hello")).exists()
    assertThat(generatedBasePackagePath.resolve("hello/sub")).exists()
  }

  @Test
  fun `exclude class double wildcard`() {
    buildFile(
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
    helloWorldTestJava.writeJava(
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

    runAndAssertBuild()

    assertThat(generatedBasePackagePath.resolve("other")).doesNotExist()
    assertThat(generatedBasePackagePath.resolve("hello")).doesNotExist()
  }

  @Test
  fun `include double wildcard but exclude specific class`() {
    buildFile(
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

    setupTestHelloWorld()

    runAndAssertBuild()

    assertThat(generatedBasePackagePath.resolve("other")).doesNotExist()
    assertThat(generatedBasePackagePath.resolve("hello")).exists()
    assertThat(generatedBasePackagePath.resolve("hello/sub")).doesNotExist()
  }

  private fun setupTestHelloWorld(): File = testFile(
    """
      @Test
      public void checkHello() {
          HelloWorld hw = new HelloWorld();
          assertThat(hw).doesNotHaveSomeBrains();
      }
      """
  )

  private fun runAndAssertBuild() {
    val result = GradleRunner.create()
      .withProjectDir(testProjectDir.root)
      .withDebug(true)
      .withPluginClasspath()
      .withArguments("-i", "-s", "test")
      .build()

    assertThat(result.task(":generateAssertJ")).isSuccessful()
    assertThat(result.task(":test")).isSuccessful()
  }

  private fun buildFile(@Language("groovy") configuration: String) {
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

  private fun testFile(@Language("java") testContent: String): File {
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

  private fun setupTestHelloAndSub(): File = testFile(
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

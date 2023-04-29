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

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import java.nio.file.Path
import java.nio.file.Paths

import static org.assertj.core.api.Assertions.assertThat

class KotlinSourcesBuild {

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder()

    File buildFile

    @Before
    void setup() {
        buildFile = testProjectDir.newFile("build.gradle.kts")
    }

    @Test
    void only_have_kotlin_sources() {
        File srcDir = testProjectDir.newFolder('src', 'main', 'kotlin')

        Path packagePath = Paths.get("org/example/")

        Path srcPackagePath = srcDir.toPath().resolve(packagePath)
        srcPackagePath.toFile().mkdirs()
        File helloWorldKt = srcPackagePath.resolve('HelloWorld2.kt').toFile()

        helloWorldKt << """
            package org.example;
            
            class HelloWorld2 {
                // Getter & Setter
                var isBrains = false
                
                // Getter
                val foo = -1
            }
            """.stripIndent()

        TestUtils.kotlinBuildFile(buildFile)

        File testDir = testProjectDir.newFolder('src', 'test', 'kotlin')

        testDir.toPath().resolve(packagePath).toFile().mkdirs()
        Path testPackagePath = testDir.toPath().resolve(packagePath)
        testPackagePath.toFile().mkdirs()
        File helloWorldTestKt = testPackagePath.resolve('HelloWorldTest.kt').toFile()

        helloWorldTestKt << """
            package org.example;
            
            import org.junit.Test;
            import org.example.Assertions.assertThat;
            
            internal class HelloWorldTest {                
                @Test
                fun check() {
                    val hw = HelloWorld2();
                    assertThat(hw).hasFoo(-1)
                                  .isNotBrains();
                }
            }
            """.stripIndent()

        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withDebug(true)
                .withPluginClasspath()
                .withArguments('-i', '-s', 'test')
                .build()

        assertThat(result.task(':generateAssertJ').outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.task(':test').outcome).isEqualTo(TaskOutcome.SUCCESS)

        def generatedPackagePath = testProjectDir.root.toPath()
                .resolve("build/generated-src/main-test/java")
                .resolve("org/example")

        assertThat(generatedPackagePath)
                .isDirectoryContaining("glob:**/Assertions.java")
                .isDirectoryContaining("glob:**/HelloWorld2Assert.java")
    }
}

/*
 * Copyright 2017. assertj-generator-gradle-plugin contributors.
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

import org.assertj.generator.gradle.TestUtils
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import java.nio.file.Path
import java.nio.file.Paths

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat

/**
 * Checks the behaviour of overriding globals in a project
 */
class EntryPointGeneration {

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder()

    private File buildFile
    private Path srcPackagePath
    private Path packagePath

    @Before
    void setup() {
        buildFile = testProjectDir.newFile('build.gradle')

        File srcDir = testProjectDir.newFolder('src', 'main', 'java')
        File testDir = testProjectDir.newFolder('src', 'test', 'java')

        packagePath = Paths.get("org/example/")

        srcPackagePath = srcDir.toPath().resolve(packagePath)
        srcPackagePath.toFile().mkdirs()
        File helloWorldJava = srcPackagePath.resolve('HelloWorld.java').toFile()

        helloWorldJava << """
            |package org.example;
            |
            |public final class HelloWorld {
            |    // Field
            |    public boolean hasSomeBrains = false;
            |}""".stripMargin()

        def testSrcDir = testDir.toPath().resolve(packagePath)
        testSrcDir.toFile().mkdirs()

        def helloWorldTestJava = testSrcDir.resolve('HelloWorldTest.java')
        helloWorldTestJava << """
            |package org.example;
            |
            |import org.junit.*;
            |
            |public class HelloWorldTest {
            |    @Test
            |    public void test() {
            |      HelloWorld ut = new HelloWorld();
            |      HelloWorldAssert.assertThat(ut).doesNotHaveSomeBrains();
            |      
            |      ut.hasSomeBrains = true;
            |      HelloWorldAssert.assertThat(ut).hasSomeBrains();
            |    }
            |}""".stripMargin()
    }


    @Test
    void change_generate_from_sourceSet() {
        TestUtils.buildFile(buildFile, """
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
        """)

        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withDebug(true)
                .withPluginClasspath()
                .withArguments('-i', '-s', 'test')
                .build()

        assert result.task(':generateAssertJ').outcome == TaskOutcome.SUCCESS
        assert result.task(':test').outcome == TaskOutcome.SUCCESS

        Path generatedPackage = testProjectDir.root.toPath()
                .resolve("build/generated-src/main-test/java")
                .resolve(packagePath)
        def files = ["HelloWorldAssert.java",
                     "Assertions.java",             // Standard
                     //"BddAssertions.java",          // BDD
                     "JUnitSoftAssertions.java",    // JUNIT_SOFT
                     "SoftAssertions.java"]         // SOFT

        files.each {
            assertThat(generatedPackage.resolve(it)).exists()
        }

    }

    @Test
    void change_generate_from_global() {

        TestUtils.buildFile(buildFile, """
            sourceSets {
                main {
                    assertJ { 
                        entryPoints { only('bdd') }
                    }
                }
            }
        """)

        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withDebug(true)
                .withPluginClasspath()
                .withArguments('-i', '-s', 'test')
                .build()

        assert result.task(':generateAssertJ').outcome == TaskOutcome.SUCCESS
        assert result.task(':test').outcome == TaskOutcome.SUCCESS

        Path generatedPackage = testProjectDir.root.toPath()
                .resolve("build/generated-src/main-test/java")
                .resolve(packagePath)
        def files = ["HelloWorldAssert.java",
                     //"Assertions.java",             // Standard
                     "BddAssertions.java",        // BDD
                     //"JUnitSoftAssertions.java",    // JUNIT_SOFT
                     //"SoftAssertions.java"         // SOFT
        ]

        files.each {
            assertThat(generatedPackage.resolve(it)).exists()
        }

    }

    @Test
    void change_entry_point_package() {
        TestUtils.buildFile(buildFile, """
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
        """.stripMargin())

        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withDebug(true)
                .withPluginClasspath()
                .withArguments('-i', '-s', 'test')
                .build()

        assert result.task(':generateAssertJ').outcome == TaskOutcome.SUCCESS
        assert result.task(':test').outcome == TaskOutcome.SUCCESS

        Path generatedSrcDir = testProjectDir.root.toPath()
                .resolve("build/generated-src/main-test/java")

        Path generatedPackageDir = generatedSrcDir.resolve(packagePath)
        Path otherPackageDir = generatedSrcDir.resolve("org/other/")

        assert generatedPackageDir.resolve("HelloWorldAssert.java").toFile().exists()
        assert otherPackageDir.resolve("BddAssertions.java").toFile().exists()
    }

    @Test
    void change_entry_point_package_and_entry_points() {
        TestUtils.buildFile(buildFile, """
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
        """)

        def runner = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withDebug(true)
                .withPluginClasspath()
                .withArguments('-i', '-s', 'test')

        def result = runner.build()

        assert result.task(':generateAssertJ').outcome == TaskOutcome.SUCCESS
        assert result.task(':test').outcome == TaskOutcome.SUCCESS

        Path generatedSrcDir = testProjectDir.root.toPath()
                .resolve("build/generated-src/main-test/java")

        Path generatedPackageDir = generatedSrcDir.resolve(packagePath)
        Path otherPackageDir = generatedSrcDir.resolve("org/other/")

        assertThat(generatedPackageDir.resolve("HelloWorldAssert.java")).exists()
        assertThat(otherPackageDir.resolve("BddAssertions.java")).exists()

        // Run it again, there may be issues with serialization
        def result2 = runner.build()

        assert result2.task(':generateAssertJ').outcome == TaskOutcome.UP_TO_DATE
        assert result2.task(':test').outcome == TaskOutcome.UP_TO_DATE

        assertThat(generatedPackageDir.resolve("HelloWorldAssert.java")).exists()
        assertThat(otherPackageDir.resolve("BddAssertions.java")).exists()
    }
}
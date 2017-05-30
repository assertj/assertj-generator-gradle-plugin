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

    File buildFile
    private Path srcPackagePath
    private Path packagePath

    @Before
    void setup() {
        buildFile = testProjectDir.newFile('build.gradle')

        File srcDir = testProjectDir.newFolder('src', 'main', 'java')

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
                .resolve("build/generated-src/test/java")
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
            assertJ {
                entryPoints = ['bdd']
            }
            
            sourceSets {
                main {
                    assertJ { }
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
                .resolve("build/generated-src/test/java")
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
            assertJ {
                entryPoints {
                    classPackage = 'org.other'
                    only 'bdd'
                }
            }
            
            sourceSets {
                main {
                    assertJ { }
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

        Path generatedSrcDir = testProjectDir.root.toPath()
                .resolve("build/generated-src/test/java")

        Path generatedPackageDir = generatedSrcDir.resolve(packagePath)
        Path otherPackageDir = generatedSrcDir.resolve("org/other/")

        assertThat(generatedPackageDir.resolve("HelloWorldAssert.java")).exists()
        assertThat(otherPackageDir.resolve("BddAssertions.java")).exists()
    }

}
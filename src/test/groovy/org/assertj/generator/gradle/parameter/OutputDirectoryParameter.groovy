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


import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import java.nio.file.Path
import java.nio.file.Paths

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import static org.assertj.generator.gradle.TestUtils.buildFile

/**
 * Checks the behaviour of overriding globals in a project
 */
class OutputDirectoryParameter {

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder()

    File buildFile
    private Path mainPackagePath
    private Path packagePath

    @Before
    void setup() {
        buildFile = testProjectDir.newFile('build.gradle')


        File srcDir = testProjectDir.newFolder('src')
        Path mainDir = srcDir.toPath().resolve('main/java')

        packagePath = Paths.get("org/example/")

        mainPackagePath = mainDir.resolve(packagePath)
        mainPackagePath.toFile().mkdirs()
        File mainJava = mainPackagePath.resolve('Main.java').toFile()

        mainJava << """
            |package org.example;
            |
            |public final class Main {
            |    // Field
            |    public boolean hasSomeBrains = false;
            |}""".stripMargin()
    }

    @Test
    void change_output_dir_locally() {
        buildFile(buildFile, """            
            sourceSets {
                main { 
                    assertJ {
                        // default: generated-srcs/\${SOURCE_SET_NAME_TAG}/java
                        outputDir = file('src-gen/foo-bar/java')
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

        assertFiles("main", "foo-bar", true)
    }

    private def assertFiles(String sourceSet, String folderName, boolean exists) {
        def generatedPackagePath = testProjectDir.root.toPath()
                .resolve("src-gen/${folderName}/java")
                .resolve(packagePath)

        def buildPath = testProjectDir.root.toPath().resolve("build")

        def path = generatedPackagePath.resolve("${sourceSet.capitalize()}Assert.java")

        assertThat(path.toFile().exists())
            .as("${sourceSet} file: ${buildPath.relativize(path)} exists")
            .isEqualTo(exists)
    }
}
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
package org.assertj.generator.gradle

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import java.nio.file.Path
import java.nio.file.Paths
import java.util.function.Consumer

import static org.assertj.core.api.Assertions.assertThat

/**
 * Checks the behaviour of overriding globals in a project
 */
class IncrementalBuild {

    // TODO Add more checks for incremental conditions and outputs
    // * Entry point files
    // * File deletion

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder()

    File buildFile
    private Path srcPackagePath
    private Path packagePath
    private File h1Java
    private File h2Java

    @Before
    void setup() {
        buildFile = testProjectDir.newFile('build.gradle')

        File srcDir = testProjectDir.newFolder('src', 'main', 'java')

        packagePath = Paths.get("org/example/")

        srcPackagePath = srcDir.toPath().resolve(this.packagePath)
        this.srcPackagePath.toFile().mkdirs()
        h1Java = this.srcPackagePath.resolve('H1.java').toFile()

        this.h1Java << """
            package org.example;
            
            public final class H1 {
                public boolean isBrainy = false;
            }
            """.stripIndent()

        h2Java = this.srcPackagePath.resolve('H2.java').toFile()

        this.h2Java << """
            package org.example;
            
            public final class H2 {
                public boolean isBrainy = false;
            }
            """.stripIndent()

        File testDir = testProjectDir.newFolder('src', 'test', 'java')

        testDir.toPath().resolve(this.packagePath).toFile().mkdirs()
        Path testPackagePath = testDir.toPath().resolve(this.packagePath)
        testPackagePath.toFile().mkdirs()
        File helloWorldTestJava = testPackagePath.resolve('H1Test.java').toFile()

        helloWorldTestJava << """
            package org.example;
            
            import org.junit.Test;
            import static org.example.H1Assert.assertThat;
            
            public final class H1Test {
                
                @Test
                public void check() {
                    H1 hw = new H1();
                    assertThat(hw).isNotBrainy();
                }
            }
            """.stripIndent()
    }


    @Test
    void incremental_build_does_not_rebuild_everything() {
        TestUtils.buildFile(buildFile, """                      
            sourceSets {
                main { assertJ {} }
            }
            """)

        def runner = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withDebug(true)
                .withPluginClasspath()
                .withArguments('-i', '-s', 'build')

        def firstBuild = runner.build()

        assert firstBuild.task(':generateAssertJ').outcome == TaskOutcome.SUCCESS
        assert firstBuild.task(':test').outcome == TaskOutcome.SUCCESS

        assertFiles()

        def secondBuild = runner.build()
        assert secondBuild.task(':generateAssertJ').outcome == TaskOutcome.UP_TO_DATE
        assert secondBuild.task(':test').outcome == TaskOutcome.UP_TO_DATE

        assertFiles()
    }

    @Test
    void incremental_build_rebuild_changed_single_file_contents() {
        TestUtils.buildFile(buildFile, """                      
            sourceSets {
                main { assertJ {} }
            }
            """)

        def runner = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withDebug(true)
                .withPluginClasspath()
                .withArguments('-i', '-s', 'build')

        def firstBuild = runner.build()

        assert firstBuild.task(':generateAssertJ').outcome == TaskOutcome.SUCCESS
        assert firstBuild.task(':test').outcome == TaskOutcome.SUCCESS

        // get files
        assertFiles()

        // modify the contents of H1 that doesn't change the compiled `.class`
        h1Java << "\n// some changed data"

        def secondBuild = runner.build()
        // Make sure it did run
        assert secondBuild.task(':generateAssertJ').outcome == TaskOutcome.UP_TO_DATE

        // However, since we didn't actually change anything, there's nothing to test, thus UP_TO_DATE
        assert secondBuild.task(':test').outcome == TaskOutcome.UP_TO_DATE

        assertFiles()
    }

    private def assertFiles(String sourceSet = "main") {
        final Path generatedPackagePath = testProjectDir.root.toPath()
                .resolve("build/generated-src/${sourceSet}-test/java")
                .resolve(packagePath)

        List<Path> files = ["H1", "H2"].collect {
            generatedPackagePath.resolve("${it}Assert.java")
        }

        assertThat(files).allSatisfy({ file ->
            assertThat(file).exists()
        } as Consumer<Path>)
    }
}
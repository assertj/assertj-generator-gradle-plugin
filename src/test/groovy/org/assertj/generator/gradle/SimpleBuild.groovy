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
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import java.nio.file.Path
import java.nio.file.Paths

/**
 * Checks the behaviour of overriding globals in a project
 */
class SimpleBuild {

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder()

    File buildFile

    @Before
    void setup() {
        buildFile = testProjectDir.newFile('build.gradle')

        File srcDir = testProjectDir.newFolder('src', 'main', 'java')

        Path packagePath = Paths.get("org/example/")

        Path srcPackagePath = srcDir.toPath().resolve(packagePath)
        srcPackagePath.toFile().mkdirs()
        File helloWorldJava = srcPackagePath.resolve('HelloWorld.java').toFile()

        helloWorldJava << """
            package org.example;
            
            public final class HelloWorld {
                
                // Field
                public boolean hasSomeBrains = false;
                
                // Getter
                public int getFoo() {
                    return -1;
                }
            }
            """.stripIndent()

        File otherWorldJava = srcPackagePath.resolve('OtherWorld.java').toFile()

        otherWorldJava << """
            package org.example;
            
            public final class OtherWorld {
                public boolean isBrainy = false;
            }
            """.stripIndent()

        File testDir = testProjectDir.newFolder('src', 'test', 'java')

        testDir.toPath().resolve(packagePath).toFile().mkdirs()
        Path testPackagePath = testDir.toPath().resolve(packagePath)
        testPackagePath.toFile().mkdirs()
        File helloWorldTestJava = testPackagePath.resolve('HelloWorldTest.java').toFile()

        helloWorldTestJava << """
            package org.example;
            
            import org.junit.Test;
            import static org.example.HelloWorldAssert.assertThat;
            
            public final class HelloWorldTest {
                
                @Test
                public void check() {
                    HelloWorld hw = new HelloWorld();
                    assertThat(hw).hasFoo(-1)
                                  .doesNotHaveSomeBrains();
                }
            }
            """
    }


    @Test
    void for_single_class() {

        buildFile << """
            // Add required plugins and source sets to the sub projects
            plugins { id "org.assertj.generator" } // Note must use this syntax

            // Override defaults
            sourceSets {
                main {
                    assertJ {
                          
                    }
                }
            }
            
            // add some classpath dependencies
            repositories {
                mavenCentral()
            }
                        
            dependencies { 
                implementation group: 'javax.annotation', name: 'javax.annotation-api', version: '1.3.2'
                
                implementation group: 'com.google.guava', name: 'guava', version: '28.1-jre'
                
                testImplementation group: 'org.assertj', name: 'assertj-core', version: '3.24.2'
                testImplementation group: 'junit', name: 'junit', version: '4.13.1'
            }
        """

        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withDebug(true)
                .withPluginClasspath()
                .withArguments('-i', '-s', 'test')
                .build()

        assert result.task(':generateAssertJ').outcome == TaskOutcome.SUCCESS
        assert result.task(':test').outcome == TaskOutcome.SUCCESS

    }

    @Test
    @Ignore("Test fails due to groovy incompatibility")
    void exclude_class() {

        TestUtils.buildFile(buildFile, """
            sourceSets {
                main {
                    assertJ {
                        source.exclude '**/org/example/OtherWorld*'
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
        assert result.task(':compileTestJava').outcome == TaskOutcome.SUCCESS
    }


}
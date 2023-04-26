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


import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import java.nio.file.Path
import java.nio.file.Paths
import java.util.function.Supplier

import static org.assertj.core.api.Assertions.assertThat

/**
 * Checks that we can include/exclude classes via the `classes` filter.
 */
class ClassesFilter {

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder()

    File buildFile
    File helloWorldTestJava
    Path generatedBasePackagePath

    @Before
    void setup() {
        buildFile = testProjectDir.newFile('build.gradle')

        File srcDir = testProjectDir.newFolder('src', 'main', 'java')

        Path packagePath = Paths.get("org/example/")

        Path srcPackagePath = srcDir.toPath().resolve(packagePath)
        srcPackagePath.toFile().mkdirs()
        File helloWorldJava = srcPackagePath.resolve('HelloWorld.java').toFile()

        helloWorldJava << """
            package org.example.hello;
            
            public final class HelloWorld {
                
                // Field
                public boolean hasSomeBrains = false;
                
                // Getter
                public int getFoo() {
                    return -1;
                }
            }
            """.stripIndent()

        File subHelloWorldJava = srcPackagePath.resolve('sub/SubHelloWorld.java').toFile()
        subHelloWorldJava.parentFile.mkdirs()

        subHelloWorldJava << """
            package org.example.hello.sub;
            
            public final class SubHelloWorld {
                // Getter
                public int getFoo() {
                    return -1;
                }
            }
            """.stripIndent()

        File otherWorldJava = srcPackagePath.resolve('OtherWorld.java').toFile()

        otherWorldJava << """
            package org.example.other;
            
            public final class OtherWorld {
                public boolean isBrainy = false;
            }
            """.stripIndent()

        File testDir = testProjectDir.newFolder('src', 'test', 'java')

        testDir.toPath().resolve(packagePath).toFile().mkdirs()
        def testPackagePath = testDir.toPath().resolve(packagePath)
        testPackagePath.toFile().mkdirs()

        helloWorldTestJava = testPackagePath.resolve('HelloWorldTest.java').toFile()

        generatedBasePackagePath = testProjectDir.root.toPath()
                .resolve("build/generated-src/main-test/java")
                .resolve(packagePath)
    }

    @Test
    void include_class_simple() {
        buildFile {
            """
            assertJ {
                entryPoints {
                    classPackage = "org.example"
                }
                classes {
                    include "org.example.hello.HelloWorld"
                }
            }
            """.stripIndent()
        }

        setupTestHelloWorld()

        runAndAssertBuild()

        assertThat(generatedBasePackagePath.resolve("hello")).exists()
        assertThat(generatedBasePackagePath.resolve("other")).doesNotExist()
        assertThat(generatedBasePackagePath.resolve("hello/sub")).doesNotExist()
    }

    @Test
    void include_class_pattern() {
        buildFile {
            """
            assertJ {
                entryPoints {
                    classPackage = "org.example"
                }
                classes {
                    include "org.example.hello.Hello*"
                }
            }
            """.stripIndent()
        }

        setupTestHelloWorld()

        runAndAssertBuild()

        assertThat(generatedBasePackagePath.resolve("hello")).exists()
        assertThat(generatedBasePackagePath.resolve("other")).doesNotExist()
        assertThat(generatedBasePackagePath.resolve("hello/sub")).doesNotExist()
    }

    @Test
    void include_class_that_does_not_exist_and_valid() {
        buildFile {
            """
            assertJ {
                entryPoints {
                    classPackage = "org.example"
                }
                classes {
                    include "org.example.hello.*", "org.example.does_not_exist"
                }
            }
            """.stripIndent()
        }

        setupTestHelloWorld()

        runAndAssertBuild()

        assertThat(generatedBasePackagePath.resolve("hello")).exists()
        assertThat(generatedBasePackagePath.resolve("other")).doesNotExist()
        assertThat(generatedBasePackagePath.resolve("hello/sub")).doesNotExist()
    }

    @Test
    void include_class_double_wildcard() {
        buildFile {
            """
            assertJ {
                entryPoints {
                    classPackage = "org.example"
                }
                classes {
                    include "org.example.hello**"
                }
            }
            """.stripIndent()
        }

        setupTestHelloAndSub()

        runAndAssertBuild()

        assertThat(generatedBasePackagePath.resolve("other")).doesNotExist()
        assertThat(generatedBasePackagePath.resolve("hello")).exists()
        assertThat(generatedBasePackagePath.resolve("hello/sub")).exists()
    }

    @Test
    void exclude_class_simple() {
        buildFile {
            """
            assertJ {
                entryPoints {
                    classPackage = "org.example"
                }
                classes {
                    exclude "org.example.other.OtherWorld"
                }
            }
            """.stripIndent()
        }

        setupTestHelloAndSub()

        runAndAssertBuild()

        assertThat(generatedBasePackagePath.resolve("hello")).exists()
        assertThat(generatedBasePackagePath.resolve("hello/sub")).exists()
        assertThat(generatedBasePackagePath.resolve("other")).doesNotExist()
    }

    @Test
    void exclude_class_pattern() {
        buildFile {
            """
            assertJ {
                entryPoints {
                    classPackage = "org.example"
                }
                classes {
                    exclude "org.example.other.*"
                }
            }
            """.stripIndent()
        }

        setupTestHelloAndSub()

        runAndAssertBuild()

        assertThat(generatedBasePackagePath.resolve("other")).doesNotExist()
        assertThat(generatedBasePackagePath.resolve("hello")).exists()
        assertThat(generatedBasePackagePath.resolve("hello/sub")).exists()
    }

    @Test
    void exclude_class_that_does_not_exist_and_valid() {
        buildFile {
            """
            assertJ {
                entryPoints {
                    classPackage = "org.example"
                }
                classes {
                    exclude "org.example.other.*", "org.example.does_not_exist"
                }
            }
            """.stripIndent()
        }

        testFile {
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
            """.stripIndent()
        }

        runAndAssertBuild()

        assertThat(generatedBasePackagePath.resolve("other")).doesNotExist()
        assertThat(generatedBasePackagePath.resolve("hello")).exists()
        assertThat(generatedBasePackagePath.resolve("hello/sub")).exists()
    }

    @Test
    void exclude_class_double_wildcard() {
        buildFile {
            """
            assertJ {
                entryPoints {
                    classPackage = "org.example"
                }
                classes {
                    exclude "org.example.**"
                }
            }
            """.stripIndent()
        }

        // Since we are excluding _everything_ we need to add a custom test
        helloWorldTestJava << """
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
            """.stripIndent()

        runAndAssertBuild()

        assertThat(generatedBasePackagePath.resolve("other")).doesNotExist()
        assertThat(generatedBasePackagePath.resolve("hello")).doesNotExist()
    }

    @Test
    void include_double_wildcard_but_exclude_specific_class() {
        buildFile {
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
            """.stripIndent()
        }

        setupTestHelloWorld()

        runAndAssertBuild()

        assertThat(generatedBasePackagePath.resolve("other")).doesNotExist()
        assertThat(generatedBasePackagePath.resolve("hello")).exists()
        assertThat(generatedBasePackagePath.resolve("hello/sub")).doesNotExist()
    }

    private File setupTestHelloWorld() {
        testFile {
            """
            @Test
            public void checkHello() {
                HelloWorld hw = new HelloWorld();
                assertThat(hw).doesNotHaveSomeBrains();
            }
            """.stripIndent()
        }
    }

    private void runAndAssertBuild() {
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withDebug(true)
                .withPluginClasspath()
                .withArguments('-i', '-s', 'test')
                .build()

        assert result.task(':generateAssertJ').outcome == TaskOutcome.SUCCESS
        assert result.task(':test').outcome == TaskOutcome.SUCCESS
    }

    private def buildFile(Supplier<String> configuration) {
        buildFile << """
            // Add required plugins and source sets to the sub projects
            plugins {
              id "org.assertj.generator" // Note must use this syntax
              id "java" 
            } 

            // Override defaults
            sourceSets {
                main {
                    ${configuration.get()}
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
        """.stripIndent()
    }

    private def testFile(Supplier<String> testContent) {
        helloWorldTestJava << """
            package org.example;
            
            import org.example.hello.*;
            import org.example.hello.sub.*;
            import org.example.other.*;
            import org.junit.Test;
            import org.assertj.core.api.Assertions;
            import static org.example.Assertions.assertThat;
            
            public final class HelloWorldTest {
                ${testContent.get()}
            }
            """.stripIndent()
    }

    private File setupTestHelloAndSub() {
        testFile {
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
            """.stripIndent()
        }
    }
}
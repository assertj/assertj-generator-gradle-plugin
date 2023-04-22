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
class TemplateChanges {

    final static TEMPLATE_CONTENT = "/* %%% \${property} %%% */"

    @Rule
    public final TemporaryFolder testProjectDir = new TemporaryFolder()

    File buildFile
    private Path srcPackagePath
    private Path packagePath
    private Path resolvedPackagePath

    @Before
    void setup() {
        buildFile = testProjectDir.newFile('build.gradle')

        File srcDir = testProjectDir.newFolder('src', 'main', 'java')

        packagePath = Paths.get("org/example/")

        srcPackagePath = srcDir.toPath().resolve(packagePath)
        srcPackagePath.toFile().mkdirs()
        File helloWorldJava = srcPackagePath.resolve('HelloWorld.java').toFile()

        helloWorldJava << """
            package org.example;
            
            public final class HelloWorld {
                // Getter
                public int getFoo() {
                    return -1;
                }
            }
            """.stripIndent()

        resolvedPackagePath = testProjectDir.root.toPath()
                .resolve("build/generated-src/main-test/java")
                .resolve(packagePath)
    }


    @Test
    void change_default_template_from_sourceSet() {

        TestUtils.buildFile(buildFile, """
            sourceSets {
                main {
                    assertJ {
                        templates {
                            methods.wholeNumberPrimitive = '${TEMPLATE_CONTENT}'
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

        Path generatedAssert = resolvedPackagePath.resolve("HelloWorldAssert.java")

        assertThat(generatedAssert.text).contains('/* %%% foo %%% */')

    }

    @Test
    void change_default_template_from_global() {

        TestUtils.buildFile(buildFile, """
            sourceSets {
                main {
                    assertJ { 
                        templates {
                           methods.wholeNumberPrimitive = '${TEMPLATE_CONTENT}'
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

        Path generatedAssert = resolvedPackagePath.resolve("HelloWorldAssert.java")

        assertThat(generatedAssert.text).contains('/* %%% foo %%% */')
    }

    @Test
    void incremental_templates_with_no_changes() {

        TestUtils.buildFile(buildFile, """            
            sourceSets {
                main {
                    assertJ {
                        templates {
                            methods.wholeNumberPrimitive = '${TEMPLATE_CONTENT}'
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

        Path generatedAssert = resolvedPackagePath.resolve("HelloWorldAssert.java")

        assertThat(generatedAssert.text).contains('/* %%% foo %%% */')

        def result2 = runner.build()

        assert result2.task(':generateAssertJ').outcome == TaskOutcome.UP_TO_DATE
        assert result2.task(':test').outcome == TaskOutcome.UP_TO_DATE

        assertThat(generatedAssert.text).contains('/* %%% foo %%% */')
    }

    @Test
    void incremental_templates_after_string_change() {

        TestUtils.buildFile(buildFile, """
            sourceSets {
                main {
                    assertJ { 
                        templates {
                            methods { wholeNumberPrimitive = '${TEMPLATE_CONTENT}' }
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

        Path generatedAssert = resolvedPackagePath.resolve("HelloWorldAssert.java")

        assertThat(generatedAssert).content().contains('/* %%% foo %%% */')

        // Now we update the content of the template assertion to make sure that the task is re-run

        def NEW_TEMPLATE_CONTENT = '/* % NEW CONTENT % */'
        buildFile.delete()
        TestUtils.buildFile(buildFile, """
            sourceSets {
                main {
                    assertJ { 
                        templates {
                            methods {
                                wholeNumberPrimitive = '${NEW_TEMPLATE_CONTENT}'
                            }
                        }
                    }
                }
            }
        """)

        def result2 = runner.build()

        assert result2.task(':generateAssertJ').outcome == TaskOutcome.SUCCESS
        assert result2.task(':test').outcome == TaskOutcome.UP_TO_DATE // no test files!

        assertThat(generatedAssert).content().contains(NEW_TEMPLATE_CONTENT)
    }

    @Test
    void incremental_templates_change_type() {
        TestUtils.buildFile(buildFile, """
            sourceSets {
                main {
                    assertJ { 
                        templates {
                            methods { wholeNumberPrimitive = '${TEMPLATE_CONTENT}' }
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

        Path generatedAssert = resolvedPackagePath.resolve("HelloWorldAssert.java")

        assertThat(generatedAssert.text).contains('/* %%% foo %%% */')

        // Now we update the content of the template assertion to make sure that the task is re-run

        def NEW_TEMPLATE_CONTENT = '/* % NEW CONTENT % */'

        File templateFolder = testProjectDir.newFolder('templates')
        File content = templateFolder.toPath().resolve("template.txt").toFile()
        content << NEW_TEMPLATE_CONTENT

        def contentPath = testProjectDir.root.toPath().relativize(content.toPath()).toString().replace('\\', '/')

        buildFile.delete()
        TestUtils.buildFile(buildFile, """
            sourceSets {
                main {
                    assertJ {
                        templates {
                            methods { wholeNumberPrimitive = file('${contentPath}') }
                        }  
                    }
                }
            }
        """)

        def result2 = runner.build()

        assert result2.task(':generateAssertJ').outcome == TaskOutcome.SUCCESS
        assert result2.task(':test').outcome == TaskOutcome.UP_TO_DATE // no test files!

        assertThat(generatedAssert.text).contains(NEW_TEMPLATE_CONTENT)
    }

    @Test
    void incremental_templates_after_file_change() {

        File templateFolder = testProjectDir.newFolder('templates')
        File content = templateFolder.toPath().resolve("template.txt").toFile()
        content << TEMPLATE_CONTENT

        def contentPath = testProjectDir.root.toPath().relativize(content.toPath()).toString().replace('\\', '/')

        TestUtils.buildFile(buildFile, """
            sourceSets {
                main {
                     assertJ {
                        templates {
                            methods { wholeNumberPrimitive = file("${contentPath}") }
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

        Path generatedAssert = resolvedPackagePath.resolve("HelloWorldAssert.java")

        assertThat(generatedAssert.text).contains('/* %%% foo %%% */')

        // Now we update the content of the template assertion to make sure that the task is re-run

        def NEW_TEMPLATE_CONTENT = '/* % NEW CONTENT % */'
        content.delete()
        content << NEW_TEMPLATE_CONTENT

        def result2 = runner.build()

        assert result2.task(':generateAssertJ').outcome == TaskOutcome.SUCCESS
        assert result2.task(':test').outcome == TaskOutcome.UP_TO_DATE // no test files!

        assertThat(generatedAssert.text).contains(NEW_TEMPLATE_CONTENT)
    }

    @Test
    void bad_template_name_should_fail() {

        TestUtils.buildFile(buildFile, """
            sourceSets {
                main {
                    assertJ {
                        templates {
                            methods { wholeNumberAssertion = '${TEMPLATE_CONTENT}' }
                        }   
                    }
                }
            }
        """)

        def result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withDebug(true)
                .withPluginClasspath()
                .withArguments('-i', '-s', 'generateAssertJ')
                .buildAndFail()

        assert result.output.contains("wholeNumberAssertion")
    }

}
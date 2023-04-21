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
package org.assertj.generator.gradle.tasks.config

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.assertj.assertions.generator.AssertionsEntryPointType
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.SourceSet
import org.gradle.util.ConfigureUtil

import javax.inject.Inject

/**
 * Defines useful parameters and options for configuration of the
 * {@link org.assertj.assertions.generator.AssertionGenerator}.
 */
@EqualsAndHashCode
@ToString
class AssertJGeneratorExtension {
    /**
     * Generate generating Soft Assertions entry point class.
     * @return templates value, never {@code null}
     */
    final Templates templates

    /**
     * Method used for improving configuration DSL
     *
     * @param closure applied via {@link org.gradle.util.ConfigureUtil#configure(Closure, Object)}
     * @return {@code this}
     */
    AssertJGeneratorExtension templates(Closure closure) {
        ConfigureUtil.configure(closure, getTemplates());
        return this;
    }

    /**
     * Skip generating classes, handy way to disable temporarily.
     * @return true if skipping this set
     */
    boolean skip = false

    /**
     * Flag specifying whether to generate hierarchical assertions. The default is false.
     * @return true if generating hierarchical
     */
    boolean hierarchical = false

    /**
     * Contains configuration regarding the "Assertion" entry point class generation. By default, only the "standard"
     * version is generated.
     * <br>
     * See the
     * <a href="http://joel-costigliola.github.io/assertj/assertj-assertions-generator.html#generated-entry-points">assertj generator docs</a>
     * on entry points.
     *
     * @return entry points configuration
     */
    final EntryPointGeneratorOptions entryPoints

    /**
     * Helper method for simplifying usage in build scripts
     * @param values Values to set
     */
    void setEntryPoints(AssertionsEntryPointType... rest) {
        this.entryPoints.only(rest)
    }

    /**
     * Exposed for build scripts
     * @param values String values passed from a build script
     */
    def setEntryPoints(Collection<String> values) {
        getEntryPoints().only(
                values.stream()
                        .map(String::toUpperCase)
                        .map(AssertionsEntryPointType::valueOf)
                        .toList() as AssertionsEntryPointType[]
        )
    }

    /**
     * Used to change "entry point" class generation.
     *
     * @param closure Applied via {@link org.gradle.util.ConfigureUtil#configure(Closure, Object)}
     * @return this
     */
    AssertJGeneratorExtension entryPoints(Closure closure) {
        ConfigureUtil.configure(closure, entryPoints)
        this
    }

    /**
     * The root directory where all generated files will be placed (within sub-folders for packages).
     * @return File the output directory for {@code sourceSet}
     */
    final DirectoryProperty outputDir

    /**
     * Sets the output directory to the path passed.
     *
     * @param outputDir File used for an output directory
     */
    void setOutputDir(String outputDir) {
        this.outputDir.set(project.file(outputDir))
    }

    private final Project project

    @Inject
    AssertJGeneratorExtension(ObjectFactory objects, Project project, SourceSet sourceSet) {
        this.project = project

        this.outputDir = objects.directoryProperty()
                .convention(project.layout.buildDirectory.dir("generated-src/${sourceSet.name}-test/java"))

        templates = objects.newInstance(Templates)
        entryPoints = objects.newInstance(EntryPointGeneratorOptions)
    }
}

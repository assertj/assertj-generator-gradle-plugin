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
package org.assertj.generator.gradle.internal.tasks.config

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.assertj.assertions.generator.AssertionsEntryPointType
import org.assertj.generator.gradle.tasks.config.AssertJGeneratorOptions
import org.assertj.generator.gradle.tasks.config.EntryPointGeneratorOptions
import org.assertj.generator.gradle.tasks.config.Templates
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.SourceSet
import org.gradle.util.ConfigureUtil

import javax.inject.Inject

/**
 * Simple, default implementation of {@link AssertJGeneratorOptions}
 */
@EqualsAndHashCode
@ToString
class DefaultAssertJGeneratorOptions implements AssertJGeneratorOptions, Serializable {

    boolean skip
    Boolean hierarchical
    private Templates templates
    protected EntryPointGeneratorOptions _entryPoints

    final DirectoryProperty outputDir

    @Inject
    DefaultAssertJGeneratorOptions(ObjectFactory objects, Project project, SourceSet sourceSet) {
        this.outputDir = objects.directoryProperty()
                .convention(
                        project.layout.buildDirectory.dir("generated-src/${sourceSet.name}-test/java"),
                )

        skip = false
        hierarchical = null
        templates = objects.newInstance(Templates)

        // default entry points
        this._entryPoints = objects.newInstance(EntryPointGeneratorOptions)
        this._entryPoints.only(AssertionsEntryPointType.STANDARD)
    }

    @Override
    Templates getTemplates() {
        this.templates
    }

    @Override
    void setHierarchical(boolean hierarchical) {
        this.hierarchical = hierarchical
    }

    @Override
    Boolean isHierarchical() {
        this.hierarchical
    }

    void setEntryPoints(AssertionsEntryPointType... rest) {
        this.entryPoints.only(rest)
    }

    void setEntryPoints(EntryPointGeneratorOptions newValue) {
        this._entryPoints = newValue
    }

    @Override
    EntryPointGeneratorOptions getEntryPoints() {
        this._entryPoints
    }

    protected EntryPointGeneratorOptions getOrCreateEntryPoints() {
        if (!this.entryPoints) {
            this.entryPoints = new EntryPointGeneratorOptions()
        }

        this.entryPoints
    }

    @Override
    AssertJGeneratorOptions entryPoints(Closure closure) {
        ConfigureUtil.configure(closure, orCreateEntryPoints)
        this
    }
}

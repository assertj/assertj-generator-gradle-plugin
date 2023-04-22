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
import groovy.transform.PackageScope
import groovy.transform.ToString
import org.assertj.assertions.generator.AssertionsEntryPointType
import org.assertj.generator.gradle.tasks.config.AssertJGeneratorOptions
import org.assertj.generator.gradle.tasks.config.EntryPointGeneratorOptions
import org.assertj.generator.gradle.tasks.config.Templates
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.SourceSet
import org.gradle.util.ConfigureUtil

import javax.inject.Inject
import java.nio.file.Path
import java.nio.file.Paths

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

    protected String outputDir

    @Inject
    DefaultAssertJGeneratorOptions(ObjectFactory objects) {
        this.outputDir = "generated-src/${SOURCE_SET_NAME_TAG}-test/java"

        skip = true
        hierarchical = null
        templates = objects.newInstance(Templates)

        // default entry points
        this._entryPoints = objects.newInstance(EntryPointGeneratorOptions)
        this._entryPoints.only(AssertionsEntryPointType.STANDARD)
    }

    Path getOutputDir(SourceSet sourceSet) {
        if (!outputDir) return null

        def path = this.outputDir.replace(SOURCE_SET_NAME_TAG, sourceSet.name)

        Paths.get(path)
    }

    @Override
    void setOutputDir(File file) {
        setOutputDir(file.toString())
    }

    @Override
    void setOutputDir(String outputDir) {
        this.outputDir = outputDir
    }

    @PackageScope
    String getOutputDirRaw() {
        this.outputDir
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

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.writeObject(outputDir)
        s.writeBoolean(skip)
        s.writeObject(_entryPoints)
        s.writeObject(templates)
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        this.outputDir = (String) s.readObject()
        this.skip = s.readBoolean()
        this._entryPoints = (EntryPointGeneratorOptions) s.readObject()

        Templates templatesFromIn = (Templates) s.readObject()
        if (templatesFromIn) {
            if (this.templates) {
                this.templates.copyFrom(templatesFromIn)
            } else {
                this.templates = templatesFromIn
            }
        }
    }
}

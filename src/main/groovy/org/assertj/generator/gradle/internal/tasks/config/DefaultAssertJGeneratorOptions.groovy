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
import org.assertj.generator.gradle.AssertJGeneratorGradlePlugin
import org.assertj.generator.gradle.tasks.config.AssertJGeneratorOptions
import org.assertj.generator.gradle.tasks.config.EntryPointGeneratorOptions
import org.assertj.generator.gradle.tasks.config.Templates
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.gradle.util.ConfigureUtil

import java.nio.file.Path
import java.nio.file.Paths

/**
 * Simple, default implementation of {@link AssertJGeneratorOptions}
 */
@EqualsAndHashCode @ToString
class DefaultAssertJGeneratorOptions implements AssertJGeneratorOptions, Serializable {

    boolean skip
    Boolean hierarchical = null
    final Templates templates = new Templates()
    String entryPointClassPackage
    protected EntryPointGeneratorOptions _entryPoints

    protected String outputDir

    DefaultAssertJGeneratorOptions() {
        this.skip = true
    }

    Path getOutputDir(SourceSet sourceSet) {
        if (!outputDir) return null

        def path
        if (sourceSet.name.contains("test")) {
            path = this.outputDir.replace(SOURCE_SET_NAME_TAG, sourceSet.name)
        } else {
            path = this.outputDir.replace(SOURCE_SET_NAME_TAG, sourceSet.getTaskName('test', ''))
        }

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
    void setHierarchical(boolean hierarchical) {
        this.hierarchical = hierarchical
    }

    @Override
    Boolean isHierarchical() {
        this.hierarchical
    }

    void setEntryPoints(AssertionsEntryPointType... rest) {
        this.entryPoints.setOnly(rest)
    }

    void setEntryPoints(EntryPointGeneratorOptions newValue) {
        this.entryPoints = newValue
    }

    @Override
    EntryPointGeneratorOptions getEntryPoints() {
        this._entryPoints
    }

    @Override
    AssertJGeneratorOptions entryPoints(Closure closure) {
        ConfigureUtil.configure(closure, _entryPoints)
        this
    }

    @Override
    AssertJGeneratorOptions defaultFromGlobals(Project project) {
        GlobalAssertJGeneratorOptions globalOpts =
                project.extensions.findByName(AssertJGeneratorGradlePlugin.ASSERTJ_GEN_CONFIGURATION_NAME) as GlobalAssertJGeneratorOptions

        if (!this.getOutputDirRaw()) {
            this.outputDir = globalOpts.outputDir
        }

        if (!this.entryPoints) {
            this.entryPoints = globalOpts.entryPoints
        }

        templates.defaults(globalOpts.templates)
        this
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.writeObject(outputDir)
        s.writeObject(entryPointClassPackage)
        s.writeBoolean(skip)
        s.writeObject(_entryPoints)
        s.writeObject(templates)
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        this.outputDir = (File)s.readObject()
        this.entryPointClassPackage = (String)s.readObject()
        this.skip = s.readBoolean()
        this._entryPoints = (EntryPointGeneratorOptions)s.readObject()
        templates.copyFrom((Templates)s.readObject())
    }
}

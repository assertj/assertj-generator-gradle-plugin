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
package org.assertj.generator.gradle.internal.tasks

import groovy.transform.EqualsAndHashCode
import org.assertj.generator.gradle.internal.tasks.config.DefaultAssertJGeneratorOptions
import org.assertj.generator.gradle.tasks.AssertJGeneratorSourceSet
import org.assertj.generator.gradle.tasks.config.AssertJGeneratorOptions
import org.assertj.generator.gradle.tasks.config.EntryPointGeneratorOptions
import org.assertj.generator.gradle.tasks.config.Templates
import org.gradle.api.Action
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.SourceSet
import org.gradle.util.ConfigureUtil

import javax.inject.Inject

/**
 * Simple, default implementation of {@link AssertJGeneratorSourceSet}
 */
@EqualsAndHashCode
class DefaultAssertJGeneratorSourceSet extends DefaultAssertJGeneratorOptions implements AssertJGeneratorSourceSet, Serializable {

    final String name
    boolean skip
    String entryPointClassPackage
    private File outputDir

    private final SourceDirectorySet assertJDirectorySet

    @Inject
    DefaultAssertJGeneratorSourceSet(ObjectFactory objectFactory, SourceSet sourceSet) {
        super(objectFactory)
        this.name = sourceSet.name
        this.assertJDirectorySet = objectFactory.sourceDirectorySet(
                "$sourceSet AssertJ Sources",
                sourceSet.name,
        )

        // We default to the java directory
        assertJ.setSrcDirs(["src/${this.name}/java"])
        // by default we can include all because the closure is applied to _every_ source set
        assertJ.include "**/*.java"
    }

    @Override
    SourceDirectorySet getAssertJ() {
        assertJDirectorySet
    }

    @Override
    AssertJGeneratorSourceSet assertJ(Closure configureClosure) {
        // turn on the plugin
        this.skip = false

        ConfigureUtil.configure(configureClosure, assertJ)
        this
    }

    @Override
    AssertJGeneratorSourceSet assertJ(Action<? super SourceDirectorySet> action) {
        // turn on for this source set
        this.skip = false

        action.execute(assertJ)
        this
    }

    @Override
    AssertJGeneratorOptions entryPoints(Closure closure) {
        ConfigureUtil.configure(closure, orCreateEntryPoints)
        this
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.writeObject(outputDir)
        s.writeObject(entryPointClassPackage)
        s.writeBoolean(skip)
        s.writeObject(entryPoints)
        s.writeObject(templates)
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        this.outputDir = (File) s.readObject()
        this.entryPointClassPackage = (String) s.readObject()
        this.skip = s.readBoolean()
        this.entryPoints = (EntryPointGeneratorOptions) s.readObject()
        templates.copyFrom((Templates) s.readObject())
    }
}

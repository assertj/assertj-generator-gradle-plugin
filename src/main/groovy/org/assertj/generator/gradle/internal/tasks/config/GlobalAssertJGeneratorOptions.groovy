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
import org.gradle.api.Project

/**
 * Simple, default implementation of {@link AssertJGeneratorOptions}
 */
@EqualsAndHashCode @ToString
class GlobalAssertJGeneratorOptions extends DefaultAssertJGeneratorOptions implements Serializable {

    GlobalAssertJGeneratorOptions() {
        this.outputDir = "generated-src/${SOURCE_SET_NAME_TAG}/java"

        // default entry points
        this._entryPoints = new EntryPointGeneratorOptions()
        this._entryPoints.only(AssertionsEntryPointType.STANDARD)
    }

    @Override
    GlobalAssertJGeneratorOptions defaultFromGlobals(Project project) {
        // no work to do, we are the global :)
        this
    }

    @PackageScope
    String getOutputDirRaw() {
        this.outputDirRaw
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.writeUTF(outputDirRaw)
        s.writeBoolean(skip)
        s.writeObject(this._entryPoints)
        s.writeObject(templates)
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        this.outputDir = s.readUTF()
        this.skip = s.readBoolean()
        this._entryPoints = (EntryPointGeneratorOptions)s.readObject()
        templates.copyFrom((Templates)s.readObject())
    }
}

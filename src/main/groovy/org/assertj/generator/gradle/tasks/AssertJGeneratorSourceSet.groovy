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
package org.assertj.generator.gradle.tasks

import org.gradle.api.Action
import org.gradle.api.file.SourceDirectorySet

/**
 * Source Set implementation used to allow definition within the JavaPlugin's
 * Source sets. This does not extend {@link org.gradle.api.tasks.SourceSet}.
 */
interface AssertJGeneratorSourceSet extends GroovyObject {

    String NAME = "assertJ"

    /**
     * The name of this source set (e.g. main or test)
     */
    String getName()

    /**
     * Returns the source set that will be read by AssertJ
     * @return AssertJ source set, never {@code null}
     */
    SourceDirectorySet getAssertJ()

    /**
     * Returns the source that will be compiled by Umple and configures using the closure
     * @param configureClosure closure for configuration
     *
     * @return Umple source, never {@code null}
     */
    AssertJGeneratorSourceSet assertJ(Closure configureClosure)

    AssertJGeneratorSourceSet assertJ(Action<? super SourceDirectorySet> action)
}

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
package org.assertj.generator.gradle.tasks.config

import org.assertj.assertions.generator.AssertionsEntryPointType
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.SourceSet
import org.gradle.kotlin.dsl.newInstance
import javax.inject.Inject

/**
 * Defines useful parameters and options for configuration of the
 * {@link org.assertj.assertions.generator.AssertionGenerator}.
 */
open class AssertJGeneratorExtension @Inject internal constructor(
  objects: ObjectFactory,
  project: Project,
  sourceSet: SourceSet
) {
  /**
   * Generate generating Soft Assertions entry point class.
   * @return templates value, never {@code null}
   */
  val templates: Templates = objects.newInstance()

  /**
   * Method used for improving configuration DSL
   * @return {@code this}
   */
  fun templates(action: Action<in Templates>): AssertJGeneratorExtension {
    action.execute(templates)
    return this
  }

  /**
   * Contains configuration regarding the "Assertion" entry point class generation. By default, only the "standard"
   * version is generated.
   *
   * See the
   * [assertj generator docs](http://joel-costigliola.github.io/assertj/assertj-assertions-generator.html#generated-entry-points)
   * on entry points.
   *
   * @return entry points configuration
   */
  @Suppress("MaxLineLength")
  val entryPoints: EntryPointGeneratorOptions = objects.newInstance()

  /**
   * Helper method for simplifying usage in build scripts
   * @param rest Values to set
   */
  fun setEntryPoints(vararg rest: AssertionsEntryPointType) {
    this.entryPoints.only(rest = rest)
  }

  /**
   * Exposed for build scripts
   * @param values String values passed from a build script
   */
  fun setEntryPoints(values: Collection<String>) {
    entryPoints.only(rest = values.toTypedArray())
  }

  /**
   * Used to change "entry point" class generation.
   * @return this
   */
  fun entryPoints(action: Action<in EntryPointGeneratorOptions>): AssertJGeneratorExtension {
    action.execute(entryPoints)
    return this
  }

  /**
   * The root directory where all generated files will be placed (within sub-folders for packages).
   * @return File the output directory for {@code sourceSet}
   */
  val outputDir: DirectoryProperty = objects.directoryProperty()
    .convention(project.layout.buildDirectory.dir("generated-src/${sourceSet.name}-test/java"))

  /**
   * Flag specifying whether to generate hierarchical assertions. The default is false.
   * @return true if generating hierarchical
   */
  var hierarchical = false

  /**
   * Skip generating classes, handy way to disable temporarily.
   * @return true if skipping this set
   */
  var skip = false
}

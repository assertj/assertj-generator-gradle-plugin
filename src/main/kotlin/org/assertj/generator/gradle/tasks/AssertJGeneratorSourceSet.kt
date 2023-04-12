package org.assertj.generator.gradle.tasks

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.file.SourceDirectorySet

/**
 * Source Set implementation used to allow definition within the JavaPlugin's
 * Source sets. This does not extend [SourceSet].
 */
interface AssertJGeneratorSourceSet {
  /**
   * The name of this source set (e.g. main or test)
   */
  val name: String

  /**
   * Returns the source set that will be read by AssertJ
   *
   * @return AssertJ source set, never `null`
   */
  val assertJ: SourceDirectorySet

  /**
   * Returns the source that will be compiled by Umple and configures using the closure
   *
   * @param configureClosure closure for configuration
   * @return Umple source, never `null`
   */
  fun assertJ(configureClosure: Closure<*>): AssertJGeneratorSourceSet
  fun assertJ(action: Action<in SourceDirectorySet>): AssertJGeneratorSourceSet

  companion object {
    const val NAME = "assertJ"
  }
}

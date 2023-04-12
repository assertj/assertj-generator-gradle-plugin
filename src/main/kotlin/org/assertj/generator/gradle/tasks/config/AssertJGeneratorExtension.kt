package org.assertj.generator.gradle.tasks.config

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.util.PatternSet
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

/**
 * Defines useful parameters and options for configuration of the generator.
 */
open class AssertJGeneratorExtension @Inject internal constructor(
  objects: ObjectFactory,
  project: Project,
  sourceSet: SourceSet,
) {
  /**
   * Directory to write generated files to
   */
  val outputDirectory: DirectoryProperty = objects.directoryProperty()
    .convention(project.layout.buildDirectory.map { it.dir("generated-src/${sourceSet.name}-test/java") })

  /**
   * Generate generating Soft Assertions entry point class.
   * @return templates value, never `null`
   */
  val templatesExtension: Property<TemplatesExtension> = objects.property<TemplatesExtension>().apply {
    set(objects.newInstance<TemplatesExtension>())
  }

  /**
   * Defines patterns for filtering source files for generation, defaults to allowing all files.
   *
   * This is passed to [org.assertj.generator.gradle.tasks.AssertJGenerationTask.source].
   */
  val source: PatternSet = objects.newInstance()

  fun templates(action: Action<in TemplatesExtension>): AssertJGeneratorExtension {
    action.execute(templatesExtension.get())
    return this
  }

  /**
   * Contains configuration regarding the "Assertion" entry point class generation. By default, only the "standard"
   * version is generated.
   * <br></br>
   * See the
   * [assertj generator docs](
     * http://joel-costigliola.github.io/assertj/assertj-assertions-generator.html#generated-entry-points)
   * on entry points.
   *
   * @return entry points configuration
   */
  @get:Input
  val entryPoints: Property<EntryPointGeneratorOptions> = objects.property<EntryPointGeneratorOptions>().apply {
    set(EntryPointGeneratorOptions())
  }

  /**
   * Used to change "entry point" class generation.
   * @return this
   */
  fun entryPoints(action: Action<in EntryPointGeneratorOptions>): AssertJGeneratorExtension {
    action.execute(entryPoints.get())
    return this
  }

  /**
   * Flag specifying whether to generate hierarchical assertions. The default is false.
   * @return true if generating hierarchical
   */
  @get:Input
  val hierarchical: Property<Boolean> = objects.property<Boolean>()
    .convention(false)

  /**
   * Skip generating classes, handy way to disable temporarily.
   * @return true if skipping this set
   */
  @get:Input
  val skip: Property<Boolean> = objects.property<Boolean>()
    .convention(false)
}

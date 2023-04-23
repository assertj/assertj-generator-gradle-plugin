package org.assertj.generator.gradle.tasks.config

import org.assertj.assertions.generator.Template
import org.assertj.assertions.generator.Template.Type.ABSTRACT_ASSERT_CLASS
import org.assertj.assertions.generator.Template.Type.ASSERTIONS_ENTRY_POINT_CLASS
import org.assertj.assertions.generator.Template.Type.ASSERTION_ENTRY_POINT
import org.assertj.assertions.generator.Template.Type.ASSERT_CLASS
import org.assertj.assertions.generator.Template.Type.BDD_ASSERTIONS_ENTRY_POINT_CLASS
import org.assertj.assertions.generator.Template.Type.BDD_ENTRY_POINT_METHOD_ASSERTION
import org.assertj.assertions.generator.Template.Type.HAS
import org.assertj.assertions.generator.Template.Type.HAS_FOR_ARRAY
import org.assertj.assertions.generator.Template.Type.HAS_FOR_CHAR
import org.assertj.assertions.generator.Template.Type.HAS_FOR_CHARACTER
import org.assertj.assertions.generator.Template.Type.HAS_FOR_ITERABLE
import org.assertj.assertions.generator.Template.Type.HAS_FOR_REAL_NUMBER
import org.assertj.assertions.generator.Template.Type.HAS_FOR_REAL_NUMBER_WRAPPER
import org.assertj.assertions.generator.Template.Type.HAS_FOR_WHOLE_NUMBER
import org.assertj.assertions.generator.Template.Type.HAS_FOR_WHOLE_NUMBER_WRAPPER
import org.assertj.assertions.generator.Template.Type.HIERARCHICAL_ASSERT_CLASS
import org.assertj.assertions.generator.Template.Type.IS
import org.assertj.assertions.generator.Template.Type.IS_WRAPPER
import org.assertj.assertions.generator.Template.Type.JUNIT_SOFT_ASSERTIONS_ENTRY_POINT_CLASS
import org.assertj.assertions.generator.Template.Type.SOFT_ASSERTIONS_ENTRY_POINT_CLASS
import org.assertj.assertions.generator.Template.Type.SOFT_ENTRY_POINT_METHOD_ASSERTION
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

/**
 * Configuration for templates that work with the generator
 */
open class Templates @Inject internal constructor(objects: ObjectFactory) {

  /**
   * Class-level templates.
   * @see ClassTemplates
   */
  val classes: ClassTemplates

  fun classes(action: Action<in ClassTemplates>): Templates {
    action.execute(classes)
    return this
  }

  /**
   * Method-level templates.
   * @see MethodTemplates
   */
  val methods: MethodTemplates

  fun methods(action: Action<in MethodTemplates>): Templates {
    action.execute(methods)
    return this
  }

  /**
   * Entry-Point templates.
   * @see EntryPointTemplates
   */
  val entryPoints: EntryPointTemplates

  fun entryPoints(action: Action<in EntryPointTemplates>): Templates {
    action.execute(entryPoints)
    return this
  }

  private val handlers: ListProperty<TemplateHandler>

  /**
   * All files associated with templates. This is used for building up dependencies.
   */
  internal val templateFiles: FileCollection

  /**
   * All template data that has been set by a user.
   */
  internal val generatorTemplates: ListProperty<SerializedTemplate>

  init {
    classes = objects.newInstance<ClassTemplates>()
    methods = objects.newInstance<MethodTemplates>()
    entryPoints = objects.newInstance<EntryPointTemplates>()
    handlers = objects.listProperty<TemplateHandler>().apply {
      add(classes)
      add(methods)
      add(entryPoints)
    }

    templateFiles = objects.fileCollection()
      .from(handlers.map { h -> h.map { it.templateFiles } })

    generatorTemplates = objects.listProperty<SerializedTemplate>().apply {
      addAll(classes.templates)
      addAll(methods.templates)
      addAll(entryPoints.templates)
    }
  }

  open class FileOrTemplate @Inject constructor(objects: ObjectFactory) {
    var file: RegularFileProperty
    var template: Property<String>

    init {
      file = objects.fileProperty()
      template = objects.property(String::class.java)
    }
  }

  open class FileOrTemplateProperty @Inject internal constructor(
    objects: ObjectFactory,
    private val project: Project
  ) : Property<FileOrTemplate> by objects.property<FileOrTemplate>().convention(objects.newInstance<FileOrTemplate>()) {
    fun file(file: Any): Property<FileOrTemplate> {
      get().file.set(project.file(file))
      return this
    }

    fun template(content: String): Property<FileOrTemplate> {
      get().template.set(content)
      return this
    }
  }

  /**
   * Used to reuse some information within the template "categories"
   */
  sealed class TemplateHandler @Inject constructor(objects: ObjectFactory) {
    internal val templateInfos: ListProperty<Info>

    internal val templates: ListProperty<SerializedTemplate>
    internal val templateFiles: FileCollection

    init {
      templateInfos = objects.listProperty<Info>().empty()

      templates = objects.listProperty<SerializedTemplate>().apply {
        addAll(templateInfos.map { infos -> infos.mapNotNull { it.toTemplate() } })
      }

      templateFiles = objects.fileCollection()
        .from(templateInfos.map { infos -> infos.mapNotNull { it.fileOrTemplate?.file?.orNull } })
    }

    internal data class Info(
      val fileOrTemplate: FileOrTemplate?,
      val type: Template.Type,
      val description: String,
    ) {
      fun toTemplate(): SerializedTemplate? {
        return if (fileOrTemplate!!.file.isPresent) {
          val file = fileOrTemplate.file.asFile.get()
          SerializedTemplate.file(type, file)
        } else if (fileOrTemplate.template.isPresent) {
          SerializedTemplate.template(type, fileOrTemplate.template.get())
        } else {
          null
        }
      }
    }

    companion object {
      @JvmStatic
      internal fun info(self: Property<FileOrTemplate>, type: Template.Type, description: String): Provider<Info> {
        return self.map { Info(it, type, description) }
      }
    }
  }

  /**
   * Class-level templates
   */
  open class ClassTemplates @Inject constructor(objects: ObjectFactory) : TemplateHandler(objects) {
    val assertionClass: FileOrTemplateProperty = objects.newInstance<FileOrTemplateProperty>()
    val hierarchicalConcrete: FileOrTemplateProperty = objects.newInstance<FileOrTemplateProperty>()
    val hierarchicalAbstract: FileOrTemplateProperty = objects.newInstance<FileOrTemplateProperty>()

    init {
      templateInfos.apply {
        // @format:off
        add(info(assertionClass, ASSERT_CLASS, "'class assertions'"))
        add(info(hierarchicalConcrete, HIERARCHICAL_ASSERT_CLASS, "'hierarchical concrete class assertions'"))
        add(info(hierarchicalAbstract, ABSTRACT_ASSERT_CLASS, "'hierarchical abstract class assertions'"))
        // @format:on
      }
    }
  }

  /**
   * Method-level templates
   */
  open class MethodTemplates @Inject constructor(objects: ObjectFactory) : TemplateHandler(objects) {
    val objectTemplate: FileOrTemplateProperty = objects.newInstance<FileOrTemplateProperty>()
    val booleanPrimitive: FileOrTemplateProperty = objects.newInstance<FileOrTemplateProperty>()
    val booleanWrapper: FileOrTemplateProperty = objects.newInstance<FileOrTemplateProperty>()
    val array: FileOrTemplateProperty = objects.newInstance<FileOrTemplateProperty>()
    val iterable: FileOrTemplateProperty = objects.newInstance<FileOrTemplateProperty>()
    val charPrimitive: FileOrTemplateProperty = objects.newInstance<FileOrTemplateProperty>()
    val character: FileOrTemplateProperty = objects.newInstance<FileOrTemplateProperty>()
    val realNumberPrimitive: FileOrTemplateProperty = objects.newInstance<FileOrTemplateProperty>()
    val realNumberWrapperAssertion: FileOrTemplateProperty = objects.newInstance<FileOrTemplateProperty>()
    val wholeNumberPrimitive: FileOrTemplateProperty = objects.newInstance<FileOrTemplateProperty>()
    val wholeNumberWrapperAssertion: FileOrTemplateProperty = objects.newInstance<FileOrTemplateProperty>()

    init {
      templateInfos.apply {
        // @format:off
        add(info(objectTemplate, HAS, "'object assertions'"))
        add(info(booleanPrimitive, IS, "'boolean assertions'"))
        add(info(booleanWrapper, IS_WRAPPER, "'boolean wrapper assertions'"))
        add(info(array, HAS_FOR_ARRAY, "'array assertions'"))
        add(info(iterable, HAS_FOR_ITERABLE, "'iterable assertions'"))
        add(info(charPrimitive, HAS_FOR_CHAR, "'char assertions'"))
        add(info(character, HAS_FOR_CHARACTER, "'Character assertions'"))
        add(info(realNumberPrimitive, HAS_FOR_REAL_NUMBER, "'real number assertions (float, double)'"))
        add(
          info(
            realNumberWrapperAssertion,
            HAS_FOR_REAL_NUMBER_WRAPPER,
            "'real number wrapper assertions (Float, Double)'"
          )
        )
        add(info(wholeNumberPrimitive, HAS_FOR_WHOLE_NUMBER, "'whole number assertions (int, long, short, byte)'"))
        add(
          info(
            wholeNumberWrapperAssertion,
            HAS_FOR_WHOLE_NUMBER_WRAPPER,
            "'whole number has assertions (Integer, Long, Short, Byte)'"
          )
        )
        // @format:on
      }
    }
  }

  /**
   * Entry point templates
   */
  open class EntryPointTemplates @Inject constructor(objects: ObjectFactory) : TemplateHandler(objects) {
    val assertions: FileOrTemplateProperty = objects.newInstance<FileOrTemplateProperty>()
    val assertionMethod: FileOrTemplateProperty = objects.newInstance<FileOrTemplateProperty>()
    val soft: FileOrTemplateProperty = objects.newInstance<FileOrTemplateProperty>()
    val softMethod: FileOrTemplateProperty = objects.newInstance<FileOrTemplateProperty>()
    val junitSoft: FileOrTemplateProperty = objects.newInstance<FileOrTemplateProperty>()
    val bdd: FileOrTemplateProperty = objects.newInstance<FileOrTemplateProperty>()
    val bddMethod: FileOrTemplateProperty = objects.newInstance<FileOrTemplateProperty>()

    init {
      templateInfos.apply {
        // @format:off// @format:off
        add(info(assertions, ASSERTIONS_ENTRY_POINT_CLASS, "'assertions entry point class'"))
        add(info(assertionMethod, ASSERTION_ENTRY_POINT, "'assertions entry point method'"))
        add(info(soft, SOFT_ASSERTIONS_ENTRY_POINT_CLASS, "'soft assertions entry point class'"))
        add(info(junitSoft, JUNIT_SOFT_ASSERTIONS_ENTRY_POINT_CLASS, "'junit soft assertions entry point class'"))
        add(info(softMethod, SOFT_ENTRY_POINT_METHOD_ASSERTION, "'soft assertions entry point method'"))
        add(info(bdd, BDD_ASSERTIONS_ENTRY_POINT_CLASS, "'BDD assertions entry point class'"))
        add(info(bddMethod, BDD_ENTRY_POINT_METHOD_ASSERTION, "'BDD assertions entry point method'"))
        // @format:on
      }
    }
  }
}

package org.assertj.generator.gradle.tasks.config

import org.assertj.assertions.generator.Template.Type
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

/**
 * Configuration for templates that work with the generator
 */
open class TemplatesExtension @Inject internal constructor(
  private val objects: ObjectFactory,
  private val project: Project,
) {
  /**
   * Class-level templates.
   *
   * @see ClassTemplates
   */
  @get:Input
  val classes: Property<ClassTemplates> = objects.property<ClassTemplates>()
    .value(objects.newInstance<ClassTemplates>())

  /**
   * Method-level templates.
   *
   * @see MethodTemplates
   */
  @get:Input
  val methods: Property<MethodTemplates> = objects.property<MethodTemplates>()
    .value(objects.newInstance<MethodTemplates>())

  /**
   * Entry-Point templates.
   *
   * @see EntryPointTemplates
   */
  @get:Input
  val entryPoints: Property<EntryPointTemplates> = objects.property<EntryPointTemplates>()
    .value(objects.newInstance<EntryPointTemplates>())

  @get:Input
  val templates: ListProperty<SerializedTemplate> = objects.listProperty<SerializedTemplate>().apply {
    addAll(classes.flatMap { it.templates })
    addAll(methods.flatMap { it.templates })
    addAll(entryPoints.flatMap { it.templates })
  }

  @get:InputFiles
  @get:Classpath
  val templateFiles: FileCollection = objects.fileCollection().apply {
    from(classes.map { it.templateFiles })
    from(methods.map { it.templateFiles })
    from(entryPoints.map { it.templateFiles })
  }

  fun classes(action: Action<in ClassTemplates>): TemplatesExtension {
    action.execute(classes.get())
    return this
  }

  fun methods(action: Action<in MethodTemplates>): TemplatesExtension {
    action.execute(methods.get())
    return this
  }

  fun entryPoints(action: Action<in EntryPointTemplates>): TemplatesExtension {
    action.execute(entryPoints.get())
    return this
  }

  interface FileOrTemplate {
    @get:InputFile
    @get:Classpath
    val file: RegularFileProperty

    @get:Input
    val template: Property<String>
  }

  open class FileOrTemplateProperty @Inject constructor(
    objects: ObjectFactory,
    private val project: Project,
  ) : Property<FileOrTemplate> by objects.property<FileOrTemplate>()
    .convention(objects.newInstance<FileOrTemplate>()) {

    fun file(path: String): Property<FileOrTemplate> {
      get().file.set(project.file(path))
      return this
    }

    fun template(content: String): Property<FileOrTemplate> {
      get().template.set(content)
      return this
    }
  }

  /**
   * Used to reuse some information within the template "categories"
   *
   * @param <T> CRTP
   </T> */
  sealed class TemplateProvider(objects: ObjectFactory) {
    @get:Input
    protected val templateInfos: ListProperty<Info> = objects.listProperty<Info>()
      .empty()

    @get:Input
    val templates: ListProperty<SerializedTemplate> = objects.listProperty<SerializedTemplate>().apply {
      addAll(templateInfos.map { infos -> infos.mapNotNull { it.toTemplate() } })
    }

    @get:InputFiles
    @get:Classpath
    val templateFiles: FileCollection = objects.fileCollection().apply {
      from(templateInfos.map { infos -> infos.map { it.fileOrTemplate.file.orNull } })
    }

    protected data class Info(
      val fileOrTemplate: FileOrTemplate,
      val type: Type,
      val description: String,
    ) {
      fun toTemplate(): SerializedTemplate? = when {
        fileOrTemplate.file.isPresent -> {
          SerializedTemplate.file(type, fileOrTemplate.file.asFile.get())
        }

        fileOrTemplate.template.isPresent ->
          SerializedTemplate.template(type, fileOrTemplate.template.get())

        else -> null
      }
    }

    protected fun Property<FileOrTemplate>.info(type: Type, description: String): Provider<Info> {
      return map { Info(it, type, description) }
    }

    companion object {
      @JvmStatic
      protected fun ObjectFactory.fileOrTemplateProperty() = newInstance<FileOrTemplateProperty>()
    }
  }

  /**
   * Class-level templates
   */
  open class ClassTemplates @Inject constructor(objects: ObjectFactory) : TemplateProvider(objects) {
    @get:Input
    val assertionClass: FileOrTemplateProperty = objects.fileOrTemplateProperty()

    @get:Input
    val hierarchicalConcrete: FileOrTemplateProperty = objects.fileOrTemplateProperty()

    @get:Input
    val hierarchicalAbstract: FileOrTemplateProperty = objects.fileOrTemplateProperty()

    init {
      templateInfos.apply {
        // @format:off
        add(assertionClass.info(Type.ASSERT_CLASS, "'class assertions'"))
        add(
          hierarchicalConcrete.info(
            Type.HIERARCHICAL_ASSERT_CLASS,
            "'hierarchical concrete class assertions'"
          )
        )
        add(hierarchicalAbstract.info(Type.ABSTRACT_ASSERT_CLASS, "'hierarchical abstract class assertions'"))
        // @format:on
      }
    }
  }

  /**
   * Method-level templates
   */
  open class MethodTemplates @Inject constructor(objects: ObjectFactory) : TemplateProvider(objects) {
    @get:Input
    val objectTemplate: FileOrTemplateProperty = objects.fileOrTemplateProperty()

    @get:Input
    val booleanPrimitive: FileOrTemplateProperty = objects.fileOrTemplateProperty()

    @get:Input
    val booleanWrapper: FileOrTemplateProperty = objects.fileOrTemplateProperty()

    @get:Input
    val array: FileOrTemplateProperty = objects.fileOrTemplateProperty()

    @get:Input
    val iterable: FileOrTemplateProperty = objects.fileOrTemplateProperty()

    @get:Input
    val charPrimitive: FileOrTemplateProperty = objects.fileOrTemplateProperty()

    @get:Input
    val character: FileOrTemplateProperty = objects.fileOrTemplateProperty()

    @get:Input
    val realNumberPrimitive: FileOrTemplateProperty = objects.fileOrTemplateProperty()

    @get:Input
    val realNumberWrapperAssertion: FileOrTemplateProperty = objects.fileOrTemplateProperty()

    @get:Input
    val wholeNumberPrimitive: FileOrTemplateProperty = objects.fileOrTemplateProperty()

    @get:Input
    val wholeNumberWrapperAssertion: FileOrTemplateProperty = objects.fileOrTemplateProperty()

    init {
      templateInfos.apply {
        // @format:off
        add(objectTemplate.info(Type.HAS, "'object assertions'"))
        add(booleanPrimitive.info(Type.IS, "'boolean assertions'"))
        add(booleanWrapper.info(Type.IS_WRAPPER, "'boolean wrapper assertions'"))
        add(array.info(Type.HAS_FOR_ARRAY, "'array assertions'"))
        add(iterable.info(Type.HAS_FOR_ITERABLE, "'iterable assertions'"))
        add(charPrimitive.info(Type.HAS_FOR_CHAR, "'char assertions'"))
        add(character.info(Type.HAS_FOR_CHARACTER, "'Character assertions'"))
        add(realNumberPrimitive.info(Type.HAS_FOR_REAL_NUMBER, "'real number assertions (float, double)'"))
        add(
          realNumberWrapperAssertion.info(
            Type.HAS_FOR_REAL_NUMBER_WRAPPER,
            "'real number wrapper assertions (Float, Double)'"
          )
        )
        add(
          wholeNumberPrimitive.info(
            Type.HAS_FOR_WHOLE_NUMBER,
            "'whole number assertions (int, long, short, byte)'"
          )
        )
        add(
          wholeNumberWrapperAssertion.info(
            Type.HAS_FOR_WHOLE_NUMBER_WRAPPER,
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
  abstract class EntryPointTemplates @Inject constructor(objects: ObjectFactory) : TemplateProvider(objects) {
    @get:Input
    val assertions: FileOrTemplateProperty = objects.fileOrTemplateProperty()

    @get:Input
    val assertionMethod: FileOrTemplateProperty = objects.fileOrTemplateProperty()

    @get:Input
    val soft: FileOrTemplateProperty = objects.fileOrTemplateProperty()

    @get:Input
    val softMethod: FileOrTemplateProperty = objects.fileOrTemplateProperty()

    @get:Input
    val junitSoft: FileOrTemplateProperty = objects.fileOrTemplateProperty()

    @get:Input
    val bdd: FileOrTemplateProperty = objects.fileOrTemplateProperty()

    @get:Input
    val bddMethod: FileOrTemplateProperty = objects.fileOrTemplateProperty()

    init {
      templateInfos.apply {
        // @format:off
        add(assertions.info(Type.ASSERTIONS_ENTRY_POINT_CLASS, "'assertions entry point class'"))
        add(assertionMethod.info(Type.ASSERTION_ENTRY_POINT, "'assertions entry point method'"))
        add(soft.info(Type.SOFT_ASSERTIONS_ENTRY_POINT_CLASS, "'soft assertions entry point class'"))
        add(
          junitSoft.info(
            Type.JUNIT_SOFT_ASSERTIONS_ENTRY_POINT_CLASS,
            "'junit soft assertions entry point class'"
          )
        )
        add(softMethod.info(Type.SOFT_ENTRY_POINT_METHOD_ASSERTION, "'soft assertions entry point method'"))
        add(bdd.info(Type.BDD_ASSERTIONS_ENTRY_POINT_CLASS, "'BDD assertions entry point class'"))
        add(bddMethod.info(Type.BDD_ENTRY_POINT_METHOD_ASSERTION, "'BDD assertions entry point method'"))
        // @format:on
      }
    }
  }
}

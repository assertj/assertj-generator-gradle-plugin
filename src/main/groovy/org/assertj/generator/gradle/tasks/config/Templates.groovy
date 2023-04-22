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
package org.assertj.generator.gradle.tasks.config

import groovy.transform.EqualsAndHashCode
import org.apache.commons.lang3.CharEncoding
import org.assertj.assertions.generator.Template
import org.assertj.core.util.Files
import org.assertj.generator.gradle.internal.tasks.AssertionsGeneratorReport
import org.assertj.generator.gradle.util.Either
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty

import javax.inject.Inject

import static org.assertj.assertions.generator.Template.Type.*

/**
 * Configuration for templates that work with the generator
 */
@EqualsAndHashCode
class Templates implements Serializable {

    /**
     * Root directory for templates
     */
    File dir

    /**
     * Class-level templates. 
     *
     * @see ClassTemplates
     */
    ClassTemplates classes = new ClassTemplates()

    def classes(Action<? extends ClassTemplates> action) {
        action.execute(classes)
        this
    }

    def classes(Closure closure) {
        // DO NOT USE ConfigureUtil.configure() it will not allow us to override the `setProperty()` method via the
        // mixin. 
        closure.delegate = this.classes
        closure.run()
        this
    }

    /**
     * Method-level templates. 
     *
     * @see MethodTemplates
     */
    MethodTemplates methods = new MethodTemplates()

    def methods(Action<? extends MethodTemplates> action) {
        action.execute(methods)
        this
    }

    def methods(Closure closure) {
        // DO NOT USE ConfigureUtil.configure() it will not allow us to override the `setProperty()` method via the
        // mixin. 
        closure.delegate = this.methods
        closure.run()
        this
    }

    /**
     * Entry-Point templates. 
     *
     * @see EntryPointTemplates
     */
    EntryPointTemplates entryPoints = new EntryPointTemplates()

    def entryPoints(Action<? extends EntryPointTemplates> action) {
        action.execute(entryPoints)
        this
    }

    def entryPoints(Closure closure) {

        // DO NOT USE ConfigureUtil.configure() it will not allow us to override the `setProperty()` method via the
        // mixin. 
        closure.delegate = this.entryPoints
        closure.run()
        this
    }

    private final List<TemplateHandler> handlers = [classes, methods, entryPoints]

    /**
     * All files associated with templates. This is used for building up dependencies.
     */
    ListProperty<File> templateFiles

    /**
     * All template strings that have been set by a user.
     */
    ListProperty<String> templateStrings

    @Inject
    Templates(ObjectFactory objects, Project project) {
        templateStrings = objects.listProperty(String)

        def handlers = this.handlers
        templateStrings.set(
                project.provider {
                    handlers.collectMany { it.strings }
                }
        )

        templateFiles = objects.listProperty(File)
        templateFiles.set(
                project.provider {
                    List<File> files = handlers.collectMany { it.files }

                    if (dir) {
                        // resolve all of them if there is a directory
                        def dirPath = dir.toPath()
                        files = files.collect {
                            dirPath.resolve(it.toPath()).toFile()
                        }
                    }

                    files
                }
        )
    }

    /**
     * Loads all the templates from the inner configurations and reports the status as it progresses. 
     * @param report
     * @return All templates overridden. 
     */
    List<Template> getTemplates(AssertionsGeneratorReport report) {
        // resolve user templates directory
        if (dir == null) dir = new File(".")

        // load any templates overridden by the user
        List<Template> userTemplates = new ArrayList<>()
        handlers.each { handler -> handler.getTemplates(userTemplates, report) }

        return userTemplates
    }

    private void loadUserTemplate(Either<File, String> userTemplate, Template.Type type, String templateDescription,
                                  List<Template> userTemplates, AssertionsGeneratorReport report) {
        if (userTemplate) {
            try {
                final String templateContent
                if (userTemplate.leftValue) {
                    File templateFile = dir.toPath().resolve(userTemplate.left.toPath()).toFile()
                    templateContent = Files.contentOf(templateFile, CharEncoding.UTF_8)

                    report.registerUserTemplate("Using custom template for " + templateDescription + " loaded from "
                            + templateFile)
                } else {
                    templateContent = userTemplate.right
                    report.registerUserTemplate("Using custom template for " + templateDescription
                            + " loaded from raw String")
                }

                userTemplates.add(new Template(type, templateContent))
            } catch (Exception ignored) {
                // best effort : if we can't read user template, use the default one.
                report.registerUserTemplate("Use default " + templateDescription
                        + " assertion template as we failed to to read user template from "
                        + userTemplate)
            }
        }
    }

    /**
     * Used to reuse some information within the template "categories"
     * @param <T>       CRTP
     */
    private abstract class TemplateHandler implements Either.EitherPropertyMixin {
        abstract def getTemplates(List<Template> userTemplates, AssertionsGeneratorReport report)

        List<File> getFiles() {
            getLeftProperties(File.class)
        }

        List<String> getStrings() {
            getRightProperties(String.class)
        }

        void writeOutputStream(ObjectOutputStream s) throws IOException {
            this.metaClass.properties.findAll { prop ->
                Either.class.isAssignableFrom(prop.type)
            }.each {
                s.writeObject(it.getProperty(this))
            }
        }

        void fromInputStream(ObjectInputStream s) throws IOException, ClassNotFoundException {
            this.metaClass.properties.findAll { prop ->
                Either.class.isAssignableFrom(prop.type)
            }.each { prop ->
                prop.setProperty(this, s.readObject())
            }
        }

    }

    /**
     * Class-level templates
     */
    @EqualsAndHashCode
    class ClassTemplates extends TemplateHandler implements Serializable {

        Either<File, String> assertionClass
        Either<File, String> hierarchicalConcrete
        Either<File, String> hierarchicalAbstract

        @Override
        def getTemplates(List<Template> userTemplates, AssertionsGeneratorReport report) {
            // @format:off
            loadUserTemplate(assertionClass, ASSERT_CLASS, "'class assertions'", userTemplates, report)
            loadUserTemplate(hierarchicalConcrete, HIERARCHICAL_ASSERT_CLASS, "'hierarchical concrete class assertions'", userTemplates, report)
            loadUserTemplate(hierarchicalAbstract, ABSTRACT_ASSERT_CLASS, "'hierarchical abstract class assertions'", userTemplates, report)
            // @format:on
        }
    }

    /**
     * Method-level templates
     */
    @EqualsAndHashCode
    class MethodTemplates extends TemplateHandler implements Serializable {

        Either<File, String> object
        Either<File, String> booleanPrimitive
        Either<File, String> booleanWrapper
        Either<File, String> array
        Either<File, String> iterable
        Either<File, String> charPrimitive
        Either<File, String> character
        Either<File, String> realNumberPrimitive
        Either<File, String> realNumberWrapperAssertion
        Either<File, String> wholeNumberPrimitive
        Either<File, String> wholeNumberWrapperAssertion


        @Override
        def getTemplates(List<Template> userTemplates, AssertionsGeneratorReport report) {
            // @format:off
            loadUserTemplate(object, HAS, "'object assertions'", userTemplates, report)

            loadUserTemplate(booleanPrimitive, IS, "'boolean assertions'", userTemplates, report)
            loadUserTemplate(booleanWrapper, IS_WRAPPER, "'boolean wrapper assertions'", userTemplates, report)

            loadUserTemplate(array, HAS_FOR_ARRAY, "'array assertions'", userTemplates, report)
            loadUserTemplate(iterable, HAS_FOR_ITERABLE, "'iterable assertions'", userTemplates, report)

            loadUserTemplate(charPrimitive, HAS_FOR_CHAR, "'char assertions'", userTemplates, report)
            loadUserTemplate(character, HAS_FOR_CHARACTER, "'Character assertions'", userTemplates, report)

            loadUserTemplate(realNumberPrimitive, HAS_FOR_REAL_NUMBER, "'real number assertions (float, double)'", userTemplates, report)
            loadUserTemplate(realNumberWrapperAssertion, HAS_FOR_REAL_NUMBER_WRAPPER, "'real number wrapper assertions (Float, Double)'", userTemplates, report)

            loadUserTemplate(wholeNumberPrimitive, HAS_FOR_WHOLE_NUMBER, "'whole number assertions (int, long, short, byte)'", userTemplates, report)
            loadUserTemplate(wholeNumberWrapperAssertion, HAS_FOR_WHOLE_NUMBER_WRAPPER, "'whole number has assertions (Integer, Long, Short, Byte)'", userTemplates, report)
            // @format:on
        }
    }


    /**
     * Entry point templates
     */
    @EqualsAndHashCode
    class EntryPointTemplates extends TemplateHandler implements Serializable {

        Either<File, String> assertions
        Either<File, String> assertionMethod
        Either<File, String> soft
        Either<File, String> softMethod
        Either<File, String> junitSoft
        Either<File, String> bdd
        Either<File, String> bddMethod

        @Override
        def getTemplates(List<Template> userTemplates, AssertionsGeneratorReport report) {
            // @format:off
            loadUserTemplate(assertions, ASSERTIONS_ENTRY_POINT_CLASS, "'assertions entry point class'", userTemplates, report)
            loadUserTemplate(assertionMethod, ASSERTION_ENTRY_POINT, "'assertions entry point method'", userTemplates, report)
            loadUserTemplate(soft, SOFT_ASSERTIONS_ENTRY_POINT_CLASS, "'soft assertions entry point class'", userTemplates, report)
            loadUserTemplate(junitSoft, JUNIT_SOFT_ASSERTIONS_ENTRY_POINT_CLASS, "'junit soft assertions entry point class'", userTemplates, report)
            loadUserTemplate(softMethod, SOFT_ENTRY_POINT_METHOD_ASSERTION, "'soft assertions entry point method'", userTemplates, report)
            loadUserTemplate(bdd, BDD_ASSERTIONS_ENTRY_POINT_CLASS, "'BDD assertions entry point class'", userTemplates, report)
            loadUserTemplate(bddMethod, BDD_ENTRY_POINT_METHOD_ASSERTION, "'BDD assertions entry point method'", userTemplates, report)
            // @format:on
        }

    }

    // Serialization code: 

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.writeObject(dir)
        classes.writeOutputStream(s)
        entryPoints.writeOutputStream(s)
        methods.writeOutputStream(s)
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        dir = s.readObject() as File

        // This awkward pattern is because the constructor is not called
        // when deserialization occurs. This could be solved with forced reflection-based
        // assignment, but that's asking for trouble..

        classes = new ClassTemplates()
        classes.fromInputStream(s)
        entryPoints = new EntryPointTemplates()
        entryPoints.fromInputStream(s)
        methods = new MethodTemplates()
        methods.fromInputStream(s)
    }
}
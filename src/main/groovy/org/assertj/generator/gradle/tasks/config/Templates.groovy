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

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString
import org.assertj.assertions.generator.Template
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
import org.gradle.api.tasks.InputFiles

import javax.inject.Inject

import static org.assertj.assertions.generator.Template.Type.*

/**
 * Configuration for templates that work with the generator
 */
@EqualsAndHashCode
class Templates implements Serializable {

    /**
     * Class-level templates. 
     *
     * @see ClassTemplates
     */
    final ClassTemplates classes

    def classes(Action<? extends ClassTemplates> action) {
        action.execute(classes)
        this
    }

    /**
     * Method-level templates.
     *
     * @see MethodTemplates
     */
    MethodTemplates methods

    def methods(Action<? extends MethodTemplates> action) {
        action.execute(methods)
        this
    }

    /**
     * Entry-Point templates. 
     *
     * @see EntryPointTemplates
     */
    final EntryPointTemplates entryPoints

    def entryPoints(Action<? extends EntryPointTemplates> action) {
        action.execute(entryPoints)
        this
    }

    private final ListProperty<TemplateHandler> handlers

    /**
     * All files associated with templates. This is used for building up dependencies.
     */
    @InputFiles
    @Classpath
    final FileCollection templateFiles

    /**
     * All template data that has been set by a user.
     */
    @Input
    final ListProperty<SerializedTemplate> templates

    @Inject
    Templates(ObjectFactory objects) {
        classes = objects.newInstance(ClassTemplates)
        methods = objects.newInstance(MethodTemplates)
        entryPoints = objects.newInstance(EntryPointTemplates)

        handlers = objects.listProperty(TemplateHandler).tap {
            add(classes)
            add(methods)
            add(entryPoints)
        }

        def handlers = this.handlers
        templateFiles = objects.fileCollection()
                .from(handlers.map { h -> h.collect { it.templateFiles } })

        templates = objects.listProperty(SerializedTemplate).tap {
            addAll(classes.templates)
            addAll(methods.templates)
            addAll(entryPoints.templates)
        }
    }

    static class FileOrTemplate {
        RegularFileProperty file
        Property<String> template

        @Inject
        FileOrTemplate(ObjectFactory objects) {
            file = objects.fileProperty()
            template = objects.property(String)
        }
    }

    static class FileOrTemplateProperty implements Property<FileOrTemplate> {
        private final Project project

        @Delegate
        private final Property<FileOrTemplate> delegate

        @Inject
        FileOrTemplateProperty(ObjectFactory objects, Project project) {
            this.project = project
            this.delegate = objects.property(FileOrTemplate)
                    .convention(objects.newInstance(FileOrTemplate))
        }

        Property<FileOrTemplate> file(Object file) {
            get().file.set(project.file(file))
            return this
        }

        Property<FileOrTemplate> template(String content) {
            get().template.set(content)
            return this
        }
    }

    /**
     * Used to reuse some information within the template "categories"
     */
    abstract static class TemplateHandler {
        protected final ListProperty<Info> templateInfos
        final ListProperty<SerializedTemplate> templates
        final FileCollection templateFiles

        @Inject
        protected TemplateHandler(ObjectFactory objects) {
            templateInfos = objects.listProperty(Info).empty()
            templates = objects.listProperty(SerializedTemplate).tap {
                addAll(
                        templateInfos.map { infos ->
                            infos.collect { it.toTemplate() }.findAll()
                        }
                )
            }
            templateFiles = objects.fileCollection().tap {
                from(
                        templateInfos.map { infos ->
                            infos.collect { it.fileOrTemplate.file.getOrNull() }.findAll()
                        }
                )
            }
        }

        @EqualsAndHashCode
        @ToString
        protected static class Info {
            final FileOrTemplate fileOrTemplate
            final Template.Type type
            final String description

            SerializedTemplate toTemplate() {
                if (fileOrTemplate.file.isPresent()) {
                    def file = fileOrTemplate.file.asFile.get()
                    new SerializedTemplate(type, null, file)
                } else if (fileOrTemplate.template.isPresent()) {
                    new SerializedTemplate(type, fileOrTemplate.template.get(), null)
                } else {
                    null
                }
            }

            private Info(
                    FileOrTemplate fileOrTemplate,
                    Template.Type type,
                    String description
            ) {
                this.fileOrTemplate = fileOrTemplate
                this.type = type
                this.description = description
            }
        }

        static protected Provider<Info> info(final Property<FileOrTemplate> self, Template.Type type, String description) {
            self.map { new Info(it, type, description) }
        }
    }

    /**
     * Class-level templates
     */
    @EqualsAndHashCode
    static class ClassTemplates extends TemplateHandler {
        FileOrTemplateProperty assertionClass
        FileOrTemplateProperty hierarchicalConcrete
        FileOrTemplateProperty hierarchicalAbstract

        @Inject
        ClassTemplates(ObjectFactory objects) {
            super(objects)

            assertionClass = objects.newInstance(FileOrTemplateProperty)
            hierarchicalConcrete = objects.newInstance(FileOrTemplateProperty)
            hierarchicalAbstract = objects.newInstance(FileOrTemplateProperty)

            templateInfos.tap {
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
    @EqualsAndHashCode
    static class MethodTemplates extends TemplateHandler {
        final FileOrTemplateProperty objectTemplate
        final FileOrTemplateProperty booleanPrimitive
        final FileOrTemplateProperty booleanWrapper
        final FileOrTemplateProperty array
        final FileOrTemplateProperty iterable
        final FileOrTemplateProperty charPrimitive
        final FileOrTemplateProperty character
        final FileOrTemplateProperty realNumberPrimitive
        final FileOrTemplateProperty realNumberWrapperAssertion
        final FileOrTemplateProperty wholeNumberPrimitive
        final FileOrTemplateProperty wholeNumberWrapperAssertion

        @Inject
        MethodTemplates(ObjectFactory objects) {
            super(objects)
            objectTemplate = objects.newInstance(FileOrTemplateProperty)
            booleanPrimitive = objects.newInstance(FileOrTemplateProperty)
            booleanWrapper = objects.newInstance(FileOrTemplateProperty)
            array = objects.newInstance(FileOrTemplateProperty)
            iterable = objects.newInstance(FileOrTemplateProperty)
            charPrimitive = objects.newInstance(FileOrTemplateProperty)
            character = objects.newInstance(FileOrTemplateProperty)
            realNumberPrimitive = objects.newInstance(FileOrTemplateProperty)
            realNumberWrapperAssertion = objects.newInstance(FileOrTemplateProperty)
            wholeNumberPrimitive = objects.newInstance(FileOrTemplateProperty)
            wholeNumberWrapperAssertion = objects.newInstance(FileOrTemplateProperty)

            templateInfos.tap {
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
                add(
                        info(
                                wholeNumberPrimitive,
                                HAS_FOR_WHOLE_NUMBER,
                                "'whole number assertions (int, long, short, byte)'"
                        )
                )
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
    @EqualsAndHashCode
    static class EntryPointTemplates extends TemplateHandler {
        final FileOrTemplateProperty assertions
        final FileOrTemplateProperty assertionMethod
        final FileOrTemplateProperty soft
        final FileOrTemplateProperty softMethod
        final FileOrTemplateProperty junitSoft
        final FileOrTemplateProperty bdd
        final FileOrTemplateProperty bddMethod

        @Inject
        EntryPointTemplates(ObjectFactory objects) {
            super(objects)

            assertions = objects.newInstance(FileOrTemplateProperty)
            assertionMethod = objects.newInstance(FileOrTemplateProperty)
            soft = objects.newInstance(FileOrTemplateProperty)
            softMethod = objects.newInstance(FileOrTemplateProperty)
            junitSoft = objects.newInstance(FileOrTemplateProperty)
            bdd = objects.newInstance(FileOrTemplateProperty)
            bddMethod = objects.newInstance(FileOrTemplateProperty)

            templateInfos.tap {
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
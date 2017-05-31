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

import com.google.common.base.Preconditions
import com.google.common.reflect.TypeToken
import groovy.transform.EqualsAndHashCode
import org.apache.commons.lang3.CharEncoding
import org.assertj.assertions.generator.Template
import org.assertj.core.util.Files
import org.assertj.core.util.VisibleForTesting
import org.assertj.generator.gradle.internal.tasks.AssertionsGeneratorReport
import org.assertj.generator.gradle.util.Either
import org.codehaus.groovy.control.ConfigurationException
import org.gradle.api.Action

import java.lang.reflect.Field

import static org.assertj.assertions.generator.Template.Type.*

@EqualsAndHashCode
class Templates implements Serializable {

    File dir

    AssertionTemplates assertions = new AssertionTemplates()
    
    def assertions(Action<? extends AssertionTemplates> action) {
        action.execute(assertions)
        this
    }

    def assertions(Closure closure) {
        closure.delegate = assertions
        closure.run()
        this
    }
    
    MethodTemplates methods = new MethodTemplates()
    
    def methods(Action<? extends MethodTemplates> action) {
        action.execute(methods)
        this
    }

    def methods(Closure closure) {
        closure.delegate = methods 
        closure.run()
        this
    }
    
    EntryPointTemplates entryPoints = new EntryPointTemplates()
    def entryPoints(Action<? extends EntryPointTemplates> action) {
        action.execute(entryPoints)
        this
    }

    def entryPoints(Closure closure) {
        closure.delegate = entryPoints
        closure.run()
        this
    }
    
    private final List<TemplateHandler> handlers
    
    def getFiles() {
        List<File> files = handlers.collect { it.files }.flatten() as List<File>
            
        if (dir) {
            // resolve all of them if there is a directory
            def dirPath = dir.toPath()
            files = files.collect {
                dirPath.resolve(it.toPath()).toFile()
            }
        }
        
        files
    }
    
    Templates() {
        handlers = new ArrayList<>(3)
        handlers.add(assertions)
        handlers.add(methods)
        handlers.add(entryPoints)
    }
    
    // assertion class templates
    // assertion method templates
    // entry point templates
    

    void copyFrom(Templates other) {
        this.dir = other.dir
        
        // Directly copy all the property values for the handlers
        handlers.each { handler ->
            handler.metaClass.properties.each { prop ->
                def fromOther = prop.getProperty(other)
                if (fromOther) {
                    prop.setProperty(this, fromOther)
                }
            }
        }
    }

    void defaults(Templates defaults) {
        if (!this.dir) {
            this.dir = defaults.dir
        }
        // Directly override if not set
        this.metaClass.properties.findAll { v -> TemplateHandler.class.isAssignableFrom(v.type) }
            .each { prop -> 
                TemplateHandler handler = prop.getProperty(this) as TemplateHandler
                TemplateHandler defHandler = prop.getProperty(defaults) as TemplateHandler
                handler.defaultFrom(defHandler)
            }
    }

    List<Template> getTemplates(AssertionsGeneratorReport report) {
        // resolve user templates directory
        if (dir == null) dir = new File(".")

        // load any templates overridden by the user
        List<Template> userTemplates = new ArrayList<>()
        handlers.each { handler -> handler.getTemplates(userTemplates, report) }
        
        return userTemplates
    }

    @VisibleForTesting
    void loadUserTemplate(Either<File, String> userTemplate, Template.Type type, String templateDescription,
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

    private trait EitherHandler {
        
        void setProperty(String name, Object value) {
            def prop
            try {
                 prop = this.metaClass.getMetaProperty(name)
            } catch (MissingPropertyException ignore) {
                throw new ConfigurationException("Property with name ${name} does not exist for ${this.metaClass.class}, check spelling.")
            }

            if (prop && value && Either.class.isAssignableFrom(prop.type)) {
                // handle the either type
                TypeToken<?> base = TypeToken.of(getClass())
                
                Field field = base.getRawType().getDeclaredField(prop.name)
                Preconditions.checkNotNull(field, "Property name is wrong? %s", prop)

                def eitherToken = base.resolveType(field.genericType)
                def leftToken = eitherToken.resolveType(Either.class.typeParameters[0])
                def rightToken = eitherToken.resolveType(Either.class.typeParameters[1])

                if (leftToken.isSupertypeOf(value.class)) {
                    // it's a "left"
                    this.metaClass.setProperty(this, name, Either.left(value))
                } else if (rightToken.isSupertypeOf(value.class)) {
                    this.metaClass.setProperty(this, name, Either.right(value))
                } else {
                    throw new ClassCastException(String.format("Can not assign type into %s %s", value.class, prop.class))
                }
            } else {
                this.metaClass.setProperty(this, name, value)
            }
        }

        def <L> List<L> getLeftProperties(Class<L> leftClazz) {
            TypeToken<? extends EitherHandler> base = (TypeToken<? extends EitherHandler>)TypeToken.of(getClass())
            
            this.metaClass.properties.findAll { prop -> Either.class.isAssignableFrom(prop.type) }
                .findAll { prop ->
                    Field f = base.rawType.getDeclaredField(prop.name)
                    def leftToken = base.resolveType(f.genericType).resolveType(Either.class.typeParameters[0])
                    leftToken.isSubtypeOf(leftClazz)
                }
                .collect { (Either<L, ?>)this.metaClass.getProperty(this, it.name) }
                .findAll { it && it.leftValue }
                .collect { v -> v.left }
        }

        def <R> List<R> getRightProperties(Class<R> value) {
            TypeToken<? extends EitherHandler> base = (TypeToken<? extends EitherHandler>)TypeToken.of(getClass())

            metaClass.properties.findAll { prop -> Either.class.isAssignableFrom(prop.type) }
                    .findAll { prop ->
                        Field f = base.rawType.getDeclaredField(prop.name)
                        def rightToken = base.resolveType(f.genericType).resolveType(Either.class.typeParameters[1])
                        value.isAssignableFrom(rightToken.rawType)
                    }
                    .collect { (Either<?, R>)this.metaClass.getProperty(this, it.name) }
                    .findAll { it && it.rightValue }
                    .collect { v -> v.right }
        }
    }

    private abstract class TemplateHandler<T extends TemplateHandler<T>> implements EitherHandler {
        abstract def getTemplates(List<Template> userTemplates, AssertionsGeneratorReport report)
        
        List<File> getFiles() {
            getLeftProperties(File.class)
        }

        /**
         * Read values from defaults and write them to the properties inside {@code this}
         * @param defaults
         */
        void defaultFrom(T defaults) {
            if (defaults && !this.is(defaults)) {
                defaults.metaClass.properties.findAll{ prop ->
                    Either.class.isAssignableFrom(prop.type) 
                }.each { prop ->
                    def defValue = prop.getProperty(defaults)
                    def currValue = prop.getProperty(this)
                    if (defValue && !currValue) {
                        prop.setProperty(this, defValue)
                    }
                }
            }
        }

        
        void writeOutputStream(ObjectOutputStream s) throws IOException {
            this.metaClass.properties.findAll{ prop ->
                Either.class.isAssignableFrom(prop.type)
            }.each {
                s.writeObject(it.getProperty(this))
            }
        }

        void fromInputStream(ObjectInputStream s) throws IOException, ClassNotFoundException {
            this.metaClass.properties.findAll{ prop ->
                Either.class.isAssignableFrom(prop.type)
            }.each { prop ->
                prop.setProperty(this, s.readObject())
            }
        }

    }

    @EqualsAndHashCode
    class AssertionTemplates extends TemplateHandler<AssertionTemplates> implements Serializable {

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

    @EqualsAndHashCode
    class MethodTemplates extends TemplateHandler<MethodTemplates> implements Serializable {

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

    @EqualsAndHashCode
    class EntryPointTemplates extends TemplateHandler<EntryPointTemplates> implements Serializable {

        Either<File, String> assertions
        Either<File, String> assertionMethod
        Either<File, String> soft
        Either<File, String> junitSoft
        Either<File, String> softMethod
        Either<File, String> bdd
        Either<File, String> bddMethod

        @Override
        def getTemplates(List<Template> userTemplates, AssertionsGeneratorReport report) {
            // @format:off
            loadUserTemplate(assertions,ASSERTIONS_ENTRY_POINT_CLASS, "'assertions entry point class'", userTemplates, report)
            loadUserTemplate(assertionMethod,ASSERTION_ENTRY_POINT,  "'assertions entry point method'", userTemplates, report)
            loadUserTemplate(soft, SOFT_ASSERTIONS_ENTRY_POINT_CLASS, "'soft assertions entry point class'", userTemplates, report)
            loadUserTemplate(junitSoft, JUNIT_SOFT_ASSERTIONS_ENTRY_POINT_CLASS, "'junit soft assertions entry point class'", userTemplates, report)
            loadUserTemplate(softMethod, SOFT_ENTRY_POINT_METHOD_ASSERTION, "'soft assertions entry point method'", userTemplates, report)
            loadUserTemplate(bdd, BDD_ASSERTIONS_ENTRY_POINT_CLASS, "'BDD assertions entry point class'", userTemplates, report)
            loadUserTemplate(bddMethod, BDD_ENTRY_POINT_METHOD_ASSERTION, "'BDD assertions entry point method'", userTemplates, report)
            // @format:on
        }
        
    }
    
    private void writeObject(ObjectOutputStream s) throws IOException {
        
        s.writeObject(dir)
        assertions.writeOutputStream(s)
        entryPoints.writeOutputStream(s)
        methods.writeOutputStream(s)
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        dir = s.readObject() as File

        assertions = new AssertionTemplates()
        assertions.fromInputStream(s)
        entryPoints = new EntryPointTemplates()
        entryPoints.fromInputStream(s)
        methods = new MethodTemplates()
        methods.fromInputStream(s)
    }
}
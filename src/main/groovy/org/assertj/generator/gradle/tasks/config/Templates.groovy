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
import org.assertj.core.util.VisibleForTesting
import org.assertj.generator.gradle.internal.tasks.AssertionsGeneratorReport

import static org.assertj.assertions.generator.Template.Type.*

@EqualsAndHashCode
class Templates implements Serializable {

    public File dir
    // assertion class templates
    public String assertionClass
    public String hierarchicalAssertionConcreteClass
    public String hierarchicalAssertionAbstractClass
    // assertion method templates
    public String objectAssertion
    public String booleanAssertion
    public String booleanWrapperAssertion
    public String arrayAssertion
    public String iterableAssertion
    public String charAssertion
    public String characterAssertion
    public String realNumberAssertion
    public String realNumberWrapperAssertion
    public String wholeNumberAssertion
    public String wholeNumberWrapperAssertion
    // entry point templates
    public String assertionsEntryPointClass
    public String assertionEntryPointMethod
    public String softEntryPointAssertionClass
    public String junitSoftEntryPointAssertionClass
    public String softEntryPointAssertionMethod
    public String bddEntryPointAssertionClass
    public String bddEntryPointAssertionMethod

    void copyFrom(Templates other) {
        this.dir = other.dir
        // assertion class templates
        this.assertionClass = other.assertionClass
        this.hierarchicalAssertionConcreteClass = other.hierarchicalAssertionConcreteClass
        this.hierarchicalAssertionAbstractClass = other.hierarchicalAssertionAbstractClass
        // assertion method templates
        this.objectAssertion = other.objectAssertion
        this.booleanAssertion = other.booleanAssertion
        this.booleanWrapperAssertion = other.booleanWrapperAssertion
        this.arrayAssertion = other.arrayAssertion
        this.iterableAssertion = other.iterableAssertion
        this.charAssertion = other.charAssertion
        this.characterAssertion = other.characterAssertion
        this.realNumberAssertion = other.realNumberAssertion
        this.realNumberWrapperAssertion = other.realNumberWrapperAssertion
        this.wholeNumberAssertion = other.wholeNumberAssertion
        this.wholeNumberWrapperAssertion = other.wholeNumberWrapperAssertion
        // entry point templates
        this.assertionsEntryPointClass = other.assertionsEntryPointClass
        this.assertionEntryPointMethod = other.assertionEntryPointMethod
        this.softEntryPointAssertionClass = other.softEntryPointAssertionClass
        this.junitSoftEntryPointAssertionClass = other.junitSoftEntryPointAssertionClass
        this.softEntryPointAssertionMethod = other.softEntryPointAssertionMethod
        this.bddEntryPointAssertionClass = other.bddEntryPointAssertionClass
        this.bddEntryPointAssertionMethod = other.bddEntryPointAssertionMethod
    }

    void defaults(Templates defaults) {
        if (!this.dir) {
            this.dir = defaults.dir
        }
        // assertion class templates
        if (!this.assertionClass) {
            this.assertionClass = defaults.assertionClass
        }
        if (!this.hierarchicalAssertionConcreteClass) {
            this.hierarchicalAssertionConcreteClass = defaults.hierarchicalAssertionConcreteClass
        }
        if (!this.hierarchicalAssertionAbstractClass) {
            this.hierarchicalAssertionAbstractClass = defaults.hierarchicalAssertionAbstractClass
        }
        // assertion method templates
        if (!this.objectAssertion) {
            this.objectAssertion = defaults.objectAssertion
        }
        if (!this.booleanAssertion) {
            this.booleanAssertion = defaults.booleanAssertion
        }
        if (!this.booleanWrapperAssertion) {
            this.booleanWrapperAssertion = defaults.booleanWrapperAssertion
        }
        if (!this.arrayAssertion) {
            this.arrayAssertion = defaults.arrayAssertion
        }
        if (!this.iterableAssertion) {
            this.iterableAssertion = defaults.iterableAssertion
        }
        if (!this.charAssertion) {
            this.charAssertion = defaults.charAssertion
        }
        if (!this.characterAssertion) {
            this.characterAssertion = defaults.characterAssertion
        }
        if (!this.realNumberAssertion) {
            this.realNumberAssertion = defaults.realNumberAssertion
        }
        if (!this.realNumberWrapperAssertion) {
            this.realNumberWrapperAssertion = defaults.realNumberWrapperAssertion
        }
        if (!this.wholeNumberAssertion) {
            this.wholeNumberAssertion = defaults.wholeNumberAssertion
        }
        if (!this.wholeNumberWrapperAssertion) {
            this.wholeNumberWrapperAssertion = defaults.wholeNumberWrapperAssertion
        }
        // entry point templates
        if (!this.assertionsEntryPointClass) {
            this.assertionsEntryPointClass = defaults.assertionsEntryPointClass
        }
        if (!this.assertionEntryPointMethod) {
            this.assertionEntryPointMethod = defaults.assertionEntryPointMethod
        }
        if (!this.softEntryPointAssertionClass) {
            this.softEntryPointAssertionClass = defaults.softEntryPointAssertionClass
        }
        if (!this.junitSoftEntryPointAssertionClass) {
            this.junitSoftEntryPointAssertionClass = defaults.junitSoftEntryPointAssertionClass
        }
        if (!this.softEntryPointAssertionMethod) {
            this.softEntryPointAssertionMethod = defaults.softEntryPointAssertionMethod
        }
        if (!this.bddEntryPointAssertionClass) {
            this.bddEntryPointAssertionClass = defaults.bddEntryPointAssertionClass
        }
        if (!this.bddEntryPointAssertionMethod) {
            this.bddEntryPointAssertionMethod = defaults.bddEntryPointAssertionMethod
        }
    }

    List<Template> getTemplates(AssertionsGeneratorReport report) {
        // resolve user templates directory
        if (dir == null) dir = new File(".")

        // load any templates overridden by the user
        List<Template> userTemplates = new ArrayList<>()
        // @format:off
        // assertion class templates
        loadUserTemplate(assertionClass, ASSERT_CLASS, "'class assertions'", userTemplates, report)
        loadUserTemplate(hierarchicalAssertionConcreteClass, HIERARCHICAL_ASSERT_CLASS, "'hierarchical concrete class assertions'", userTemplates, report)
        loadUserTemplate(hierarchicalAssertionAbstractClass, ABSTRACT_ASSERT_CLASS, "'hierarchical abstract class assertions'", userTemplates, report)
        // assertion method templates
        loadUserTemplate(objectAssertion, HAS, "'object assertions'", userTemplates, report)
        loadUserTemplate(booleanAssertion, IS, "'boolean assertions'", userTemplates, report)
        loadUserTemplate(booleanWrapperAssertion, IS_WRAPPER, "'boolean wrapper assertions'", userTemplates, report)
        loadUserTemplate(arrayAssertion, HAS_FOR_ARRAY, "'array assertions'", userTemplates, report)
        loadUserTemplate(iterableAssertion, HAS_FOR_ITERABLE, "'iterable assertions'", userTemplates, report)
        loadUserTemplate(realNumberAssertion, HAS_FOR_REAL_NUMBER, "'real number assertions (float, double)'", userTemplates, report)
        loadUserTemplate(realNumberWrapperAssertion, HAS_FOR_REAL_NUMBER_WRAPPER, "'real number wrapper assertions (Float, Double)'", userTemplates, report)
        loadUserTemplate(wholeNumberAssertion, HAS_FOR_WHOLE_NUMBER, "'whole number assertions (int, long, short, byte)'", userTemplates, report)
        loadUserTemplate(wholeNumberWrapperAssertion, HAS_FOR_WHOLE_NUMBER_WRAPPER, "'whole number has assertions (Integer, Long, Short, Byte)'", userTemplates, report)
        loadUserTemplate(charAssertion, HAS_FOR_CHAR, "'char assertions'", userTemplates, report)
        loadUserTemplate(characterAssertion, HAS_FOR_CHARACTER, "'Character assertions'", userTemplates, report)
        // entry point templates
        loadUserTemplate(assertionsEntryPointClass,ASSERTIONS_ENTRY_POINT_CLASS, "'assertions entry point class'", userTemplates, report)
        loadUserTemplate(assertionEntryPointMethod,ASSERTION_ENTRY_POINT,  "'assertions entry point method'", userTemplates, report)
        loadUserTemplate(softEntryPointAssertionClass, SOFT_ASSERTIONS_ENTRY_POINT_CLASS, "'soft assertions entry point class'", userTemplates, report)
        loadUserTemplate(junitSoftEntryPointAssertionClass, JUNIT_SOFT_ASSERTIONS_ENTRY_POINT_CLASS, "'junit soft assertions entry point class'", userTemplates, report)
        loadUserTemplate(softEntryPointAssertionMethod, SOFT_ENTRY_POINT_METHOD_ASSERTION, "'soft assertions entry point method'", userTemplates, report)
        loadUserTemplate(bddEntryPointAssertionClass, BDD_ASSERTIONS_ENTRY_POINT_CLASS, "'BDD assertions entry point class'", userTemplates, report)
        loadUserTemplate(bddEntryPointAssertionMethod, BDD_ENTRY_POINT_METHOD_ASSERTION, "'BDD assertions entry point method'", userTemplates, report)
        // @format:on
        return userTemplates
    }

    @VisibleForTesting
    void loadUserTemplate(String userTemplate, Template.Type type, String templateDescription,
                          List<Template> userTemplates, AssertionsGeneratorReport report) {
        if (userTemplate != null) {
            try {
                File templateFile = new File(dir, userTemplate)

                final String templateContent
                if (templateFile.exists()) {
                    templateContent = Files.contentOf(templateFile, CharEncoding.UTF_8)
                    report.registerUserTemplate("Using custom template for " + templateDescription + " loaded from "
                            + dir + userTemplate)
                } else {
                    templateContent = userTemplate
                    report.registerUserTemplate("Using custom template for " + templateDescription
                            + " loaded from raw String")
                }

                userTemplates.add(new Template(type, templateContent))
            } catch (Exception ignored) {
                // best effort : if we can't read user template, use the default one.
                report.registerUserTemplate("Use default " + templateDescription
                        + " assertion template as we failed to to read user template from "
                        + dir + userTemplate)
            }
        }
    }
}
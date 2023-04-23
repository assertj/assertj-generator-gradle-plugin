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
package org.assertj.generator.gradle.internal.tasks

import com.google.common.reflect.TypeToken
import org.assertj.assertions.generator.AssertionsEntryPointType

import static com.google.common.collect.Maps.newTreeMap
import static com.google.common.collect.Sets.newTreeSet
import static org.apache.commons.lang3.StringUtils.remove
import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace

class AssertionsGeneratorReport {
    File directoryPathWhereAssertionFilesAreGenerated
    Collection<String> inputPackages
    Collection<String> inputClasses
    Exception exception
    Collection<TypeToken<?>> excludedClassesFromAssertionGeneration
    final List<String> userTemplates
    final Set<String> inputClassesNotFound

    private static final String INDENT = "- "
    private static final String SECTION_START = "--- "
    private static final String SECTION_END = " ---\n"
    private static final String Line = System.lineSeparator()

    private final Set<String> generatedCustomAssertionFileNames
    private final Map<AssertionsEntryPointType, File> assertionsEntryPointFilesByType

    AssertionsGeneratorReport() {
        assertionsEntryPointFilesByType = newTreeMap()
        generatedCustomAssertionFileNames = newTreeSet()
        inputClassesNotFound = newTreeSet()
        directoryPathWhereAssertionFilesAreGenerated = null
        userTemplates = new ArrayList<>()
    }

    void addGeneratedAssertionFile(File generatedCustomAssertionFile) throws IOException {
        generatedCustomAssertionFileNames.add(generatedCustomAssertionFile.getCanonicalPath())
    }

    String getReportContent() {
        new StringBuilder().tap {
            append(Line)
            append("====================================\n")
            append("AssertJ assertions generation report\n")
            append("====================================\n")
            buildGeneratorParametersReport(it)
            append(Line)
            append(SECTION_START).append("Generator results").append(SECTION_END)

            if (generationError()) {
                buildGeneratorReportError(it)
            } else if (nothingGenerated()) {
                buildGeneratorReportWhenNothingWasGenerated(it)
            } else {
                buildGeneratorReportSuccess(it)
            }
        }.toString()
    }

    private StringBuilder buildGeneratorReportSuccess(final StringBuilder self) {
        self.tap {
            append(Line)
            append("Directory where custom assertions files have been generated:\n")
            append(INDENT).append(directoryPathWhereAssertionFilesAreGenerated).append(Line)
            append(Line)
            append("Custom assertions files generated:\n")
            for (String fileName : generatedCustomAssertionFileNames) {
                append(INDENT).append(fileName).append(Line)
            }
            if (!inputClassesNotFound.isEmpty()) {
                append(Line)
                append("No custom assertions files generated for the following input classes as they were not found:\n")
                for (inputClassNotFound in inputClassesNotFound) {
                    append(INDENT).append(inputClassNotFound).append(Line)
                }
            }
            reportEntryPointClassesGeneration(it)
        }
    }

    private StringBuilder reportEntryPointClassesGeneration(final StringBuilder self) {
        self.tap {
            for (type in assertionsEntryPointFilesByType.keySet()) {
                if (assertionsEntryPointFilesByType.get(type) != null) {
                    String entryPointClassName = remove(type.getFileName(), ".java")
                    append(Line)
                            .append(entryPointClassName).append(" entry point class has been generated in file:\n")
                            .append(INDENT).append(assertionsEntryPointFilesByType.get(type).getAbsolutePath())
                            .append(Line)
                }
            }
        }

    }

    private StringBuilder buildGeneratorReportWhenNothingWasGenerated(final StringBuilder self) {
        self.tap {
            append(Line)
            append("No assertions generated as no classes have been found from given classes/packages.\n")
            if (!inputClasses?.isEmpty()) {
                append(INDENT).append("Given classes : ").append(Arrays.toString(inputClasses))
                append(Line)
            }
            if (!inputPackages?.isEmpty()) {
                append(INDENT).append("Given packages : ").append(Arrays.toString(inputPackages))
                append(Line)
            }
            if (!excludedClassesFromAssertionGeneration.isEmpty()) {
                append(INDENT).append("Excluded classes : ").append(excludedClassesFromAssertionGeneration)
            }
        }
    }

    private StringBuilder buildGeneratorReportError(final StringBuilder self) {
        self.tap {
            append(Line)
            append("Assertions failed with error : ").append(exception.getMessage())
            append(Line)
            if (!inputClasses?.isEmpty()) {
                append(INDENT).append("Given classes were : ").append(Arrays.toString(inputClasses))
                append(Line)
            }
            if (!inputPackages?.isEmpty()) {
                append(INDENT).append("Given packages were : ").append(Arrays.toString(inputPackages))
                append(Line)
            }
            append(Line)
            append("Full error stack : ").append(getStackTrace(exception))
        }
    }

    private StringBuilder buildGeneratorParametersReport(final StringBuilder reportBuilder) {
        reportBuilder.tap {
            append(Line)
            append(SECTION_START).append("Generator input parameters").append(SECTION_END)
                    .append(Line)
            if (!userTemplates.isEmpty()) {
                append("The following templates will replace the ones provided by AssertJ when generating AssertJ assertions :\n")
                for (String inputPackage : userTemplates) {
                    append(INDENT).append(inputPackage).append(Line)
                }
                append(Line)
            }
            if (!inputPackages?.isEmpty()) {
                append("Generating AssertJ assertions for classes in following packages and subpackages:\n")
                for (inputPackage in inputPackages) {
                    append(INDENT).append(inputPackage).append(Line)
                }
            }
            if (!inputClasses?.isEmpty()) {
                if (!inputPackages?.isEmpty()) {
                    reportBuilder.append(Line)
                }
                append("Generating AssertJ assertions for classes:\n")
                for (inputClass in inputClasses) {
                    append(INDENT).append(inputClass).append(Line)
                }
            }
            if (!excludedClassesFromAssertionGeneration?.isEmpty()) {
                append(Line)
                append("Input classes excluded from assertions generation:\n")
                for (excludedClass in excludedClassesFromAssertionGeneration) {
                    append(INDENT).append(excludedClass.type.typeName).append(Line)
                }
            }
        }
    }

    private boolean generationError() {
        return exception != null
    }

    private boolean nothingGenerated() {
        return generatedCustomAssertionFileNames.isEmpty()
    }

    void reportEntryPointGeneration(AssertionsEntryPointType assertionsEntryPointType,
                                    File assertionsEntryPointFile) {
        this.assertionsEntryPointFilesByType.put(assertionsEntryPointType, assertionsEntryPointFile)
    }

    void reportInputClassesNotFound(Set<Class<?>> classes, String[] inputClassNames) {
        Set<String> classesFound = newTreeSet()
        for (Class<?> clazz : classes) {
            classesFound.add(clazz.getName())
        }
        for (String inputClass : inputClassNames) {
            if (!classesFound.contains(inputClass)) {
                inputClassesNotFound.add(inputClass)
            }
        }
    }

    void registerUserTemplate(String userTemplateDescription) {
        userTemplates.add(userTemplateDescription)
    }
}
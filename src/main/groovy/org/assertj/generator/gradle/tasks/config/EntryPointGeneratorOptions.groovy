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
import org.assertj.assertions.generator.AssertionsEntryPointType

import java.util.stream.Collectors

/**
 * Used to represent the different {@link AssertionsEntryPointType} values in a simpler and more "gradle-like"
 * way when configuring.
 */
@EqualsAndHashCode
class EntryPointGeneratorOptions {

    private final Set<AssertionsEntryPointType> _entryPoints = EnumSet.of(AssertionsEntryPointType.STANDARD)

    Set<AssertionsEntryPointType> getEntryPoints() {
        _entryPoints.asImmutable()
    }

    /**
     * An optional package name for the Assertions entry point class. If omitted, the package will be determined
     * heuristically from the generated assertions.
     * @return Package string for entry point classes
     */
    String classPackage

    private void forEnum(boolean value, AssertionsEntryPointType e) {
        if (value) {
            _entryPoints.add(e)
        } else {
            _entryPoints.remove(e)
        }
    }

    void only(AssertionsEntryPointType... rest) {
        _entryPoints.clear()

        _entryPoints.addAll(rest.toList())
    }

    void only(String... rest) {
        def asEnums = Arrays.stream(rest)
                .map { it.toUpperCase() }
                .map { AssertionsEntryPointType.valueOf(it) }
                .collect(Collectors.toSet())

        only(asEnums as AssertionsEntryPointType[])
    }

    /**
     * @see AssertionsEntryPointType#STANDARD
     */
    boolean getStandard() {
        entryPoints.contains(AssertionsEntryPointType.STANDARD)
    }

    void setStandard(boolean value) {
        forEnum(value, AssertionsEntryPointType.STANDARD)
    }

    /**
     * @see AssertionsEntryPointType#BDD
     */
    boolean getBdd() {
        entryPoints.contains(AssertionsEntryPointType.BDD)
    }

    void setBdd(boolean value) {
        forEnum(value, AssertionsEntryPointType.BDD)
    }

    /**
     * @see AssertionsEntryPointType#SOFT
     */
    boolean getSoft() {
        entryPoints.contains(AssertionsEntryPointType.SOFT)
    }

    void setSoft(boolean value) {
        forEnum(value, AssertionsEntryPointType.SOFT)
    }

    /**
     * @see AssertionsEntryPointType#BDD_SOFT
     */
    boolean getBddSoft() {
        entryPoints.contains(AssertionsEntryPointType.BDD_SOFT)
    }

    void setBddSoft(boolean value) {
        forEnum(value, AssertionsEntryPointType.BDD_SOFT)
    }

    /**
     * @see AssertionsEntryPointType#JUNIT_SOFT
     */
    boolean getJunitSoft() {
        entryPoints.contains(AssertionsEntryPointType.JUNIT_SOFT)
    }

    void setJunitSoft(boolean value) {
        forEnum(value, AssertionsEntryPointType.JUNIT_SOFT)
    }

    /**
     * @see AssertionsEntryPointType#JUNIT_BDD_SOFT
     */
    boolean getJunitBddSoft() {
        entryPoints.contains(AssertionsEntryPointType.JUNIT_BDD_SOFT)
    }

    void setJunitBddSoft(boolean value) {
        forEnum(value, AssertionsEntryPointType.JUNIT_BDD_SOFT)
    }

    /**
     * @see AssertionsEntryPointType#AUTO_CLOSEABLE_SOFT
     */
    boolean getAutoCloseableSoft() {
        entryPoints.contains(AssertionsEntryPointType.AUTO_CLOSEABLE_SOFT)
    }

    void setAutoCloseableSoft(boolean value) {
        forEnum(value, AssertionsEntryPointType.AUTO_CLOSEABLE_SOFT)
    }

    /**
     * @see AssertionsEntryPointType#AUTO_CLOSEABLE_BDD_SOFT
     */
    boolean getAutoCloseableBddSoft() {
        entryPoints.contains(AssertionsEntryPointType.AUTO_CLOSEABLE_BDD_SOFT)
    }

    void setAutoCloseableBddSoft(boolean value) {
        forEnum(value, AssertionsEntryPointType.AUTO_CLOSEABLE_BDD_SOFT)
    }
}

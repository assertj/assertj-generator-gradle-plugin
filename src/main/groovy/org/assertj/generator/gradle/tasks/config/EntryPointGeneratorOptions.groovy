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

import com.google.common.collect.Iterators
import groovy.transform.EqualsAndHashCode
import org.assertj.assertions.generator.AssertionsEntryPointType

/**
 * Used to represent the different {@link AssertionsEntryPointType} values in a simpler and more "gradle-like"
 * way when configuring.
 */
@EqualsAndHashCode
class EntryPointGeneratorOptions implements Iterable<AssertionsEntryPointType>, Serializable {

    private final Set<AssertionsEntryPointType> entryPoints =
            EnumSet.noneOf(AssertionsEntryPointType)

    @Override
    Iterator<AssertionsEntryPointType> iterator() {
        Iterators.unmodifiableIterator(entryPoints.iterator())
    }

    private void forEnum(boolean value, AssertionsEntryPointType e) {
        if (value) {
            entryPoints.add(e)
        } else {
            entryPoints.remove(e)
        }
    }

    void setOnly(AssertionsEntryPointType... rest) {
        entryPoints.clear()

        if (rest.length > 0) {
            entryPoints.addAll(EnumSet.copyOf(Arrays.asList(rest)))
        }
    }

    /**
     * Generate Assertions entry point class.
     */
    boolean getStandard() {
        entryPoints.contains(AssertionsEntryPointType.STANDARD)
    }

    void setStandard(boolean value) {
        forEnum(value, AssertionsEntryPointType.STANDARD)
    }

    /**
     * Generate generating BDD Assertions entry point class.
     */
    boolean getBdd() {
        entryPoints.contains(AssertionsEntryPointType.BDD)
    }

    void setBdd(boolean value) {
        forEnum(value, AssertionsEntryPointType.BDD)
    }

    /**
     * Generate generating JUnit Soft Assertions entry point class.
     */
    boolean getJunitSoft() {
        entryPoints.contains(AssertionsEntryPointType.JUNIT_SOFT)
    }

    void setJunitSoft(boolean value) {
        forEnum(value, AssertionsEntryPointType.JUNIT_SOFT)
    }

    /**
     * Generate generating Soft Assertions entry point class.
     */
    boolean getSoft() {
        entryPoints.contains(AssertionsEntryPointType.SOFT)
    }

    void setSoft(boolean value) {
        forEnum(value, AssertionsEntryPointType.SOFT)
    }

    private void writeObject(ObjectOutputStream s) throws IOException {
        s.writeObject(entryPoints)
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        entryPoints.clear()
        entryPoints.addAll((Set<AssertionsEntryPointType>)s.readObject())
    }
}

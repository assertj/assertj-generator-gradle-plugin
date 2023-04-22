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
package org.assertj.generator.gradle.tasks.config;

import groovy.lang.Closure;
import java.util.Collection;
import org.assertj.assertions.generator.AssertionGenerator;
import org.assertj.assertions.generator.AssertionsEntryPointType;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.util.ConfigureUtil;

/**
 * Defines useful parameters and options for configuration of the
 * {@link AssertionGenerator}.
 */
public interface AssertJGeneratorOptions {

    /**
     * Generate generating Soft Assertions entry point class.
     * @return templates value, never {@code null}
     */
    Templates getTemplates();

    /**
     * Method used for improving configuration DSL
     *
     * @param closure applied via {@link org.gradle.util.ConfigureUtil#configure(Closure, Object)}
     * @return {@code this}
     */
    default AssertJGeneratorOptions templates(Closure closure) {
        ConfigureUtil.configure(closure, getTemplates());
        return this;
    }

    /**
     * Contains configuration regarding the "Assertion" entry point class generation. By default, only the "standard"
     * version is generated.
     * <br>
     * See the
     * <a href="http://joel-costigliola.github.io/assertj/assertj-assertions-generator.html#generated-entry-points">assertj generator docs</a>
     * on entry points.
     *
     * @return entry points configuration
     */
    EntryPointGeneratorOptions getEntryPoints();

    /**
     * Helper method for simplifying usage in build scripts
     * @param values Values to set
     */
    void setEntryPoints(AssertionsEntryPointType... values);

    /**
     * Exposed for build scripts
     * @param values String values passed from a build script
     */
    default void setEntryPoints(Collection<String> values) {
        getEntryPoints().only(values.stream()
                .map(String::toUpperCase)
                .map(AssertionsEntryPointType::valueOf)
                .toList()
                .toArray(new AssertionsEntryPointType[0]));
    }

    /**
     * Used to change "entry point" class generation.
     *
     * @param closure Applied via {@link org.gradle.util.ConfigureUtil#configure(Closure, Object)}
     * @return this
     */
    AssertJGeneratorOptions entryPoints(Closure closure);

    /**
     * The root directory where all generated files will be placed (within sub-folders for packages).
     * @return File the output directory for {@code sourceSet}
     */
    DirectoryProperty getOutputDir();

    /**
     * Flag specifying whether to generate hierarchical assertions. The default is false.
     * @return true if generating hierarchical
     */
    Boolean isHierarchical();

    /**
     * Set the hierarchical value, {@code null} is not allowed here as by setting the value, it has a value.
     * @param hierarchical true if hierarchical
     *
     * @see #isHierarchical()
     */
    void setHierarchical(boolean hierarchical);

    /**
     * Skip generating classes, handy way to disable temporarily.
     * @return true if skipping this set (default: {@code true}, set to {@code false} before configuration)
     */
    boolean isSkip();

    /**
     * Set the config param {@code skip} true
     * @param skip true if skipping
     *
     * @see #isSkip()
     */
    void setSkip(boolean skip);
}

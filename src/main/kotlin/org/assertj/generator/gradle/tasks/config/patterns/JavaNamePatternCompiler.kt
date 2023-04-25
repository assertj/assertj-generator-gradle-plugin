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
package org.assertj.generator.gradle.tasks.config.patterns

private const val WILDCARD_MARKER = "!!WILDCARD_MARKER!!"

internal object JavaNamePatternCompiler {
  fun compilePattern(pattern: String): Regex {
    val escapedPackageCharacters = pattern
      .replace(".", "\\.")
      .replace("$", "\\$")

    // Order matters because if we do single "*" first we double remove the "**"
    val withWildcardMarkers = escapedPackageCharacters
      .replace("**", "[\\w\\.]$WILDCARD_MARKER")
      .replace("*", "\\w$WILDCARD_MARKER")

    val withRegexWildcards = withWildcardMarkers.replace(WILDCARD_MARKER, "*")

    return Regex(withRegexWildcards)
  }
}

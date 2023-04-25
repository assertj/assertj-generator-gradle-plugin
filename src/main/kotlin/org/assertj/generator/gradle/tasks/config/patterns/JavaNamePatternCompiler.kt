package org.assertj.generator.gradle.tasks.config.patterns

internal object JavaNamePatternCompiler {
  fun compilePattern(pattern: String): Regex {
    return Regex(
      pattern
        .replace(".", "\\.")
        .replace("$", "\\$")
        .replace("**", "[\\w\\.]!!WILDCARD_MARKER!!")
        .replace("*", "\\w!!WILDCARD_MARKER!!")
        .replace("!!WILDCARD_MARKER!!", "*")
    )
  }
}

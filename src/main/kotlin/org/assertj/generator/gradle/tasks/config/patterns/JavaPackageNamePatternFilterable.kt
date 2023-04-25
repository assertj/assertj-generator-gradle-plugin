package org.assertj.generator.gradle.tasks.config.patterns

import com.google.common.reflect.TypeToken
import org.assertj.generator.gradle.tasks.config.patterns.JavaNamePatternCompiler.compilePattern

/**
 * Implements a similar construction to [org.gradle.api.tasks.util.PatternFilterable] that will match
 * [TypeToken] instances.
 */
open class JavaPackageNamePatternFilterable internal constructor() :
  JavaIdentifierNamePatternFilterableBase<String, JavaPackageNamePatternFilterable>() {

  override fun compilePatterns(patterns: Iterable<String>): Sequence<PatternPredicate<String>> {
    return patterns.asSequence().map { PackagePatternPredicate.compile(it) }
  }

  internal data class PackagePatternPredicate(
    override val pattern: String,
    private val namePattern: Regex,
  ) : PatternPredicate<String> {
    override fun test(t: String): Boolean {
      return namePattern.matches(t)
    }

    companion object {
      fun compile(pattern: String) = PackagePatternPredicate(pattern, compilePattern(pattern))
    }
  }
}

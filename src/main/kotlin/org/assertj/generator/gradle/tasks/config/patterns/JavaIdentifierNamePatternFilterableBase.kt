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

import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.function.Predicate

@Suppress("UNCHECKED_CAST")
sealed class JavaIdentifierNamePatternFilterableBase<T, SELF>
  where SELF : JavaIdentifierNamePatternFilterableBase<T, SELF> {
  private var includePredicates = mutableSetOf<PatternPredicate<T>>()
  private var excludePredicates = mutableSetOf<PatternPredicate<T>>()

  internal abstract fun compilePatterns(patterns: Iterable<String>): Sequence<PatternPredicate<T>>

  internal fun asPredicate(): Predicate<T> {
    return Predicate { t ->
      (includePredicates.isEmpty() || includePredicates.any { it.test(t) }) &&
        (excludePredicates.isEmpty() || excludePredicates.none { it.test(t) })
    }
  }

  fun setIncludes(includes: Iterable<String>): SELF {
    this.includePredicates = compilePatterns(includes).toMutableSet()
    return this as SELF
  }

  fun setExcludes(excludes: Iterable<String>): SELF {
    this.excludePredicates = compilePatterns(excludes).toMutableSet()
    return this as SELF
  }

  fun include(vararg includes: String): SELF {
    this.includePredicates += compilePatterns(includes.asIterable())
    return this as SELF
  }

  fun include(includes: Iterable<String>): SELF {
    this.includePredicates += compilePatterns(includes)
    return this as SELF
  }

  fun exclude(vararg excludes: String): SELF {
    this.excludePredicates += compilePatterns(excludes.asIterable())
    return this as SELF
  }

  fun exclude(excludes: Iterable<String>): SELF {
    this.excludePredicates += compilePatterns(excludes)
    return this as SELF
  }

  internal interface PatternPredicate<T> : Predicate<T> {
    val pattern: String
  }

  @Throws(IOException::class)
  protected fun writeObjectImpl(o: ObjectOutputStream) {
    o.writeInt(includePredicates.size)
    for (pattern in includePredicates.map { it.pattern }) {
      o.writeUTF(pattern)
    }

    o.writeInt(excludePredicates.size)
    for (pattern in excludePredicates.map { it.pattern }) {
      o.writeUTF(pattern)
    }
  }

  @Throws(IOException::class, ClassNotFoundException::class)
  protected fun readObjectImpl(i: ObjectInputStream) {
    val includesSize = i.readInt()
    setIncludes((0 until includesSize).map { i.readUTF() })

    val excludesSize = i.readInt()
    setExcludes((0 until excludesSize).map { i.readUTF() })
  }
}

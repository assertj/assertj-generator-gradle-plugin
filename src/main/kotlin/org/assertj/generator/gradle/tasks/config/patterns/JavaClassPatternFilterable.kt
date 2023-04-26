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

import com.google.common.reflect.TypeToken
import org.assertj.generator.gradle.tasks.config.patterns.JavaNamePatternCompiler.compilePattern
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

/**
 * Implements a similar construction to [org.gradle.api.tasks.util.PatternFilterable] that will match
 * [TypeToken] instances.
 */
open class JavaClassPatternFilterable internal constructor() :
  JavaIdentifierNamePatternFilterableBase<TypeToken<*>, JavaClassPatternFilterable>(), Serializable {

  override fun compilePatterns(patterns: Iterable<String>): Sequence<PatternPredicate<TypeToken<*>>> {
    return patterns.asSequence().map { TypeNamePredicate.compile(it) }
  }

  @Throws(IOException::class)
  protected fun writeObject(o: ObjectOutputStream) {
    super.writeObjectImpl(o)
  }

  @Throws(IOException::class, ClassNotFoundException::class)
  protected fun readObject(i: ObjectInputStream) {
    super.readObjectImpl(i)
  }

  internal data class TypeNamePredicate(
    override val pattern: String,
    private val namePattern: Regex,
  ) : PatternPredicate<TypeToken<*>> {
    override fun test(t: TypeToken<*>): Boolean {
      return namePattern.matches(t.type.typeName)
    }

    companion object {
      fun compile(pattern: String): TypeNamePredicate = TypeNamePredicate(pattern, compilePattern(pattern))
    }
  }

  companion object {
    private const val serialVersionUID = 9418631994L
  }
}

package org.assertj.generator.gradle.tasks.config

import com.google.common.collect.Iterators
import org.assertj.assertions.generator.AssertionsEntryPointType
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.util.Arrays
import java.util.Locale

/**
 * Used to represent the different [AssertionsEntryPointType] values in a simpler and more "gradle-like"
 * way when configuring.
 */
class EntryPointGeneratorOptions internal constructor() : Iterable<AssertionsEntryPointType>, Serializable {
  private val entryPoints = mutableSetOf(AssertionsEntryPointType.STANDARD)

  /**
   * An optional package name for the Assertions entry point class. If omitted, the package will be determined
   * heuristically from the generated assertions.
   */
  var classPackage: String? = null

  override fun iterator(): MutableIterator<AssertionsEntryPointType> {
    return Iterators.unmodifiableIterator(entryPoints.iterator())
  }

  private fun forEnum(isEnabled: Boolean, value: AssertionsEntryPointType) {
    if (isEnabled) {
      entryPoints.add(value)
    } else {
      entryPoints.remove(value)
    }
  }

  fun only(vararg rest: AssertionsEntryPointType) {
    only(Arrays.stream(rest).toList())
  }

  fun only(rest: Collection<AssertionsEntryPointType>) {
    entryPoints.clear()
    entryPoints.addAll(rest)
  }

  fun only(vararg rest: String) {
    val asEnums = rest.asSequence()
      .map { it.uppercase(Locale.getDefault()) }
      .map { AssertionsEntryPointType.valueOf(it) }
      .toSet()
    only(asEnums)
  }

  /**
   * Generate Assertions entry point class.
   */
  var standard: Boolean
    get() = AssertionsEntryPointType.STANDARD in entryPoints
    set(value) {
      forEnum(value, AssertionsEntryPointType.STANDARD)
    }

  /**
   * Generate generating BDD Assertions entry point class.
   */
  var bdd: Boolean
    get() = AssertionsEntryPointType.BDD in entryPoints
    set(value) {
      forEnum(value, AssertionsEntryPointType.BDD)
    }

  /**
   * Generate generating JUnit Soft Assertions entry point class.
   */
  var junitSoft: Boolean
    get() = AssertionsEntryPointType.JUNIT_SOFT in entryPoints
    set(value) {
      forEnum(value, AssertionsEntryPointType.JUNIT_SOFT)
    }

  /**
   * Generate generating Soft Assertions entry point class.
   */
  var soft: Boolean
    get() = AssertionsEntryPointType.SOFT in entryPoints
    set(value) {
      forEnum(value, AssertionsEntryPointType.SOFT)
    }

  @Throws(IOException::class)
  private fun writeObject(output: ObjectOutputStream) {
    output.writeInt(entryPoints.size)
    for (entryPoint in entryPoints) {
      output.writeObject(entryPoint)
    }

    output.writeUTF(classPackage ?: NO_CONTENT_PRESENT)
  }

  @Throws(IOException::class)
  private fun readObject(input: ObjectInputStream) {
    val entryPointsSize = input.readInt()

    entryPoints.clear()
    entryPoints += (0 until entryPointsSize).map {
      input.readObject() as AssertionsEntryPointType
    }

    this.classPackage = input.readUTF().takeIf { it != NO_CONTENT_PRESENT }
  }

  companion object {
    private const val serialVersionUID = 183539L

    private const val NO_CONTENT_PRESENT = "!! NO_CONTENT_PRESENT !!"
  }
}

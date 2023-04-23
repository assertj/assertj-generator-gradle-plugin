package org.assertj.generator.gradle.tasks.config

import org.assertj.assertions.generator.AssertionsEntryPointType
import java.util.EnumSet

/**
 * Used to represent the different [AssertionsEntryPointType] values in a simpler and more "gradle-like"
 * way when configuring.
 */
open class EntryPointGeneratorOptions {
  val entryPoints: Set<AssertionsEntryPointType> get() = _entryPoints

  private val _entryPoints = EnumSet.of(AssertionsEntryPointType.STANDARD)

  /**
   * An optional package name for the Assertions entry point class. If omitted, the package will be determined
   * heuristically from the generated assertions.
   * @return Package string for entry point classes
   */
  var classPackage: String? = null

  private fun forEnum(value: Boolean, e: AssertionsEntryPointType) {
    if (value) {
      _entryPoints.add(e)
    } else {
      _entryPoints.remove(e)
    }
  }

  fun only(vararg rest: AssertionsEntryPointType) {
    _entryPoints.clear()
    _entryPoints.addAll(rest)
  }

  fun only(vararg rest: String) {
    val asEnums = rest.asSequence()
      .map { it.uppercase() }
      .map { AssertionsEntryPointType.valueOf(it) }
      .toSet()
    only(rest = asEnums.toTypedArray())
  }

  /**
   * @see AssertionsEntryPointType.STANDARD
   */
  var standard: Boolean
    get() = entryPoints.contains(AssertionsEntryPointType.STANDARD)
    set(value) {
      forEnum(value, AssertionsEntryPointType.STANDARD)
    }

  /**
   * @see AssertionsEntryPointType.BDD
   */
  var bdd: Boolean
    get() = entryPoints.contains(AssertionsEntryPointType.BDD)
    set(value) {
      forEnum(value, AssertionsEntryPointType.BDD)
    }

  /**
   * @see AssertionsEntryPointType.SOFT
   */
  var soft: Boolean
    get() = entryPoints.contains(AssertionsEntryPointType.SOFT)
    set(value) {
      forEnum(value, AssertionsEntryPointType.SOFT)
    }

  /**
   * @see AssertionsEntryPointType.BDD_SOFT
   */
  var bddSoft: Boolean
    get() = entryPoints.contains(AssertionsEntryPointType.BDD_SOFT)
    set(value) {
      forEnum(value, AssertionsEntryPointType.BDD_SOFT)
    }

  /**
   * @see AssertionsEntryPointType.JUNIT_SOFT
   */
  var junitSoft: Boolean
    get() = entryPoints.contains(AssertionsEntryPointType.JUNIT_SOFT)
    set(value) {
      forEnum(value, AssertionsEntryPointType.JUNIT_SOFT)
    }

  /**
   * @see AssertionsEntryPointType.JUNIT_BDD_SOFT
   */
  var junitBddSoft: Boolean
    get() = entryPoints.contains(AssertionsEntryPointType.JUNIT_BDD_SOFT)
    set(value) {
      forEnum(value, AssertionsEntryPointType.JUNIT_BDD_SOFT)
    }

  /**
   * @see AssertionsEntryPointType.AUTO_CLOSEABLE_SOFT
   */
  var autoCloseableSoft: Boolean
    get() = entryPoints.contains(AssertionsEntryPointType.AUTO_CLOSEABLE_SOFT)
    set(value) {
      forEnum(value, AssertionsEntryPointType.AUTO_CLOSEABLE_SOFT)
    }

  /**
   * @see AssertionsEntryPointType.AUTO_CLOSEABLE_BDD_SOFT
   */
  var autoCloseableBddSoft: Boolean
    get() = entryPoints.contains(AssertionsEntryPointType.AUTO_CLOSEABLE_BDD_SOFT)
    set(value) {
      forEnum(value, AssertionsEntryPointType.AUTO_CLOSEABLE_BDD_SOFT)
    }
}

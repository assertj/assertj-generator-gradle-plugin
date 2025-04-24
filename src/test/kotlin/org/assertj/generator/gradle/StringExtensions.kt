package org.assertj.generator.gradle

internal fun String.capitalized(): String {
  return this[0].uppercaseChar() + this.substring(1)
}

package org.assertj.generator.gradle

import org.intellij.lang.annotations.Language
import java.io.File
import java.nio.file.Path
import kotlin.io.path.writeText

internal fun File.writeJava(@Language("java") content: String): Unit = writeText(content.trimIndent())
internal fun Path.writeJava(@Language("java") content: String): Unit = writeText(content.trimIndent())

internal fun File.writeGroovy(@Language("groovy") content: String): Unit = writeText(content.trimIndent())
internal fun Path.writeGroovy(@Language("groovy") content: String): Unit = writeText(content.trimIndent())

internal fun File.writeKotlin(@Language("kotlin") content: String): Unit = writeText(content.trimIndent())
internal fun Path.writeKotlin(@Language("kotlin") content: String): Unit = writeText(content.trimIndent())

package org.assertj.generator.gradle

import org.intellij.lang.annotations.Language
import java.io.File
import java.nio.file.Path
import kotlin.io.path.writeText

fun File.writeJava(@Language("java") content: String): Unit = writeText(content.trimIndent())
fun Path.writeJava(@Language("java") content: String): Unit = writeText(content.trimIndent())

fun File.writeGroovy(@Language("groovy") content: String): Unit = writeText(content.trimIndent())
fun Path.writeGroovy(@Language("groovy") content: String): Unit = writeText(content.trimIndent())

fun File.writeKotlin(@Language("kotlin") content: String): Unit = writeText(content.trimIndent())
fun Path.writeKotlin(@Language("kotlin") content: String): Unit = writeText(content.trimIndent())

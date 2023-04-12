package org.assertj.generator.gradle.tasks.config

import org.assertj.assertions.generator.Template
import org.assertj.assertions.generator.Template.Type
import java.io.File
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

class SerializedTemplate private constructor(
  type: Type,
  content: String?,
  file: File?,
) : Serializable {
  var type: Type = type
    private set

  var content: String? = content
    private set

  var file: File? = file
    private set

  fun maybeLoadTemplate(): Template? {
    return when {
      content != null -> Template(type, content!!)
      file != null -> Template(type, file!!.readText())
      else -> null
    }
  }

  @Throws(IOException::class)
  private fun writeObject(output: ObjectOutputStream) {
    output.writeObject(type)
    output.writeUTF(content ?: NO_CONTENT_PRESENT)
    output.writeObject(file ?: NO_FILE_PRESENT)
  }

  @Throws(IOException::class)
  private fun readObject(input: ObjectInputStream) {
    this.type = input.readObject() as Type

    val content = input.readUTF()
    if (content != NO_CONTENT_PRESENT) {
      this.content = content
    }

    val file = input.readObject() as File
    if (file != NO_FILE_PRESENT) {
      this.file = file
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as SerializedTemplate

    if (type != other.type) return false
    if (content != other.content) return false
    return file == other.file
  }

  override fun hashCode(): Int {
    var result = type.hashCode()
    result = 31 * result + (content?.hashCode() ?: 0)
    result = 31 * result + (file?.hashCode() ?: 0)
    return result
  }

  override fun toString(): String {
    return "SerializedTemplate(type=$type, content=$content, file=$file)"
  }

  companion object {
    private const val NO_CONTENT_PRESENT = "!! NO_CONTENT_PRESENT !!"
    private val NO_FILE_PRESENT = File("!! NO_FILE_PRESENT !!")

    private const val serialVersionUID = 207186L

    fun template(type: Type, content: String) = SerializedTemplate(type, content, file = null)
    fun file(type: Type, file: File) = SerializedTemplate(type, content = null, file)
  }
}

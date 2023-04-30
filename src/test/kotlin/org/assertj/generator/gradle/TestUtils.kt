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
package org.assertj.generator.gradle

import org.intellij.lang.annotations.Language
import java.io.File

/**
 * Utilities used in test scripts
 */
internal object TestUtils {
  @JvmStatic
  fun File.writeBuildFile(@Language("Groovy") content: String) {
    writeGroovy(
      """
      // Add required plugins and source sets to the sub projects
      plugins { 
          id "org.assertj.generator" // Note must use this syntax
          id "java"
      } 
      
      ${content.trimIndent()}
      
      repositories {
          mavenCentral()
      }
      
      dependencies {
          implementation group: 'javax.annotation', name: 'javax.annotation-api', version: '1.3.2'
          
          // https://mvnrepository.com/artifact/org.assertj/assertj-core
          testImplementation group: 'org.assertj', name: 'assertj-core', version: '3.24.2'
          
          testImplementation group: 'junit', name: 'junit', version: '4.13.1'
      }
      """.trimIndent()
    )
  }

  @JvmStatic
  fun File.writeDefaultBuildFile(): Unit = this.writeBuildFile("")

  fun File.writeBuildKts(@Language("Kotlin") content: String = "") {
    writeKotlin(
      """
      plugins { 
          id("org.assertj.generator")
          `java`
          id("org.jetbrains.kotlin.jvm") version "1.8.10"
      } 
      
      $content
      
      repositories {
          mavenCentral()
      }
      
      dependencies {
          implementation(kotlin("stdlib"))
          implementation("javax.annotation:javax.annotation-api:1.3.2")

          testImplementation("org.assertj:assertj-core:3.24.2")
          testImplementation("junit:junit:4.13.1")
      }
      """.trimIndent()
    )
  }

  fun File.writeDefaultBuildKts(): Unit = this.writeBuildKts("")
}

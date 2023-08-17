plugins {
  id("org.assertj.generator")
  `java`
  kotlin("jvm") version "1.9.0"
}

buildscript {
  dependencies {
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.0")
  }
}

repositories {
  mavenCentral()
}

dependencies {
  implementation(kotlin("stdlib"))
  implementation("javax.annotation:javax.annotation-api:1.3.2")

  testImplementation("org.assertj:assertj-core:3.24.2")
  testImplementation("junit:junit:4.13.1")
}
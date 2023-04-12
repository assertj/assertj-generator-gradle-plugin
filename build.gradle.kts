import com.gradle.publish.PublishTask.GRADLE_PUBLISH_KEY
import com.gradle.publish.PublishTask.GRADLE_PUBLISH_SECRET

plugins {
  id("groovy")
  id("org.jetbrains.kotlin.jvm") version "1.8.20"
  id("io.gitlab.arturbosch.detekt") version "1.22.0"
  id("com.gradle.plugin-publish") version "1.2.0"
  id("java-gradle-plugin")
}

val setupPluginUpload by tasks.registering {
  // TODO switch this to use github action
  if ("CI" in System.getenv() && "TRAVIS" in System.getenv()) {
    // Used for publishing from Travis

    val key = System.getenv(GRADLE_PUBLISH_KEY) ?: System.getProperty(GRADLE_PUBLISH_KEY)
    val secret = System.getenv(GRADLE_PUBLISH_SECRET) ?: System.getProperty(GRADLE_PUBLISH_SECRET)

    if (key == null || secret == null) {
      error("GRADLE_PUBLISH_KEY and/or GRADLE_PUBLISH_SECRET are not defined environment variables")
    }

    System.setProperty(GRADLE_PUBLISH_KEY, key)
    System.setProperty(GRADLE_PUBLISH_SECRET, secret)
  }
}

tasks.named("publishPlugins") {
  dependsOn(setupPluginUpload)
}

group = "org.assertj"
version = "2.2.0-SNAPSHOT"

gradlePlugin {
  website.set("https://github.com/assertj/assertj-generator-gradle-plugin")
  vcsUrl.set("https://github.com/assertj/assertj-generator-gradle-plugin.git")

  plugins {
    create("assertJGeneratorPlugin") {
      id = "org.assertj.generator"

      displayName = "AssertJ Generator Gradle Plugin"
      description = "Run the AssertJ generator against your source files to generate test sources."

      tags.set(listOf("assertj", "testing", "generator"))

      implementationClass = "org.assertj.generator.gradle.AssertJGeneratorGradlePlugin"
    }
  }
}

repositories {
  mavenCentral()
}

dependencies {
  api(gradleApi())

  api("org.assertj:assertj-core:3.24.2")
  api("org.assertj:assertj-assertions-generator:2.2.1")

  implementation(gradleKotlinDsl())
  implementation("com.google.guava:guava:31.1-jre")

  testImplementation(gradleTestKit())
  testImplementation("junit:junit:4.13.2")
}

detekt {
    parallel = true
    autoCorrect = true

    buildUponDefaultConfig = true // preconfigure defaults
    config = files("$rootDir/config/detekt-config.yml")

    allRules = false // activate all available (even unstable) rules.
}

dependencies {
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.22.0")
}


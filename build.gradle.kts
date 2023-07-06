import com.gradle.publish.PublishTask.GRADLE_PUBLISH_KEY
import com.gradle.publish.PublishTask.GRADLE_PUBLISH_KEY_ENV
import com.gradle.publish.PublishTask.GRADLE_PUBLISH_SECRET
import com.gradle.publish.PublishTask.GRADLE_PUBLISH_SECRET_ENV

plugins {
  id("java-gradle-plugin")
  id("org.jetbrains.kotlin.jvm") version "1.9.0"

  id("io.gitlab.arturbosch.detekt") version "1.23.0"
  id("com.autonomousapps.dependency-analysis") version "1.20.0"

  id("com.gradle.plugin-publish") version "1.2.0"
}

val setupPluginUpload by tasks.registering {
  if (System.getenv("GITHUB_ACTION") == null) return@registering

  val key = System.getenv(GRADLE_PUBLISH_KEY_ENV) ?: System.getProperty(GRADLE_PUBLISH_KEY)
  val secret = System.getenv(GRADLE_PUBLISH_SECRET_ENV) ?: System.getProperty(GRADLE_PUBLISH_SECRET)

  if (key == null || secret == null) {
    error("GRADLE_PUBLISH_KEY and/or GRADLE_PUBLISH_SECRET are not defined environment variables")
  }

  System.setProperty(GRADLE_PUBLISH_KEY, key)
  System.setProperty(GRADLE_PUBLISH_SECRET, secret)

  // This is the git tag for a release
  val githubRefName = System.getenv("GITHUB_REF_NAME")?.trimStart('v')
  version = checkNotNull(githubRefName) { "No GITHUB_REF_NAME env value defined" }
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
  implementation(kotlin("stdlib"))

  api(gradleApi())
  api("com.google.code.findbugs:jsr305:3.0.2")
  api("org.assertj:assertj-assertions-generator:2.2.1")

  implementation(gradleKotlinDsl())
  implementation("com.google.guava:guava:32.1.1-jre")

  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.0")
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api:1.9.0")
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api:1.9.0") {
    capabilities {
     requireCapability("org.jetbrains.kotlin:kotlin-gradle-plugin-api-gradle76")
    }
  }

  testCompileOnly("org.jetbrains:annotations:24.0.1")

  testImplementation(localGroovy())
  testImplementation(gradleTestKit())
  testImplementation("junit:junit:4.13.2")
  testImplementation("org.assertj:assertj-core:3.24.2")
}

tasks.check {
  dependsOn(tasks.named("projectHealth"))
}

detekt {
  parallel = true
  autoCorrect = true

  buildUponDefaultConfig = true // preconfigure defaults
  config.from("$rootDir/config/detekt-config.yml")

  allRules = false // activate all available (even unstable) rules.
}

dependencies {
  detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.0")
}
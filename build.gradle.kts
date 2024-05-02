import com.gradle.publish.PublishTask.GRADLE_PUBLISH_KEY
import com.gradle.publish.PublishTask.GRADLE_PUBLISH_KEY_ENV
import com.gradle.publish.PublishTask.GRADLE_PUBLISH_SECRET
import com.gradle.publish.PublishTask.GRADLE_PUBLISH_SECRET_ENV

plugins {
  id("java-gradle-plugin")
  id("org.jetbrains.kotlin.jvm") version "1.9.23"

  id("io.gitlab.arturbosch.detekt") version "1.23.6"
  id("com.autonomousapps.dependency-analysis") version "1.31.0"

  id("com.gradle.plugin-publish") version "1.2.1"
}

val setupPluginUpload by tasks.registering {
  if (!providers.environmentVariable("GITHUB_ACTION").isPresent) return@registering

  val key = providers.environmentVariable(GRADLE_PUBLISH_KEY_ENV)
    .orElse(providers.systemProperty(GRADLE_PUBLISH_KEY))

  val secret = providers.environmentVariable(GRADLE_PUBLISH_SECRET_ENV)
    .orElse(providers.systemProperty(GRADLE_PUBLISH_SECRET))

  if (!key.isPresent || !secret.isPresent) {
    error("GRADLE_PUBLISH_KEY and/or GRADLE_PUBLISH_SECRET are not defined environment or system variables")
  }

  // This is the git tag for a release
  val githubRefName = providers.environmentVariable("GITHUB_REF_NAME").map { it.trimStart('v') }
  version = checkNotNull(githubRefName.orNull) { "No GITHUB_REF_NAME env value defined" }
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
  implementation("com.google.guava:guava:33.2.0-jre")

  val kotlinVersion = "1.6.21"
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api:$kotlinVersion")

  testCompileOnly("org.jetbrains:annotations:24.1.0")

  testImplementation(localGroovy())
  testImplementation(gradleTestKit())

  val junitVersion = "5.10.2"
  testImplementation("org.assertj:assertj-core:3.25.3")
  testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
  testImplementation("net.navatwo:gradle-plugin-better-testing-junit5:0.0.0")

  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
}

tasks.test {
  useJUnitPlatform()

  jvmArgs("-Djunit.jupiter.extensions.autodetection.enabled=true")
}

sourceSets {
  test {
    resources {
      srcDir("src/test/projects")
    }
  }
}

tasks.check {
  dependsOn(tasks.named("projectHealth"))
}

detekt {
  parallel = true
  autoCorrect = true

  buildUponDefaultConfig = true // preconfigure defaults
  config.from("$rootDir/detekt.yaml")

  allRules = false // activate all available (even unstable) rules.
}

dependencies {
  detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.6")
}
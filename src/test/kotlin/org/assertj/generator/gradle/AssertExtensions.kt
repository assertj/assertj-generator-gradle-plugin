package org.assertj.generator.gradle

import org.assertj.core.api.AbstractObjectAssert
import org.gradle.testkit.runner.BuildTask
import org.gradle.testkit.runner.TaskOutcome

fun <SELF : AbstractObjectAssert<SELF, BuildTask?>> SELF.isSuccessful(): SELF = isOutcome(TaskOutcome.SUCCESS)
fun <SELF : AbstractObjectAssert<SELF, BuildTask?>> SELF.isUpToDate(): SELF = isOutcome(TaskOutcome.UP_TO_DATE)

private fun <SELF : AbstractObjectAssert<SELF, BuildTask?>> SELF.isOutcome(outcome: TaskOutcome): SELF {
  extracting { it?.outcome }.`as` { "task.outcome == $outcome" }.isEqualTo(outcome)
  return this
}

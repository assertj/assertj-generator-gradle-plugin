package org.assertj.generator.gradle

import org.assertj.core.api.AbstractObjectAssert
import org.gradle.testkit.runner.BuildTask
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.testkit.runner.TaskOutcome.FROM_CACHE
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.gradle.testkit.runner.TaskOutcome.UP_TO_DATE

internal fun <SELF : AbstractObjectAssert<SELF, BuildTask?>> SELF.isSuccessful(): SELF = isOutcome(SUCCESS)

internal fun <SELF : AbstractObjectAssert<SELF, BuildTask?>> SELF.isSuccessOrCached(): SELF = isOutcome(
  SUCCESS,
  FROM_CACHE
)

internal fun <SELF : AbstractObjectAssert<SELF, BuildTask?>> SELF.isUpToDate(): SELF = isOutcome(UP_TO_DATE)

private fun <SELF : AbstractObjectAssert<SELF, BuildTask?>> SELF.isOutcome(vararg outcomes: TaskOutcome): SELF {
  extracting { it?.outcome }
    .`as` { "task.outcome in { ${outcomes.joinToString(", ")} }" }
    .isIn(outcomes.toSet())
  return this
}

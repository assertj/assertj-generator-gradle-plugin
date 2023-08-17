package org.example

import org.junit.Test
import org.example.Assertions.assertThat

internal class HelloWorldTest {
  @Test
  fun check() {
    val hw = HelloWorld2()
    assertThat(hw).hasFoo(-1)
        .isNotBrains()
  }
}
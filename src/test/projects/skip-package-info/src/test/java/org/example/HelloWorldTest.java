package org.example;

import org.junit.Test;

import static org.example.HelloWorldAssert.assertThat;

public final class HelloWorldTest {

  @Test
  public void check() {
    HelloWorld hw = new HelloWorld();
    assertThat(hw).hasFoo(-1)
        .doesNotHaveSomeBrains();
  }
}
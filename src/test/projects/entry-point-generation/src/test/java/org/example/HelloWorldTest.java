package org.example;

import org.junit.*;

public class HelloWorldTest {
  @Test
  public void test() {
    HelloWorld ut = new HelloWorld();
    HelloWorldAssert.assertThat(ut).doesNotHaveSomeBrains();

    ut.hasSomeBrains = true;
    HelloWorldAssert.assertThat(ut).hasSomeBrains();
  }
}
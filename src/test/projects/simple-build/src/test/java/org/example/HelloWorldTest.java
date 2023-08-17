package org.example;

import org.junit.Test;

import static org.example.Assertions.assertThat;

public final class HelloWorldTest {

  @Test
  public void check() {
    HelloWorld hw = new HelloWorld();
    assertThat(hw).hasFoo(-1)
        .doesNotHaveSomeBrains();
  }

  @Test
  public void checkClassWithNested() {
    OtherNestedWorld ow = new OtherNestedWorld();
    assertThat(ow).isNotBrainy();
  }

  @Test
  public void checkNestedClass() {
    OtherNestedWorld.Nested n = new OtherNestedWorld.Nested();
    assertThat(n).isNotSomethingElse();
  }
}
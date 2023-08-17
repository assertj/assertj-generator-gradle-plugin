package org.example;

import org.junit.Test;

import static org.example.H1Assert.assertThat;

public final class H1Test {
  @Test
  public void check() {
    H1 hw = new H1();
    assertThat(hw).isNotBrainy();
  }
}
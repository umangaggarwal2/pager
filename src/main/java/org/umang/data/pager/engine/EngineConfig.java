package org.umang.data.pager.engine;

public record EngineConfig(String dir) {

  private static final EngineConfig DEFAULT = new EngineConfig("/tmp/");

  public static EngineConfig defaults() {
    return DEFAULT;
  }
}

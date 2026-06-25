package org.umang.data.pager.engine;

public record EngineState(IndexState indexState, StorageState storageState,
                          RuntimeState runtimeState) {

  public record IndexState(long size) {

  }

  public record StorageState(long fileSize) {

  }

  public record RuntimeState(long lastReadLatencyNanos, long lastWriteLatencyNanos) {

  }
}

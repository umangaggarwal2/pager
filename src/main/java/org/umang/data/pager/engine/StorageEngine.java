package org.umang.data.pager.engine;

import static org.umang.data.pager.index.InMemoryIndex.NOT_FOUND;

import java.io.IOException;
import java.nio.file.Path;
import org.umang.data.pager.codec.Record;
import org.umang.data.pager.engine.EngineState.IndexState;
import org.umang.data.pager.engine.EngineState.RuntimeState;
import org.umang.data.pager.engine.EngineState.StorageState;
import org.umang.data.pager.index.InMemoryIndex;
import org.umang.data.pager.index.OffsetIndex;
import org.umang.data.pager.storage.AppendOnlyStore;
import org.umang.data.pager.storage.MmapFileStore;

public class StorageEngine implements KeyValueStore, StateRegistry {

  private final OffsetIndex index;
  private final AppendOnlyStore store;

  private long lastReadLatencyNanos = 0;
  private long lastWriteLatencyNanos = 0;

  public StorageEngine(OffsetIndex index, AppendOnlyStore store) throws IOException {
    this.index = index;
    this.store = store;
  }

  public static StorageEngine open(String filename) throws IOException {
    return open(filename, EngineConfig.defaults());
  }

  public static StorageEngine open(String filename, EngineConfig engineConfig) throws IOException {
    Path filePath = Path.of(engineConfig.dir() + filename.trim() + ".pager.db");
    return new StorageEngine(new InMemoryIndex(), new MmapFileStore(filePath));
  }

  @Override
  public byte[] get(byte[] key) throws IOException {
    long startNanos = System.nanoTime();
    try {
      long offset = index.getOffset(key);
      if (offset == NOT_FOUND) {
        return null;
      }
      Record record = store.read(offset);
      if (record == null) {
        return null;
      }
      return record.value();
    } finally {
      lastReadLatencyNanos = System.nanoTime() - startNanos;
    }
  }

  @Override
  public void put(byte[] key, byte[] value) throws IOException {
    long startNanos = System.nanoTime();
    try {
      Record record = new Record(key, value);
      long offset = store.write(record);
      index.putOffset(key, offset);
    } finally {
      lastWriteLatencyNanos = System.nanoTime() - startNanos;
    }
  }

  @Override
  public void delete(byte[] key) throws IOException {
    long startNanos = System.nanoTime();
    try {
      long offset = index.getOffset(key);
      if (offset == NOT_FOUND) {
        return;
      }
      store.delete(offset);
      index.delete(key);
    } finally {
      lastWriteLatencyNanos = System.nanoTime() - startNanos;
    }
  }

  @Override
  public void close() throws IOException {
    index.close();
    store.close();
  }

  @Override
  public EngineState state() throws IOException {
    return new EngineState(new IndexState(index.size()), new StorageState(store.fileSize()),
        new RuntimeState(lastReadLatencyNanos, lastWriteLatencyNanos));
  }
}

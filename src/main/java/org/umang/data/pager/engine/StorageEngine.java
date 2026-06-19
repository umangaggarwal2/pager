package org.umang.data.pager.engine;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Optional;
import org.umang.data.pager.storage.DataStore;
import org.umang.data.pager.storage.MmapFileStore;

public class StorageEngine implements KeyValueStore {

  private final DataStore indexStore;
  private final DataStore dataStore;

  public StorageEngine(DataStore indexStore, DataStore dataStore) throws IOException {
    this.indexStore = indexStore;
    this.dataStore = dataStore;
  }

  public static StorageEngine open(String filename) throws IOException {
    Path filePath = Path.of("/tmp/" + filename.trim() + ".pager.db");
    Path indexPath = Path.of("/tmp/" + filename.trim() + ".pager.index");
    return new StorageEngine(new MmapFileStore(indexPath), new MmapFileStore(filePath));
  }

  @Override
  public Optional<ByteBuffer> get(ByteBuffer key) throws IOException {
    return Optional.empty();
  }

  @Override
  public void put(ByteBuffer key, ByteBuffer value) throws IOException {

  }

  @Override
  public void delete(ByteBuffer key) throws IOException {

  }

  @Override
  public void close() throws IOException {

  }
}

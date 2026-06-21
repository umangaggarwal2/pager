package org.umang.data.pager.engine;

import java.io.IOException;

public interface KeyValueStore extends AutoCloseable {

  byte[] get(byte[] key) throws IOException;

  void put(byte[] key, byte[] value) throws IOException;

  void delete(byte[] key) throws IOException;

  @Override
  void close() throws IOException;
}

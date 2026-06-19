package org.umang.data.pager.engine;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Optional;

public interface KeyValueStore extends AutoCloseable {

  Optional<ByteBuffer> get(ByteBuffer key) throws IOException;

  void put(ByteBuffer key, ByteBuffer value) throws IOException;

  void delete(ByteBuffer key) throws IOException;

  @Override
  void close() throws IOException;
}

package org.umang.data.pager.index;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class InMemoryIndex implements OffsetIndex {

  public static final long NOT_FOUND = -1;

  private final Map<ByteBuffer, Long> offsetMap;

  public InMemoryIndex() {
    this.offsetMap = new HashMap<>();
  }

  @Override
  public long getOffset(byte[] key) {
    return offsetMap.getOrDefault(ByteBuffer.wrap(Arrays.copyOf(key, key.length)), -1L);
  }

  @Override
  public void putOffset(byte[] key, long offset) {
    offsetMap.put(ByteBuffer.wrap(Arrays.copyOf(key, key.length)), offset);
  }

  @Override
  public void delete(byte[] key) {
    offsetMap.remove(ByteBuffer.wrap(Arrays.copyOf(key, key.length)));
  }

  @Override
  public void close() throws IOException {
  }
}

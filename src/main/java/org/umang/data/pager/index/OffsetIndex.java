package org.umang.data.pager.index;

import java.io.IOException;

public interface OffsetIndex extends AutoCloseable {

  long getOffset(byte[] key);
  void putOffset(byte[] key, long offset);
  void delete(byte[] key);
  long size();

  @Override
  void close() throws IOException;
}

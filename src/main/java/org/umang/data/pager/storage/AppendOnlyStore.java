package org.umang.data.pager.storage;

import java.io.IOException;
import org.umang.data.pager.codec.Record;

public interface AppendOnlyStore extends AutoCloseable {

  Record read(long offset);

  long write(Record record) throws IOException;

  void delete(long offset);

  @Override
  void close() throws IOException;
}

package org.umang.data.pager.storage;

import java.io.IOException;
import org.umang.data.pager.codec.Record;

public interface DataStore {

  Record read(long offset);

  long write(Record record) throws IOException;

  void delete(long offset);
}

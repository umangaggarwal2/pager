package org.umang.data.pager.engine;

import java.io.IOException;

public interface StateRegistry extends AutoCloseable {

  EngineState state() throws IOException;
}

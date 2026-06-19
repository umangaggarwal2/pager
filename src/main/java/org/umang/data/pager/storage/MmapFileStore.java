package org.umang.data.pager.storage;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.umang.data.pager.codec.Record;
import org.umang.data.pager.codec.RecordCodec;

public class MmapFileStore implements DataStore {

  private static final int SHIFT = 30;
  private static final int MASK = 0x3FFFFFFF;
  // 1 GB chunks
  private static final long CHUNK_SIZE = 1L << SHIFT;
  private static final int MAX_CHUNKS = 1024;

  private final FileChannel fileChannel;
  private final MappedByteBuffer[] chunks = new MappedByteBuffer[MAX_CHUNKS];
  private int allocatedChunks = 0;
  private long globalAppendOffset = 0;


  public MmapFileStore(Path path) throws IOException {
    fileChannel = FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE,
        StandardOpenOption.CREATE);
    growStorage();
  }

  private void growStorage() throws IOException {
    if (allocatedChunks >= MAX_CHUNKS) {
      throw new IllegalStateException("Storage engine hit absolute max Terabyte limit!");
    }
    long newLength = (long) (allocatedChunks + 1) * CHUNK_SIZE;
    fileChannel.truncate(newLength);
    chunks[allocatedChunks] =
        fileChannel.map(FileChannel.MapMode.READ_WRITE, (long) allocatedChunks * CHUNK_SIZE,
            CHUNK_SIZE);
    allocatedChunks++;
  }

  @Override
  public Record read(long globalOffset) {
    int chunkIndex = (int) (globalOffset >> SHIFT);
    int localOffset = (int) (globalOffset & MASK);
    MappedByteBuffer chunk = chunks[chunkIndex];
    return RecordCodec.decode(chunk, localOffset);
  }

  @Override
  public long write(Record record) throws IOException {
    int len = RecordCodec.encodedSize(record);
    long space = CHUNK_SIZE - (globalAppendOffset & MASK);
    if (len > space) {
      globalAppendOffset += space;
    }
    int chunkIndex = (int) (globalAppendOffset >> SHIFT);
    while (chunkIndex >= allocatedChunks) {
      growStorage();
    }
    int localOffset = (int) (globalAppendOffset & MASK);
    MappedByteBuffer chunk = chunks[chunkIndex];
    RecordCodec.encode(chunk, localOffset, record);
    long recordOffset = globalAppendOffset;
    globalAppendOffset += len;
    return recordOffset;
  }

  @Override
  public void delete(long globalOffset) {
    int chunkIndex = (int) (globalOffset >> SHIFT);
    int localOffset = (int) (globalOffset & MASK);
    MappedByteBuffer chunk = chunks[chunkIndex];
    RecordCodec.markTombstone(chunk, localOffset);
  }
}

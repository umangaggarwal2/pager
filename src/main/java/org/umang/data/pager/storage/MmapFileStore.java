package org.umang.data.pager.storage;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.umang.data.pager.codec.Record;
import org.umang.data.pager.codec.RecordCodec;

public class MmapFileStore implements AppendOnlyStore {

  private static final int SHIFT = 30;
  private static final int MASK = 0x3FFFFFFF;
  // 1 GB chunks
  private static final long CHUNK_SIZE = 1L << SHIFT;
  private static final int MAX_CHUNKS = 1024;
  private static final ByteBuffer ZERO = ByteBuffer.wrap(new byte[] {0});

  private final MappedByteBuffer[] chunks = new MappedByteBuffer[MAX_CHUNKS];
  private final FileChannel fileChannel;
  private int allocatedChunks = 0;
  private long globalAppendOffset = 0;
  private Superblock superblock;


  public MmapFileStore(Path path) throws IOException {
    fileChannel = FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE,
        StandardOpenOption.CREATE);
    initSuperblock();
  }

  @Override
  public Record read(long globalOffset) {
    if (globalOffset < Superblock.SIZE || globalOffset >= globalAppendOffset) {
      throw new IllegalArgumentException("Offset outside file");
    }
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
    superblock.setGlobalAppendIndex(globalAppendOffset);
    return recordOffset;
  }

  @Override
  public void delete(long globalOffset) {
    if (globalOffset < Superblock.SIZE || globalOffset >= globalAppendOffset) {
      throw new IllegalArgumentException("Offset outside file");
    }
    int chunkIndex = (int) (globalOffset >> SHIFT);
    int localOffset = (int) (globalOffset & MASK);
    MappedByteBuffer chunk = chunks[chunkIndex];
    RecordCodec.markTombstone(chunk, localOffset);
  }

  private void initSuperblock() throws IOException {
    if (fileChannel.size() == 0) {
      growStorage();
      superblock = Superblock.initialize(chunks[0]);
      this.globalAppendOffset = superblock.globalAppendIndex();
    } else {
      superblock = Superblock.extract(
          fileChannel.map(MapMode.READ_WRITE, Superblock.OFFSET_MAGIC, Superblock.SIZE));
      this.globalAppendOffset = superblock.globalAppendIndex();
      initStorage();
    }
  }

  private void growStorage() throws IOException {
    validateStorage();
    long requiredSize = (long) (allocatedChunks + 1) * CHUNK_SIZE;
    ensureCapacity(requiredSize);
    // superblock is first 21 bytes of chunks[0]
    chunks[allocatedChunks] =
        fileChannel.map(FileChannel.MapMode.READ_WRITE, (long) allocatedChunks * CHUNK_SIZE,
            CHUNK_SIZE);
    allocatedChunks++;
  }

  private void initStorage() throws IOException {
    allocatedChunks = Math.max(1, (int) ((globalAppendOffset + CHUNK_SIZE - 1) >> SHIFT));
    validateStorage();
    // superblock is first 21 bytes of chunks[0]
    for (int i = 0; i < allocatedChunks; i++) {
      chunks[i] =
          fileChannel.map(FileChannel.MapMode.READ_WRITE, (long) i * CHUNK_SIZE, CHUNK_SIZE);
    }
  }

  private void validateStorage() {
    if (allocatedChunks >= MAX_CHUNKS) {
      throw new IllegalStateException("Storage engine hit absolute max Terabyte limit!");
    }
  }

  private void ensureCapacity(long requiredSize) throws IOException {
    if (fileChannel.size() >= requiredSize) {
      return;
    }
    fileChannel.position(requiredSize - 1);
    ZERO.rewind();
    fileChannel.write(ZERO);
  }

  @Override
  public void close() throws IOException {
    fileChannel.close();
  }
}

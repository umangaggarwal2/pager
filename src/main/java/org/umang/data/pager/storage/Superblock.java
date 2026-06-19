package org.umang.data.pager.storage;

import java.nio.MappedByteBuffer;

public final class Superblock {

  // "PAGER"
  public static final long MAGIC = 0x7061676572L;
  public static final int VERSION = 1;
  public static final long INITIAL_APPEND_INDEX = 21L;
  public static final int SIZE = 21;

  public static final int OFFSET_MAGIC = 0;
  private static final int OFFSET_VERSION = 8;
  private static final int OFFSET_GLOBAL_APPEND_INDEX = 12;
  private static final int OFFSET_CLEAN_SHUTDOWN = 20;

  private final MappedByteBuffer buf;

  public Superblock(MappedByteBuffer buf) {
    this.buf = buf;
  }

  public static Superblock initialize(MappedByteBuffer buf) {
    Superblock superBlock = new Superblock(buf);
    superBlock.buf.putLong(OFFSET_MAGIC, MAGIC);
    superBlock.buf.putInt(OFFSET_VERSION, VERSION);
    superBlock.buf.putLong(OFFSET_GLOBAL_APPEND_INDEX, INITIAL_APPEND_INDEX);
    return superBlock;
  }

  public static Superblock extract(MappedByteBuffer buf) {
    Superblock superblock = new Superblock(buf);
    superblock.validate();
    return superblock;
  }

  public long magic() {
    return buf.getLong(OFFSET_MAGIC);
  }

  public int version() {
    return buf.getInt(OFFSET_VERSION);
  }

  public long globalAppendIndex() {
    return buf.getLong(OFFSET_GLOBAL_APPEND_INDEX);
  }

  public void setGlobalAppendIndex(long globalAppendIndex) {
    buf.putLong(OFFSET_GLOBAL_APPEND_INDEX, globalAppendIndex);
  }

  public byte cleanShutdown() {
    return buf.get(OFFSET_CLEAN_SHUTDOWN);
  }

  public void setCleanShutdown(boolean clean) {
    buf.put(OFFSET_CLEAN_SHUTDOWN, (byte) (clean ? 1 : 0));
  }

  private void validate() {
    if (magic() != MAGIC) {
      throw new IllegalStateException(
          "Bad magic: 0x" + Long.toHexString(magic()) + " (expected 0x" + Long.toHexString(MAGIC)
              + "). " + "This file is not a Pager store.");
    }
    if (version() != VERSION) {
      throw new IllegalStateException(
          "Unsupported on-disk format version: " + version() + " (this build supports v" + VERSION
              + ").");
    }
  }
}

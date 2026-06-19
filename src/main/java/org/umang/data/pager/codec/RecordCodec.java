package org.umang.data.pager.codec;

import java.nio.MappedByteBuffer;

/**
 * Encodes and decodes (key, value) records as a length-prefixed byte stream:
 * <pre>
 *   [tombstone  : 1B]
 *   [keyLen     : 4B]
 *   [keyBytes   : keyLen]
 *   [valueLen   : 4B]
 *   [valueBytes : valueLen]
 * </pre>
 */
public final class RecordCodec {

  public static final int TOMBSTONE_BYTES = 1;
  public static final int LEN_FIELD_BYTES = Integer.BYTES;

  private RecordCodec() {
  }

  public static int encodedSize(Record record) {
    return TOMBSTONE_BYTES + LEN_FIELD_BYTES + record.key().length + LEN_FIELD_BYTES
        + record.value().length;
  }

  public static void encode(MappedByteBuffer buf, int index, Record record) {
    int curIdx = index;
    byte[] key = record.key();
    byte[] val = record.value();
    byte tombstone = 0;
    buf.put(curIdx, tombstone);
    curIdx += TOMBSTONE_BYTES;
    buf.putInt(curIdx, key.length);
    curIdx += LEN_FIELD_BYTES;
    buf.put(curIdx, key, 0, key.length);
    curIdx += key.length;
    buf.putInt(curIdx, val.length);
    curIdx += LEN_FIELD_BYTES;
    buf.put(curIdx, val, 0, val.length);
  }

  public static Record decode(MappedByteBuffer buf, int index) {
    int curIdx = index;
    byte tombstone = buf.get(curIdx);
    if (tombstone != 0) {
      return null;
    }
    curIdx += TOMBSTONE_BYTES;
    int keyLen = buf.getInt(curIdx);
    curIdx += LEN_FIELD_BYTES;
    byte[] key = new byte[keyLen];
    buf.get(curIdx, key, 0, keyLen);
    curIdx += keyLen;
    int valLen = buf.getInt(curIdx);
    curIdx += LEN_FIELD_BYTES;
    byte[] value = new byte[valLen];
    buf.get(curIdx, value, 0, valLen);
    return new Record(key, value);
  }

  public static void markTombstone(MappedByteBuffer buf, int index) {
    byte tombstone = 1;
    buf.put(index, tombstone);
  }
}
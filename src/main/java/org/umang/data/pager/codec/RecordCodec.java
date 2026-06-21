package org.umang.data.pager.codec;

import java.nio.ByteBuffer;
import java.util.zip.CRC32;

/**
 * Encodes and decodes (key, value) records as a length-prefixed byte stream:
 * <pre>
 *   [magic]     : 1B]
 *   [tombstone  : 1B]
 *   [totalLen   : 4B]
 *   [keyLen     : 4B]
 *   [valueLen   : 4B]
 *   [CRC32      : 4B]
 *   [keyBytes   : keyLen]
 *   [valueBytes : valueLen]
 * </pre>
 */
public final class RecordCodec {

  public static final int HEADER_BYTES = 18;
  public static final int MAGIC_FIELD_BYTES = 1;
  public static final int TOMBSTONE_BYTES = 1;
  public static final int LEN_FIELD_BYTES = Integer.BYTES;
  public static final int CHECKSUM_FIELD_BYTES = Integer.BYTES;

  // MAGIC = 'R'
  private static final int MAGIC = 0x52;
  private static final int CRC_OFFSET =
      MAGIC_FIELD_BYTES + TOMBSTONE_BYTES + LEN_FIELD_BYTES + LEN_FIELD_BYTES + LEN_FIELD_BYTES;

  private RecordCodec() {
  }

  public static int encodedSize(Record record) {
    return HEADER_BYTES + record.key().length + record.value().length;
  }

  public static void encode(final ByteBuffer buf, final int index, Record record) {
    int curIdx = index;

    byte[] key = record.key();
    byte[] value = record.value();

    int totalLength = encodedSize(record);

    buf.put(curIdx, (byte) MAGIC);
    curIdx += MAGIC_FIELD_BYTES;

    buf.put(curIdx, (byte) 0);
    curIdx += TOMBSTONE_BYTES;

    buf.putInt(curIdx, totalLength);
    curIdx += LEN_FIELD_BYTES;

    buf.putInt(curIdx, key.length);
    curIdx += LEN_FIELD_BYTES;

    buf.putInt(curIdx, value.length);
    curIdx += LEN_FIELD_BYTES;

    // Placeholder CRC
    int crcOffset = curIdx;
    buf.putInt(curIdx, 0);
    curIdx += CHECKSUM_FIELD_BYTES;

    buf.put(curIdx, key);
    curIdx += key.length;

    buf.put(curIdx, value);

    int crc = computeCrc(buf, index, totalLength);
    buf.putInt(crcOffset, crc);
  }

  public static Record decode(final ByteBuffer buf, final int index) {
    if (!validateHeader(buf, index)) {
      return null;
    }
    int curIdx = index + MAGIC_FIELD_BYTES + TOMBSTONE_BYTES;
    int totalLen = buf.getInt(curIdx);
    if ((long) index + totalLen > buf.limit()) {
      return null;
    }
    curIdx += LEN_FIELD_BYTES;
    int keyLen = buf.getInt(curIdx);
    if (keyLen < 0) {
      return null;
    }
    curIdx += LEN_FIELD_BYTES;
    int valLen = buf.getInt(curIdx);
    if (valLen < 0) {
      return null;
    }
    long expectedLength = (long) HEADER_BYTES + keyLen + valLen;
    if (totalLen != expectedLength) {
      return null;
    }
    curIdx += LEN_FIELD_BYTES;
    int crc = buf.getInt(curIdx);
    curIdx += CHECKSUM_FIELD_BYTES;
    byte[] key = new byte[keyLen];
    buf.get(curIdx, key, 0, keyLen);
    curIdx += keyLen;
    byte[] value = new byte[valLen];
    buf.get(curIdx, value, 0, valLen);
    if (crc != computeCrc(buf, index, totalLen)) {
      return null;
    }
    return new Record(key, value);
  }

  public static void markTombstone(final ByteBuffer buf, final int index) {
    if (!validateHeader(buf, index)) {
      return;
    }
    int curIdx = index + MAGIC_FIELD_BYTES;
    byte tombstone = 1;
    buf.put(curIdx, tombstone);
  }

  private static boolean validateHeader(final ByteBuffer buf, final int index) {
    int curIdx = index;
    byte magic = buf.get(curIdx);
    if (magic != MAGIC) {
      return false;
    }
    curIdx += MAGIC_FIELD_BYTES;
    byte tombstone = buf.get(curIdx);
    if (tombstone != 0) {
      return false;
    }
    curIdx += TOMBSTONE_BYTES;
    int totalLen = buf.getInt(curIdx);
    return totalLen >= HEADER_BYTES;
  }

  private static int computeCrc(final ByteBuffer buf, final int index, final int totalLength) {
    CRC32 crc32 = new CRC32();

    // before CRC
    ByteBuffer first = buf.duplicate();
    first.position(index);
    first.limit(index + CRC_OFFSET);
    crc32.update(first);

    // after CRC
    ByteBuffer second = buf.duplicate();
    second.position(index + HEADER_BYTES);
    second.limit(index + totalLength);
    crc32.update(second);

    return (int) crc32.getValue();
  }
}
// Auto-generated: DO NOT EDIT

package io.openmessaging.demo.net.jpountz.lz4;

import io.openmessaging.demo.net.jpountz.util.ByteBufferUtils;
import io.openmessaging.demo.net.jpountz.util.SafeUtils;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static io.openmessaging.demo.net.jpountz.lz4.LZ4Utils.hash;
import static io.openmessaging.demo.net.jpountz.lz4.LZ4Utils.hash64k;

/**
 * Compressor. 
 */
final class LZ4JavaSafeCompressor extends LZ4Compressor {

  public static final LZ4Compressor INSTANCE = new LZ4JavaSafeCompressor();

  static int compress64k(byte[] src, int srcOff, int srcLen, byte[] dest, int destOff, int destEnd) {
    final int srcEnd = srcOff + srcLen;
    final int srcLimit = srcEnd - LZ4Constants.LAST_LITERALS;
    final int mflimit = srcEnd - LZ4Constants.MF_LIMIT;

    int sOff = srcOff, dOff = destOff;

    int anchor = sOff;

    if (srcLen >= LZ4Constants.MIN_LENGTH) {

      final short[] hashTable = new short[LZ4Constants.HASH_TABLE_SIZE_64K];

      ++sOff;

      main:
      while (true) {

        // find a match
        int forwardOff = sOff;

        int ref;
        int step = 1;
        int searchMatchNb = 1 << LZ4Constants.SKIP_STRENGTH;
        do {
          sOff = forwardOff;
          forwardOff += step;
          step = searchMatchNb++ >>> LZ4Constants.SKIP_STRENGTH;

          if (forwardOff > mflimit) {
            break main;
          }

          final int h = hash64k(SafeUtils.readInt(src, sOff));
          ref = srcOff + SafeUtils.readShort(hashTable, h);
          SafeUtils.writeShort(hashTable, h, sOff - srcOff);
        } while (!LZ4SafeUtils.readIntEquals(src, ref, sOff));

        // catch up
        final int excess = LZ4SafeUtils.commonBytesBackward(src, ref, sOff, srcOff, anchor);
        sOff -= excess;
        ref -= excess;

        // sequence == refsequence
        final int runLen = sOff - anchor;

        // encode literal length
        int tokenOff = dOff++;

        if (dOff + runLen + (2 + 1 + LZ4Constants.LAST_LITERALS) + (runLen >>> 8) > destEnd) {
          throw new LZ4Exception("maxDestLen is too small");
        }

        if (runLen >= LZ4Constants.RUN_MASK) {
          SafeUtils.writeByte(dest, tokenOff, LZ4Constants.RUN_MASK << LZ4Constants.ML_BITS);
          dOff = LZ4SafeUtils.writeLen(runLen - LZ4Constants.RUN_MASK, dest, dOff);
        } else {
          SafeUtils.writeByte(dest, tokenOff, runLen << LZ4Constants.ML_BITS);
        }

        // copy literals
        LZ4SafeUtils.wildArraycopy(src, anchor, dest, dOff, runLen);
        dOff += runLen;

        while (true) {
          // encode offset
          SafeUtils.writeShortLE(dest, dOff, (short) (sOff - ref));
          dOff += 2;

          // count nb matches
          sOff += LZ4Constants.MIN_MATCH;
          ref += LZ4Constants.MIN_MATCH;
          final int matchLen = LZ4SafeUtils.commonBytes(src, ref, sOff, srcLimit);
          if (dOff + (1 + LZ4Constants.LAST_LITERALS) + (matchLen >>> 8) > destEnd) {
            throw new LZ4Exception("maxDestLen is too small");
          }
          sOff += matchLen;

          // encode match len
          if (matchLen >= LZ4Constants.ML_MASK) {
            SafeUtils.writeByte(dest, tokenOff, SafeUtils.readByte(dest, tokenOff) | LZ4Constants.ML_MASK);
            dOff = LZ4SafeUtils.writeLen(matchLen - LZ4Constants.ML_MASK, dest, dOff);
          } else {
            SafeUtils.writeByte(dest, tokenOff, SafeUtils.readByte(dest, tokenOff) | matchLen);
          }

          // test end of chunk
          if (sOff > mflimit) {
            anchor = sOff;
            break main;
          }

          // fill table
          SafeUtils.writeShort(hashTable, hash64k(SafeUtils.readInt(src, sOff - 2)), sOff - 2 - srcOff);

          // test next position
          final int h = hash64k(SafeUtils.readInt(src, sOff));
          ref = srcOff + SafeUtils.readShort(hashTable, h);
          SafeUtils.writeShort(hashTable, h, sOff - srcOff);

          if (!LZ4SafeUtils.readIntEquals(src, sOff, ref)) {
            break;
          }

          tokenOff = dOff++;
          SafeUtils.writeByte(dest, tokenOff, 0);
        }

        // prepare next loop
        anchor = sOff++;
      }
    }

    dOff = LZ4SafeUtils.lastLiterals(src, anchor, srcEnd - anchor, dest, dOff, destEnd);
    return dOff - destOff;
  }

  @Override
  public int compress(byte[] src, final int srcOff, int srcLen, byte[] dest, final int destOff, int maxDestLen) {

    SafeUtils.checkRange(src, srcOff, srcLen);
    SafeUtils.checkRange(dest, destOff, maxDestLen);
    final int destEnd = destOff + maxDestLen;

    if (srcLen < LZ4Constants.LZ4_64K_LIMIT) {
      return compress64k(src, srcOff, srcLen, dest, destOff, destEnd);
    }

    final int srcEnd = srcOff + srcLen;
    final int srcLimit = srcEnd - LZ4Constants.LAST_LITERALS;
    final int mflimit = srcEnd - LZ4Constants.MF_LIMIT;

    int sOff = srcOff, dOff = destOff;
    int anchor = sOff++;

    final int[] hashTable = new int[LZ4Constants.HASH_TABLE_SIZE];
    Arrays.fill(hashTable, anchor);

    main:
    while (true) {

      // find a match
      int forwardOff = sOff;

      int ref;
      int step = 1;
      int searchMatchNb = 1 << LZ4Constants.SKIP_STRENGTH;
      int back;
      do {
        sOff = forwardOff;
        forwardOff += step;
        step = searchMatchNb++ >>> LZ4Constants.SKIP_STRENGTH;

        if (forwardOff > mflimit) {
          break main;
        }

        final int h = hash(SafeUtils.readInt(src, sOff));
        ref = SafeUtils.readInt(hashTable, h);
        back = sOff - ref;
        SafeUtils.writeInt(hashTable, h, sOff);
      } while (back >= LZ4Constants.MAX_DISTANCE || !LZ4SafeUtils.readIntEquals(src, ref, sOff));


      final int excess = LZ4SafeUtils.commonBytesBackward(src, ref, sOff, srcOff, anchor);
      sOff -= excess;
      ref -= excess;

      // sequence == refsequence
      final int runLen = sOff - anchor;

      // encode literal length
      int tokenOff = dOff++;

      if (dOff + runLen + (2 + 1 + LZ4Constants.LAST_LITERALS) + (runLen >>> 8) > destEnd) {
        throw new LZ4Exception("maxDestLen is too small");
      }

      if (runLen >= LZ4Constants.RUN_MASK) {
        SafeUtils.writeByte(dest, tokenOff, LZ4Constants.RUN_MASK << LZ4Constants.ML_BITS);
        dOff = LZ4SafeUtils.writeLen(runLen - LZ4Constants.RUN_MASK, dest, dOff);
      } else {
        SafeUtils.writeByte(dest, tokenOff, runLen << LZ4Constants.ML_BITS);
      }

      // copy literals
      LZ4SafeUtils.wildArraycopy(src, anchor, dest, dOff, runLen);
      dOff += runLen;

      while (true) {
        // encode offset
        SafeUtils.writeShortLE(dest, dOff, back);
        dOff += 2;

        // count nb matches
        sOff += LZ4Constants.MIN_MATCH;
        final int matchLen = LZ4SafeUtils.commonBytes(src, ref + LZ4Constants.MIN_MATCH, sOff, srcLimit);
        if (dOff + (1 + LZ4Constants.LAST_LITERALS) + (matchLen >>> 8) > destEnd) {
          throw new LZ4Exception("maxDestLen is too small");
        }
        sOff += matchLen;

        // encode match len
        if (matchLen >= LZ4Constants.ML_MASK) {
          SafeUtils.writeByte(dest, tokenOff, SafeUtils.readByte(dest, tokenOff) | LZ4Constants.ML_MASK);
          dOff = LZ4SafeUtils.writeLen(matchLen - LZ4Constants.ML_MASK, dest, dOff);
        } else {
          SafeUtils.writeByte(dest, tokenOff, SafeUtils.readByte(dest, tokenOff) | matchLen);
        }

        // test end of chunk
        if (sOff > mflimit) {
          anchor = sOff;
          break main;
        }

        // fill table
        SafeUtils.writeInt(hashTable, hash(SafeUtils.readInt(src, sOff - 2)), sOff - 2);

        // test next position
        final int h = hash(SafeUtils.readInt(src, sOff));
        ref = SafeUtils.readInt(hashTable, h);
        SafeUtils.writeInt(hashTable, h, sOff);
        back = sOff - ref;

        if (back >= LZ4Constants.MAX_DISTANCE || !LZ4SafeUtils.readIntEquals(src, ref, sOff)) {
          break;
        }

        tokenOff = dOff++;
        SafeUtils.writeByte(dest, tokenOff, 0);
      }

      // prepare next loop
      anchor = sOff++;
    }

    dOff = LZ4SafeUtils.lastLiterals(src, anchor, srcEnd - anchor, dest, dOff, destEnd);
    return dOff - destOff;
  }


  static int compress64k(ByteBuffer src, int srcOff, int srcLen, ByteBuffer dest, int destOff, int destEnd) {
    final int srcEnd = srcOff + srcLen;
    final int srcLimit = srcEnd - LZ4Constants.LAST_LITERALS;
    final int mflimit = srcEnd - LZ4Constants.MF_LIMIT;

    int sOff = srcOff, dOff = destOff;

    int anchor = sOff;

    if (srcLen >= LZ4Constants.MIN_LENGTH) {

      final short[] hashTable = new short[LZ4Constants.HASH_TABLE_SIZE_64K];

      ++sOff;

      main:
      while (true) {

        // find a match
        int forwardOff = sOff;

        int ref;
        int step = 1;
        int searchMatchNb = 1 << LZ4Constants.SKIP_STRENGTH;
        do {
          sOff = forwardOff;
          forwardOff += step;
          step = searchMatchNb++ >>> LZ4Constants.SKIP_STRENGTH;

          if (forwardOff > mflimit) {
            break main;
          }

          final int h = hash64k(ByteBufferUtils.readInt(src, sOff));
          ref = srcOff + SafeUtils.readShort(hashTable, h);
          SafeUtils.writeShort(hashTable, h, sOff - srcOff);
        } while (!LZ4ByteBufferUtils.readIntEquals(src, ref, sOff));

        // catch up
        final int excess = LZ4ByteBufferUtils.commonBytesBackward(src, ref, sOff, srcOff, anchor);
        sOff -= excess;
        ref -= excess;

        // sequence == refsequence
        final int runLen = sOff - anchor;

        // encode literal length
        int tokenOff = dOff++;

        if (dOff + runLen + (2 + 1 + LZ4Constants.LAST_LITERALS) + (runLen >>> 8) > destEnd) {
          throw new LZ4Exception("maxDestLen is too small");
        }

        if (runLen >= LZ4Constants.RUN_MASK) {
          ByteBufferUtils.writeByte(dest, tokenOff, LZ4Constants.RUN_MASK << LZ4Constants.ML_BITS);
          dOff = LZ4ByteBufferUtils.writeLen(runLen - LZ4Constants.RUN_MASK, dest, dOff);
        } else {
          ByteBufferUtils.writeByte(dest, tokenOff, runLen << LZ4Constants.ML_BITS);
        }

        // copy literals
        LZ4ByteBufferUtils.wildArraycopy(src, anchor, dest, dOff, runLen);
        dOff += runLen;

        while (true) {
          // encode offset
          ByteBufferUtils.writeShortLE(dest, dOff, (short) (sOff - ref));
          dOff += 2;

          // count nb matches
          sOff += LZ4Constants.MIN_MATCH;
          ref += LZ4Constants.MIN_MATCH;
          final int matchLen = LZ4ByteBufferUtils.commonBytes(src, ref, sOff, srcLimit);
          if (dOff + (1 + LZ4Constants.LAST_LITERALS) + (matchLen >>> 8) > destEnd) {
            throw new LZ4Exception("maxDestLen is too small");
          }
          sOff += matchLen;

          // encode match len
          if (matchLen >= LZ4Constants.ML_MASK) {
            ByteBufferUtils.writeByte(dest, tokenOff, ByteBufferUtils.readByte(dest, tokenOff) | LZ4Constants.ML_MASK);
            dOff = LZ4ByteBufferUtils.writeLen(matchLen - LZ4Constants.ML_MASK, dest, dOff);
          } else {
            ByteBufferUtils.writeByte(dest, tokenOff, ByteBufferUtils.readByte(dest, tokenOff) | matchLen);
          }

          // test end of chunk
          if (sOff > mflimit) {
            anchor = sOff;
            break main;
          }

          // fill table
          SafeUtils.writeShort(hashTable, hash64k(ByteBufferUtils.readInt(src, sOff - 2)), sOff - 2 - srcOff);

          // test next position
          final int h = hash64k(ByteBufferUtils.readInt(src, sOff));
          ref = srcOff + SafeUtils.readShort(hashTable, h);
          SafeUtils.writeShort(hashTable, h, sOff - srcOff);

          if (!LZ4ByteBufferUtils.readIntEquals(src, sOff, ref)) {
            break;
          }

          tokenOff = dOff++;
          ByteBufferUtils.writeByte(dest, tokenOff, 0);
        }

        // prepare next loop
        anchor = sOff++;
      }
    }

    dOff = LZ4ByteBufferUtils.lastLiterals(src, anchor, srcEnd - anchor, dest, dOff, destEnd);
    return dOff - destOff;
  }

  @Override
  public int compress(ByteBuffer src, final int srcOff, int srcLen, ByteBuffer dest, final int destOff, int maxDestLen) {

    if (src.hasArray() && dest.hasArray()) {
      return compress(src.array(), srcOff + src.arrayOffset(), srcLen, dest.array(), destOff + dest.arrayOffset(), maxDestLen);
    }
    src = ByteBufferUtils.inNativeByteOrder(src);
    dest = ByteBufferUtils.inNativeByteOrder(dest);

    ByteBufferUtils.checkRange(src, srcOff, srcLen);
    ByteBufferUtils.checkRange(dest, destOff, maxDestLen);
    final int destEnd = destOff + maxDestLen;

    if (srcLen < LZ4Constants.LZ4_64K_LIMIT) {
      return compress64k(src, srcOff, srcLen, dest, destOff, destEnd);
    }

    final int srcEnd = srcOff + srcLen;
    final int srcLimit = srcEnd - LZ4Constants.LAST_LITERALS;
    final int mflimit = srcEnd - LZ4Constants.MF_LIMIT;

    int sOff = srcOff, dOff = destOff;
    int anchor = sOff++;

    final int[] hashTable = new int[LZ4Constants.HASH_TABLE_SIZE];
    Arrays.fill(hashTable, anchor);

    main:
    while (true) {

      // find a match
      int forwardOff = sOff;

      int ref;
      int step = 1;
      int searchMatchNb = 1 << LZ4Constants.SKIP_STRENGTH;
      int back;
      do {
        sOff = forwardOff;
        forwardOff += step;
        step = searchMatchNb++ >>> LZ4Constants.SKIP_STRENGTH;

        if (forwardOff > mflimit) {
          break main;
        }

        final int h = hash(ByteBufferUtils.readInt(src, sOff));
        ref = SafeUtils.readInt(hashTable, h);
        back = sOff - ref;
        SafeUtils.writeInt(hashTable, h, sOff);
      } while (back >= LZ4Constants.MAX_DISTANCE || !LZ4ByteBufferUtils.readIntEquals(src, ref, sOff));


      final int excess = LZ4ByteBufferUtils.commonBytesBackward(src, ref, sOff, srcOff, anchor);
      sOff -= excess;
      ref -= excess;

      // sequence == refsequence
      final int runLen = sOff - anchor;

      // encode literal length
      int tokenOff = dOff++;

      if (dOff + runLen + (2 + 1 + LZ4Constants.LAST_LITERALS) + (runLen >>> 8) > destEnd) {
        throw new LZ4Exception("maxDestLen is too small");
      }

      if (runLen >= LZ4Constants.RUN_MASK) {
        ByteBufferUtils.writeByte(dest, tokenOff, LZ4Constants.RUN_MASK << LZ4Constants.ML_BITS);
        dOff = LZ4ByteBufferUtils.writeLen(runLen - LZ4Constants.RUN_MASK, dest, dOff);
      } else {
        ByteBufferUtils.writeByte(dest, tokenOff, runLen << LZ4Constants.ML_BITS);
      }

      // copy literals
      LZ4ByteBufferUtils.wildArraycopy(src, anchor, dest, dOff, runLen);
      dOff += runLen;

      while (true) {
        // encode offset
        ByteBufferUtils.writeShortLE(dest, dOff, back);
        dOff += 2;

        // count nb matches
        sOff += LZ4Constants.MIN_MATCH;
        final int matchLen = LZ4ByteBufferUtils.commonBytes(src, ref + LZ4Constants.MIN_MATCH, sOff, srcLimit);
        if (dOff + (1 + LZ4Constants.LAST_LITERALS) + (matchLen >>> 8) > destEnd) {
          throw new LZ4Exception("maxDestLen is too small");
        }
        sOff += matchLen;

        // encode match len
        if (matchLen >= LZ4Constants.ML_MASK) {
          ByteBufferUtils.writeByte(dest, tokenOff, ByteBufferUtils.readByte(dest, tokenOff) | LZ4Constants.ML_MASK);
          dOff = LZ4ByteBufferUtils.writeLen(matchLen - LZ4Constants.ML_MASK, dest, dOff);
        } else {
          ByteBufferUtils.writeByte(dest, tokenOff, ByteBufferUtils.readByte(dest, tokenOff) | matchLen);
        }

        // test end of chunk
        if (sOff > mflimit) {
          anchor = sOff;
          break main;
        }

        // fill table
        SafeUtils.writeInt(hashTable, hash(ByteBufferUtils.readInt(src, sOff - 2)), sOff - 2);

        // test next position
        final int h = hash(ByteBufferUtils.readInt(src, sOff));
        ref = SafeUtils.readInt(hashTable, h);
        SafeUtils.writeInt(hashTable, h, sOff);
        back = sOff - ref;

        if (back >= LZ4Constants.MAX_DISTANCE || !LZ4ByteBufferUtils.readIntEquals(src, ref, sOff)) {
          break;
        }

        tokenOff = dOff++;
        ByteBufferUtils.writeByte(dest, tokenOff, 0);
      }

      // prepare next loop
      anchor = sOff++;
    }

    dOff = LZ4ByteBufferUtils.lastLiterals(src, anchor, srcEnd - anchor, dest, dOff, destEnd);
    return dOff - destOff;
  }


}

package shardableobjectids;/*
 *
 */

/*
 * Copyright Georg Koester 2012. Licensed under Apache License 2.0
 * Huge Parts:
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides modified URL safe sortable Base64 encoding and decoding. Sorting on
 * 6 bit borders only.
 */
public class Base64Mod {

    /**
     * BASE32 characters are 6 bits in length. They are formed by taking a block
     * of 3 octets to form a 24-bit string, which is converted into 4 BASE64
     * characters.
     */
    private static final int BITS_PER_ENCODED_BYTE = 6;
    private static final int BYTES_PER_UNENCODED_BLOCK = 3;
    private static final int BYTES_PER_ENCODED_BLOCK = 4;

    private static final byte PAD = '=';

    /**
     * This is NOT standard Base64, but an encoding that preserves sorting.
     */
    private static final byte[] URL_SAFE_ENCODE_TABLE = { '-', '0', '1', '2',
            '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F',
            'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S',
            'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '_', 'a', 'b', 'c', 'd', 'e',
            'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r',
            's', 't', 'u', 'v', 'w', 'x', 'y', 'z'

    };

    /**
     *
     */
    private static final byte[] DECODE_TABLE = { -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, 0, -1, -1, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, -1, -1,
            -1, -1, -1, -1, -1, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22,
            23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, -1, -1, -1,
            -1, 37, -1, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51,
            52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, };

    /**
     * Base64 uses 6-bit fields.
     */
    /** Mask used to extract 6 bits, used when encoding */
    private static final int MASK_6BITS = 0x3f;
    /** Mask used to extract 8 bits, used in decoding bytes */
    protected static final int MASK_8BITS = 0xff;

    // The static final fields above are used for the original static byte[]
    // methods on Base64.
    // The private member fields below are used with the new streaming approach,
    // which requires
    // some state be preserved between calls of encode() and decode().

    public static byte[] encode(byte[] in) {
        return encode(in, 0, in.length);
    }

    public static byte[] encode(byte[] in, int offset, int len) {

        long encodedSizeBound = getEncodedSizeBound(len);
        if (encodedSizeBound > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("len too big");
        }
        byte[] target = new byte[(int) encodedSizeBound];

        int encoded = encode(in, offset, len, target, 0);
        if (encoded != target.length) {
            return Arrays.copyOf(target, encoded);
        }
        return target;
    }

    public static String encodeToString(byte[] in) {
        return encodeToString(in, 0, in.length);
    }

    public static String encodeToString(byte[] in, int offset, int len) {

        long encodedSizeBound = getEncodedSizeBound(len);
        if (encodedSizeBound > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("len too big");
        }
        byte[] target = new byte[(int) encodedSizeBound];

        int encoded = encode(in, offset, len, target, 0);
        return new String(target, 0, encoded, Charset.forName("UTF-8"));

    }

    /**
     * <p>
     * Encodes all of the provided data, starting at inPos, for inAvail bytes.
     * 
     * </p>
     * <p>
     * Thanks to "commons" project in ws.apache.org for the bitwise operations,
     * and general approach.
     * http://svn.apache.org/repos/asf/webservices/commons/trunk/modules/util/
     * </p>
     * 
     * @param in
     *            byte[] array of binary data to base64 encode.
     * @param inPos
     *            Position to start reading data from.
     * @param inAvail
     *            Amount of bytes available from input for encoding.
     * @return
     */
    public static int encode(byte[] in, int inPos, int inAvail, byte[] target,
            int targetOffset) {
        // inAvail < 0 is how we're informed of EOF in the underlying data we're
        // encoding.
        if (inAvail < 0) {
            return 0;
        } else {

            long resultLenMax = getEncodedSizeBound(inAvail);
            if (inAvail > Integer.MAX_VALUE
                    || resultLenMax > target.length - targetOffset) {
                throw new IllegalArgumentException(
                        "Sorry inAvail must be < Integer.MAX and fit into target, "
                                + "target has "
                                + (target.length - targetOffset)
                                + " available, but needs " + resultLenMax);
            }
            int pos = targetOffset;
            int modulus = 0;
            int bitWorkArea = 0;
            byte[] encodeTable = URL_SAFE_ENCODE_TABLE;
            for (int i = 0; i < inAvail; i++) {
                modulus = (modulus + 1) % BYTES_PER_UNENCODED_BLOCK;
                int b = in[inPos++];
                if (b < 0) {
                    b += 256;
                }
                bitWorkArea = (bitWorkArea << 8) + b; // BITS_PER_BYTE
                if (0 == modulus) { // 3 bytes = 24 bits = 4 * 6 bits to extract
                    target[pos++] = encodeTable[(bitWorkArea >> 18)
                            & MASK_6BITS];
                    target[pos++] = encodeTable[(bitWorkArea >> 12)
                            & MASK_6BITS];
                    target[pos++] = encodeTable[(bitWorkArea >> 6) & MASK_6BITS];
                    target[pos++] = encodeTable[bitWorkArea & MASK_6BITS];
                }
            }

            if (0 == modulus) {
                // no leftovers to process and not using chunking
            } else {

                switch (modulus) { // 0-2
                case 1: // 8 bits = 6 + 2
                    target[pos++] = encodeTable[(bitWorkArea >> 2) & MASK_6BITS]; // top
                                                                                  // 6
                                                                                  // bits
                    target[pos++] = encodeTable[(bitWorkArea << 4) & MASK_6BITS]; // remaining
                                                                                  // 2
                    // URL-SAFE skips the padding to further reduce size.
                    // if (encodeTable == STANDARD_ENCODE_TABLE) {
                    // throw new IllegalArgumentException(
                    // "Only for url safe with no padding");
                    // }
                    break;

                case 2: // 16 bits = 6 + 6 + 4
                    target[pos++] = encodeTable[(bitWorkArea >> 10)
                            & MASK_6BITS];
                    target[pos++] = encodeTable[(bitWorkArea >> 4) & MASK_6BITS];
                    target[pos++] = encodeTable[(bitWorkArea << 2) & MASK_6BITS];
                    // URL-SAFE skips the padding to further reduce size.
                    // if (encodeTable == STANDARD_ENCODE_TABLE) {
                    // throw new IllegalArgumentException(
                    // "Only for url safe with no padding");
                    // }
                    break;
                }
            }

            return pos - targetOffset;
        }
    }

    public static byte[] decode(CharSequence s) {
        return decode(new StringByteSupplier(s, 0), s.length());
    }

    public static byte[] decode(CharSequence in, int offset, int len) {
        return decode(new StringByteSupplier(in, offset), len);
    }

    public static byte[] decode(byte[] in) {
        return decode(new ByteArrByteSupplier(in, 0), in.length);
    }

    public static byte[] decode(byte[] in, int offset, int len) {
        return decode(new ByteArrByteSupplier(in, offset), len);
    }

    public static byte[] decode(ByteSupplier in, int len) {
        long decodedSizeBound = getDecodedSizeBound(len);
        if (decodedSizeBound > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("len too big");
        }
        byte[] target = new byte[(int) decodedSizeBound];

        int decoded = decode(in, len, target, 0);
        if (decoded != target.length) {
            return Arrays.copyOf(target, decoded);
        }
        return target;
    }

    /**
     * <p>
     * Decodes all of the provided data, starting at inPos, for inAvail bytes.
     * 
     * </p>
     * <p>
     * Ignores all non-base64 characters. This is how chunked (e.g. 76
     * character) data is handled, since CR and LF are silently ignored, but has
     * implications for other bytes, too. This method subscribes to the
     * garbage-in, garbage-out philosophy: it will not check the provided data
     * for validity.
     * </p>
     * <p>
     * Thanks to "commons" project in ws.apache.org for the bitwise operations,
     * and general approach.
     * http://svn.apache.org/repos/asf/webservices/commons/trunk/modules/util/
     * </p>
     * 
     * @param in
     *            byte[] array of ascii data to base64 decode.
     * @param inPos
     *            Position to start reading data from.
     * @param inAvail
     *            Amount of bytes available from input for encoding.
     */
    public static int decode(ByteSupplier in, int inAvail, byte[] target,
            int targetOffset) {
        long decodedSizeBound = getDecodedSizeBound(inAvail);
        if (decodedSizeBound > target.length - targetOffset) {
            throw new IllegalArgumentException(
                    "target size not sufficient, need " + decodedSizeBound
                            + ", missing: "
                            + (decodedSizeBound - target.length - targetOffset));
        }
        int modulus = 0;
        int bitWorkArea = 0;
        int pos = targetOffset;
        for (int i = 0; i < inAvail; i++) {
            byte b = in.nextByte();
            if (b == PAD) {
                break;
            } else {
                if (b >= 0 && b < DECODE_TABLE.length) {
                    int result = DECODE_TABLE[b];
                    if (result >= 0) {
                        modulus = (modulus + 1) % BYTES_PER_ENCODED_BLOCK;
                        bitWorkArea = (bitWorkArea << BITS_PER_ENCODED_BYTE)
                                + result;
                        if (modulus == 0) {
                            target[pos++] = (byte) ((bitWorkArea >> 16) & MASK_8BITS);
                            target[pos++] = (byte) ((bitWorkArea >> 8) & MASK_8BITS);
                            target[pos++] = (byte) (bitWorkArea & MASK_8BITS);
                        }
                    }
                }
            }
        }

        // Two forms of EOF as far as base64 decoder is concerned: actual
        // EOF (-1) and first time '=' character is encountered in stream.
        // This approach makes the '=' padding characters completely optional.
        if (modulus != 0) {

            // We have some spare bits remaining
            // Output all whole multiples of 8 bits and ignore the rest
            switch (modulus) {
            // case 1: // 6 bits - ignore entirely
            // break;
            case 2: // 12 bits = 8 + 4
                bitWorkArea = bitWorkArea >> 4; // dump the extra 4 bits
                target[pos++] = (byte) ((bitWorkArea) & MASK_8BITS);
                break;
            case 3: // 18 bits = 8 + 8 + 2
                bitWorkArea = bitWorkArea >> 2; // dump 2 bits
                target[pos++] = (byte) ((bitWorkArea >> 8) & MASK_8BITS);
                target[pos++] = (byte) ((bitWorkArea) & MASK_8BITS);
                break;
            }
        }

        return pos - targetOffset;
    }

    protected static long getEncodedSizeBound(long inAvail) {
        return ((inAvail + BYTES_PER_UNENCODED_BLOCK - 1) / BYTES_PER_UNENCODED_BLOCK)
                * BYTES_PER_ENCODED_BLOCK;
    }

    public static long getDecodedSizeBound(long in) {
        long len = ((in + BYTES_PER_ENCODED_BLOCK - 1) / BYTES_PER_ENCODED_BLOCK)
                * BYTES_PER_UNENCODED_BLOCK;
        return len;
    }

    public static boolean isBase64(byte octet) {
        return octet == PAD
                || (octet >= 0 && octet < DECODE_TABLE.length && DECODE_TABLE[octet] != -1);
    }

    public static boolean isBase64(CharSequence base64) {
        return isBase64(base64, 0, base64.length());
    }

    /**
     * Tests a given String to see if it contains only valid characters within
     * the modified Base64 alphabet. Currently the method treats whitespace as
     * valid.
     * 
     * @param base64
     *            String to test
     * @return <code>true</code> if all characters in the String are valid
     *         characters in the Base64 alphabet or if the String is empty;
     *         <code>false</code>, otherwise
     * 
     */
    public static boolean isBase64(CharSequence base64, int offset, int len) {
        for (int i = offset; i < offset + len; i++) {
            char c = base64.charAt(i);
            if (c >= 255 || ((!isWhiteSpace((byte) c)) && !isBase64((byte) c))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tests a given byte array to see if it contains only valid characters
     * within the modified Base64 alphabet. Currently the method treats
     * whitespace as valid.
     * 
     * @param arrayOctet
     *            byte array to test
     * @return <code>true</code> if all bytes are valid characters in the Base64
     *         alphabet or if the byte array is empty; <code>false</code>,
     *         otherwise
     */
    public static boolean isBase64(byte[] arrayOctet) {
        for (int i = 0; i < arrayOctet.length; i++) {
            if (!isBase64(arrayOctet[i]) && !isWhiteSpace(arrayOctet[i])) {
                return false;
            }
        }
        return true;
    }

    private static boolean isWhiteSpace(byte b) {
        return " \r\n\t".indexOf((char) b) != -1;
    }

    public interface ByteSupplier {
        byte nextByte();
    }

    public static class StringByteSupplier implements ByteSupplier {
        private CharSequence s;
        private int pos;

        public StringByteSupplier(CharSequence s, int offset) {
            reset(s, offset);
        }

        public void reset(CharSequence newS, int offset) {
            pos = offset;
            s = newS;
        }

        public byte nextByte() {
            return (byte) s.charAt(pos++);
        }
    }

    public static class ByteArrByteSupplier implements ByteSupplier {
        private byte[] a;
        private int pos;

        public ByteArrByteSupplier(byte[] aParam, int offset) {
            reset(aParam, offset);
        }

        public void reset(byte[] aParam, int offset) {
            pos = offset;
            a = aParam;
        }

        public byte nextByte() {
            return a[pos++];
        }
    }

    public static String decodingTableGenerator() {
        byte max = 0;
        Map<Byte, Integer> mappedChars = new HashMap<Byte, Integer>();
        for (int i = 0; i < URL_SAFE_ENCODE_TABLE.length; i++) {
            byte curr = URL_SAFE_ENCODE_TABLE[i];
            mappedChars.put(curr, i);
            if (curr > max)
                max = curr;
        }

        StringBuilder out = new StringBuilder();
        for (int i = 0; i <= max; i++) {
            int index = -1;
            if (mappedChars.containsKey((byte) i)) {
                index = mappedChars.get((byte) i);
            }
            out.append(Integer.toString(index)).append(", ");
        }
        return out.toString();
    }

}

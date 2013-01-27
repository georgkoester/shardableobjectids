package shardableobjectids;

//ObjectId.java

/**
 * Inspired by ObjectId of org.bson.types.
 * Copyright Georg Koester 2012. Licensed under Apache License 2.0
 *
 * Huge Parts:   Copyright (C) 2008 10gen Inc.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */


import java.nio.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.commons.codec.binary.Base64;
import org.bson.types.ObjectId;

/**
 * A globally unique identifier for objects.
 * <p>Consists of 12 bytes, divided as follows:
 * <blockquote><pre>
 * <table border="1">
 * <tr><td>0</td><td>1</td><td>2</td><td>3</td><td>4</td><td>5</td><td>6</td>
 *     <td>7</td><td>8</td><td>9</td><td>10</td><td>11</td></tr>
 * <tr><td colspan="3">machine</td><td colspan="2">pid</td>
 *     <td colspan="4">time</td><td colspan="3">inc</td></tr>
 * </table>
 * </pre></blockquote>
 */
public class ShardableObjectId implements Comparable<ShardableObjectId>, java.io.Serializable {

    private static final long serialVersionUID = -4415279469780082175L;

    /**
     * Gets a new object id.
     *
     * @return the new id
     */
    public static ShardableObjectId get() {
        return new ShardableObjectId();
    }

    /**
     * Checks if a string could be an <code>ShardableObjectId</code>.
     *
     * @return whether the string could be a shardable object id
     */
    public static boolean isValid(String s) {
        if (s == null)
            return false;

        final int len = s.length();
        if (len == 24) {
            for (int i = 0; i < len; i++) {
                char c = s.charAt(i);
                if (c >= '0' && c <= '9')
                    continue;
                if (c >= 'a' && c <= 'f')
                    continue;
                if (c >= 'A' && c <= 'F')
                    continue;

                return false;
            }
        } else if (len == 16) {
            if (!Base64.isBase64(s)) {
                return false;
            }
        } else {
            return false;
        }

        return true;
    }

    /**
     * Turn an object into an <code>ShardableObjectId</code>, if possible.
     * Strings will be converted into <code>ShardableObjectId</code>s, if possible, and <code>ShardableObjectId</code>s will
     * be cast and returned.  Passing in <code>null</code> returns <code>null</code>.
     *
     * @param o the object to convert
     * @return an <code>ShardableObjectId</code> if it can be massaged, null otherwise
     */
    public static ShardableObjectId massageToObjectId(Object o) {
        if (o == null)
            return null;

        if (o instanceof ShardableObjectId)
            return (ShardableObjectId) o;

        if (o instanceof String) {
            String s = o.toString();
            if (isValid(s))
                return new ShardableObjectId(s);
        }

        return null;
    }

    /* somehow didn't work for me:
	private static Boolean staticLock = new Boolean(true);
	private static boolean registeredBSONCodecs = false;

	public static void registerBSONCodecs() {
		synchronized (staticLock) {
			if (!registeredBSONCodecs) {
				BSON.addEncodingHook(ShardableObjectId.class,
						new Transformer() {

							public Object transform(Object o) {
								if (o instanceof ShardableObjectId) {
									return ((ShardableObjectId) o)
													.toByteArray();
								} else {
									return o;
								}
							}
						});
				registeredBSONCodecs = true;
			}
		}
 }
 */

    public ShardableObjectId(Date time) {
        this(time, getGenMachineId(), _nextInc.getAndIncrement());
    }

    public ShardableObjectId(Date time, int inc) {
        this(time, getGenMachineId(), inc);
    }

    public ShardableObjectId(Date time, int machine, int inc) {
        _time = (int) (time.getTime() / 1000);
        _machine = machine;
        _inc = inc;
        _new = false;
    }

    /**
     * Creates a new instance from a string.
     *
     * @param s the string to convert
     * @throws IllegalArgumentException if the string is not a valid id
     */
    public ShardableObjectId(String s) {
        this(s, false);
    }

    public ShardableObjectId(String s, boolean babble) {

        if (!isValid(s))
            throw new IllegalArgumentException("invalid ShardableObjectId [" + s + "]");

        if (babble)
            s = babbleToMongod(s);

        byte b[];
        if (s.length() == 16) {
            b = Base64.decodeBase64(s);
        } else {
            b = new byte[12];
            for (int i = 0; i < b.length; i++) {
                b[i] = (byte) Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16);
            }
        }

        ByteBuffer bb = ByteBuffer.wrap(b);
        _machine = bb.getInt();
        _time = bb.getInt();
        _inc = bb.getInt();
        _new = false;
    }

    public ShardableObjectId(byte[] b) {
        if (b.length != 12)
            throw new IllegalArgumentException("need 12 bytes");
        ByteBuffer bb = ByteBuffer.wrap(b);
        _machine = bb.getInt();
        _time = bb.getInt();
        _inc = bb.getInt();
        _new = false;
    }

    /**
     * Creates a ShardableObjectId
     *
     * @param time    time in seconds
     * @param machine machine ID
     * @param inc     incremental value
     */
    public ShardableObjectId(int time, int machine, int inc) {
        _time = time;
        _machine = machine;
        _inc = inc;
        _new = false;
    }

    /**
     * Create a new shardable object id.
     */
    public ShardableObjectId() {
        long currentTimeMillis = System.currentTimeMillis();
        _time = (int) (currentTimeMillis / 1000);
        _machine = getGenMachineId();
        _inc = _nextInc.getAndIncrement();
        _new = true;
    }

    @Override
    public int hashCode() {
        int x = _time;
        x += (_machine * 111);
        x += (_inc * 17);
        return x;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o)
            return true;

        ShardableObjectId other = massageToObjectId(o);
        if (other == null)
            return false;

        return
                _time == other._time &&
                        _machine == other._machine &&
                        _inc == other._inc;
    }

    public String toStringBabble() {
        return babbleToMongod(toStringMongod());
    }

    public String toStringMongod() {
        byte b[] = toByteArray();

        StringBuilder buf = new StringBuilder(24);

        for (int i = 0; i < b.length; i++) {
            int x = b[i] & 0xFF;
            String s = Integer.toHexString(x);
            if (s.length() == 1)
                buf.append("0");
            buf.append(s);
        }

        return buf.toString();
    }

    public String toStringBase64URLSafe() {
        // Georg.Koester: this operation is so slow I cannot believe it,
        // but for now we live with it. In my use case probably only
        // used for creation - which for me is not a loop activity.
        // Slow why: newing and copying multiple times.
        return new Base64(0 // no chunking
                          , null, true).encodeAsString(toByteArray()).trim();
    }

    public byte[] toByteArray() {
        byte b[] = new byte[12];
        ByteBuffer bb = ByteBuffer.wrap(b);
        // by default BB is big endian like we need
        bb.putInt(_machine);
        bb.putInt(_time);
        bb.putInt(_inc);
        return b;
    }

    static String _pos(String s, int p) {
        return s.substring(p * 2, (p * 2) + 2);
    }

    public static String babbleToMongod(String b) {
        if (!isValid(b))
            throw new IllegalArgumentException("invalid shardable object id: " + b);

        StringBuilder buf = new StringBuilder(24);
        for (int i = 7; i >= 0; i--)
            buf.append(_pos(b, i));
        for (int i = 15; i >= 8; i--)
            buf.append(_pos(b, i));

        return buf.toString();
    }

    @Override
    public String toString() {
        return toStringBase64URLSafe();
    }

    int _compareUnsigned(int i, int j) {
        long li = 0xFFFFFFFFL;
        li = i & li;
        long lj = 0xFFFFFFFFL;
        lj = j & lj;
        long diff = li - lj;
        if (diff < Integer.MIN_VALUE)
            return Integer.MIN_VALUE;
        if (diff > Integer.MAX_VALUE)
            return Integer.MAX_VALUE;
        return (int) diff;
    }

    /**
     * Ordering here is by machine,time,inc - so
     * follows the distribution on the machines.
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(ShardableObjectId id) {
        if (id == null)
            return -1;

        int x = _compareUnsigned(_machine, id._machine);
        if (x != 0)
            return x;

        x = _compareUnsigned(_time, id._time);
        if (x != 0)
            return x;

        return _compareUnsigned(_inc, id._inc);
    }

    public int getMachine() {
        return _machine;
    }

    /**
     * Gets the time of this ID, in milliseconds
     *
     * @return
     */
    public long getTime() {
        return _time * 1000L;
    }

    /**
     * Gets the time of this ID, in seconds
     *
     * @return
     */
    public int getTimeSecond() {
        return _time;
    }

    public int getInc() {
        return _inc;
    }

    public int _time() {
        return _time;
    }

    public int _machine() {
        return _machine;
    }

    public int _inc() {
        return _inc;
    }

    public boolean isNew() {
        return _new;
    }

    public void notNew() {
        _new = false;
    }

    /**
     * Gets the generated machine ID, identifying the machine / process / class loader
     *
     * @return
     */
    public static int getGenMachineId() {
        return ObjectId.getGenMachineId();
    }

    /**
     * Gets the current value of the auto increment
     *
     * @return
     */
    public static int getCurrentInc() {
        return _nextInc.get();
    }

    final int _machine;
    final int _time;
    final int _inc;

    boolean _new;

    public static int _flip(int x) {
        int z = 0;
        z |= ((x << 24) & 0xFF000000);
        z |= ((x << 8) & 0x00FF0000);
        z |= ((x >> 8) & 0x0000FF00);
        z |= ((x >> 24) & 0x000000FF);
        return z;
    }

    private static AtomicInteger _nextInc = new AtomicInteger((new java.util.Random()).nextInt());

}

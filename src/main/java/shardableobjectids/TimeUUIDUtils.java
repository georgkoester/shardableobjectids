package shardableobjectids;

import java.util.Random;
import java.util.UUID;

public class TimeUUIDUtils {

    public static byte[] toSortableBytes(UUID uuid) {
        return toSortableBytes(uuid, new byte[16], 0);
    }

    public static byte[] toSortableBytes(UUID uuid, byte[] to, int offset) {
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();

        // high
        for (int i = 0; i < 2; i++) {
            to[i] = (byte) (msb >>> 8 * (1 - i));
        }
        // mid
        for (int i = 0; i < 2; i++) {
            to[i + 2] = (byte) (msb >>> 8 * (3 - i));
        }
        // low
        for (int i = 0; i < 4; i++) {
            to[i + 4] = (byte) (msb >>> 8 * (7 - i));
        }

        // second long:
        for (int i = 0; i < 8; i++) {
            to[i + 8] = (byte) (lsb >>> 8 * (7 - i));
        }

        return to;

    }

    public static UUID sortableBytesToUuid(byte[] from, int offset) {
        long msb = 0;
        long lsb = 0;

        // high
        for (int i = 0; i < 2; i++) {
            long t = from[offset + i];
            msb |= (t & 0xFF) << 8 * (1 - i);
        }
        // mid
        for (int i = 0; i < 2; i++) {
            long t = from[offset + i + 2];
            msb |= (t & 0xFF) << 8 * (3 - i);
        }
        // low
        for (int i = 0; i < 4; i++) {
            long t = from[offset + i + 4];
            msb |= (t & 0xFF) << 8 * (7 - i);
        }

        // second long:
        for (int i = 0; i < 8; i++) {
            long t = from[offset + i + 8];
            lsb |= (t & 0xFF) << 8L * (7 - i);
        }

        return new UUID(msb, lsb);
    }

    public static String toSortableUrlSafeBase64(UUID in) {
        return toSortableUrlSafeBase64(in, new byte[16]);
    }

    public static String toSortableUrlSafeBase64(UUID in, byte[] tempBuf) {
        if (tempBuf.length < 16) {
            throw new IllegalArgumentException(
                    "temp buf needs to be at least 16");
        }
        toSortableBytes(in, tempBuf, 0);
        return Base64Mod.encodeToString(tempBuf, 0, 16);
    }

    public static UUID fromSortableUrlSafeBase64(String s) {
        return fromSortableUrlSafeBase64(s, 0);
    }

    public static UUID fromSortableUrlSafeBase64(byte[] from, int offset) {
        if (from.length - offset < 22) {
            throw new IllegalArgumentException("Need at least 22 bytes");
        }
        byte[] decoded = Base64Mod.decode(from, offset, 22);
        return sortableBytesToUuid(decoded, 0);
    }

    public static UUID fromSortableUrlSafeBase64(CharSequence from, int offset) {
        return fromSortableUrlSafeBase64(from, offset, new byte[18]);
    }

    public static UUID fromSortableUrlSafeBase64(CharSequence from, int offset,
            byte[] tempBuf) {
        if (from.length() - offset < 22) {
            throw new IllegalArgumentException("Need at least 22 bytes");
        }
        int decoded = Base64Mod.decode(new Base64Mod.StringByteSupplier(from,
                offset), 22, tempBuf, 0);
        if (decoded < 16) {
            throw new IllegalArgumentException("Not enough bytes, only read: "
                    + decoded);
        }
        return sortableBytesToUuid(tempBuf, 0);
    }

    public static final long NUM_100NS_INTERVALS_SINCE_UUID_EPOCH = 0x01b21dd213814000L;

    public static long extractJavaTime(UUID uuid) {
        return (uuid.timestamp() - NUM_100NS_INTERVALS_SINCE_UUID_EPOCH) / 10000;
    }

    public static long extractTenthOfMicrosTime(UUID uuid) {
        return uuid.timestamp() - NUM_100NS_INTERVALS_SINCE_UUID_EPOCH;
    }

    /**
     * Time uuid: 60 bits of time with 100 nanosec resolution as a unique id.
     * 
     * To generate a current time uuid use eg. <code>com.eaio.uuid</code> or
     * similar.
     * 
     * To achieve uniqueness without depending on local clock algo this
     * generates random node and clock sequence. Only advisable if need to
     * generate a uuid for a non-current time.
     * 
     * <p>
     * Comparison of uuids:
     * 
     * <pre>
     * Normale shardable object id in Georg's base64 enc:
     * dE4itlJAVggMsKef
     * 
     * Time UUID
     * 100 nanos since 1582, 2^60
     * 
     * Time UUID in georg's base64 (almost sortable, possibly also a problem with negative-pos comp)
     * 5adf-3Yj3UuCn-0FKg--1-
     * 
     * Time UUID in standard format:
     * 2a42be00-26b5-11e3-8d2b-005056c00008
     * 
     * Time UUID in hex:
     * 000001415a9d57c5000001415a9d57c5
     * 
     * Comparison:
     *                       human-easily-copyable   len    case sens    text-time-comparable  able to compare before 1970    far-future ready           
     * shardable oid          none, all not easy     16        y               n (design goal)           n                     collis after 2030
     * oid                      n                    16        y               y                         n                     out after 2030 (no more sortable)
     * time uuid base64         n                    22        n               y                         y (1582)                    y
     * time uuid hex            n                    32        n               y                         y (1582)                    y
     * 
     * When to use what:
     * If events reproduce every millisecond and are kept for more than 40 years: uuid
     * If ids are more rare or kept less than 100 years: shardable oid or oid (events may repeat after 120 years)
     * If sorting of events based on time is required and continuency after 2038 or less-than-second resolution
     * is required: time uuid (oid only resolves seconds and loops after 2038) 
     * In practice:
     * server ids: oid
     * client ids/session ids: uuid (permits long-time survival)
     * user-generated content like email: time uuid (unique over cluster even after 60 years)
     * versions: uuid or content-dependent
     * internal messages: soid, oid (messages live less than 60 years)
     * need direct shardability (but consider hashing with eg. murmur3!): soid (new uuids group up in same location of indexes)
     * non-revelation of time: random value, rand UUID, smaller random value with retry-on-duplicate.
     * 
     * Idea: improve oid by adding a byte to time, and start time at 1000 before jesus: short, always positive time, far-future-ready
     * 2^31 / (84600*365)
     * 69.545
     * 2^39 / (84600*365)
     * 17803.549
     * 2^47 / (84600*365)
     * 4557708.745
     * 2^60 / (84600*365)/10000000
     * 3733.675
     * </pre>
     * 
     * @param targetTimeTenthOfMicros
     * @param rand
     * @return
     */
    public static UUID createForGivenTimeAndRand(long targetTimeTenthOfMicros,
            Random rand) {
        long nodeAndClock = createRandNodeAndClock(rand);
        return new UUID(createTime(targetTimeTenthOfMicros), nodeAndClock);
    }

    protected static long createRandNodeAndClock(Random rand) {
        long nodeAndClock = rand.nextLong() & 0x3FFFFFFFFFFFFFFFL;
        // variant:
        nodeAndClock |= 0x8000000000000000L;
        return nodeAndClock;
    }

    public static long createTime(long targetTimeTenthOfMicros) {

        // UTC time
        long timeToUse = (targetTimeTenthOfMicros)
                + NUM_100NS_INTERVALS_SINCE_UUID_EPOCH;

        // time low
        long time = timeToUse << 32;

        // time mid
        time |= (timeToUse & 0xFFFF00000000L) >> 16;

        // time hi and version
        time |= 0x1000 | ((timeToUse >> 48) & 0x0FFF); // version 1
        return time;
    }

    public static int compare(UUID a, UUID b) {

        int res = compare(a.timestamp(), b.timestamp());
        if (res == 0) {
            res = compare(a.getLeastSignificantBits(),
                    b.getLeastSignificantBits());
        }
        return res;
    }

    protected static int compare(long a, long b) {
        if (a > b) {
            return 1;
        }
        if (b > a) {
            return -1;
        }
        return 0;
    }
}

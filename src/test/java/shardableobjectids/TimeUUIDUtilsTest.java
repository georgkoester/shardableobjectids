package shardableobjectids;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DateFormat;
import java.util.Date;
import java.util.Random;
import java.util.UUID;

import org.apache.commons.codec.binary.Hex;
import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class TimeUUIDUtilsTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void shouldCreateTimeUuidFromGivenTimeAndCheckHelpers() {
        long time = new Date().getTime() * 10000;
        Random rand = new Random();
        UUID uuid = TimeUUIDUtils.createForGivenTimeAndRand(time, rand);
        assertEquals(time, uuid.timestamp()
                - TimeUUIDUtils.NUM_100NS_INTERVALS_SINCE_UUID_EPOCH);
        uuid.clockSequence(); // must not fail

        assertEquals(time / 10000, TimeUUIDUtils.extractJavaTime(uuid));
        assertEquals(time, TimeUUIDUtils.extractTenthOfMicrosTime(uuid));
    }

    @Test
    public void shouldConvertUuidsToSortableByteArrayAndBack() {
        UUID uuid = new UUID(2196272690428776931L, -5474406476992741368L);
        byte[] sortableBytes = TimeUUIDUtils.toSortableBytes(uuid);
        UUID uuid2 = TimeUUIDUtils.sortableBytesToUuid(sortableBytes, 0);
        assertEquals(uuid, uuid2);
    }

    @Test
    public void shouldCompareTimeUUID() {
        UUID uuid = new UUID(2196272690428776931L, -5474406476992741368L);
        UUID uuidLowerNode = new UUID(2196272690428776931L,
                -5474406476993741368L);
        long time = uuid.timestamp();

        UUID uuidHigherTime = new UUID(TimeUUIDUtils.createForGivenTimeAndRand(
                time + 10001, new Random()).getMostSignificantBits(),
                uuid.getLeastSignificantBits());
        assertEquals(0, TimeUUIDUtils.compare(uuid, uuid));
        assertEquals(-1, TimeUUIDUtils.compare(uuidLowerNode, uuid));
        assertEquals(1, TimeUUIDUtils.compare(uuid, uuidLowerNode));
        assertEquals(-1, TimeUUIDUtils.compare(uuid, uuidHigherTime));
        assertEquals(1, TimeUUIDUtils.compare(uuidHigherTime, uuid));
    }

    /*******
     * more UUID and objectId testing
     */
    @Test
    @Ignore
    public void testDateIntValsArePositiveAfter1970() {
        long year = 84600l * 365 * 1000;
        long start = new Date().getTime() - (50l * year);
        for (int i = 0; i < 70; i++) {
            Date d = new Date(start + i * year);
            byte[] b = new byte[4];
            ByteBuffer.wrap(b).order(ByteOrder.BIG_ENDIAN)
                    .putInt(checkedCast(d.getTime() / 1000));
            System.out.println("" + DateFormat.getDateTimeInstance().format(d)
                    + ": " + d.getTime() / 1000 + " " + Hex.encodeHexString(b)
                    + " " + toBinaryString(checkedCast(d.getTime() / 1000)));
        }
    }

    /**
     * In contrast to {@link Integer#toBinaryString(int)} shows leading zeros.
     * 
     * @param i
     * @return
     */
    public static String toBinaryString(int i) {
        StringBuilder o = new StringBuilder(32);
        for (int j = 31; j >= 0; j--) {
            o.append(((i >> j) & 1));
        }
        return o.toString();
    }

    public static String toBinaryString(long l) {
        StringBuilder o = new StringBuilder(64);
        for (int j = 63; j >= 0; j--) {
            o.append(((l >> j) & 1));
        }
        return o.toString();
    }

    private int checkedCast(long l) {
        int retval = (int) l;
        if (retval != l) {
            throw new IllegalArgumentException("passed long exceeds int domain");
        }
        return retval;
    }

    @Test
    public void testObjectIdDoesnGetNegativeSecondVals() {
        java.util.Random r = new java.util.Random();
        int max = (int) (new Date().getTime() / 1000);
        for (int i = 0; i < 10000; i++) {
            int abs = Math.abs(r.nextInt(max));
            Date date = new Date(abs * 1000l);
            ObjectId objectId = new ObjectId(date);
            if (objectId._time() < 0) {
                System.out.println("" + objectId._time() + "  "
                        + DateFormat.getDateTimeInstance().format(date) + " "
                        + date.getTime() + " " + abs);
            }
        }

    }

    @Test
    public void testObjectIdValueSortByTimeWorks() {
        java.util.Random r = new java.util.Random();
        int max = checkedCast(new Date().getTime() / 1000);
        for (int i = 0; i < 1000; i++) {
            ObjectId objectId = new ObjectId(new Date(
                    r.nextInt(Integer.MAX_VALUE) * 1000l));
            ObjectId objectId2 = new ObjectId(new Date(
                    r.nextInt(Integer.MAX_VALUE) * 1000l));
            String o1Str = makestrBase64(objectId);
            String o2Str = makestrBase64(objectId2);
            int diff = objectId._time() - objectId2._time();
            if (diff < 0)
                diff = -1;
            if (diff > 0)
                diff = 1;

            int strComp = o1Str.compareTo(o2Str);
            if (strComp < 0)
                strComp = -1;
            if (strComp > 0)
                strComp = 1;
            if (diff != strComp) {
                System.err.println("Problem: " + strComp + "!=" + diff + ": "
                        + objectId._time() + " " + o1Str + " "
                        + objectId2._time() + " " + o2Str);
            } else {
                // System.err.println("OK");
            }

        }
    }

    @Test
    public void testUUIDValueSortByTimeWorks() {
        java.util.Random r = new java.util.Random();
        UUID proto = TimeUUIDUtils.createForGivenTimeAndRand(0, r);
        int max = checkedCast(new Date().getTime() / 1000);
        for (int i = 0; i < 1000; i++) {
            int nextInt = r.nextInt();
            UUID objectId = new UUID(TimeUUIDUtils.createTime(nextInt * 1000l),
                    proto.getLeastSignificantBits());
            UUID objectId2 = new UUID(TimeUUIDUtils.createTime(r.nextInt()
            // (nextInt - 15 + r.nextInt(30))
                    * 1000l), proto.getLeastSignificantBits());
            checkTextNormalDiffEqual(objectId, objectId2);

        }
    }

    @Test
    public void testUUIDValueSortByTimeWorks1Diff() {
        java.util.Random r = new java.util.Random();
        UUID proto = TimeUUIDUtils.createForGivenTimeAndRand(0, r);
        int max = checkedCast(new Date().getTime() / 1000);
        for (int i = 0; i < 1000; i++) {
            // int nextInt = r.nextInt();
            long nextLong = r.nextLong();
            UUID objectId = new UUID(TimeUUIDUtils.createTime(nextLong),
                    proto.getLeastSignificantBits());
            UUID objectId2 = new UUID(TimeUUIDUtils.createTime(nextLong + 1),
                    proto.getLeastSignificantBits());
            checkTextNormalDiffEqual(objectId, objectId2);

        }
    }

    protected void checkTextNormalDiffEqual(UUID objectId, UUID objectId2) {
        String o1Str = makestrBase64(objectId);
        String o2Str = makestrBase64(objectId2);
        long timeFromUUID = TimeUUIDUtils.extractTenthOfMicrosTime(objectId);
        long timeFromUUID2 = TimeUUIDUtils.extractTenthOfMicrosTime(objectId2);
        long diff = timeFromUUID - timeFromUUID2;
        if (diff < 0)
            diff = -1;
        if (diff > 0)
            diff = 1;

        int strComp = o1Str.compareTo(o2Str);
        if (strComp < 0)
            strComp = -1;
        if (strComp > 0)
            strComp = 1;
        if (diff != strComp) {
            System.err.println("Problem: " + strComp + "!=" + diff + ": "
                    + objectId.timestamp() + " " + timeFromUUID + " " + o1Str
                    + " " + toBinaryString(objectId.getMostSignificantBits())
                    + " " + objectId2.timestamp() + " " + timeFromUUID2 + " "
                    + o2Str + " "
                    + toBinaryString(objectId2.getMostSignificantBits()));
            fail();
        } else {
            // System.err.println("OK");
        }
    }

    protected String makestrhex(UUID objectId) {
        byte[] byteArray = TimeUUIDUtils.toSortableBytes(objectId);
        return Hex.encodeHexString(byteArray);
    }

    protected String makestrhex(ObjectId objectId) {
        byte[] byteArray = objectId.toByteArray();
        return Hex.encodeHexString(byteArray);
    }

    protected String makestrBase64(ObjectId objectId) {
        byte[] byteArray = objectId.toByteArray();
        // slow too, basically same as creating object by myself:
        // return new String(Base64Mod.encodeBase64(byteArray, false, true,
        // Integer.MAX_VALUE), Charsets.UTF_8).trim();
        // return new Base64Mod(0 // no chunking
        // , null, true).encodeAsString(byteArray).trim();
        return Base64Mod.encodeToString(byteArray, 0, byteArray.length);
    }

    protected String makestrBase64(UUID objectId) {
        byte[] ba = TimeUUIDUtils.toSortableBytes(objectId);
        return Base64Mod.encodeToString(ba);
    }
}

package shardableobjectids;
/*
 * Copyright Georg Koester 2012. Licensed under Apache License 2.0
 */
import org.apache.commons.codec.binary.Base64Mod;
import org.junit.Test;
import sun.util.resources.CalendarData;

import java.util.Calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ShardableObjectIdWithMoPrefixTest {

	@Test
	public void testTwoDifferent() {
		assertFalse(new ShardableObjectIdWithMoPrefix().equals(new ShardableObjectIdWithMoPrefix()));
	}

	@Test
	public void testSerializationWorks() {
        ShardableObjectIdWithMoPrefix shardableObjectId = new ShardableObjectIdWithMoPrefix();
		byte[] byteArray = shardableObjectId.toByteArray();
        ShardableObjectIdWithMoPrefix alias = new ShardableObjectIdWithMoPrefix(byteArray);
		assertEquals(shardableObjectId, alias);

		String string = shardableObjectId.toString();
		String mongoString = shardableObjectId.toStringMongod();

		assertEquals(shardableObjectId, new ShardableObjectIdWithMoPrefix(string));
		assertEquals(shardableObjectId, new ShardableObjectIdWithMoPrefix(mongoString));
	}

    @Test
    public void testBase64URLSafeLengthIs22() {
        // do a couple, maybe someday we'll find sth! ; )
        for (int i = 0 ; i<10; i++) {
            assertEquals(22, new ShardableObjectIdWithMoPrefix().toStringBase64URLSafe().length());
        }
    }

    @Test
    public void testSortingWorksWithBase64Strings() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, 2012);
        c.set(Calendar.MONTH, 0);
        c.set(Calendar.DAY_OF_MONTH, 1);
        //System.out.println(c.getTime());
        ShardableObjectIdWithMoPrefix first = new ShardableObjectIdWithMoPrefix(c.getTime());
        c.set(Calendar.YEAR, 2011);
        c.set(Calendar.MONTH, 11);
        c.set(Calendar.DAY_OF_MONTH, 31);
        //System.out.println(c.getTime());
        ShardableObjectIdWithMoPrefix second = new ShardableObjectIdWithMoPrefix(c.getTime());


        assertTrue(0 < first.getTime() - second.getTime());
        assertTrue(0 < first.toStringBase64URLSafe().compareTo(second.toStringBase64URLSafe()));
    }
}

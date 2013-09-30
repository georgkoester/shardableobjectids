package shardableobjectids;

/*
 * Copyright Georg Koester 2012. Licensed under Apache License 2.0
 */
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class ShardableObjectIdTest {

    @Test
    public void testTwoDifferent() {
        assertFalse(new ShardableObjectId().equals(new ShardableObjectId()));
    }

    @Test
    public void testSerializationWorks() {
        ShardableObjectId shardableObjectId = new ShardableObjectId();
        byte[] byteArray = shardableObjectId.toByteArray();
        ShardableObjectId alias = new ShardableObjectId(byteArray);
        assertEquals(shardableObjectId, alias);

        String string = shardableObjectId.toString();
        String mongoString = shardableObjectId.toStringMongod();

        assertEquals(shardableObjectId, new ShardableObjectId(string));
        assertEquals(shardableObjectId, new ShardableObjectId(mongoString));
    }

    @Test
    public void testBase64URLSafeLengthIs16() {
        // do a couple, maybe someday we'll find sth! ; )
        for (int i = 0; i < 10; i++) {
            assertEquals(16, new ShardableObjectId().toStringBase64URLSafe()
                    .length());
        }
    }

    @Test
    public void testSortableBase64URLSafeWorks() {
        // do a couple, maybe someday we'll find sth! ; )
        for (int i = 0; i < 10; i++) {
            assertEquals(16, new ShardableObjectId()
                    .toStringSortableBase64URLSafe().length());
        }

        ShardableObjectId soid = new ShardableObjectId();
        String string = soid.toStringSortableBase64URLSafe();
        ShardableObjectId soid2 = new ShardableObjectId(string);
        assertEquals(soid, soid2);
    }

    @Test
    public void testParseNormalBase64() {
        ShardableObjectId soid = new ShardableObjectId();
        @SuppressWarnings("deprecation")
        ShardableObjectId soid2 = ShardableObjectId.parseNormalBase64(soid
                .toStringBase64URLSafe());

        assertEquals(soid, soid2);
    }
}

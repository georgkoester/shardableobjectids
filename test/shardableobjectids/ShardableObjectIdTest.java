package shardableobjectids;

import static org.junit.Assert.*;

import org.junit.Test;
import shardableobjectids.ShardableObjectId;

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
        for (int i = 0 ; i<10; i++) {
            assertEquals(16, new ShardableObjectId().toStringBase64URLSafe().length());
        }
    }
}

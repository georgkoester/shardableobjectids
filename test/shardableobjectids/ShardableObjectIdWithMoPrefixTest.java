package shardableobjectids;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

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
}

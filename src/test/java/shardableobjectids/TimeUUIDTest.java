package shardableobjectids;

import static org.junit.Assert.assertEquals;

import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TimeUUIDTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void shouldGenerateAndParseSortableUrlSafeBase64() {
        String string = TimeUUID.getAsSortableUrlSafeBase64();
        UUID uuid2 = TimeUUIDUtils.fromSortableUrlSafeBase64(string);

        UUID parse = TimeUUID.parse(string, 0, string.length());
        assertEquals(uuid2, parse);

    }

    @Test
    public void shouldParseHexUuid() {
        UUID uuid = TimeUUID.get();
        String string = uuid.toString();
        UUID parse = TimeUUID.parse(string, 0, string.length());
        assertEquals(uuid, parse);
    }

}

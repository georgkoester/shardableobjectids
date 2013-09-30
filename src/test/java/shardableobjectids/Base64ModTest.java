package shardableobjectids;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class Base64ModTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testUrlSafeEncodeAndDecodeStrings() {

        Random r = new Random();

        for (int i = 0; i < 100; i++) {
            byte[] in = new byte[i];

            r.nextBytes(in);

            String encoded = Base64Mod.encodeToString(in);
            byte[] res = Base64Mod.decode(encoded);
            assertTrue("" + Arrays.asList(in) + "!=" + Arrays.asList(res),
                    Arrays.equals(in, res));
        }

    }

    @Test
    public void testUrlSafeEncodeAndDecodeBytes() {

        Random r = new Random();

        for (int i = 0; i < 100; i++) {
            byte[] in = new byte[i];

            r.nextBytes(in);

            byte[] encoded = Base64Mod.encode(in);
            byte[] res = Base64Mod.decode(encoded);
            assertTrue("" + Arrays.asList(in) + "!=" + Arrays.asList(res),
                    Arrays.equals(in, res));
        }

    }

    @Test
    public void testBase64Check() {
        assertTrue(Base64Mod.isBase64("adfjio34"));
        assertTrue(Base64Mod.isBase64("adfjio34"));
        assertTrue(Base64Mod.isBase64("a dfjio34"));
        assertTrue(Base64Mod.isBase64(""));
        assertFalse(Base64Mod.isBase64("adfjio[34"));
        assertFalse(Base64Mod.isBase64("adfjio你34"));
        assertFalse(Base64Mod.isBase64("adfjioü34"));
    }

}

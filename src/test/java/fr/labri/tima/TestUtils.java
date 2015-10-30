package fr.labri.tima;

import java.io.InputStream;

import static org.junit.Assert.fail;

/**
 * Created by morandat on 30/10/2015.
 */
public class TestUtils {
    public static InputStream getInputStream(String ressource) {
        InputStream stream = TestRenderer.class.getResourceAsStream(ressource);
        if (stream == null)
            fail("Resource do not exists: " + ressource);
        return stream;
    }
}

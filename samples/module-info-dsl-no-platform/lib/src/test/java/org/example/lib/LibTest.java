package org.example.lib;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LibTest {

    @Test
    void testLib() throws IOException {
        assertEquals("org.example.lib", Lib.class.getModule().getName());
        assertEquals("org.example.lib", LibTest.class.getModule().getName());
        try (InputStream is = LibTest.class.getResourceAsStream("/data.txt")) {
            assertNotNull(is);
            assertEquals("2*21", new String(is.readAllBytes(), UTF_8));
        }
    }
}

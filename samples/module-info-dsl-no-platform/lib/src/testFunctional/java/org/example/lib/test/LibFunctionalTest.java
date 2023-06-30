package org.example.lib.test;

import org.example.lib.Lib;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LibFunctionalTest {

    @Test
    void testLibFunctional() throws IOException {
        assertEquals("org.example.lib", Lib.class.getModule().getName());
        assertEquals("org.example.lib.test.functional", LibFunctionalTest.class.getModule().getName());
        try (InputStream is = LibFunctionalTest.class.getResourceAsStream("/data.txt")) {
            assertNotNull(is);
            assertEquals("42", new String(is.readAllBytes(), UTF_8));
        }
    }
}

package org.example.app;

import org.example.lib.Lib;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AppTest {

    @Test
    public void testApp() throws IOException {
        assertEquals("org.example.lib", Lib.class.getModule().getName());
        assertEquals("org.example.app", App.class.getModule().getName());
        assertEquals("org.example.app", AppTest.class.getModule().getName());
        try (InputStream is = AppTest.class.getResourceAsStream("/data.txt")) {
            assertNotNull(is);
            assertEquals("ABC", new String(is.readAllBytes(), UTF_8));
        }
    }

}
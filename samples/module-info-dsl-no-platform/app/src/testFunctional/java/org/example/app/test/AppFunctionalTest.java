package org.example.app.test;

import org.junit.jupiter.api.Test;
import org.example.app.App;

import java.io.IOException;
import java.io.InputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AppFunctionalTest {

    @Test
    void testAppFunctional() throws IOException {
        assertEquals("org.example.app", App.class.getModule().getName());
        assertEquals("org.example.app.test.functional", AppFunctionalTest.class.getModule().getName());
        try (InputStream is = AppFunctionalTest.class.getResourceAsStream("/data.txt")) {
            assertNotNull(is);
            assertEquals("DEF", new String(is.readAllBytes(), UTF_8));
        }
    }

}
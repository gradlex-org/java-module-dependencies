package org.my.app.test;

import org.junit.jupiter.api.Test;
import org.my.app.App;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class AppTest {

    @Test
    public void appDoesNotExplode() {
        assertTrue(App.doWork());
    }

}
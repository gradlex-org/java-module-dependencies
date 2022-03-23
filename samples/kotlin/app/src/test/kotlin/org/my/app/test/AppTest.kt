package org.my.app.test

import org.junit.jupiter.api.Test
import org.my.app.App

import org.junit.jupiter.api.Assertions.assertTrue

class AppTest {

    @Test
    fun appDoesNotExplode() {
        assertTrue(App.doWork());
    }

}
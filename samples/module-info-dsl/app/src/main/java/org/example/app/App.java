package org.example.app;

import org.example.lib.Lib;
import org.slf4j.LoggerFactory;

public class App {

    public static void main(String[] args) {
        LoggerFactory.getLogger(App.class).info("App running...");
        doWork();
    }

    public static void doWork() {
        new Lib();
    }
}
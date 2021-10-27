package org.my.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.xmlbeans.impl.tool.XMLBean;

public class App {
    public static void main(String[] args) {
        doWork();
    }

    public static boolean doWork() {
        ObjectMapper om = new ObjectMapper();
        System.out.println(App.class.getModule().getName());

        try {
            new XMLBean();
            throw new RuntimeException("Boom!");
        } catch (NoClassDefFoundError e) {
            // This is expected at runtime!
        }
        return true;
    }
}
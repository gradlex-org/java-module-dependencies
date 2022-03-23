package org.my.app

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.xmlbeans.impl.tool.XMLBean
import org.slf4j.spi.LoggerFactoryBinder

fun main() {
    App.doWork()
}

object App {
    fun doWork(): Boolean {
        val om = ObjectMapper()
        if (!om.canSerialize(LoggerFactoryBinder::class.java)) {
            throw RuntimeException("Boom!")
        }
        println(App::class.java.module.name)
        try {
            XMLBean()
            throw RuntimeException("Boom!")
        } catch (e: NoClassDefFoundError) {
            // This is expected at runtime!
        }
        return true
    }
}
package com.mo7ammed64.novelnun.ui.nav

import java.net.URLClassLoader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DestInitializationTest {
    @Test fun `rail destinations are valid regardless of which destination initializes first`() {
        val baseName = Dest::class.java.name
        val locations = arrayOf(
            Dest::class.java.protectionDomain.codeSource.location,
            Unit::class.java.protectionDomain.codeSource.location,
        ).distinct().toTypedArray()

        // Fresh class loaders reproduce a cold start for each access order. Without isolation,
        // another test may initialize the companion first and hide the circular-init bug.
        listOf("Home", "Search", "Saved", "Settings", "Details", "Reader").forEach { first ->
            URLClassLoader(locations, ClassLoader.getPlatformClassLoader()).use { loader ->
                val firstClass = Class.forName("$baseName\$$first", true, loader)
                assertNotNull(firstClass.getField("INSTANCE").get(null))
                val base = Class.forName(baseName, true, loader)
                val companion = base.getField("Companion").get(null)
                val destinations = companion.javaClass.getMethod("getRailDestinations").invoke(companion) as List<*>
                val routes = destinations.map { destination ->
                    assertNotNull("Null rail item when $first was initialized first", destination)
                    destination!!.javaClass.getMethod("getRoute").invoke(destination)
                }
                assertEquals(listOf("home", "search", "saved", "settings"), routes)
            }
        }
    }
}

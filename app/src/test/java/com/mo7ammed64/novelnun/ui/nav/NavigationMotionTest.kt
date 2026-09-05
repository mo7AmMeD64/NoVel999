package com.mo7ammed64.novelnun.ui.nav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NavigationMotionTest {
    @Test fun `rail motion follows destination order in both directions`() {
        assertEquals(1, railTransitionDirection(Dest.Home.route, Dest.Settings.route))
        assertEquals(-1, railTransitionDirection(Dest.Settings.route, Dest.Search.route))
    }

    @Test fun `deeper pages and unchanged tabs do not use rail motion`() {
        assertNull(railTransitionDirection(Dest.Details.route, Dest.Reader.route))
        assertNull(railTransitionDirection(Dest.Home.route, Dest.Home.route))
        assertNull(railTransitionDirection(null, Dest.Home.route))
    }
}

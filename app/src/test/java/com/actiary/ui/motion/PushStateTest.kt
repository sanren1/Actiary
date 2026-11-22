package com.actiary.ui.motion

import com.actiary.ui.settings.SettingsPage.APPEARANCE
import com.actiary.ui.settings.SettingsPage.HOME
import com.actiary.ui.settings.SettingsPage.TYPES
import com.actiary.ui.settings.showsBottomBar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PushStateTest {
    @Test
    fun pushFromIdle() {
        val state = PushState.Idle(HOME).target(APPEARANCE) as PushState.Animating
        assertEquals(HOME, state.lower)
        assertEquals(APPEARANCE, state.upper)
        assertEquals(APPEARANCE, state.top)
        assertEquals(0f, state.startCoverage, 0f)
    }

    @Test
    fun reverseMidwayRedirectsTheSameLayers() {
        val opening = PushState.Idle(HOME).target(APPEARANCE) as PushState.Animating
        val closing = opening.target(HOME, coverage = 0.4f) as PushState.Animating
        assertEquals(opening.lower, closing.lower)
        assertEquals(opening.upper, closing.upper)
        assertEquals(0.4f, closing.startCoverage, 0f)
        assertEquals(HOME, closing.top)
    }

    @Test
    fun doubleBackFinishesTheFirstPopBeforeMovingTheNextPair() {
        val motion = PushMotion(APPEARANCE)
        motion.navigate(HOME)
        val firstPop = motion.state as PushState.Animating
        motion.update(firstPop, 0.4f)

        motion.drag(0.2f)
        assertSame(firstPop, motion.state)
        motion.navigate(TYPES)
        assertSame(firstPop, motion.state)
        assertEquals(0.4f, motion.coverage, 0f)

        motion.finish(firstPop)
        val nextPush = motion.state as PushState.Animating
        assertEquals(HOME, nextPush.lower)
        assertEquals(TYPES, nextPush.upper)
        assertEquals(TYPES, nextPush.top)
        assertEquals(0f, nextPush.startCoverage, 0f)
    }

    @Test
    fun repeatedTargetIsNoop() {
        val state = PushState.Idle(HOME).target(APPEARANCE)
        assertSame(state, state.target(APPEARANCE))
    }

    @Test
    fun dragThenCancelReturnsToChildAndKeepsBarHidden() {
        val drag = PushState.Idle(APPEARANCE).dragTo(0.4f) as PushState.Dragging
        assertEquals(0.6f, drag.coverage, 0.0001f)
        assertFalse(drag.top.showsBottomBar())
        val settling = drag.settle(commit = false) as PushState.Animating
        assertEquals(drag.coverage, settling.startCoverage, 0f)
        assertEquals(APPEARANCE, settling.top)
        assertFalse(settling.top.showsBottomBar())
    }

    @Test
    fun dragThenCommitShowsBarOnReturningToSettingsHome() {
        val home = PushState.Idle(APPEARANCE).dragTo(0.8f).settle(commit = true)
        assertEquals(HOME, home.top)
        assertTrue(home.top.showsBottomBar())
    }
}

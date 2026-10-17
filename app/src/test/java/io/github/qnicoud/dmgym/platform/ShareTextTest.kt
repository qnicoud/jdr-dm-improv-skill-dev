package io.github.qnicoud.dmgym.platform

import android.app.Application
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ShareTextTest {
    private val app = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun `sharing opens a chooser around a plain text send`() {
        assertTrue(app.shareText("bonjour", "Partager"))

        val chooser = shadowOf(app).nextStartedActivity
        assertEquals(Intent.ACTION_CHOOSER, chooser.action)
        @Suppress("DEPRECATION")
        val send = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
        assertEquals(Intent.ACTION_SEND, send.action)
        assertEquals("text/plain", send.type)
        assertEquals("bonjour", send.getStringExtra(Intent.EXTRA_TEXT))
    }

    @Test
    fun `no application able to receive the text reports a failure`() {
        shadowOf(app).checkActivities(true)

        assertFalse(app.shareText("bonjour", "Partager"))
    }
}

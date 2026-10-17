package io.github.qnicoud.dmgym.platform

import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import io.github.qnicoud.dmgym.app.SplashVariant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Chaque variante pointe vers un style existant, au bon fond et avec la pastille brique. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SplashThemesTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    private val backgrounds =
        mapOf(
            SplashVariant.PARCHMENT_LIGHT to "#FBF5EA",
            SplashVariant.PARCHMENT_DARK to "#1B1612",
            SplashVariant.FOREST_LIGHT to "#F5FBF4",
            SplashVariant.FOREST_DARK to "#0F1511",
            SplashVariant.NIGHT_LIGHT to "#FBF8FF",
            SplashVariant.NIGHT_DARK to "#121318",
            SplashVariant.MIST_LIGHT to "#F6FAFC",
            SplashVariant.MIST_DARK to "#0F1417",
        )

    private fun attr(name: String): Int = context.resources.getIdentifier(name, "attr", context.packageName)

    @Test
    fun `every variant has its own existing style`() {
        val ids = SplashVariant.entries.map { it.styleRes() }
        assertEquals(ids.size, ids.toSet().size)
        ids.forEach { assertNotEquals(0, it) }
    }

    @Test
    fun `every style has the palette background and the brick icon backdrop`() {
        SplashVariant.entries.forEach { variant ->
            val themed = android.view.ContextThemeWrapper(context, variant.styleRes())
            val attrs = intArrayOf(attr("windowSplashScreenBackground"), attr("windowSplashScreenIconBackgroundColor"))
            val array = themed.obtainStyledAttributes(attrs)
            assertEquals(variant.name, Color.parseColor(backgrounds.getValue(variant)), array.getColor(0, 0))
            assertEquals(variant.name, Color.parseColor("#8A2E1C"), array.getColor(1, 0))
            array.recycle()
        }
    }
}

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

    private fun luminance(color: Int): Double {
        fun channel(v: Int): Double {
            val c = v / 255.0
            return if (c <= 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * channel(Color.red(color)) + 0.7152 * channel(Color.green(color)) +
            0.0722 * channel(Color.blue(color))
    }

    private fun contrast(
        a: Int,
        b: Int,
    ): Double {
        val (hi, lo) = luminance(a).let { la -> luminance(b).let { lb -> maxOf(la, lb) to minOf(la, lb) } }
        return (hi + 0.05) / (lo + 0.05)
    }

    /** Couleurs de remplissage des chemins du d20, dans l'ordre : pastille puis face. */
    private fun iconFills(): List<Int> {
        val parser = context.resources.getXml(io.github.qnicoud.dmgym.R.drawable.ic_launcher_foreground)
        val fills = mutableListOf<Int>()
        while (parser.next() != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == org.xmlpull.v1.XmlPullParser.START_TAG && parser.name == "path") {
                val fill = (0 until parser.attributeCount).firstOrNull { parser.getAttributeName(it) == "fillColor" }
                if (fill != null) fills += parser.getAttributeIntValue(fill, 0)
            }
        }
        return fills
    }

    @Test
    fun `the die face keeps a 3 to 1 contrast with its surroundings in every variant`() {
        val (disc, face) = iconFills()
        assertEquals(Color.parseColor("#FBF5EA"), face)
        SplashVariant.entries.forEach { variant ->
            val themed = android.view.ContextThemeWrapper(context, variant.styleRes())
            val array = themed.obtainStyledAttributes(intArrayOf(attr("windowSplashScreenIconBackgroundColor")))
            val backdrop = array.getColor(0, 0)
            array.recycle()
            // Le dé est entouré par la pastille du dessin ; la pastille système est de la même couleur.
            assert(contrast(face, disc) >= 3.0) { "${variant.name}: face/disc ${contrast(face, disc)}" }
            assert(contrast(face, backdrop) >= 3.0) { "${variant.name}: face/backdrop ${contrast(face, backdrop)}" }
        }
    }
}

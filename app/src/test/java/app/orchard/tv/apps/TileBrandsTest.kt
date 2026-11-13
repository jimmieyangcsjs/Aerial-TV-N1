package app.orchard.tv.apps

import android.graphics.Bitmap
import android.graphics.Canvas
import app.orchard.tv.ui.theme.Glyph
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.math.abs

/** Package rules and vector-mark geometry for TV tiles at 1080p and 4K. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TileBrandsTest {
    @Test
    fun installedAppsThatNeedModernArtUseTheirOwnVectorMarks() {
        assertEquals(BrandMark.Aerial, TileRecipes.of("app.orchard.tv")?.mark)
        assertEquals(BrandMark.N1Remote, TileRecipes.of("com.gathin.n1remote")?.mark)
        assertEquals(BrandMark.N1Home, TileRecipes.of("com.gathin.tvhome")?.mark)
        assertEquals(BrandMark.AvBox, TileRecipes.of("com.github.avbox.osc")?.mark)
        assertEquals(BrandMark.PiliPlus, TileRecipes.of("com.example.piliplus")?.mark)
        assertEquals(BrandMark.DangbeiMarket, TileRecipes.of("com.dangbeimarket")?.mark)
        assertEquals(BrandMark.DangbeiAssistant, TileRecipes.of("com.dangbei.zhushou")?.mark)
        assertEquals(BrandMark.EsFileExplorer, TileRecipes.of("com.estrongs.android.pop")?.mark)
        assertEquals(BrandMark.SimpleLauncher, TileRecipes.of("com.phicomm.launcherush.debug")?.mark)
        assertEquals(BrandMark.SuperSu, TileRecipes.of("eu.chainfire.supersu")?.mark)
        assertEquals(BrandMark.Provision, TileRecipes.of("com.android.provision")?.mark)
        assertEquals(BrandMark.FileManager, TileRecipes.of("com.phicomm.filemanager")?.mark)
    }

    @Test
    fun settingsUsesAerialGearAndOrionKeepsItsOwnArtwork() {
        for (pkg in listOf("com.android.settings", "com.android.tv.settings")) {
            val settings = TileRecipes.of(pkg)
            assertEquals(Glyph.Gear, settings?.glyph)
            assertTrue(settings?.top != null && settings.bottom != null)
        }
        assertNull(TileRecipes.of("com.oriontv"))
    }

    @Test
    fun vectorMarksKeepTheirAspectRatioAtBothOutputSizes() {
        val hd = VectorMarkBounds.fit(BrandMark.AvBox.aspect, 352f, 198f)
        val uhd = VectorMarkBounds.fit(BrandMark.AvBox.aspect, 704f, 396f)

        assertTrue(abs(hd.width / hd.height - BrandMark.AvBox.aspect) < 0.001f)
        assertTrue(abs(uhd.width / uhd.height - BrandMark.AvBox.aspect) < 0.001f)
        assertEquals(hd.width * 2f, uhd.width, 0.001f)
        assertEquals(hd.height * 2f, uhd.height, 0.001f)
    }

    @Test
    fun vectorMarksStayInsideTheTileSafeZone() {
        for ((w, h) in listOf(352f to 198f, 704f to 396f)) {
            for (mark in BrandMark.entries) {
                val box = VectorMarkBounds.fit(mark.aspect, w, h)
                assertTrue(box.left >= w * 0.16f)
                assertTrue(box.top >= h * 0.16f)
                assertTrue(box.right <= w * 0.84f)
                assertTrue(box.bottom <= h * 0.84f)
            }
        }
    }

    @Test
    fun vectorMarksAreRasterizedAtTheOutputResolution() {
        TileArtFactory.useCanvasDensity(2f)
        val hd = TileArtFactory.forBrand(BrandMark.AvBox)
        assertEquals(352, hd.back.width)
        assertEquals(198, hd.back.height)
        assertNotNull(hd.fore)

        TileArtFactory.useCanvasDensity(4f)
        val uhd = TileArtFactory.forBrand(BrandMark.AvBox)
        assertEquals(704, uhd.back.width)
        assertEquals(396, uhd.back.height)
        assertNotNull(uhd.fore)
    }

    @Test
    fun vectorMarksRenderAtNative4KResolution() {
        TileArtFactory.useCanvasDensity(4f)
        val marks = BrandMark.entries
        val tiles = marks.map { mark ->
            mark to TileArtFactory.flatten(TileArtFactory.forBrand(mark))
        }
        val dir = File("build/reports/tile-art-4k").apply { mkdirs() }
        val columns = 2
        val rows = (tiles.size + columns - 1) / columns
        val sheet = Bitmap.createBitmap(704 * columns, 396 * rows, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(sheet)

        tiles.forEachIndexed { index, (mark, tile) ->
            assertEquals(704, tile.width)
            assertEquals(396, tile.height)
            val out = File(dir, "${mark.name.lowercase()}-4k.png")
            out.outputStream().use { assertTrue(tile.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            canvas.drawBitmap(
                tile,
                ((index % columns) * 704).toFloat(),
                ((index / columns) * 396).toFloat(),
                null,
            )
            tile.recycle()
        }

        File(dir, "all-marks-4k.png").outputStream().use {
            assertTrue(sheet.compress(Bitmap.CompressFormat.PNG, 100, it))
        }
        sheet.recycle()
    }
}

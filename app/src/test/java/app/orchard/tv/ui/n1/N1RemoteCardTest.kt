package app.orchard.tv.ui.n1

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import app.orchard.tv.n1.N1RemoteStatus
import app.orchard.tv.n1.N1RemoteStatusPolicy
import app.orchard.tv.ui.n1.N1RemoteHomeOverlay
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.android.controller.ActivityController

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w960dp-h540dp-land-xhdpi")
class N1RemoteCardTest {
    @get:Rule val compose = createEmptyComposeRule()
    private var host: ActivityController<ComponentActivity>? = null
    private var visible by mutableStateOf(true)
    private var status by mutableStateOf<N1RemoteStatus?>(onlineStatus)

    @After
    fun close() {
        compose.runOnUiThread { host?.close() }
    }

    @Test
    fun screensaverRevealsTheCodeOnlyAfterTheQuietScreensaverOpening() {
        compose.mainClock.autoAdvance = false
        mountScreensaver()

        compose.mainClock.advanceTimeBy(2_999)
        compose.onAllNodesWithTag("n1-remote-card").assertCountEquals(0)

        compose.mainClock.advanceTimeBy(2)
        compose.onNodeWithTag("n1-remote-card").assertIsDisplayed()
    }

    @Test
    fun screensaverHidesTheCodeAsSoonAsTheServiceLeaves() {
        compose.mainClock.autoAdvance = false
        mountScreensaver()
        compose.mainClock.advanceTimeBy(3_100)
        compose.onNodeWithTag("n1-remote-card").assertIsDisplayed()

        compose.runOnUiThread { status = null }
        repeat(28) { compose.mainClock.advanceTimeByFrame() }

        compose.onAllNodesWithTag("n1-remote-card").assertCountEquals(0)
    }

    @Test
    fun screensaverUsesACompactCodeWithoutAFullCardFrame() {
        compose.mainClock.autoAdvance = false
        mountScreensaver()
        compose.mainClock.advanceTimeBy(3_100)

        val card = compose.onNodeWithTag("n1-remote-card").getUnclippedBoundsInRoot()
        val qr = compose.onNodeWithTag("n1-remote-qr").getUnclippedBoundsInRoot()

        assertEquals(132f, (qr.right - qr.left).value, 0.5f)
        assertEquals(132f, (qr.bottom - qr.top).value, 0.5f)
        assertEquals(152f, (card.right - card.left).value, 0.5f)
        assertEquals(188f, (card.bottom - card.top).value, 0.5f)
    }

    @Test
    fun homeUsesTheSameFloatingCodeTreatmentAtASmallerSize() {
        mountHome(shelfVisible = true)

        val card = compose.onNodeWithTag("n1-remote-card").getUnclippedBoundsInRoot()
        val qr = compose.onNodeWithTag("n1-remote-qr").getUnclippedBoundsInRoot()

        assertEquals(56f, (qr.right - qr.left).value, 0.5f)
        assertEquals(56f, (qr.bottom - qr.top).value, 0.5f)
        assertEquals(180f, (card.right - card.left).value, 0.5f)
        assertEquals(56f, (card.bottom - card.top).value, 0.5f)
    }

    @Test
    fun homeShowsTheCodeAboveTheShelfWithoutCoveringTiles() {
        mountHome(shelfVisible = true)

        val card = compose.onNodeWithTag("n1-remote-card").getUnclippedBoundsInRoot()

        assertEquals(22f, card.top.value, 0.5f)
        assertEquals(78f, card.bottom.value, 0.5f)
    }

    @Test
    fun homeHidesTheCodeOnceTheGridOccupiesTheTopOfTheScreen() {
        mountHome(shelfVisible = false)

        compose.onAllNodesWithTag("n1-remote-card").assertCountEquals(0)
    }

    private fun mountScreensaver() {
        compose.runOnUiThread {
            val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
            host = controller
            controller.get().setContent {
                Box(Modifier.fillMaxSize()) {
                    N1RemoteScreensaverOverlay(visible = visible, status = status)
                }
            }
        }
    }

    private fun mountHome(shelfVisible: Boolean) {
        compose.runOnUiThread {
            val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
            host = controller
            controller.get().setContent {
                Box(Modifier.fillMaxSize()) {
                    N1RemoteHomeOverlay(
                        status = status,
                        shelfVisible = shelfVisible,
                    )
                }
            }
        }
    }

    private companion object {
        val onlineStatus: N1RemoteStatus = N1RemoteStatusPolicy.parse(
            "192.0.2.10",
            8088,
            "246810",
            true,
            "http://192.0.2.10:8088/?pin=246810",
        )!!
    }
}

package app.orchard.tv.ui.n1

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.orchard.tv.R
import app.orchard.tv.n1.N1RemoteBridge
import app.orchard.tv.n1.N1RemoteStatus
import app.orchard.tv.n1.N1RemoteStatusPolicy
import app.orchard.tv.ui.theme.Motion
import app.orchard.tv.ui.theme.Type
import app.orchard.tv.wallpaper.PhoneServer
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.roundToInt

private val RemoteAccent = Color(0xFF68E5F3)
private val RemoteSurface = Color(0xF21A2028)
private val RemoteShape = RoundedCornerShape(18.dp)
private const val SCREENSAVER_CARD_DELAY_MS = 3_000L
private const val QR_QUIET_MODULES = 4

/** The compact Home card, shown only while the remote is online at the TV's current address. */
@Composable
fun N1RemoteHomeCard(status: N1RemoteStatus?, modifier: Modifier = Modifier) {
    var held by remember { mutableStateOf<N1RemoteStatus?>(null) }
    if (status != null) held = status
    AnimatedVisibility(
        visible = status != null,
        enter = fadeIn(tween(420, easing = Motion.EaseOut)),
        exit = fadeOut(tween(180, easing = Motion.EaseOut)),
        modifier = modifier,
    ) {
        held?.let { RemoteCard(it, large = false) }
    }
}

/**
 * The screensaver card stays out of the opening three seconds, then fades in at the lower right.
 * It disappears at once when the service or its current LAN address goes away.
 */
@Composable
fun N1RemoteScreensaverOverlay(visible: Boolean, status: N1RemoteStatus?) {
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(visible, status?.url) {
        ready = false
        if (!visible || status == null) return@LaunchedEffect
        delay(SCREENSAVER_CARD_DELAY_MS)
        ready = true
    }
    val reveal by animateFloatAsState(
        if (visible && ready && status != null) 1f else 0f,
        tween(700, easing = Motion.EaseOut),
        label = "n1-remote-reveal",
    )
    Box(Modifier.fillMaxSize()) {
        if (visible && ready) {
            status?.let {
                RemoteCard(
                    it,
                    large = true,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 38.dp, bottom = 34.dp)
                        .graphicsLayer { alpha = reveal },
                )
            }
        }
    }
}

/**
 * Polls the read-only N1 Remote status provider. The provider starts the service if needed, while
 * the current LAN address check hides a stale code during network changes.
 */
@Composable
fun rememberN1RemoteStatus(): N1RemoteStatus? {
    val context = LocalContext.current.applicationContext
    var status by remember { mutableStateOf<N1RemoteStatus?>(null) }
    LaunchedEffect(context) {
        while (true) {
            val reported = withContext(Dispatchers.IO) { N1RemoteBridge.read(context) }
            status = N1RemoteStatusPolicy.usable(reported, PhoneServer.lanAddress(context))
            delay(N1RemoteStatusPolicy.refreshDelay(reported))
        }
    }
    return status
}

@Composable
private fun RemoteCard(status: N1RemoteStatus, large: Boolean, modifier: Modifier = Modifier) {
    val width = if (large) 226.dp else 236.dp
    val height = if (large) 270.dp else 72.dp
    Box(
        modifier
            .testTag("n1-remote-card")
            // Informational only: a remote key or tap must go on to wake the screensaver.
            .focusProperties { canFocus = false }
            .size(width, height)
            .clip(RemoteShape)
            .drawBehind {
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF23313C), RemoteSurface, Color(0xF211161C)),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height),
                    ),
                )
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.25f),
                    topLeft = Offset(0f, size.height * 0.58f),
                    size = Size(size.width, size.height * 0.42f),
                )
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, RemoteAccent.copy(alpha = 0.75f), Color.Transparent),
                    ),
                    start = Offset(size.width * 0.1f, 0f),
                    end = Offset(size.width * 0.9f, 0f),
                    strokeWidth = 1.5.dp.toPx(),
                )
            }
            .border(1.dp, Color.White.copy(alpha = 0.12f), RemoteShape)
            .padding(if (large) 14.dp else 9.dp),
    ) {
        if (large) LargeContents(status) else CompactContents(status)
    }
}

@Composable
private fun CompactContents(status: N1RemoteStatus) {
    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        QrCode(status.url, 52.dp)
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot()
                Spacer(Modifier.width(7.dp))
                BasicText(
                    stringResource(R.string.n1_remote_name),
                    style = Type.Headline.copy(
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.W700,
                        letterSpacing = 0.025.em,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(4.dp))
            BasicText(
                stringResource(R.string.n1_remote_scan_hint),
                style = Type.Caption.copy(color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun LargeContents(status: N1RemoteStatus) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusDot()
            Spacer(Modifier.width(8.dp))
            BasicText(
                stringResource(R.string.n1_remote_name),
                style = Type.Headline.copy(
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.W700,
                    letterSpacing = 0.05.em,
                ),
            )
        }
        Spacer(Modifier.height(9.dp))
        QrCode(status.url, 172.dp)
        Spacer(Modifier.height(11.dp))
        BasicText(
            stringResource(R.string.n1_remote_scan_title),
            style = Type.Title.copy(color = Color.White, fontSize = 15.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(3.dp))
        BasicText(
            stringResource(R.string.n1_remote_scan_hint),
            style = Type.Caption.copy(color = RemoteAccent.copy(alpha = 0.82f), fontSize = 10.sp),
            maxLines = 1,
        )
    }
}

@Composable
private fun StatusDot() {
    Canvas(Modifier.size(7.dp)) {
        drawCircle(RemoteAccent.copy(alpha = 0.22f), radius = size.minDimension * 0.72f)
        drawCircle(RemoteAccent, radius = size.minDimension * 0.38f)
    }
}

/**
 * Drawn from QR modules rather than a scaled bitmap, with a complete four-module quiet zone. This
 * keeps every module square and crisp at both 1080p (172 dp is 344 px) and 4K (688 px).
 */
@Composable
private fun QrCode(text: String, size: Dp) {
    val matrix = remember(text) {
        QRCodeWriter().encode(
            text,
            BarcodeFormat.QR_CODE,
            0,
            0,
            mapOf(
                EncodeHintType.MARGIN to 0,
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.CHARACTER_SET to "UTF-8",
            ),
        )
    }
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val totalModules = matrix.width + QR_QUIET_MODULES * 2
            val module = min(this.size.width, this.size.height) / totalModules
            val originX = (this.size.width - totalModules * module) / 2f
            val originY = (this.size.height - totalModules * module) / 2f
            drawRect(Color.White)
            for (y in 0 until matrix.height) {
                for (x in 0 until matrix.width) {
                    if (!matrix[x, y]) continue
                    val left = (originX + (x + QR_QUIET_MODULES) * module).roundToInt().toFloat()
                    val top = (originY + (y + QR_QUIET_MODULES) * module).roundToInt().toFloat()
                    val right = (originX + (x + QR_QUIET_MODULES + 1) * module).roundToInt().toFloat()
                    val bottom = (originY + (y + QR_QUIET_MODULES + 1) * module).roundToInt().toFloat()
                    drawRect(Color.Black, Offset(left, top), Size(right - left, bottom - top))
                }
            }
        }
    }
}

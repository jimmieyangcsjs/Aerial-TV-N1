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
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.graphics.Shadow
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
private val RemoteQrPaper = Color(0xFFF4FAFB)
private val RemoteQrInk = Color(0xFF07151B)
private val RemotePanelShape = RoundedCornerShape(14.dp)
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
 * Home's remote code belongs to the shelf above the grid. Once scrolling or focus brings tiles up
 * into that space, the card leaves through the same fade so it never covers an app.
 */
@Composable
fun BoxScope.N1RemoteHomeOverlay(status: N1RemoteStatus?, shelfVisible: Boolean) {
    N1RemoteHomeCard(
        status = status.takeIf { shelfVisible },
        modifier = Modifier.align(Alignment.TopCenter).padding(top = 22.dp),
    )
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
    val width = if (large) 152.dp else 180.dp
    val height = if (large) 188.dp else 56.dp
    Box(
        modifier
            .testTag("n1-remote-card")
            // Informational only: a remote key or tap must go on to wake the screensaver.
            .focusProperties { canFocus = false }
            .size(width, height)
    ) {
        if (large) LargeContents(status) else CompactContents(status)
    }
}

@Composable
private fun CompactContents(status: N1RemoteStatus) {
    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        QrCode(status.url, 56.dp)
        Spacer(Modifier.width(10.dp))
        Column(
            Modifier.width(114.dp).height(56.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            BasicText(
                stringResource(R.string.n1_remote_scan_hint),
                style = Type.Caption.copy(
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 0.01.em,
                    shadow = lowKeyTextShadow(),
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            BasicText(
                stringResource(R.string.n1_remote_name),
                style = Type.Caption.copy(
                    color = Color.White.copy(alpha = 0.42f),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 0.03.em,
                    shadow = lowKeyTextShadow(),
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun lowKeyTextShadow() = Shadow(
    color = Color.Black.copy(alpha = 0.72f),
    offset = Offset(0f, 1.5f),
    blurRadius = 4f,
)

@Composable
private fun LargeContents(status: N1RemoteStatus) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        QrCode(status.url, 132.dp)
        Spacer(Modifier.height(8.dp))
        RemoteInfoPanel(Modifier.width(152.dp).height(48.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusDot()
                Spacer(Modifier.width(7.dp))
                BasicText(
                    stringResource(R.string.n1_remote_name),
                    style = Type.Headline.copy(
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.W700,
                        letterSpacing = 0.035.em,
                    ),
                    maxLines = 1,
                )
            }
            Spacer(Modifier.height(3.dp))
            BasicText(
                stringResource(R.string.n1_remote_scan_title),
                style = Type.Caption.copy(color = RemoteAccent.copy(alpha = 0.86f), fontSize = 10.sp),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun RemoteInfoPanel(modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .clip(RemotePanelShape)
            .drawBehind {
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xE6253440), RemoteSurface, Color(0xE611161C)),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height),
                    ),
                )
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, RemoteAccent.copy(alpha = 0.68f), Color.Transparent),
                    ),
                    start = Offset(size.width * 0.12f, 0f),
                    end = Offset(size.width * 0.88f, 0f),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .border(1.dp, Color.White.copy(alpha = 0.11f), RemotePanelShape)
            .padding(horizontal = 11.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.Center,
        content = content,
    )
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
 * keeps every module square and crisp while the floating layout scales cleanly at 1080p and 4K.
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
    val shape = RoundedCornerShape(if (size > 72.dp) 10.dp else 7.dp)
    Box(
        Modifier
            .testTag("n1-remote-qr")
            .size(size)
            .clip(shape)
            .background(RemoteQrPaper)
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        RemoteAccent.copy(alpha = 0.58f),
                        Color.White.copy(alpha = 0.18f),
                        RemoteAccent.copy(alpha = 0.36f),
                    ),
                ),
                shape,
            ),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val totalModules = matrix.width + QR_QUIET_MODULES * 2
            val module = min(this.size.width, this.size.height) / totalModules
            val originX = (this.size.width - totalModules * module) / 2f
            val originY = (this.size.height - totalModules * module) / 2f
            drawRect(RemoteQrPaper)
            for (y in 0 until matrix.height) {
                for (x in 0 until matrix.width) {
                    if (!matrix[x, y]) continue
                    val left = (originX + (x + QR_QUIET_MODULES) * module).roundToInt().toFloat()
                    val top = (originY + (y + QR_QUIET_MODULES) * module).roundToInt().toFloat()
                    val right = (originX + (x + QR_QUIET_MODULES + 1) * module).roundToInt().toFloat()
                    val bottom = (originY + (y + QR_QUIET_MODULES + 1) * module).roundToInt().toFloat()
                    drawRect(RemoteQrInk, Offset(left, top), Size(right - left, bottom - top))
                }
            }
        }
    }
}

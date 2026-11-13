package app.orchard.tv.apps

/**
 * Marks Aerial redraws as vector paths instead of reusing a phone icon or promotional banner.
 * The aspect ratio is the mark's own, so it is only ever scaled uniformly.
 */
enum class BrandMark(val aspect: Float) {
    Aerial(1.60f),
    N1Remote(0.82f),
    N1Home(1.10f),
    AvBox(1.75f),
    PiliPlus(0.82f),
    DangbeiMarket(1.30f),
    DangbeiAssistant(1f),
    EsFileExplorer(1.08f),
    SimpleLauncher(0.96f),
    SuperSu(1f),
    Provision(1f),
    FileManager(1.10f),
}

internal data class MarkBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

/** The largest centred box a mark may occupy without touching a tile's parallax safe zone. */
internal object VectorMarkBounds {
    fun fit(aspect: Float, tileWidth: Float, tileHeight: Float): MarkBox {
        val maxWidth = tileWidth * 0.62f
        val maxHeight = tileHeight * 0.64f
        val height = minOf(maxHeight, maxWidth / aspect)
        val width = height * aspect
        val left = (tileWidth - width) / 2f
        val top = (tileHeight - height) / 2f
        return MarkBox(left, top, left + width, top + height)
    }
}

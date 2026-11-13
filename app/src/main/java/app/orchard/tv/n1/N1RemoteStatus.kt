package app.orchard.tv.n1

data class N1RemoteStatus(
    val ip: String,
    val port: Int,
    val pin: String,
    val online: Boolean,
    val url: String,
)

object N1RemoteStatusPolicy {
    fun parse(ip: String?, port: Int, pin: String?, online: Boolean, url: String?): N1RemoteStatus? {
        if (ip == null || !isIpv4(ip)) return null
        if (port !in 1..65535) return null
        if (pin == null || pin.length != 6 || pin.any { it !in '0'..'9' }) return null
        if (url != "http://$ip:$port/?pin=$pin") return null
        return N1RemoteStatus(ip, port, pin, online, url)
    }

    fun refreshDelay(status: N1RemoteStatus?): Long = if (status?.online == true) 10_000L else 1_200L

    fun usable(status: N1RemoteStatus?, currentLanAddress: String?): N1RemoteStatus? =
        status?.takeIf { it.online && it.ip == currentLanAddress }

    private fun isIpv4(value: String): Boolean {
        val parts = value.split('.')
        return parts.size == 4 && parts.all { part ->
            part.isNotEmpty() && part.all { it in '0'..'9' } && part.toIntOrNull() in 0..255
        }
    }
}

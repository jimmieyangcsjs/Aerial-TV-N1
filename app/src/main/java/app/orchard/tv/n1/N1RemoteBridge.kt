package app.orchard.tv.n1

import android.content.Context
import android.net.Uri

/** Reads the N1 Remote status without exposing its PIN over the network. */
object N1RemoteBridge {
    private val statusUri = Uri.parse("content://com.gathin.n1remote.status/status")
    private val columns = arrayOf("ip", "port", "pin", "serviceOnline", "url")

    fun read(context: Context): N1RemoteStatus? = runCatching {
        context.contentResolver.query(statusUri, columns, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            N1RemoteStatusPolicy.parse(
                ip = cursor.getString(cursor.getColumnIndexOrThrow("ip")),
                port = cursor.getInt(cursor.getColumnIndexOrThrow("port")),
                pin = cursor.getString(cursor.getColumnIndexOrThrow("pin")),
                online = cursor.getInt(cursor.getColumnIndexOrThrow("serviceOnline")) == 1,
                url = cursor.getString(cursor.getColumnIndexOrThrow("url")),
            )
        }
    }.getOrNull()
}

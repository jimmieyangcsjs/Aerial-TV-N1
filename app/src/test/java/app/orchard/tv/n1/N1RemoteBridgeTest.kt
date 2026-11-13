package app.orchard.tv.n1

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class N1RemoteBridgeTest {
    private val context: Context = RuntimeEnvironment.getApplication()

    @Test
    fun readsEveryStatusFieldFromTheProvider() {
        Robolectric.buildContentProvider(StatusProvider::class.java)
            .create("com.gathin.n1remote.status")

        val status = N1RemoteBridge.read(context)

        assertEquals("192.0.2.10", status?.ip)
        assertEquals(8988, status?.port)
        assertEquals("246810", status?.pin)
        assertEquals(true, status?.online)
        assertEquals("http://192.0.2.10:8988/?pin=246810", status?.url)
    }

    private class StatusProvider : ContentProvider() {
        override fun onCreate() = true

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?,
        ): Cursor = MatrixCursor(arrayOf("ip", "port", "pin", "serviceOnline", "url")).apply {
            addRow(arrayOf<Any>("192.0.2.10", 8988, "246810", 1, "http://192.0.2.10:8988/?pin=246810"))
        }

        override fun getType(uri: Uri): String? = null
        override fun insert(uri: Uri, values: ContentValues?): Uri? = null
        override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
        override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
    }
}

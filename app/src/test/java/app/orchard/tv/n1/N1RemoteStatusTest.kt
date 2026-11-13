package app.orchard.tv.n1

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class N1RemoteStatusTest {
    @Test
    fun acceptsProviderStatusForTheSameAddressAndPin() {
        val status = N1RemoteStatusPolicy.parse(
            ip = "192.168.1.20",
            port = 8088,
            pin = "123456",
            online = true,
            url = "http://192.168.1.20:8088/?pin=123456",
        )

        assertEquals("192.168.1.20", status?.ip)
        assertEquals(8088, status?.port)
        assertEquals("123456", status?.pin)
        assertEquals(true, status?.online)
        assertEquals("http://192.168.1.20:8088/?pin=123456", status?.url)
    }

    @Test
    fun rejectsMalformedProviderValues() {
        assertNull(
            N1RemoteStatusPolicy.parse(
                "192.168.1", 8088, "123456", true, "http://192.168.1:8088/?pin=123456",
            ),
        )
        assertNull(
            N1RemoteStatusPolicy.parse(
                "192.168.1.256", 8088, "123456", true, "http://192.168.1.256:8088/?pin=123456",
            ),
        )
        assertNull(
            N1RemoteStatusPolicy.parse(
                "192.168.1.20", 0, "123456", true, "http://192.168.1.20:0/?pin=123456",
            ),
        )
        assertNull(
            N1RemoteStatusPolicy.parse(
                "192.168.1.20", 70000, "123456", true, "http://192.168.1.20:70000/?pin=123456",
            ),
        )
        assertNull(
            N1RemoteStatusPolicy.parse(
                "192.168.1.20", 8088, "12345", true, "http://192.168.1.20:8088/?pin=12345",
            ),
        )
        assertNull(
            N1RemoteStatusPolicy.parse(
                "192.168.1.20", 8088, "12345a", true, "http://192.168.1.20:8088/?pin=12345a",
            ),
        )
    }

    @Test
    fun refreshesQuicklyWhileTheServiceStarts() {
        assertEquals(1_200L, N1RemoteStatusPolicy.refreshDelay(null))
    }

    @Test
    fun refreshesSlowlyOnceTheServiceIsOnline() {
        val online = N1RemoteStatusPolicy.parse(
            "192.168.1.20", 8088, "123456", true, "http://192.168.1.20:8088/?pin=123456",
        )

        assertEquals(10_000L, N1RemoteStatusPolicy.refreshDelay(online))
    }

    @Test
    fun onlyUsesAStatusWhoseAddressIsStillAssignedToTheTv() {
        val status = N1RemoteStatusPolicy.parse(
            "192.168.1.20", 8088, "123456", true, "http://192.168.1.20:8088/?pin=123456",
        )

        assertEquals(status, N1RemoteStatusPolicy.usable(status, "192.168.1.20"))
        assertNull(N1RemoteStatusPolicy.usable(status, "192.168.1.21"))
        assertNull(N1RemoteStatusPolicy.usable(status, null))
        assertNull(N1RemoteStatusPolicy.usable(status?.copy(online = false), "192.168.1.20"))
    }
}

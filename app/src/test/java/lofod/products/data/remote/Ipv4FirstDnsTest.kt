package lofod.products.data.remote

import okhttp3.Dns
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.InetAddress

class Ipv4FirstDnsTest {

    @Test
    fun lookup_putsIpv4AddressesBeforeIpv6() {
        val ipv6 = InetAddress.getByName("2001:db8::1")
        val ipv4 = InetAddress.getByName("192.0.2.1")
        val dns = Ipv4FirstDns(fixedDns(ipv6, ipv4))

        assertEquals(listOf(ipv4, ipv6), dns.lookup("example.com"))
    }

    @Test
    fun lookup_keepsIpv6WhenNoIpv4() {
        val ipv6 = InetAddress.getByName("2001:db8::1")
        val dns = Ipv4FirstDns(fixedDns(ipv6))

        assertEquals(listOf(ipv6), dns.lookup("example.com"))
    }

    private fun fixedDns(vararg addresses: InetAddress): Dns =
        object : Dns {
            override fun lookup(hostname: String): List<InetAddress> = addresses.toList()
        }
}

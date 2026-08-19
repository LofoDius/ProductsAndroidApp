package lofod.products.data.remote

import okhttp3.Dns
import java.net.Inet6Address
import java.net.InetAddress

/**
 * OkHttp 4 connects to DNS results sequentially and typically sees IPv6 first.
 * On dual-stack Wi-Fi a broken IPv6 route then burns the full connect timeout
 * (browsers hide this with Happy Eyeballs). Prefer IPv4, keep IPv6 as fallback.
 */
class Ipv4FirstDns(
    private val delegate: Dns = Dns.SYSTEM
) : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        return delegate.lookup(hostname).sortedBy { it is Inet6Address }
    }
}

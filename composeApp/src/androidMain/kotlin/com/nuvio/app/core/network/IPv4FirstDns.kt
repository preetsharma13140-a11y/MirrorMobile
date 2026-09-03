package com.mirror.app.core.network

import okhttp3.Dns
import java.net.Inet4Address
import java.net.InetAddress

/**
 * DNS resolver that prefers IPv4 addresses. For known API hosts where IPv6
 * connectivity is unreliable (e.g. TMDB, YouTube), IPv6 addresses are stripped
 * entirely so OkHttp never attempts an unreachable IPv6 connection.
 */
class IPv4FirstDns(private val delegate: Dns = Dns.SYSTEM) : Dns {

    override fun lookup(hostname: String): List<InetAddress> {
        val addresses = delegate.lookup(hostname)

        // For these hosts, IPv6 routes are often unreliable — return IPv4 only
        // to avoid 15-60 second connection timeouts on broken IPv6 paths.
        if (isIPv4OnlyHost(hostname)) {
            val ipv4Only = addresses.filterIsInstance<Inet4Address>()
            return ipv4Only.ifEmpty { addresses } // fall back to all if no IPv4
        }

        // For all other hosts just put IPv4 first as a soft preference
        return addresses.sortedBy { if (it is Inet4Address) 0 else 1 }
    }

    private fun isIPv4OnlyHost(hostname: String): Boolean {
        val lower = hostname.lowercase()
        return lower.endsWith("themoviedb.org") ||
            lower.endsWith("tmdb.org") ||
            lower.endsWith("image.tmdb.org") ||
            lower.endsWith("api.themoviedb.org") ||
            lower.endsWith("googlevideo.com") ||
            lower.endsWith("youtube.com") ||
            lower.endsWith("googlevideo.com")
    }
}
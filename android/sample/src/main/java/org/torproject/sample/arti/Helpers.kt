// SPDX-FileCopyrightText: 2022 Michael Pöhn <michael@poehn.at>
// SPDX-License-Identifier: MIT
package org.torproject.sample.arti

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL

object Helpers {
    fun checkTorProxyConnectivity(proxyHost: String?, proxyPort: Int): TorConnectionStatus {
        val result = httpProxyGet("https://check.torproject.org", proxyHost, proxyPort)
        if (result != null && result.contains("Congratulations. This browser is configured to use Tor.")) {
            return TorConnectionStatus.TOR
        } else if (result != null && result.contains(" Sorry. You are not using Tor. ")) {
            return TorConnectionStatus.DIRECT
        }
        return TorConnectionStatus.ERROR
    }

    fun httpProxyGet(targetURL: String?, proxyHost: String?, proxyPort: Int): String? {
        val proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress(proxyHost, proxyPort))
        var connection: HttpURLConnection? = null
        try {
            //Create connection
            connection = URL(targetURL).openConnection(proxy) as HttpURLConnection?
            connection?.requestMethod = "GET"

            //Get Response
            val `is` = connection?.getInputStream()
            val rd = BufferedReader(InputStreamReader(`is`))
            val response = StringBuilder()
            var line: String?
            while ((rd.readLine().also { line = it }) != null) {
                response.append(line)
                response.append('\r')
            }
            rd.close()
            return response.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            connection?.disconnect()
        }
    }
}

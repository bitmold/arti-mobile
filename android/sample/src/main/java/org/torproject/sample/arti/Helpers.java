// SPDX-FileCopyrightText: 2022 Michael Pöhn <michael@poehn.at>
// SPDX-License-Identifier: MIT

package org.torproject.sample.arti;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;

public class Helpers {

    public static TorConnectionStatus checkTorProxyConnectivity(String proxyHost, Integer proxyPort) {
        String result = Helpers.httpProxyGet("https://check.torproject.org", proxyHost, proxyPort);
        if (result != null && result.contains("Congratulations. This browser is configured to use Tor.")) {
            return TorConnectionStatus.TOR;
        } else if (result != null && result.contains(" Sorry. You are not using Tor. ")) {
            return TorConnectionStatus.DIRECT;
        }
        return TorConnectionStatus.ERROR;
    }

    public static String httpProxyGet(String targetURL, String proxyHost, int proxyPort) {

        Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(proxyHost, proxyPort));

        HttpURLConnection connection = null;
        try {
            //Create connection
            URL url = new URL(targetURL);
            connection = (HttpURLConnection)url.openConnection(proxy);
            connection.setRequestMethod("GET");

            //Get Response
            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            StringBuilder response = new StringBuilder();
            String line;
            while((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();
            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            if(connection != null) {
                connection.disconnect();
            }
        }
    }
}

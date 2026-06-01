// SPDX-FileCopyrightText: 2022 Michael Pöhn <michael@poehn.at>
// SPDX-License-Identifier: MIT

package org.torproject.sample.arti;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.util.Log;


import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;
import java.util.List;

import IPtProxy.IPtProxy;

import org.torproject.arti.ArtiProxy;

public class App extends Application {

    private ArtiProxy mArtiProxy;

    @Override
    public void onCreate() {
        super.onCreate();
//        Log.d("###", String.format("obfs2 port: %d", IPtProxy.obfs2Port()));
//        Log.d("###", String.format("obfs3 port: %d", IPtProxy.obfs3Port()));
//        Log.d("###", String.format("obfs4 port: %d", IPtProxy.obfs4Port()));
//        Log.d("###", String.format("lyrebird version: %s", IPtProxy.lyrebirdVersion()));
//        Log.d("###", String.format("snowflake port: %d", IPtProxy.snowflakePort()));
//        Log.d("###", String.format("snowflake version: %s", IPtProxy.snowflakeVersion()));

        // run obfs4/lyrebird client
//        IPtProxy.startLyrebird("DEBUG", false, false, null);

        // run snowflake client (values copied from )
        // TODO: fix this, once we've updated to the latest iptproxy version
//        final String stunServers = "stun:stun.l.google.com:19302,stun:stun.antisip.com:3478,stun:stun.bluesip.net:3478,stun:stun.dus.net:3478,stun:stun.epygi.com:3478,stun:stun.sonetel.com:3478,stun:stun.sonetel.net:3478,stun:stun.stunprotocol.org:3478,stun:stun.uls.co.za:3478,stun:stun.voipgate.com:3478,stun:stun.voys.nl:3478";
//        final String target = "https://snowflake-broker.torproject.net.global.prod.fastly.net/";
//        final String front = "github.githubassets.com";
//        final String ampCache = "https://cdn.ampproject.org/";
        // IPtProxy.startSnowflake(stunServers, target, front, ampCache, null, null, null, false, false);
//        IPtProxy.startSnowflake(
//                stunServers, // String ice,
//                target, //String url,
//                front, // String fronts,
//                null, // ampCache, // String ampCache,
//                null, // String sqsQueueURL,
//                null, // String sqsCredsStr,
//                null, // String logFile,
//                false, // boolean logToStateDir,
//                false, // boolean keepLocalAddresses,
//                false, // boolean unsafeLogging,
//                1 // long maxPeers
//        );
    }

    public void connectTorDirect() {
        mArtiProxy = ArtiProxy.Builder(this)
                .setLogListener((log) -> {
                    Log.e("artilog", log);
                    App.logOutput(getApplicationContext(), log + "\n");
                })
                .setWrapWebView(true)
                .build();
        mArtiProxy.start();

    }

    public void connectWithLyrebird(int port, List<String> bridgeLines) {
        IPtProxy.setStateLocation(new File(getCacheDir(), "pt_state").getAbsolutePath());

        IPtProxy.startLyrebird("DEBUG", false, false, null);
//      sample bridge lines:
//      "obfs4 69.235.46.22:30913 F79914011EB368C94E58F6CCF8A55A92EFD5F496 cert=ZKLm+4biqgPIf/g1s3slv8jLSzIzLSXAHFOfBLqtrNvnTM6LVbxe/K8e8jJKiXwOpvkoDw iat-mode=0",
//      "obfs4 82.74.251.112:9449 628B95EEAE48758CBAA2812AE99E1AB5B3BE44D4 cert=i7tmgWvq4X2rncJz4FQsQWwkXiEWVE7Nvm1gffYn5ZlVsA0kBF6c/8041dTB4mi0TSShWA iat-mode=0"
        mArtiProxy = ArtiProxy.Builder(this)
                .setObfs4Port(port)
                .setBridgeLines(bridgeLines)
                .setLogListener((log) -> {
                    Log.e("artilog", log);
                    App.logOutput(getApplicationContext(), log);
                })
                .build();
        mArtiProxy.start();

    }
    public void connectWithSnowflake(String stunServers, String target, String front,
                                     List<String> bridgeLines) {
        IPtProxy.setStateLocation(new File(getCacheDir(), "pt_state").getAbsolutePath());

        IPtProxy.startSnowflake(
                stunServers, // String ice,
                target, //String url,
                front, // String fronts,
                null, // ampCache, // String ampCache,
                null, // String sqsQueueURL,
                null, // String sqsCredsStr,
                null, // String logFile,
                false, // boolean logToStateDir,
                false, // boolean keepLocalAddresses,
                false, // boolean unsafeLogging,
                1 // long maxPeers
                );

        mArtiProxy = ArtiProxy.Builder(this)
                .setBridgeLines(bridgeLines)
                .setSnowflakePort((int) IPtProxy.snowflakePort())
                .setLogListener((log) -> {
                    Log.e("artilog", log);
                    App.logOutput(getApplicationContext(), log);
                })
                .build();
        mArtiProxy.start();
    }

    public void stopArti () {
        if (mArtiProxy != null)
            mArtiProxy.stop();
    }

    public static void logOutput(Context context, String logMessage) {
        Intent intent = new Intent("LOG_MESSAGE");
        intent.putExtra("logMessage", logMessage);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }


}

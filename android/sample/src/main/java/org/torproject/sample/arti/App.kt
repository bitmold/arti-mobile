// SPDX-FileCopyrightText: 2022 Michael Pöhn <michael@poehn.at>
// SPDX-License-Identifier: MIT
package org.torproject.sample.arti

import IPtProxy.IPtProxy
import IPtProxy.OnTransportEvents
import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.torproject.arti.ArtiProxy
import java.io.File
import java.lang.Exception

class App : Application() {
    private var mArtiProxy: ArtiProxy? = null

    override fun onCreate() {
        super.onCreate()

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

    fun connectTorDirect() {
        mArtiProxy = ArtiProxy.Builder(this)
            .setLogListener { log: String? ->
                Log.e(TAG, log!!)
                logOutput(applicationContext, log + "\n")
            }
            .setWrapWebView(true)
            .build()
        mArtiProxy?.start()

        Log.d(TAG, "SOCKS Port: ${mArtiProxy?.socksPort}")
    }

    fun connectWithLyrebird(bridgeLines: MutableList<String?>?) {

        val lyrebirdController = IPtProxy.newController(
            File(cacheDir, "pt_state").absolutePath,
            true,
            false,
            "DEBUG",
            object : OnTransportEvents {
                override fun connected(name: String?) {
                    logOutput(this@App, name)
                }

                override fun error(name: String?, error: Exception?) {
                    logOutput(this@App, "$name $error")
                }

                override fun stopped(name: String?, error: Exception?) {
                    logOutput(this@App, "$name $error")
                }
            })

         lyrebirdController.start(IPtProxy.Obfs4, "socks5://127.0.0.1:9150")
        //      sample bridge lines:
//      "obfs4 69.235.46.22:30913 F79914011EB368C94E58F6CCF8A55A92EFD5F496 cert=ZKLm+4biqgPIf/g1s3slv8jLSzIzLSXAHFOfBLqtrNvnTM6LVbxe/K8e8jJKiXwOpvkoDw iat-mode=0",
//      "obfs4 82.74.251.112:9449 628B95EEAE48758CBAA2812AE99E1AB5B3BE44D4 cert=i7tmgWvq4X2rncJz4FQsQWwkXiEWVE7Nvm1gffYn5ZlVsA0kBF6c/8041dTB4mi0TSShWA iat-mode=0"
        mArtiProxy = ArtiProxy.Builder(this)
            .setObfs4Port(lyrebirdController.port(IPtProxy.Obfs4).toInt())
            .setBridgeLines(bridgeLines)
            .setLogListener { log: String? ->
                Log.e(TAG, log.toString())
                logOutput(applicationContext, log)
            }
            .build()
        mArtiProxy?.start()
    }

    fun connectWithSnowflake(
        stunServers: String?, target: String?, front: String?,
        bridgeLines: MutableList<String?>?
    ) {
        val snowflakeController = IPtProxy.newController(
            File(cacheDir, "pt_state").absolutePath,
            true,
            false,
            "DEBUG",
            object : OnTransportEvents {
                override fun connected(name: String?) {
                    logOutput(this@App, name)
                }

                override fun error(name: String?, error: Exception?) {
                    logOutput(this@App, "$name $error")
                }

                override fun stopped(name: String?, error: Exception?) {
                    logOutput(this@App, "$name $error")
                }

            })

        snowflakeController.apply {
            snowflakeIceServers = stunServers
            snowflakeBrokerUrl = target
            snowflakeFrontDomains = front
            snowflakeMaxPeers = 1
        }

        snowflakeController.start(IPtProxy.Snowflake, "127.0.0.1:${mArtiProxy?.socksPort}")

        mArtiProxy = ArtiProxy.Builder(this)
            .setBridgeLines(bridgeLines)
            .setSnowflakePort(snowflakeController.port(IPtProxy.Snowflake).toInt())
            .setLogListener { log: String? ->
                Log.e(TAG, log.toString())
                logOutput(applicationContext, log)
            }
            .build()
        mArtiProxy!!.start()
    }

    fun stopArti() = mArtiProxy?.stop()

    companion object {
        private const val TAG = "artilog"
        const val ACTION_LOG_MESSAGE = "LOG_MESSAGE"
        const val EXTRA_LOG_MESSAGE_KEY = "logMessage"
        fun logOutput(context: Context, logMessage: String?) {
            val intent = Intent(ACTION_LOG_MESSAGE)
            intent.putExtra("logMessage", logMessage)
            LocalBroadcastManager.getInstance(context).sendBroadcast(
                Intent(ACTION_LOG_MESSAGE).putExtra(
                    EXTRA_LOG_MESSAGE_KEY,
                    logMessage
                )
            )
        }
    }
}

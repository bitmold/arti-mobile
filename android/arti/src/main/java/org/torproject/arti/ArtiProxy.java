package org.torproject.arti;

import android.content.Context;

import androidx.webkit.ProxyConfig;
import androidx.webkit.ProxyController;
import androidx.webkit.WebViewFeature;

import org.jspecify.annotations.NonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Provides a SOCKS5 proxy and DNS resolver for using Arti to access the Tor network.
 *
 * <p>
 * Here's an examples:
 * <code><pre>
 *     ArtiProxy p = ArtiProxy.Builder(context).build();
 *     p.start();
 *     ...
 *     p.stop();
 *   </pre></code>
 * </p>
 *
 * <p>
 * ArtiProxy is not designed to be thread safe. If you need high level access to Arti on Android
 * consider using ArtiService instead.
 * </p>
 */
public class ArtiProxy {

    private static final int DEFAULT_SOCKS_PORT = 9150;
    private static final int DEFAULT_DNS_PORT = 9151;

    private final int socksPort;
    private final int dnsPort;
    private final int obfs4Port;
    private final int snowflakePort;
    private final String bridgeLines;
    private final String cacheDir;
    private final String stateDir;
    private final ArtiLogListener logCallback;

    private ArtiProxy(ArtiProxyBuilder builder) {

        socksPort = builder.socksPort;
        dnsPort = builder.dnsPort;
        obfs4Port = builder.obfs4Port == null ? 0 : builder.obfs4Port;
        snowflakePort = builder.snowflakePort == null ? 0 : builder.snowflakePort;
        cacheDir = builder.cacheDir.getAbsolutePath();
        stateDir = builder.stateDir.getAbsolutePath();
        logCallback = builder.logListener;

        if (builder.bridgeLines != null && !builder.bridgeLines.isEmpty()) {
            StringBuilder allBridgeLines = new StringBuilder();
            boolean firstLine = true;
            for (String bridgeLine : builder.bridgeLines) {
                if (firstLine) {
                    firstLine = false;
                } else {
                    allBridgeLines.append("\n");
                }
                allBridgeLines.append(bridgeLine);
            }
            bridgeLines = allBridgeLines.toString();
        } else {
            bridgeLines = null;
        }

        if (builder.wrapWebView) {
            wrapWebView();
        }
    }

    private void wrapWebView() {
        String proxyHost = String.format(Locale.ROOT, "socks://127.0.0.1:%d", this.socksPort);

        if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            ProxyConfig proxyConfig = new ProxyConfig.Builder()
                    .addProxyRule(proxyHost) // proxy for tor
                    .addDirect().build();
            ProxyController.getInstance().setProxyOverride(proxyConfig, command -> {
                //do nothing
            }, () -> {

            });
        }
    }

    public void start() {
        ArtiJNI.startArtiProxyJNI(
                cacheDir,
                stateDir,
                obfs4Port,
                snowflakePort,
                null,
                bridgeLines,
                socksPort,
                dnsPort,
                mainLogListener
        );
    }

    public void stop() {
        // TODO: this would turn off log output, so uncommenting for now. Should be
        // refactored so that it's independent of start/stop
        // logListeners.remove(this.logCallback);
        ArtiJNI.stopArtiProxyJNI();
    }

    public static ArtiProxyBuilder Builder(Context context) {
        if (context == null) {
            // fail early and make sure devs get a proper error message
            // we might want to add appropriate annotations too
            throw new NullPointerException(
                    "Can not initialize ArtiProxy.Builder(Context context): context must not be null.");
        }

        return new ArtiProxyBuilder(context.getApplicationContext());
    }

    public int getSocksPort() {
        return socksPort;
    }

    @SuppressWarnings("unused")
    public int getDnsPort() {
        return dnsPort;
    }

    @SuppressWarnings("unused")
    public String getCacheDir() {
        return cacheDir;
    }

    @SuppressWarnings("unused")
    public String getStateDir() {
        return stateDir;
    }

    @SuppressWarnings("unused")
    public ArtiLogListener getLogCallback() {
        return logCallback;
    }

    /**
     * Use this builder to configure and build ArtiProxy instances.
     */
    public static class ArtiProxyBuilder {

        private int socksPort = DEFAULT_SOCKS_PORT;
        private int dnsPort = DEFAULT_DNS_PORT;
        private Integer obfs4Port;
        private Integer snowflakePort;
        private List<String> bridgeLines = new ArrayList<>();
        private File cacheDir = null;
        private File stateDir = null;
        private boolean wrapWebView = false;
        private ArtiLogListener logListener;

        private final Context context;

        private ArtiProxyBuilder(Context context) {
            this.context = context.getApplicationContext();
        }

        /**
         * Change the local SOCKS5 port Arti will provide for accessing the Tor network.
         * (defaults to 9150)
         */
        @SuppressWarnings("unused")
        public ArtiProxyBuilder setSocksPort(int socksPort) {
            this.socksPort = socksPort;
            return this;
        }

        /**
         * Change the local dns resolver port Arti will provide for retrieving domain
         * names over the Tor network. (defaults to 9151)
         */
        @SuppressWarnings("unused")
        public ArtiProxyBuilder setDnsPort(int dnsPort) {
            this.dnsPort = dnsPort;
            return this;
        }

        /**
         * Set this to register a local obfs4 client with Arti. (e.g. IPtProxy.startLyrebird())
         * <p>
         * This will enable support for connecting to the Tor network over obf4 bridges.
         * You will also have to configure some bridge lines with setBridgeLines().
         */
        public ArtiProxyBuilder setObfs4Port(Integer obfs4Port) {
            this.obfs4Port = obfs4Port;
            return this;
        }

        /**
         * Set this to register a local snowflake client with Arti. (e.g. IPtProxy.startSnowflake())
         * <p>
         * This will enable support for connecting to the Tor network over snowflake bridges.
         * You will also have to configure some bridge lines with setBridgeLines().
         */
        public ArtiProxyBuilder setSnowflakePort(Integer snowflakePort) {
            this.snowflakePort = snowflakePort;
            return this;
        }

        /**
         * Use this to supply bridge lines e.g. from moat or bridges.torproject.org to Arti.
         * You also need to register at least one local pluggable transport client. e.g.
         * ArtiProxy.Builder.setSnowflakePort(), ArtiProxy.Builder.setObfs4Port()
         */
        public ArtiProxyBuilder setBridgeLines(List<String> bridgeLines) {
            this.bridgeLines = bridgeLines;
            return this;
        }

        /**
         * Change the directory where Arti stores its cached data.
         * TODO: consider moving this out ouf ArtiProxy to make it independent of Android APIs.
         */
        @SuppressWarnings("unused")
        public ArtiProxyBuilder setCacheDir(File cacheDir) {
            this.cacheDir = cacheDir;
            return this;
        }

        /**
         * Change the directory where Arti stores its persistent data.
         * TODO: consider moving this out ouf ArtiProxy to make it independent of Android APIs.
         */
        @SuppressWarnings("unused")
        public ArtiProxyBuilder setStateDir(File stateDir) {
            this.stateDir = stateDir;
            return this;
        }

        /**
         * globally configure WebViews in the context of this app to route traffic through this proxy.
         * TODO: consider moving this out ouf ArtiProxy to make it independent of Android APIs.
         */
        public ArtiProxyBuilder setWrapWebView(boolean wrapWebView) {
            this.wrapWebView = wrapWebView;
            return this;
        }

        /**
         * Register a listener for receiving the log output of Arti.
         *
         * <p>
         * NOTE: this callback get called from threads which are managed by Arti. So it's
         * highly recommended consider passing log callbacks on to a thread that's managed
         * by you.
         * </p>
         */
        public ArtiProxyBuilder setLogListener(ArtiLogListener logListener) {
            this.logListener = logListener;
            return this;
        }

        /**
         * Build an ArtiProxy instance.
         */
        public @NonNull ArtiProxy build() {
            if (cacheDir == null) {
                cacheDir = new File(context.getCacheDir().getAbsolutePath() + "/arti_cache");
            }
            cacheDir.mkdirs();

            if (stateDir == null) {
                stateDir = new File(context.getFilesDir().getAbsolutePath() + "/arti_state");
            }
            stateDir.mkdirs();

            if (logListener != null) {
                logListeners.add(logListener);
            }

            return new ArtiProxy(this);
        }
    }

    /**
     * List of consumer log callback listeners used by mainLogListener for tracking callback
     * consumers.
     */
    private static final List<ArtiLogListener> logListeners = Collections.synchronizedList(new ArrayList<>());
    /**
     * Central internal log callback object. On the rust side we've registered a "global default
     * trace dispatcher". We re-register for a second time it panics. So for supporting multiple
     * log callback consumers, we use this central callback object for multiplexing log events.
     */
    private static final ArtiLogListener mainLogListener = logLine -> {
        synchronized (logListeners) {
            for (ArtiLogListener logListener : logListeners) {
                logListener.log(logLine);
            }
        }
    };
}

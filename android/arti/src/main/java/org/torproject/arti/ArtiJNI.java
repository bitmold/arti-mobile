package org.torproject.arti;

/**
 * It's discouraged to use this directly. Use ArtiProxy instead.
 */
class ArtiJNI {

    static {
        System.loadLibrary("arti_mobile");
    }

    static native String startArtiProxyJNI(
            String cacheDir,
            String stateDir,
            int obfs4Port,
            int snowflakePort,
            String obfs4proxyPath,
            String bridgeLine,
            int socksPort,
            int dnsPort,
            ArtiLogListener loggingCallback);


    static native void stopArtiProxyJNI();
}

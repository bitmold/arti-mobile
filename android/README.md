
Android library for Arti with JNI interface

- published as experimental maven dependency at:

repositories {
      maven { url "https://raw.githubusercontent.com/guardianproject/gpmaven/master" }
}

dependencies {
    implementation("org.torproject:arti-mobile:1.7.0")
}


Sample use:

	//show arti logs into Android logcat
	Arti.initLogging(); 

	//enable localhost:9150 socks proxy for use with WebView and other proxy capable communication
	ArtiSocksProxy.start(this); 

 	//set SOCKS proxy on WebView
  	String proxyHost = "socks://127.0.0.1:9150";

        ProxyConfig proxyConfig = new ProxyConfig.Builder()
                .addProxyRule(proxyHost) //http proxy for tor
                .addDirect().build();

        ProxyController.getInstance().setProxyOverride(proxyConfig, new Executor() {
            @Override
            public void execute(Runnable command) {
                //do nothing
            }
        }, new Runnable() {
            @Override
            public void run() {
        ProxyController.getInstance().setProxyOverride(proxyConfig, command -> {
            //do nothing
        }, () -> {
            }
        });


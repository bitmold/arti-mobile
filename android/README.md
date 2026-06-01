
Android library for Arti with JNI interface

- published as experimental maven dependency at:

repositories {
      maven { url "https://raw.githubusercontent.com/guardianproject/gpmaven/master" }
}

dependencies {
    implementation("org.torproject:arti-mobile:1.7.0.1")
}


Sample use:

  	var artiProxy = ArtiProxy.Builder(this)
                .setLogListener((log) -> {
                    Log.d("artilog", log);
                    App.logOutput(getApplicationContext(), log + "\n");
                })
                .setWrapWebView(true)
                .build();
        mArtiProxy.start();
        var socksPort = mArtiProxy.getSocksPort();



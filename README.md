# Arti Mobile: Tor in Rust for Android and iOS

The official Arti Mobile library, supported by Tor Project, bringing the new Tor Rust runtime to mobile devices.


This is a shared project for exposing a standard API for Tor Arti to Android and iOS applications. 

---

This was based on the following projects:
- Trinity's https://gitlab.torproject.org/trinity-1686a/arti-mobile-example/
- uniq's https://codeberg.org/uniqx/arti-android
- ahf's https://gitlab.torproject.org/ahf/arti-orbot-ios

---

Learn more at: https://guide.onionmobile.dev/tor-on-android/arti-mobile-on-android and https://arti.torproject.org/

For more explanations on what it is doing. You should read Arti documentation [for Android](https://gitlab.torproject.org/tpo/core/arti/-/blob/main/doc/Android.md) and [for iOS](https://gitlab.torproject.org/tpo/core/arti/-/blob/main/doc/iOS.md).

## Quick Integration

- View the SAMPLE project in the "Android" folder, and find additional Android integration guidance on the [Onion Mobile Devsite](https://guide.onionmobile.dev/tor-on-android/arti-mobile-on-android)
- View the SAMPLE proejct in the "iOS" folder, and find additional iOS integration guidance on the [Onion Mobile Devsite](https://guide.onionmobile.dev/tor-on-ios/arti-and-onionmasq-on-ios)

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


## Build

### To build for Android

- install Rust and Android Studio. Make sure you can run an Hello World with both.
- go in the common folder and run `make android`.
- take a coffee, or two.
- open the android folder in Android Studio or use gradle to build your app as usual.

### To build for iOS

- grab a Mac (you can't create an iOS app on a PC)
- install Rust and XCode. Make sure you can run a Hello World with both.
- go in the common folder and run `make ios`.
- take a coffee, or two.
- open the ios folder in XCode and compile your app as usual.

### Build rust code using vagrant

```
vagrant up
vagrant ssh -c "cd /vagrant/common && make android"
```

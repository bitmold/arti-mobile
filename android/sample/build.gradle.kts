// SPDX-FileCopyrightText: 2022 Michael Pöhn <michael@poehn.at>
// SPDX-License-Identifier: MIT
import com.android.build.api.dsl.ApplicationExtension

plugins {
    alias(libs.plugins.android.application)
}
configure<ApplicationExtension> {

    namespace = "org.torproject.sample.arti"

    compileSdk {
        version = release(35)
    }
    defaultConfig {
        applicationId = namespace
        minSdk = 24
        targetSdk = 35
        versionCode = 11
        versionName = "1.1"
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }


    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_24
        targetCompatibility = JavaVersion.VERSION_24
    }
}

dependencies {
    implementation(project(":arti"))

    implementation(libs.iptproxy)
    implementation(libs.localbroadcastmanager)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
}

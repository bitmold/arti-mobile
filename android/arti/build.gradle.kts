// SPDX-FileCopyrightText: 2023 Michael Pöhn <michael@poehn.at>
// SPDX-License-Identifier: MIT

import com.android.build.api.dsl.LibraryExtension
plugins {
    alias(libs.plugins.android.library)
}

configure<LibraryExtension> {
    namespace = "org.torproject.arti"
    compileSdk {
        version = release(35)
    }

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
        singleVariant("debug") {
            withSourcesJar()
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_24
        targetCompatibility = JavaVersion.VERSION_24
    }
}

dependencies {
    implementation(libs.webkit)
}

apply(file("publishLocal.gradle"))

plugins {
    id("com.android.library")
}

android {
    // Java sources share this namespace (no extra .sdk sub-package).
    namespace = "com.tencent.trtc.aiconversation"
    compileSdk = 34

    defaultConfig {
        minSdk = 24

        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    api(":LiteAVSDK_Professional_13.6.0.20757@aar")
}

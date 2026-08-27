plugins { id("com.android.application") }

android {
    namespace="com.cronoslot"
    compileSdk=35
    defaultConfig {
        applicationId="com.cronoslot"
        minSdk=26
        targetSdk=35
        versionCode=201
        versionName="2.1.1"
    }
    compileOptions {
        sourceCompatibility=JavaVersion.VERSION_17
        targetCompatibility=JavaVersion.VERSION_17
    }
}
dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity:1.10.1")
    implementation("androidx.core:core:1.15.0")
    implementation("androidx.camera:camera-core:1.4.1")
    implementation("androidx.camera:camera-camera2:1.4.1")
    implementation("androidx.camera:camera-lifecycle:1.4.1")
    implementation("androidx.camera:camera-view:1.4.1")
    implementation("org.apache.poi:poi-ooxml:5.4.1")
}

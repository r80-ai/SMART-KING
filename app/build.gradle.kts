import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.net.URI
import java.net.HttpURLConnection
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest

plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.plugin.compose")
  id("com.google.devtools.ksp")
  id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
  id("com.google.gms.google-services")
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.pdfmaster.qvrx"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug { signingConfig = signingConfigs.getByName("debugConfig") }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  androidResources {
    noCompress += listOf("onnx")
  }
  packaging {
    resources {
      excludes += "/META-INF/DEPENDENCIES"
    }
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform("androidx.compose:compose-bom:2024.09.00"))
  implementation(platform("com.google.firebase:firebase-bom:34.17.0"))
  implementation("androidx.activity:activity-compose:1.10.1")
  implementation("androidx.camera:camera-camera2:1.5.0")
  implementation("androidx.camera:camera-core:1.5.0")
  implementation("androidx.camera:camera-lifecycle:1.5.0")
  implementation("androidx.camera:camera-view:1.5.0")
  implementation("androidx.compose.material:material-icons-core")
  implementation("androidx.compose.material:material-icons-extended")
  implementation("androidx.compose.material3:material3")
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.ui:ui-graphics")
  implementation("androidx.compose.ui:ui-tooling-preview")
  implementation("androidx.core:core-ktx:1.18.0")
  implementation("androidx.documentfile:documentfile:1.0.1")
  implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
  implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
  implementation("androidx.navigation:navigation-compose:2.8.9")
  implementation("androidx.room:room-ktx:2.7.0")
  implementation("androidx.room:room-runtime:2.7.0")
  implementation("androidx.work:work-runtime-ktx:2.10.0")
  implementation("com.google.api-client:google-api-client-android:2.2.0")
  implementation("com.google.apis:google-api-services-drive:v3-rev20230815-2.0.0")
  implementation("com.google.apis:google-api-services-sheets:v4-rev20230815-2.0.0")
  implementation("io.coil-kt:coil-compose:2.7.0")
  implementation("com.squareup.retrofit2:converter-moshi:2.12.0")
  implementation("com.google.firebase:firebase-ai")
  implementation("com.google.firebase:firebase-appcheck-recaptcha")
  implementation("com.google.firebase:firebase-appcheck-debug")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
  implementation("com.squareup.okhttp3:logging-interceptor:4.10.0")
  implementation("com.squareup.moshi:moshi-kotlin:1.15.2")
  implementation("com.squareup.okhttp3:okhttp:4.10.0")
  implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")
  implementation("org.bouncycastle:bcpkix-jdk18on:1.78.1")
  implementation("com.google.mlkit:text-recognition:16.0.1")
  implementation("com.tom-roush:pdfbox-android:2.0.27.0") {
    exclude(group = "org.bouncycastle")
  }
  implementation("com.microsoft.onnxruntime:onnxruntime-android:1.18.0")
  implementation("com.squareup.retrofit2:retrofit:2.12.0")
  implementation("com.google.zxing:core:3.5.3")
  implementation("androidx.print:print:1.0.0")

  testImplementation("androidx.compose.ui:ui-test-junit4")
  testImplementation("com.microsoft.onnxruntime:onnxruntime:1.18.0")
  testImplementation("androidx.test:core:1.6.1")
  testImplementation("androidx.test.ext:junit:1.3.0")
  testImplementation("junit:junit:4.13.2")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
  testImplementation("org.robolectric:robolectric:4.16.1")

  androidTestImplementation(platform("androidx.compose:compose-bom:2024.09.00"))
  androidTestImplementation("androidx.compose.ui:ui-test-junit4")
  androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
  androidTestImplementation("androidx.test.ext:junit:1.3.0")
  androidTestImplementation("androidx.test:runner:1.6.2")

  debugImplementation("androidx.compose.ui:ui-test-manifest")
  debugImplementation("androidx.compose.ui:ui-tooling")

  "ksp"("androidx.room:room-compiler:2.7.0")
  "ksp"("com.squareup.moshi:moshi-kotlin-codegen:1.15.2")
}

val verifyBiRefNetModel = tasks.register("verifyBiRefNetModel") {
    val modelAsset = file("src/main/assets/birefnet_lite_fp16.onnx")
    val expectedSize = 98484532L
    val expectedSha256 = "eff9216bb2f9d3f023d9c2b7196845a7485739ab1f231593633e4d2344ffc516"

    doLast {
        if (!modelAsset.exists()) {
            throw GradleException("ERROR: src/main/assets/birefnet_lite_fp16.onnx does not exist!")
        }
        val len = modelAsset.length()
        if (len < 500L) {
            val content = modelAsset.readText()
            if (content.contains("git-lfs")) {
                println("[verifyBiRefNetModel] Detected Git LFS pointer ($len bytes). Fetching canonical binary weights for build...")
                var currentUrl = "https://huggingface.co/BRPOD123/birefnet-lite-512/resolve/main/onnx/model_fp16.onnx"
                var redirectCount = 0
                while (redirectCount < 6) {
                    val url = URI(currentUrl).toURL()
                    val connection = url.openConnection() as HttpURLConnection
                    connection.instanceFollowRedirects = true
                    connection.connectTimeout = 30000
                    connection.readTimeout = 60000
                    val status = connection.responseCode
                    if (status in 300..399) {
                        val newUrl = connection.getHeaderField("Location")
                        if (newUrl != null) {
                            currentUrl = newUrl
                            redirectCount++
                            continue
                        }
                    }
                    val inStream: InputStream = connection.inputStream
                    inStream.use { input ->
                        val outStream = FileOutputStream(modelAsset)
                        outStream.use { output ->
                            input.copyTo(output)
                        }
                    }
                    break
                }
            }
        }
        val finalLen = modelAsset.length()
        if (finalLen != expectedSize) {
            throw GradleException("ERROR: app/src/main/assets/birefnet_lite_fp16.onnx size ($finalLen bytes) does not match expected size $expectedSize bytes!")
        }
        val digest = MessageDigest.getInstance("SHA-256")
        modelAsset.inputStream().use { stream ->
            val buf = ByteArray(65536)
            var read: Int
            while (stream.read(buf).also { read = it } != -1) {
                digest.update(buf, 0, read)
            }
        }
        val bytes = digest.digest()
        val hash = StringBuilder()
        for (b in bytes) {
            hash.append(String.format("%02x", b))
        }
        val hashStr = hash.toString()
        if (!hashStr.equals(expectedSha256, ignoreCase = true)) {
            throw GradleException("ERROR: SHA-256 mismatch for birefnet_lite_fp16.onnx! Actual: $hashStr, Expected: $expectedSha256")
        }
        println("[verifyBiRefNetModel] SUCCESS: birefnet_lite_fp16.onnx verified ($finalLen bytes, SHA-256: $hashStr)")
    }
}

tasks.named("preBuild").configure {
    dependsOn(verifyBiRefNetModel)
}
tasks.matching { it.name.startsWith("merge") && it.name.endsWith("Assets") }.configureEach {
    dependsOn(verifyBiRefNetModel)
}


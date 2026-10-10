import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.net.URI
import java.net.HttpURLConnection
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
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
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.camera.camera2)
  implementation(libs.androidx.camera.core)
  implementation(libs.androidx.camera.lifecycle)
  implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.documentfile)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  implementation(libs.firebase.ai)
  // Uncomment to use Firestore:
  // implementation(libs.firebase.firestore)

  // Uncomment ALL FOUR of the following dependencies together to use Firebase Auth and Google
  // Sign-In via Credential Manager:
  // implementation(libs.firebase.auth)
  // implementation(libs.androidx.credentials)
  // implementation(libs.androidx.credentials.play.services)
  // implementation(libs.googleid)
  implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.firebase.appcheck.debug)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.bouncycastle.bcprov)
  implementation(libs.bouncycastle.bcpkix)
  implementation(libs.mlkit.text.recognition)
  implementation(libs.pdfbox.android) {
    exclude(group = "org.bouncycastle")
  }
  implementation("com.microsoft.onnxruntime:onnxruntime-android:1.18.0")
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  implementation("com.google.zxing:core:3.5.3")
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation("com.microsoft.onnxruntime:onnxruntime:1.18.0")
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}


dependencies {
    implementation("androidx.print:print:1.0.0")
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


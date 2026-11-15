// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

// Opt in when the checkout is in a synced folder (for example macOS Documents).
// Sync conflict copies in generated resources and ndk-build dependency files break incremental
// builds and even native clean. Keep every module's disposable output outside that folder.
val externalBuildRoot = providers.gradleProperty("diplay.buildRoot").orNull
if (externalBuildRoot != null) {
    require(java.io.File(externalBuildRoot).isAbsolute) { "diplay.buildRoot must be an absolute path" }
    val outputRoot = file(externalBuildRoot).canonicalFile
    allprojects {
        val modulePath = if (path == ":") "root" else path.removePrefix(":").replace(':', '/')
        layout.buildDirectory.set(outputRoot.resolve(modulePath))
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}

val publishableModules = setOf("core", "data-store", "library", "library-no-op")

subprojects {
    if (name in publishableModules) {
        group = "com.github.nakamuuu.peekt"
        version = providers.gradleProperty("peekt.versionName").get()
    }
}
